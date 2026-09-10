#!/bin/bash
# 2 つの Maven 発行物ツリーの同一性の検査。
#
# 使い方:
#   scripts/release/compare-maven-artifacts.sh <発行物 A の jp/> <発行物 B の jp/>
#   scripts/release/compare-maven-artifacts.sh --selftest
#
# 同じ commit・同じ JDK・同じランナー OS で 2 回発行した結果が、署名の有無を除いて同じ内容に
# なることを確かめる。Android の配布物は消費者検証に渡した未署名の発行物と、公開時に署名鍵つきで
# 作り直した発行物が別のファイルになるため、公開前にこの検査で「検証したものと出すものが同じ」
# ことを担保する。
#
# 比較の規則:
#
#   pom / Gradle module metadata   byte 単位で比較する
#   アーカイブ (aar / jar)          エントリ名の一覧と、各エントリの内容を比較する
#                                  (zip に記録される更新時刻・圧縮方法の差は無視する)
#   署名 (`.asc`) と checksum       比較しない (署名の有無がそもそもの差なので)
#   maven-metadata*.xml            比較しない
#
# 比較の対象は成果物 (pom / Gradle module metadata / aar / sources jar / javadoc jar) に限る。
# `maven-metadata-local.xml` はローカルリポジトリへの発行の副産物であり、発行時刻
# (`<lastUpdated>`) とそのマシンで発行済みの version 一覧を持つ。成果物の内容とは無関係に
# 実行ごとに変わるため、比較に含めると必ず差異になる。
#
# 片方にしか無いファイルも差異として扱う。差異が 1 件でもあれば、該当するファイルを列挙して
# 失敗する。
#
# 依存するのは bash / find / sort / cmp / unzip / シェルに付属するハッシュコマンドだけで、
# ネットワークへは出ない。自己テストは比較用のツリーを組み立てるために python3 も使う
# (zip を作るのに使うだけで、比較そのものは本番と同じ経路を通る)。

set -euo pipefail

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <発行物 A の jp/> <発行物 B の jp/>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# 一時作業ディレクトリ。最初に必要になったときだけ作り、スクリプトの終了時に片付ける。
# 比較のたびに作って関数の戻りで消す形にはしない — RETURN の trap は設定した関数だけでなく
# 呼び出し元の戻りでも走り、片付け済みの変数を参照して壊れる。
KSR_WORK=""
ksr_ensure_work() {
    if [ -z "${KSR_WORK}" ]; then
        KSR_WORK="$(mktemp -d)"
        trap 'rm -rf "${KSR_WORK}"' EXIT
    fi
}

# 内容比較に使うハッシュコマンド。macOS は shasum、Linux は sha256sum を持つ。
if command -v shasum > /dev/null 2>&1; then
    readonly HASH_TOOL="shasum"
elif command -v sha256sum > /dev/null 2>&1; then
    readonly HASH_TOOL="sha256sum"
else
    fail "shasum / sha256sum のいずれも見つかりません (内容比較に必要)"
fi

# 比較対象のファイルを、ルートからの相対パスとして改行区切りで並べる。
# 署名・checksum・リポジトリのメタデータは比較対象から外す。
list_files() {
    local root="$1"
    ( cd "${root}" && find . -type f \
        ! -name '*.asc' ! -name '*.md5' ! -name '*.sha1' \
        ! -name '*.sha256' ! -name '*.sha512' \
        ! -name 'maven-metadata*.xml' \
        | sed 's|^\./||' | LC_ALL=C sort )
}

# アーカイブの内容を「<エントリ名> <内容のハッシュ>」の行として並べる。
# ディレクトリエントリ (名前が `/` で終わる) は内容を持たないので名前だけを出す。
archive_contents() {
    local archive="$1"
    local entry hash
    while IFS= read -r entry; do
        [ -n "${entry}" ] || continue
        case "${entry}" in
            */)
                echo "${entry} -"
                ;;
            *)
                hash="$(unzip -p "${archive}" "${entry}" | "${HASH_TOOL}" | awk '{print $1}')"
                echo "${entry} ${hash}"
                ;;
        esac
    done < <(unzip -Z1 "${archive}" | LC_ALL=C sort)
}

cmd_compare() {
    local left_root="$1" right_root="$2"

    [ -d "${left_root}" ]  || fail "発行物のディレクトリがありません: ${left_root}"
    [ -d "${right_root}" ] || fail "発行物のディレクトリがありません: ${right_root}"
    command -v unzip > /dev/null 2>&1 || fail "unzip が必要です (アーカイブの内容比較に使います)"

    local differences=0

    report_difference() {
        echo "::error::$1" >&2
        differences=$((differences + 1))
    }

    ksr_ensure_work
    local work="${KSR_WORK}/compare"
    mkdir -p "${work}"

    list_files "${left_root}"  > "${work}/left-files"
    list_files "${right_root}" > "${work}/right-files"

    if [ ! -s "${work}/left-files" ]; then
        fail "比較する成果物が 1 件も見つかりません: ${left_root}"
    fi

    local relative left right
    while IFS= read -r relative; do
        [ -n "${relative}" ] || continue
        report_difference "片方にしかありません (A のみ): ${relative}"
    done < <(LC_ALL=C comm -23 "${work}/left-files" "${work}/right-files")

    while IFS= read -r relative; do
        [ -n "${relative}" ] || continue
        report_difference "片方にしかありません (B のみ): ${relative}"
    done < <(LC_ALL=C comm -13 "${work}/left-files" "${work}/right-files")

    local compared=0
    while IFS= read -r relative; do
        [ -n "${relative}" ] || continue
        left="${left_root}/${relative}"
        right="${right_root}/${relative}"
        compared=$((compared + 1))

        case "${relative}" in
            *.aar|*.jar|*.zip)
                archive_contents "${left}"  > "${work}/left-entries"
                archive_contents "${right}" > "${work}/right-entries"
                if ! cmp -s "${work}/left-entries" "${work}/right-entries"; then
                    report_difference "アーカイブの内容が異なります: ${relative}"
                    # どのエントリが違うのかまで出す (エントリ名だけの差も内容の差もこの diff に出る)。
                    diff "${work}/left-entries" "${work}/right-entries" >&2 || true
                fi
                ;;
            *)
                if ! cmp -s "${left}" "${right}"; then
                    report_difference "内容が異なります: ${relative}"
                fi
                ;;
        esac
    done < <(LC_ALL=C comm -12 "${work}/left-files" "${work}/right-files")

    if [ "${differences}" -gt 0 ]; then
        fail "発行物に ${differences} 件の差異があります (署名を除く比較)"
    fi

    echo "発行物 ${compared} 件が一致します (署名を除く比較): ${left_root} / ${right_root}"
}

# --- 自己テスト ----------------------------------------------------------------------
#
# 発行物を模した 2 つのツリーを組み立てて比較する。一致する場合だけでなく、この検査が
# 止めたい差異 (成果物の内容・アーカイブの中身・片側にしか無いファイル) で実際に失敗すること、
# 逆に無視すべき差異 (署名・checksum・リポジトリのメタデータ・zip の更新時刻) では失敗しない
# ことを確かめる。「一致した」だけを見ても、検査が何も見ていない状態と区別が付かないため。

selftest() {
    ksr_ensure_work
    local work="${KSR_WORK}/selftest"
    mkdir -p "${work}"

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

    # zip を 1 つ作る。引数は <出力先> <エントリの更新時刻の年> <エントリ名=内容 ...>。
    # 年を引数に取るのは、更新時刻の差が比較へ漏れないことを確かめるため。
    make_archive() {
        local archive="$1" year="$2"; shift 2
        KSR_ARCHIVE="${archive}" KSR_YEAR="${year}" KSR_ENTRIES="$*" python3 -c '
import os
import zipfile

archive = os.environ["KSR_ARCHIVE"]
year = int(os.environ["KSR_YEAR"])
entries = os.environ["KSR_ENTRIES"].split()
with zipfile.ZipFile(archive, "w") as zf:
    for entry in entries:
        name, _, body = entry.partition("=")
        info = zipfile.ZipInfo(name, date_time=(year, 1, 1, 0, 0, 0))
        zf.writestr(info, body)
'
    }

    # 発行物 1 件ぶんのツリーを組み立てる。<ルート> <aar の中身> [pom の中身] [更新時刻の年]
    stage() {
        local root="$1" aar_body="$2" pom_body="${3:-pom}" year="${4:-2020}"
        local dir="${root}/kamusoft/ksdialogs-core/0.0.0-selftest"
        mkdir -p "${dir}"
        echo "${pom_body}" > "${dir}/ksdialogs-core-0.0.0-selftest.pom"
        echo "module" > "${dir}/ksdialogs-core-0.0.0-selftest.module"
        make_archive "${dir}/ksdialogs-core-0.0.0-selftest.aar" "${year}" \
            "classes.jar=${aar_body}" "AndroidManifest.xml=manifest"
    }

    local left="${work}/left/jp" right="${work}/right/jp"

    echo "[同じ内容]"
    stage "${left}" body
    stage "${right}" body
    local output
    output="$(cmd_compare "${left}" "${right}")" || output="失敗"
    check "$(if [ "${output#*3 件が一致}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "同じ内容の 3 成果物が一致する" "${output}"

    echo "[無視すべき差異]"
    # 署名・checksum・リポジトリのメタデータは片側にしか無くても差異にしない。
    local right_dir="${right}/kamusoft/ksdialogs-core/0.0.0-selftest"
    echo "signature" > "${right_dir}/ksdialogs-core-0.0.0-selftest.aar.asc"
    echo "signature" > "${right_dir}/ksdialogs-core-0.0.0-selftest.pom.asc"
    echo "checksum"  > "${right_dir}/ksdialogs-core-0.0.0-selftest.pom.sha1"
    echo "metadata"  > "${right_dir}/maven-metadata-local.xml"
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" \
        "署名 / checksum / maven-metadata の有無は差異にしない"

    # 同じ中身を別の更新時刻で固めたアーカイブ。zip に記録される時刻は比較に効かない。
    stage "${left}" body pom 2020
    stage "${right}" body pom 2031
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" \
        "アーカイブの更新時刻の差は差異にしない"

    echo "[止めたい差異]"
    stage "${left}" body
    stage "${right}" body different-pom
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "pom の内容が違えば失敗する"
    output="$(cmd_compare "${left}" "${right}" 2>&1)" || true
    check "$(if [ "${output#*.pom}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "差異のあるファイル名を出力する" "${output}"

    stage "${left}" body
    stage "${right}" different-body
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "アーカイブの中身が違えば失敗する"
    output="$(cmd_compare "${left}" "${right}" 2>&1)" || true
    check "$(if [ "${output#*.aar}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "中身の違うアーカイブ名を出力する" "${output}"

    stage "${left}" body
    stage "${right}" body
    make_archive "${right}/kamusoft/ksdialogs-core/0.0.0-selftest/ksdialogs-core-0.0.0-selftest.aar" 2020 \
        "classes.jar=body" "AndroidManifest.xml=manifest" "extra.txt=extra"
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "アーカイブのエントリが増えれば失敗する"

    stage "${left}" body
    stage "${right}" body
    rm -f "${right}/kamusoft/ksdialogs-core/0.0.0-selftest/ksdialogs-core-0.0.0-selftest.module"
    check "$(if ( cmd_compare "${left}" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "片方にしか無い成果物があれば失敗する"
    output="$(cmd_compare "${left}" "${right}" 2>&1)" || true
    check "$(if [ "${output#*A のみ}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "どちら側にしか無いかを出力する" "${output}"

    echo "[走査先の指定誤り]"
    check "$(if ( cmd_compare "${work}/absent" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "ディレクトリが無ければ失敗する"
    mkdir -p "${work}/empty"
    check "$(if ( cmd_compare "${work}/empty" "${right}" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "成果物が 1 件も無いツリーは緑で通さない"

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
    cmd_compare "$1" "$2"
}

main "$@"
