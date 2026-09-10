#!/bin/bash
# nuget.org に当該 version の package が存在するかの判定。
#
# 使い方:
#   scripts/release/check-nuget-version.sh <flat container の基点 URL> <Package ID> <version>
#   scripts/release/check-nuget-version.sh --selftest
#
# publish job は、書き込みを始める前に当該 version の外部状態を見直す。その一部として
# nuget.org の flat container (`<基点>/<小文字の Package ID>/index.json`) を引き、版一覧に
# 当該 version があるかを見る。
#
# この照会は「公開済みの binary と、これから tag を打つ source の対応」を守る安全ゲートで
# あり、判定できない応答を「未公開」と読み替えてはいけない。通信が落ちている間に新規の
# dispatch が publish へ進み、`--skip-duplicate` で既存 package を受理したうえで別の commit へ
# tag を打つ経路が開くため、判定不能は失敗として扱う。
#
# 終了コード:
#
#   0  当該 version が存在する (index が 200 で、版一覧に完全一致がある)
#   1  当該 version は存在しない (index が 404、または版一覧に完全一致が無い)
#   2  判定できない (通信失敗・200 と 404 以外の status・JSON として読めない index)
#
# 版の比較は完全一致で行う。flat container の index は version を小文字へ正規化するため、
# 入力の version も小文字へ揃えてから比べる。部分一致で見ると `1.0.1` の照会が `1.0.10` の
# 存在で「公開済み」に倒れる。

set -euo pipefail

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <flat container の基点 URL> <Package ID> <version>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

# 版一覧の JSON と version を突き合わせる。
#
#   標準入力: index.json の本文
#   引数:     小文字へ揃えた version
#   終了:     0 = 一致あり / 1 = 一致なし / 2 = JSON として読めない
match_version() {
    local wanted="$1"
    KS_WANTED_VERSION="${wanted}" python3 -c '
import json
import os
import sys

wanted = os.environ["KS_WANTED_VERSION"]
try:
    document = json.load(sys.stdin)
except (ValueError, UnicodeDecodeError):
    sys.exit(2)
if not isinstance(document, dict):
    sys.exit(2)
versions = document.get("versions")
if not isinstance(versions, list):
    sys.exit(2)
for version in versions:
    if not isinstance(version, str):
        continue
    if version.strip().lower() == wanted:
        sys.exit(0)
sys.exit(1)
'
}

# HTTP の応答から判定を出す。
#
#   引数: <status> <本文のファイル> <version>
#
# status が空文字 (curl が応答を得られなかった) の場合も判定不能にする。
cmd_decide() {
    local status="$1" body_file="$2" version="$3"
    local wanted
    wanted="$(printf '%s' "${version}" | tr '[:upper:]' '[:lower:]')"

    # curl が接続そのものに失敗すると status は `000` になる。応答を得られなかった
    # 印なので、空の status と同じ扱いにする。
    case "${status}" in
        000) status="" ;;
    esac

    case "${status}" in
        404)
            echo "index が無い (この Package ID の公開版は 1 つも無い)" >&2
            return 1
            ;;
        200) ;;
        *)
            echo "::error::nuget.org の応答から公開状態を判定できない (HTTP status: ${status:-なし})" >&2
            return 2
            ;;
    esac

    if [ ! -f "${body_file}" ]; then
        echo "::error::nuget.org の応答本文を読めない" >&2
        return 2
    fi

    local matched=0
    match_version "${wanted}" < "${body_file}" || matched=$?
    case "${matched}" in
        0)
            echo "版一覧に ${version} がある" >&2
            return 0
            ;;
        1)
            echo "版一覧に ${version} は無い" >&2
            return 1
            ;;
        *)
            echo "::error::nuget.org の index を JSON として読めない" >&2
            return 2
            ;;
    esac
}

# flat container の index を引いて判定する。
cmd_check() {
    local base="$1" package_id="$2" version="$3"
    local path body status
    path="$(printf '%s' "${package_id}" | tr '[:upper:]' '[:lower:]')"
    body="$(mktemp)"
    # 応答が得られなければ status は空になる。--fail は使わない (4xx / 5xx でも
    # status を受け取って種別で分けるため)。
    status="$(curl --silent --show-error --location \
        --connect-timeout 20 --max-time 60 --retry 2 --retry-connrefused \
        --output "${body}" --write-out '%{http_code}' \
        "${base}/${path}/index.json" 2>/dev/null || true)"
    local decided=0
    cmd_decide "${status}" "${body}" "${version}" || decided=$?
    rm -f "${body}"
    return "${decided}"
}

# --- 自己テスト ----------------------------------------------------------------------
#
# 応答の種別ごとに判定が分かれることを確かめる。ネットワークは要らない。

selftest() {
    local failures=0 work
    work="$(mktemp -d)"

    check() {
        local ok="$1" name="$2" detail="${3:-}"
        if [ "${ok}" = "0" ]; then
            echo "  OK   ${name}"
        else
            echo "  NG   ${name}${detail:+ (${detail})}"
            failures=$((failures + 1))
        fi
    }

    # 本文を書いた一時ファイルを cmd_decide に渡し、終了コードを確かめる。
    expect() {
        local name="$1" status="$2" body="$3" version="$4" expected="$5"
        local file="${work}/body.json" code=0
        printf '%s' "${body}" > "${file}"
        cmd_decide "${status}" "${file}" "${version}" > /dev/null 2>&1 || code=$?
        check "$([ "${code}" = "${expected}" ] && echo 0 || echo 1)" \
            "${name} -> exit ${expected}" "exit ${code}"
    }

    local index='{"versions":["0.1.0-beta.1","0.1.0","1.0.10"]}'

    echo "[公開済み]"
    expect "200 / 版一覧に一致がある" 200 "${index}" "0.1.0" 0
    expect "200 / 大文字の入力も小文字の一覧に一致する" 200 '{"versions":["1.0.0-beta.1"]}' "1.0.0-BETA.1" 0

    echo "[未公開]"
    expect "200 / 版一覧に無い" 200 "${index}" "0.2.0" 1
    expect "200 / 版一覧が空" 200 '{"versions":[]}' "0.1.0" 1
    expect "404 / index が無い" 404 "" "0.1.0" 1
    # 部分一致で見ていたら `1.0.1` が `1.0.10` に当たって 0 になる
    expect "200 / 前方一致する別の版があっても未公開" 200 "${index}" "1.0.1" 1

    echo "[判定不能 — 未公開に倒れないこと]"
    # ここが本題。応答を得られない・壊れた応答は「未公開 (exit 1)」ではなく失敗にする
    expect "通信失敗 (status なし)" "" "" "0.1.0" 2
    expect "接続できない (curl の 000)" 000 "" "0.1.0" 2
    expect "500" 500 "" "0.1.0" 2
    expect "503" 503 "" "0.1.0" 2
    expect "429" 429 "" "0.1.0" 2
    expect "200 / JSON として壊れている" 200 '{"versions":[' "0.1.0" 2
    expect "200 / HTML が返ってきた" 200 '<html>error</html>' "0.1.0" 2
    expect "200 / versions が配列ではない" 200 '{"versions":"0.1.0"}' "0.1.0" 2
    expect "200 / versions が無い" 200 '{"data":[]}' "0.1.0" 2

    echo "[本文のファイルが無い]"
    local code=0
    cmd_decide 200 "${work}/missing.json" "0.1.0" > /dev/null 2>&1 || code=$?
    check "$([ "${code}" = "2" ] && echo 0 || echo 1)" "200 だが本文が無い -> exit 2" "exit ${code}"

    rm -rf "${work}"

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
    if [ $# -ne 3 ]; then
        usage
        exit 2
    fi
    cmd_check "$1" "$2" "$3"
}

main "$@"
