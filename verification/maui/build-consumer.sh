#!/bin/bash
# .NET MAUI 消費者のビルドと依存検査。
#
# net10.0-android と net10.0-ios を Release でビルドする。iOS は Simulator RID を明示して
# 署名情報を要求しない形にする。restore は実行ごとに空のパッケージ展開先を使い、
# 解決版・取得元・platform TFM のアセットを check-dependencies.py で検査する。
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <フォルダフィード>] [--work <dir>]
#
# --reference を与えると、そのフォルダフィードをそのまま使いフィード準備を行わない。
# 与えない dry-run では prepare-feed.sh を呼んで pack する。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="maui"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

readonly PROJECT="VerificationApp.csproj"
readonly NUGET_ORG_SOURCE="https://api.nuget.org/v3/index.json"

ksv_parse_args "$@"

work="$(ksv_prepare_work "${KSV_WORK}" "maui")"

reference="${KSV_REFERENCE}"
if [ "${KSV_MODE}" = "dry-run" ] && [ -z "${reference}" ]; then
    # フィード準備の出力はコマンド置換で最終行 (参照先) だけを取るため、そのままでは pack の
    # ログが残らず失敗理由が見えない。標準エラーにも流して CI のログに残す。
    reference="$("${SCRIPT_DIR}/prepare-feed.sh" \
        --mode "${KSV_MODE}" --version "${KSV_VERSION}" --work "${work}" | tee /dev/stderr | tail -n 1)"
fi

if [ "${KSV_MODE}" = "dry-run" ]; then
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    reference="$(cd "${reference}" && pwd -P)"
    config="${SCRIPT_DIR}/nuget.dry-run.config"
    expected_source="${reference}"
    export KSV_LOCAL_FEED="${reference}"
else
    config="${SCRIPT_DIR}/nuget.smoke.config"
    expected_source="${NUGET_ORG_SOURCE}"
fi

# packageSourceMapping は global packages folder に展開済みのパッケージには働かない。
# 実行ごとに空の展開先を使い、ユーザー環境や CI のキャッシュを参照しない。
packages="$(ksv_reset_dir "${work}" packages)"

# 手元の作業ツリーを汚さないよう、中間出力も作業ディレクトリへ逃がす。
obj="${work}/obj/"
bin="${work}/bin/"

# iOS Simulator の RID はホストのアーキテクチャに合わせる。実機向けの RID を選ぶと
# 署名情報を要求され、検証範囲の外に出る。
case "$(uname -m)" in
    arm64|aarch64) ios_rid="iossimulator-arm64" ;;
    *) ios_rid="iossimulator-x64" ;;
esac

common_args=(
    # 構成は restore と build で同じ形の指定にする。dotnet restore は -c を受け付けない。
    "-p:Configuration=Release"
    "-p:KsDialogsVersion=${KSV_VERSION}"
    "-p:RestoreConfigFile=${config}"
    "-p:RestorePackagesPath=${packages}"
    "-p:BaseIntermediateOutputPath=${obj}"
    "-p:BaseOutputPath=${bin}"
)

# Android のビルドが使う JVM を明示できるようにする (CI の JDK 選択に追随する)。
if [ -n "${JAVA_HOME:-}" ]; then
    common_args+=("-p:JavaSdkDirectory=${JAVA_HOME}")
fi

# 中間出力と成果物を作業ディレクトリへ逃がすと、消費者プロジェクト直下に残った過去のビルド
# 出力は「自分の中間出力ディレクトリ」ではなくなり、既定の除外から外れる。生成済みの
# AssemblyInfo がコンパイル対象に混ざって属性の重複エラーになるため、ビルドの前に取り除く。
# 対象は消費者プロジェクト直下の生成物 2 つに限る (追跡しているファイルは含まない)。
for stale in obj bin; do
    stale_path="${SCRIPT_DIR}/${stale}"
    if [ -d "${stale_path}" ] && [ ! -L "${stale_path}" ]; then
        rm -rf "${stale_path}"
    fi
done

echo "==== restore ===="
(cd "${SCRIPT_DIR}" && dotnet restore "${PROJECT}" "${common_args[@]}")

python3 "${SCRIPT_DIR}/check-dependencies.py" \
    --assets "${obj}project.assets.json" \
    --packages "${packages}" \
    --expected-version "${KSV_VERSION}" \
    --expected-source "${expected_source}" \
    | ksv_evidence "解決版と取得元"

echo "==== Release ビルド (net10.0-android) ===="
android_log="${work}/build-android.log"
(cd "${SCRIPT_DIR}" && dotnet build "${PROJECT}" -f net10.0-android --no-restore "${common_args[@]}") \
    2>&1 | tee "${android_log}"

# native ライブラリの重複 (XA4301) はビルド警告であり、検証の失敗にはしない。
# 数だけを証跡に残して、配布物の推移依存が重複を持ち込んでいないかを追えるようにする。
android_duplicates="${work}/xa4301.txt"
grep -F "XA4301" "${android_log}" | sort -u > "${android_duplicates}" || true
if [ -s "${android_duplicates}" ]; then
    ksv_evidence "XA4301 (native ライブラリの重複)" < "${android_duplicates}"
else
    echo "検出なし" | ksv_evidence "XA4301 (native ライブラリの重複)"
fi

echo "==== Release ビルド (net10.0-ios, ${ios_rid}) ===="
(cd "${SCRIPT_DIR}" && dotnet build "${PROJECT}" -f net10.0-ios --no-restore \
    "-p:RuntimeIdentifier=${ios_rid}" "${common_args[@]}")

echo "${expected_source}"
