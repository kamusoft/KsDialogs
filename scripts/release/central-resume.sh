#!/bin/bash
# 同じ version で再実行したときに、Maven Central の枠 (Android / KMP) をどう引き継ぐかの判定。
#
# 使い方:
#   scripts/release/central-resume.sh decide <状態>
#   scripts/release/central-resume.sh resume <枠名> <deployment ID を書いたファイル>
#   scripts/release/central-resume.sh --selftest
#
# release workflow の publish job は、Android と KMP の 2 つの deployment (枠) を持ち回る。
# 失敗した run を同じ version で再実行したとき、枠ごとに「前回の deployment がどこまで進んで
# いたか」で次にすべきことが変わる。この判定を workflow の step に直書きすると、実際の
# Portal を相手にしないと分岐を確かめられない。判定だけをここへ切り出し、状態から動作を
# 決める部分を自己テストで網羅する。
#
# サブコマンドの契約:
#
#   decide <状態>   状態 1 つを動作 1 語へ写す。ネットワークへは出ない。
#                   状態は central-portal.sh status / wait-validated が出す語彙
#                   (PENDING / VALIDATING / VALIDATED / PUBLISHING / PUBLISHED /
#                   FAILED / NOT_FOUND) と、前回の deployment ID を持っていないことを
#                   表す NONE を受け付ける。それ以外は失敗する (未知の状態を既定の動作へ
#                   丸めると、知らない状態のまま upload を重ねてしまう)。
#   resume          枠の ID ファイルを読み、ID があれば central-portal.sh status で状態を
#                   照会してから decide に写す。ID ファイルが無い・空なら照会せず NONE。
#                   動作 1 語を標準出力へ、経緯を標準エラーへ出す。
#
# 動作の語彙 (標準出力に 1 語):
#
#   upload           新しく upload する (前回の deployment は引き継がない)
#   drop-and-upload  前回の deployment を drop してから upload し直す
#   skip-upload      upload を skip し、前回の deployment をそのまま release へ回す
#   wait-published   release は要求済みとみなし、PUBLISHED になるまで待つ (release は送らない)
#   skip-all         公開済み。upload も release も行わない
#   settle-first     検証が未決着。wait-validated で決着させてから、もう一度この判定に掛ける
#
# 状態と動作の対応:
#
#   NONE          -> upload           前回の ID が無いので引き継ぐものが無い
#   NOT_FOUND     -> upload           ID はあるが deployment が消えている (drop 済みなど)
#   FAILED        -> drop-and-upload  検証に落ちた deployment は残せない (同じ version で
#                                     保留が 2 件並ぶと release の対象が決まらない)
#   VALIDATED     -> skip-upload      検証済みの保留。作り直す理由が無い
#   PUBLISHING    -> wait-published   release は受理済み。もう一度 release を送ると失敗する
#   PUBLISHED     -> skip-all         公開済み。同じ version は上書きできない
#   PENDING       -> settle-first     まだ決着していない
#   VALIDATING    -> settle-first     同上
#
# 環境変数:
#   KSR_CENTRAL_PORTAL  状態照会に使うコマンド (既定はこのスクリプトと同じ場所の
#                       central-portal.sh)。自己テストと手元確認のための差し替え口。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

# 前回の deployment ID を持っていないことを表す擬似的な状態。
readonly STATE_NONE="NONE"

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <サブコマンド> <引数>

  decide <状態>                 状態 (NONE / NOT_FOUND / FAILED / VALIDATED /
                                PUBLISHING / PUBLISHED / PENDING / VALIDATING) を
                                動作 1 語へ写す
  resume <枠名> <ID ファイル>   ID ファイルから状態を照会して動作 1 語を出す
  --selftest                    ネットワークに出ずに自己テストを実行する
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# 状態照会。自己テストはこの関数だけを差し替える。
portal_status() {
    local id="$1"
    "${KSR_CENTRAL_PORTAL:-${SCRIPT_DIR}/central-portal.sh}" status "${id}"
}

# 状態 1 つを動作 1 語へ写す。未知の状態は失敗にする。
cmd_decide() {
    local state="$1"
    case "${state}" in
        "${STATE_NONE}"|NOT_FOUND) echo "upload" ;;
        FAILED)                    echo "drop-and-upload" ;;
        VALIDATED)                 echo "skip-upload" ;;
        PUBLISHING)                echo "wait-published" ;;
        PUBLISHED)                 echo "skip-all" ;;
        PENDING|VALIDATING)        echo "settle-first" ;;
        *)                         fail "引き継ぎ方を決められない状態: ${state}" ;;
    esac
}

# 枠の ID ファイルから動作を決める。
cmd_resume() {
    local slot="$1" id_file="$2"
    local id="" state

    if [ -f "${id_file}" ]; then
        # 前後の空白と改行を落とす (artifact 経由で往復すると末尾の改行が付く)。
        id="$(tr -d '[:space:]' < "${id_file}")"
    fi

    if [ -z "${id}" ]; then
        state="${STATE_NONE}"
        echo "${slot} 枠: 前回の deployment ID が無い" >&2
    else
        state="$(portal_status "${id}")" \
            || fail "${slot} 枠: deployment ${id} の状態を照会できない"
        echo "${slot} 枠: 前回の deployment ${id} は ${state}" >&2
    fi

    # 判定の失敗をそのまま返す。`set -e` は条件式の中で呼ばれた関数には効かないので、
    # 代入の成否をここで明示的に見る。
    local action
    action="$(cmd_decide "${state}")" || return 1
    echo "${slot} 枠の動作: ${action}" >&2
    echo "${action}"
}

# --- 自己テスト ----------------------------------------------------------------------
#
# decide は 8 状態すべてと未知の状態を、resume は ID ファイルの有無を検査する。
# 状態照会はモックへ差し替えるので、ネットワークへは出ない。

selftest() {
    SELFTEST_WORK="$(mktemp -d)"
    trap 'rm -rf "${SELFTEST_WORK}"' EXIT
    local work="${SELFTEST_WORK}"

    MOCK_STATE_FILE="${work}/state"
    MOCK_CALLS="${work}/calls"
    : > "${MOCK_CALLS}"

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

    # portal_status のモック。照会した ID を記録し、仕込んだ状態を返す。
    portal_status() {
        echo "$1" >> "${MOCK_CALLS}"
        cat "${MOCK_STATE_FILE}"
    }

    expect_decide() {
        local state="$1" expected="$2"
        local actual
        actual="$(cmd_decide "${state}")"
        check "$([ "${actual}" = "${expected}" ] && echo 0 || echo 1)" \
            "${state} -> ${expected}" "${actual}"
    }

    echo "[状態から動作への対応]"
    expect_decide "${STATE_NONE}" "upload"
    expect_decide NOT_FOUND "upload"
    expect_decide FAILED "drop-and-upload"
    expect_decide VALIDATED "skip-upload"
    expect_decide PUBLISHING "wait-published"
    expect_decide PUBLISHED "skip-all"
    expect_decide PENDING "settle-first"
    expect_decide VALIDATING "settle-first"

    # 6 状態が互いに区別できることを、動作の重なりが無いことで示す。
    local distinct
    distinct="$(for state in "${STATE_NONE}" NOT_FOUND FAILED VALIDATED PUBLISHING PUBLISHED; do
        cmd_decide "${state}"
    done | sort -u | wc -l | tr -d ' ')"
    # NONE と NOT_FOUND はどちらも upload なので、6 状態から出る動作は 5 種になる。
    check "$([ "${distinct}" = "5" ] && echo 0 || echo 1)" \
        "6 状態が 5 種の動作へ分かれる (NONE と NOT_FOUND だけが同じ)" "${distinct}"

    echo "[未知の状態]"
    check "$(if ( cmd_decide UNKNOWN_STATE > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "知らない状態は失敗にする (既定の動作へ丸めない)"
    check "$(if ( cmd_decide "" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "空の状態は失敗にする"

    echo "[resume]"
    local action
    : > "${MOCK_CALLS}"
    action="$(cmd_resume android "${work}/absent-id" 2>/dev/null)"
    check "$([ "${action}" = "upload" ] && echo 0 || echo 1)" \
        "ID ファイルが無ければ upload" "${action}"
    check "$([ ! -s "${MOCK_CALLS}" ] && echo 0 || echo 1)" \
        "ID ファイルが無ければ状態を照会しない" "$(cat "${MOCK_CALLS}")"

    printf '' > "${work}/empty-id"
    : > "${MOCK_CALLS}"
    action="$(cmd_resume android "${work}/empty-id" 2>/dev/null)"
    check "$([ "${action}" = "upload" ] && echo 0 || echo 1)" \
        "ID ファイルが空でも upload" "${action}"
    check "$([ ! -s "${MOCK_CALLS}" ] && echo 0 || echo 1)" \
        "ID ファイルが空なら状態を照会しない" "$(cat "${MOCK_CALLS}")"

    # artifact 経由で往復した ID には末尾の改行が付く。照会の前に落とす。
    printf '11111111-2222-3333-4444-555555555555\n' > "${work}/id"
    echo "VALIDATED" > "${MOCK_STATE_FILE}"
    : > "${MOCK_CALLS}"
    action="$(cmd_resume kmp "${work}/id" 2>/dev/null)"
    check "$([ "${action}" = "skip-upload" ] && echo 0 || echo 1)" \
        "ID があれば照会した状態で決める" "${action}"
    check "$([ "$(cat "${MOCK_CALLS}")" = "11111111-2222-3333-4444-555555555555" ] && echo 0 || echo 1)" \
        "改行を落とした ID で照会する" "$(cat "${MOCK_CALLS}")"

    echo "PUBLISHING" > "${MOCK_STATE_FILE}"
    action="$(cmd_resume kmp "${work}/id" 2>/dev/null)"
    check "$([ "${action}" = "wait-published" ] && echo 0 || echo 1)" \
        "PUBLISHING の枠は release を送り直さない" "${action}"

    echo "FAILED" > "${MOCK_STATE_FILE}"
    local resume_output
    resume_output="$(cmd_resume android "${work}/id" 2>&1 >/dev/null)"
    check "$(if [ "${resume_output#*drop-and-upload}" != "${resume_output}" ]; then echo 0; else echo 1; fi)" \
        "経緯は標準エラーへ出す (標準出力は動作 1 語だけ)" "${resume_output}"

    echo "UNKNOWN_STATE" > "${MOCK_STATE_FILE}"
    check "$(if ( cmd_resume android "${work}/id" > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "照会した状態が未知なら失敗する"

    if [ "${failures}" -eq 0 ]; then
        echo "失敗なし"
        return 0
    fi
    echo "失敗 ${failures} 件" >&2
    return 1
}

# --- 入口 ----------------------------------------------------------------------------

main() {
    if [ $# -eq 0 ]; then
        usage
        exit 2
    fi

    local subcommand="$1"
    shift

    case "${subcommand}" in
        --selftest)
            selftest
            ;;
        decide)
            [ $# -eq 1 ] || { usage; exit 2; }
            cmd_decide "$1"
            ;;
        resume)
            [ $# -eq 2 ] || { usage; exit 2; }
            cmd_resume "$1" "$2"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "::error::不明なサブコマンド: ${subcommand}" >&2
            usage
            exit 2
            ;;
    esac
}

main "$@"
