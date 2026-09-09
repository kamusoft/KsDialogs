#!/bin/bash
# Android 消費者のビルドと依存検査。
#
# 消費者アプリ 2 つ (宣言的 UI 系の :app と View 系本体だけの :app-core) の release variant を
# ビルドし、release runtime classpath の依存ツリーから次を検査する。
#
#   1. :app で jp.kamusoft:ksdialogs-core が jp.kamusoft:ksdialogs と同じ version で
#      推移的に解決されること
#   2. :app-core の classpath に androidx.compose の座標が 1 つも無いこと
#      (View 系本体だけの利用者に宣言的 UI が届かないこと。cross/ADR-0019)
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <local maven repository>] [--work <dir>]
#
# --reference を与えると、その参照先をそのまま使いフィード準備を行わない。
# 与えない dry-run では prepare-feed.sh を呼んでローカル Maven リポジトリへ発行する。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="android"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"
# shellcheck source=../lib/android-sdk.sh
. "${REPO_ROOT}/verification/lib/android-sdk.sh"

readonly CENTRAL_URL="https://repo.maven.apache.org/maven2"

# 依存ツリーから 1 つの座標の解決版を集める。版が競合したときの Gradle の表記
# (「宣言 -> 解決」) では解決側を取る。
ksv_resolved_versions() {
    local tree="$1"
    local artifact="$2"

    awk -v artifact="${artifact}" '
        {
            pattern = "jp[.]kamusoft:" artifact ":[^ ]+"
            if (match($0, pattern)) {
                coordinate = substr($0, RSTART, RLENGTH)
                split(coordinate, parts, ":")
                version = parts[3]
                arrow = index($0, "-> ")
                if (arrow > 0) {
                    split(substr($0, arrow + 3), tokens, " ")
                    version = tokens[1]
                }
                print version
            }
        }
    ' "${tree}" | sort -u
}

# 依存ツリーの取得。Gradle の失敗と「行が見つからない」を区別する。パイプで grep へ繋ぐと
# Gradle が落ちたときも空文字になり、原因と食い違うメッセージで終わる。
ksv_dependency_tree() {
    local module="$1"
    local output="$2"

    if ! (cd "${SCRIPT_DIR}" && ./gradlew "${gradle_args[@]}" -q \
        "${module}:dependencies" --configuration releaseRuntimeClasspath) > "${output}"; then
        cat "${output}" >&2
        ksv_fail "依存ツリーの取得に失敗しました (${module}:dependencies)"
    fi
}

ksv_parse_args "$@"
ksv_ensure_android_home

reference="${KSV_REFERENCE}"
if [ "${KSV_MODE}" = "dry-run" ] && [ -z "${reference}" ]; then
    work="$(ksv_prepare_work "${KSV_WORK}" "android")"
    # フィード準備の出力はコマンド置換で最終行 (参照先) だけを取るため、そのままでは準備の
    # ログが残らず失敗理由が見えない。標準エラーにも流して CI のログに残す。
    reference="$("${SCRIPT_DIR}/prepare-feed.sh" \
        --mode "${KSV_MODE}" --version "${KSV_VERSION}" --work "${work}" | tee /dev/stderr | tail -n 1)"
fi

gradle_args=(
    "--console=plain"
    "-Pksdialogs.mode=${KSV_MODE}"
    "-Pksdialogs.version=${KSV_VERSION}"
)
if [ "${KSV_MODE}" = "dry-run" ]; then
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    reference="$(cd "${reference}" && pwd -P)"
    gradle_args+=("-Pksdialogs.reference=${reference}")
fi

echo "==== Release ビルド (:app / :app-core) ===="
(cd "${SCRIPT_DIR}" && ./gradlew "${gradle_args[@]}" :app:assembleRelease :app-core:assembleRelease)

app_tree="$(mktemp)"
core_tree="$(mktemp)"
trap 'rm -f "${app_tree}" "${core_tree}"' EXIT

ksv_dependency_tree ":app" "${app_tree}"
ksv_dependency_tree ":app-core" "${core_tree}"

failures=""
failure_count=0

ksv_add_failure() {
    failures="${failures}${1}
"
    failure_count=$((failure_count + 1))
}

facade_versions="$(ksv_resolved_versions "${app_tree}" "ksdialogs")"
core_versions="$(ksv_resolved_versions "${app_tree}" "ksdialogs-core")"

if [ "${facade_versions}" != "${KSV_VERSION}" ]; then
    ksv_add_failure ":app の ksdialogs の解決版が要求と異なる: 要求 ${KSV_VERSION} / 解決 ${facade_versions:-(未解決)}"
fi
if [ "${core_versions}" != "${KSV_VERSION}" ]; then
    ksv_add_failure ":app の推移の ksdialogs-core の解決版が ksdialogs と一致しない: ksdialogs ${KSV_VERSION} / ksdialogs-core ${core_versions:-(未解決)}"
fi

core_only_versions="$(ksv_resolved_versions "${core_tree}" "ksdialogs-core")"
if [ "${core_only_versions}" != "${KSV_VERSION}" ]; then
    ksv_add_failure ":app-core の ksdialogs-core の解決版が要求と異なる: 要求 ${KSV_VERSION} / 解決 ${core_only_versions:-(未解決)}"
fi

compose_lines="$(grep "androidx.compose" "${core_tree}" || true)"
if [ -n "${compose_lines}" ]; then
    ksv_add_failure ":app-core の release runtime classpath に androidx.compose の座標がある"
fi

{
    echo "参照先: $([ "${KSV_MODE}" = "dry-run" ] && echo "${reference}" || echo "${CENTRAL_URL}")"
    echo
    echo ":app (releaseRuntimeClasspath の jp.kamusoft 行)"
    grep "jp.kamusoft" "${app_tree}" || echo "  (行なし)"
    echo
    echo ":app-core (releaseRuntimeClasspath の jp.kamusoft 行)"
    grep "jp.kamusoft" "${core_tree}" || echo "  (行なし)"
    echo
    echo ":app-core の androidx.compose 座標: $(printf '%s' "${compose_lines}" | grep -c "androidx.compose" || true) 件"
} | ksv_evidence "解決版と取得元"

if [ "${failure_count}" -gt 0 ]; then
    printf '%s' "${failures}" | while IFS= read -r failure; do
        echo "エラー: ${failure}" >&2
    done
    ksv_fail "Android 消費者の依存検査が ${failure_count} 件失敗しました"
fi

echo "ksdialogs と推移の ksdialogs-core が ${KSV_VERSION} で一致し、-core 単独の消費者に androidx.compose は無い"

if [ "${KSV_MODE}" = "dry-run" ]; then
    echo "${reference}"
else
    echo "${CENTRAL_URL}"
fi
