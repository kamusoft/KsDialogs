#!/bin/bash
# Maven 発行物に GPG 署名 (`.asc`) が揃っているかの検査。
#
# 使い方:
#   scripts/release/check-signatures.sh <枠名> <発行物の jp/ ディレクトリ>
#   scripts/release/check-signatures.sh --selftest
#
# 第 2 引数には mavenLocal へ発行した結果の `jp/` (groupId の先頭セグメント) を渡す。配下を
# 再帰的に走査し、成果物の 1 件ごとに `<成果物>.asc` が対で存在することを確かめる。1 件でも
# 欠けていれば、欠けているファイルを列挙して失敗する。
#
# Maven Central への upload は署名の無い成果物を受け付けない。署名鍵の secret が空でも
# Gradle の発行タスク自体は成功してしまうため、upload の前にこの検査を挟んで、鍵が届いて
# いないことを不可逆操作の前に露見させる。
#
# 枠名は Maven Central への 2 つの deployment に対応し、そこに揃っているべき publication を
# 決める。署名の有無だけを見ると、発行そのものが一部のターゲットで落ちて成果物がまるごと
# 無い状態も「欠けた署名 0 件」で通ってしまうため、publication の実在も同じ検査で見る。
#
#   android  ksdialogs-core / ksdialogs
#   kmp      ksdialogs-kmp (root) / ksdialogs-kmp-android /
#            ksdialogs-kmp-iosarm64 / ksdialogs-kmp-iossimulatorarm64 / ksdialogs-kmp-iosx64
#
# 署名の対象とする拡張子:
#
#   .aar     Android ライブラリ本体
#   .pom     Maven の POM
#   .jar     sources jar / javadoc jar / KMP の metadata jar
#   .module  Gradle module metadata
#   .klib    Kotlin/Native のターゲット別ライブラリ (cinterop klib を含む)
#   .json    KMP の SwiftPM 連携メタデータ
#
# checksum ファイル (`.md5` / `.sha1` / `.sha256` / `.sha512`) と `maven-metadata*.xml` は
# 成果物ではないので対象にしない。成果物が 1 件も見つからない場合は、走査先の指定を誤った
# ものとして失敗する (空ディレクトリを緑で通さない)。

set -euo pipefail

# 枠ごとに揃っているべき publication の artifactId。
readonly ANDROID_ARTIFACT_IDS=(
    "ksdialogs-core"
    "ksdialogs"
)
readonly KMP_ARTIFACT_IDS=(
    "ksdialogs-kmp"
    "ksdialogs-kmp-android"
    "ksdialogs-kmp-iosarm64"
    "ksdialogs-kmp-iossimulatorarm64"
    "ksdialogs-kmp-iosx64"
)

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <枠名: android|kmp> <発行物の jp/ ディレクトリ>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# 枠名から必須 publication の一覧を標準出力へ 1 行 1 件で出す。
required_artifact_ids() {
    local slot="$1"
    case "${slot}" in
        android) printf '%s\n' "${ANDROID_ARTIFACT_IDS[@]}" ;;
        kmp)     printf '%s\n' "${KMP_ARTIFACT_IDS[@]}" ;;
        *)       fail "知らない枠名: ${slot} (android か kmp)" ;;
    esac
}

# 発行物のツリーに publication が揃っているかを見る。
# 「その artifactId のディレクトリに pom が 1 件以上ある」ことを実在の判定とする。
#
# 第 3 引数には検査する artifactId を改行区切りで渡す。枠名からの解決を呼び出し側で
# 済ませておくのは、プロセス置換の中で枠名の誤りを失敗させると、その失敗が呼び出し側へ
# 伝わらず「対象 0 件」として素通りするため。
check_publications() {
    local slot="$1" root="$2" artifact_ids="$3"
    local artifact_id missing=()

    while IFS= read -r artifact_id; do
        [ -n "${artifact_id}" ] || continue
        if [ -z "$(find "${root}" -type d -name "${artifact_id}" -exec find {} -maxdepth 2 -name '*.pom' -print -quit \; 2>/dev/null)" ]; then
            missing+=("${artifact_id}")
        fi
    done <<< "${artifact_ids}"

    if [ "${#missing[@]}" -gt 0 ]; then
        local name
        for name in "${missing[@]}"; do
            echo "::error::publication の発行物がありません: ${name}" >&2
        done
        fail "${slot} 枠の publication が ${#missing[@]} 件足りません"
    fi
}

# 成果物 1 件ごとに `.asc` が対であるかを見る。
check_signature_pairs() {
    local root="$1"
    local artifacts=()
    while IFS= read -r -d '' file; do
        artifacts+=("${file}")
    done < <(
        find "${root}" -type f \
            \( -name '*.aar' -o -name '*.pom' -o -name '*.jar' -o -name '*.module' \
               -o -name '*.klib' -o -name '*.json' \) \
            -print0 | sort -z
    )

    if [ "${#artifacts[@]}" -eq 0 ]; then
        fail "署名を検査する成果物が 1 件も見つかりません: ${root}"
    fi

    local artifact missing=()
    for artifact in "${artifacts[@]}"; do
        if [ ! -f "${artifact}.asc" ]; then
            missing+=("${artifact#"${root}"/}")
        fi
    done

    if [ "${#missing[@]}" -gt 0 ]; then
        local name
        for name in "${missing[@]}"; do
            echo "::error::署名 (.asc) がありません: ${name}" >&2
        done
        fail "成果物 ${#artifacts[@]} 件のうち ${#missing[@]} 件に署名がありません"
    fi

    echo "成果物 ${#artifacts[@]} 件すべてに署名 (.asc) があります: ${root}"
}

cmd_check() {
    local slot="$1" root="$2"

    # 枠名の解決を走査より前に済ませる。知らない枠名はここで止まる。
    # 解決はコマンド置換 (サブシェル) なので、その中の失敗は `set -e` では捕まらない
    # (呼び出しが if の条件などに入ると `set -e` 自体が効かない)。成否を明示的に見る
    local artifact_ids
    artifact_ids="$(required_artifact_ids "${slot}")" || return 1

    [ -d "${root}" ] || fail "発行物のディレクトリがありません: ${root}"

    check_publications "${slot}" "${root}" "${artifact_ids}"
    check_signature_pairs "${root}"
}

# --- 自己テスト ----------------------------------------------------------------------
#
# 一時ディレクトリに発行物を模したツリーを組み立て、揃っている場合と欠けている場合を
# 検査する。実際の発行もネットワークも要らない。

selftest() {
    SELFTEST_WORK="$(mktemp -d)"
    trap 'rm -rf "${SELFTEST_WORK}"' EXIT
    local work="${SELFTEST_WORK}"

    local failures=0

    check() {
        local ok="$1" name="$2" detail="${3:-}"
        if [ "${ok}" = "0" ]; then
            echo "  OK   ${name}"
        else
            echo "  NG   ${name}${detail:+ (${detail})}"
            failures=$((failures + 1))
        fi
    }

    # 成果物 1 件と対の署名を置く。
    put() {
        local path="$1"
        mkdir -p "$(dirname "${path}")"
        echo "artifact" > "${path}"
        echo "signature" > "${path}.asc"
    }

    # Android 枠の発行物を模したツリーを組み立てる。
    stage_android() {
        local root="$1" version="0.0.0-selftest"
        local artifact_id
        rm -rf "${root}"
        for artifact_id in "${ANDROID_ARTIFACT_IDS[@]}"; do
            local dir="${root}/kamusoft/${artifact_id}/${version}"
            put "${dir}/${artifact_id}-${version}.pom"
            put "${dir}/${artifact_id}-${version}.module"
            put "${dir}/${artifact_id}-${version}.aar"
            put "${dir}/${artifact_id}-${version}-sources.jar"
            put "${dir}/${artifact_id}-${version}-javadoc.jar"
            # 署名しないファイル。これらが対象に混ざっていないことも同時に見る。
            echo "checksum" > "${dir}/${artifact_id}-${version}.pom.sha1"
            echo "metadata" > "${dir}/maven-metadata-local.xml"
        done
    }

    # KMP 枠の発行物を模したツリーを組み立てる。root は SwiftPM 連携メタデータの JSON を、
    # iOS ターゲットは klib を持つ。
    stage_kmp() {
        local root="$1" version="0.0.0-selftest"
        local artifact_id
        rm -rf "${root}"
        for artifact_id in "${KMP_ARTIFACT_IDS[@]}"; do
            local dir="${root}/kamusoft/${artifact_id}/${version}"
            put "${dir}/${artifact_id}-${version}.pom"
            put "${dir}/${artifact_id}-${version}.module"
            put "${dir}/${artifact_id}-${version}-sources.jar"
            put "${dir}/${artifact_id}-${version}-javadoc.jar"
            case "${artifact_id}" in
                ksdialogs-kmp)
                    put "${dir}/${artifact_id}-${version}.jar"
                    put "${dir}/${artifact_id}-${version}-swiftpm-metadata.json"
                    put "${dir}/${artifact_id}-${version}-kotlin-tooling-metadata.json"
                    ;;
                ksdialogs-kmp-ios*)
                    put "${dir}/${artifact_id}-${version}.klib"
                    put "${dir}/${artifact_id}-${version}-cinterop-swiftPMImport.klib"
                    put "${dir}/${artifact_id}-${version}-metadata.jar"
                    ;;
                *)
                    put "${dir}/${artifact_id}-${version}.aar"
                    ;;
            esac
        done
    }

    echo "[署名が揃っている発行物]"
    local root="${work}/jp"
    stage_android "${root}"
    check "$(if ( cmd_check android "${root}" > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" \
        "Android 枠は 2 publication が揃えば通る"

    stage_kmp "${root}"
    check "$(if ( cmd_check kmp "${root}" > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" \
        "KMP 枠は 5 publication が揃えば通る"

    local output
    output="$(cmd_check kmp "${root}")"
    check "$(if [ "${output#*成果物 }" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "検査した成果物の件数を出力する" "${output}"

    echo "[署名の欠落]"
    stage_kmp "${root}"
    rm -f "${root}/kamusoft/ksdialogs-kmp-iosarm64/0.0.0-selftest/ksdialogs-kmp-iosarm64-0.0.0-selftest-cinterop-swiftPMImport.klib.asc"
    check "$(if ( cmd_check kmp "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "cinterop klib の .asc が 1 件欠ければ失敗する"
    output="$(cmd_check kmp "${root}" 2>&1)" || true
    check "$(if [ "${output#*cinterop-swiftPMImport.klib}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "欠けたファイル名を出力する" "${output}"

    stage_kmp "${root}"
    rm -f "${root}/kamusoft/ksdialogs-kmp/0.0.0-selftest/ksdialogs-kmp-0.0.0-selftest-swiftpm-metadata.json.asc"
    check "$(if ( cmd_check kmp "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "SwiftPM 連携メタデータ (json) の .asc が欠ければ失敗する"

    stage_android "${root}"
    rm -f "${root}/kamusoft/ksdialogs/0.0.0-selftest/ksdialogs-0.0.0-selftest.aar.asc"
    check "$(if ( cmd_check android "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "Android の aar の .asc が欠ければ失敗する"

    echo "[publication の欠落]"
    stage_kmp "${root}"
    rm -rf "${root}/kamusoft/ksdialogs-kmp-iosx64"
    check "$(if ( cmd_check kmp "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "iOS ターゲットの publication がまるごと無ければ失敗する"
    output="$(cmd_check kmp "${root}" 2>&1)" || true
    check "$(if [ "${output#*ksdialogs-kmp-iosx64}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "欠けた publication 名を出力する" "${output}"

    # Android 枠の発行物を KMP 枠として渡すと、5 件のうち 4 件が足りないことで止まる。
    stage_android "${root}"
    check "$(if ( cmd_check kmp "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "枠を取り違えた発行物は publication の不足で止まる"

    echo "[走査先の指定誤り]"
    check "$(if ( cmd_check android "${work}/absent" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "ディレクトリが無ければ失敗する"
    check "$(if ( cmd_check unknown "${root}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "知らない枠名は失敗する"

    if [ "${failures}" -eq 0 ]; then
        echo "失敗なし"
        return 0
    fi
    echo "失敗 ${failures} 件" >&2
    return 1
}

# --- 入口 ----------------------------------------------------------------------------

main() {
    if [ $# -eq 1 ] && [ "$1" = "--selftest" ]; then
        selftest
        return
    fi
    if [ $# -eq 1 ] && { [ "$1" = "-h" ] || [ "$1" = "--help" ]; }; then
        usage
        return 0
    fi
    if [ $# -ne 2 ]; then
        usage
        exit 2
    fi
    cmd_check "$1" "$2"
}

main "$@"
