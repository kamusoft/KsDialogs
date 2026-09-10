#!/bin/bash
# 部分 publish の続きを埋めてよいかの判定と、その根拠になる印 (marker) の読み書き。
#
# 使い方:
#   scripts/release/check-resume-eligibility.sh decide <試行回数> <monorepo tag の状態> <外部状態> <marker の状態>
#   scripts/release/check-resume-eligibility.sh marker-write <ファイル> <version> <commit> <run id>
#   scripts/release/check-resume-eligibility.sh marker-state <ファイル> <version> <commit> <run id>
#   scripts/release/check-resume-eligibility.sh --selftest
#
# publish job は、書き込みを始める前に当該 version の外部状態 (配信リポジトリの tag、
# Maven Central の公開済み、nuget.org の存在) と monorepo の tag を見直す。この判定は、
# 見直した結果から「進んでよい / 続きを埋めてよい / 止まるべき」を決める。
#
# decide の引数:
#
#   試行回数            GitHub Actions の run_attempt (1 が新規 dispatch、2 以上が同じ run の再試行)
#   monorepo tag の状態 absent      当該 version の tag が無い
#                       same-commit tag があり、起動 commit を指している
#                       other-commit tag があり、別の commit を指している
#   外部状態            none    当該 version の外部状態が 1 つも無い
#                       present 配信リポジトリの tag / Central の公開済み / nuget.org の存在の
#                               いずれかが既にある
#   marker の状態       absent   この run が「外部状態は無い」と確認した印が無い
#                       match    印があり、version・commit・run id がすべて今回と一致する
#                       mismatch 印はあるが内容が食い違う
#
# 判定 (標準出力に 1 語、理由と案内は標準エラー):
#
#   proceed              まだ何も出ていない。最初から進む (exit 0)
#   resume               この run が作った外部状態の続きを埋める (exit 0)
#   skip-all             monorepo の tag が起動 commit にある = レジストリと配信リポジトリへの
#                        publish は完了済み。それらは何もしない (exit 0)
#   fail-new-dispatch    新規 dispatch なのに外部状態だけが先にある (exit 1)
#   fail-tag-mismatch    monorepo の tag が別の commit を指している (exit 1)
#   fail-no-marker       再試行だが、外部状態がこの run から出たという印が無い (exit 1)
#   fail-marker-mismatch 印はあるが、version・commit・run id が今回と食い違う (exit 1)
#
# 続きを埋める経路を「同じ run の再試行」に限るのは、公開済みの binary と monorepo の tag が
# 指す source の対応を保証するため。ただし試行回数だけでは足りない — 外部状態を理由に
# 拒否された run をそのまま再実行すると、試行 2 回目として resume が通り、別の commit から
# 作られた binary に今回の commit の tag を打ててしまう。そこで「外部状態が 1 つも無い」と
# 確認できた試行だけが marker を残し、以後の試行はその marker が今回の version・commit・run と
# 一致するときにだけ続きを埋める。marker が無い / 食い違う再試行は止めて、部分 publish を
# 行った run そのものの再実行へ案内する。
#
# monorepo の tag が起動 commit にあるときは、source と binary の対応が tag で示されている
# ので完了済みとして扱う。この skip はレジストリと配信リポジトリへの publish に限り、
# tag より後の冪等な後処理 (GitHub Release の作成・インストール例の反映) は skip しない
# (tag を作った直後に落ちた run の再実行が、後処理を残したまま緑になるため)。

set -euo pipefail

# 失敗したときに案内する回復手順。
readonly RETRY_GUIDANCE="失敗した run を GitHub Actions の画面から再実行する (Re-run failed jobs)。新規 dispatch では続きを埋められない"
readonly MARKER_GUIDANCE="続きを埋められるのは、外部状態が 1 つも無い状態から publish に入った run の再実行だけ。部分 publish を行った run そのものを再実行する"

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") decide <試行回数> <absent|same-commit|other-commit> <none|present> <absent|match|mismatch>
        $(basename "${BASH_SOURCE[0]}") marker-write <ファイル> <version> <commit> <run id>
        $(basename "${BASH_SOURCE[0]}") marker-state <ファイル> <version> <commit> <run id>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# 4 つの入力から判定 1 語を出す。
cmd_decide() {
    local attempt="$1" tag_state="$2" external="$3" marker="$4"

    case "${attempt}" in
        ''|*[!0-9]*) fail "試行回数が数字ではない: ${attempt}" ;;
    esac
    [ "${attempt}" -ge 1 ] || fail "試行回数は 1 以上: ${attempt}"

    case "${external}" in
        none|present) ;;
        *) fail "知らない外部状態: ${external} (none か present)" ;;
    esac

    case "${tag_state}" in
        absent|same-commit|other-commit) ;;
        *) fail "知らない tag の状態: ${tag_state} (absent / same-commit / other-commit)" ;;
    esac

    case "${marker}" in
        absent|match|mismatch) ;;
        *) fail "知らない marker の状態: ${marker} (absent / match / mismatch)" ;;
    esac

    if [ "${tag_state}" = "other-commit" ]; then
        echo "monorepo の tag が起動 commit とは別の commit を指している" >&2
        echo "fail-tag-mismatch"
        return 1
    fi

    if [ "${tag_state}" = "same-commit" ]; then
        echo "monorepo の tag が起動 commit にある (レジストリと配信リポジトリへの publish は完了済み)" >&2
        echo "skip-all"
        return 0
    fi

    if [ "${external}" = "none" ]; then
        echo "当該 version の外部状態は無い" >&2
        echo "proceed"
        return 0
    fi

    if [ "${attempt}" -lt 2 ]; then
        echo "新規 dispatch (試行 1 回目) だが、当該 version の外部状態が既にある" >&2
        echo "${RETRY_GUIDANCE}" >&2
        echo "fail-new-dispatch"
        return 1
    fi

    case "${marker}" in
        match)
            echo "この run が作った外部状態の続きを埋める (試行 ${attempt} 回目)" >&2
            echo "resume"
            return 0
            ;;
        absent)
            echo "外部状態があるが、この run が publish に入ったときの印が無い" >&2
            echo "${MARKER_GUIDANCE}" >&2
            echo "fail-no-marker"
            return 1
            ;;
        *)
            echo "印はあるが、今回の version・commit・run のいずれかと食い違う" >&2
            echo "${MARKER_GUIDANCE}" >&2
            echo "fail-marker-mismatch"
            return 1
            ;;
    esac
}

# 印を書く。呼ぶのは decide が proceed を返した試行だけ
# (= 当該 version の外部状態が 1 つも無いことを、この run が自分で確認した試行)。
cmd_marker_write() {
    local file="$1" version="$2" commit="$3" run_id="$4"

    [ -n "${version}" ] || fail "marker の version が空"
    [ -n "${commit}" ] || fail "marker の commit が空"
    [ -n "${run_id}" ] || fail "marker の run id が空"

    mkdir -p "$(dirname "${file}")"
    {
        printf 'version=%s\n' "${version}"
        printf 'commit=%s\n' "${commit}"
        printf 'run=%s\n' "${run_id}"
    } > "${file}"
    echo "印を書いた: version=${version} / commit=${commit} / run=${run_id}" >&2
}

# 印を今回の値と突き合わせ、absent / match / mismatch を出す。
cmd_marker_state() {
    local file="$1" version="$2" commit="$3" run_id="$4"

    if [ ! -f "${file}" ] || [ -z "$(tr -d '[:space:]' < "${file}")" ]; then
        echo "印は無い" >&2
        echo "absent"
        return 0
    fi

    local expected actual
    # 末尾の空白と空行を落としてから比べる。artifact の往復で改行が増減しても
    # 内容の一致だけを見る (コマンド置換が末尾の改行を落とすので両辺の形が揃う)。
    expected="$(printf 'version=%s\ncommit=%s\nrun=%s\n' "${version}" "${commit}" "${run_id}")"
    actual="$(sed -e 's/[[:space:]]*$//' -e '/^$/d' "${file}")"
    if [ "${actual}" = "${expected}" ]; then
        echo "印は今回の version・commit・run と一致する" >&2
        echo "match"
        return 0
    fi
    echo "印の内容が今回と食い違う (印: $(tr '\n' ' ' < "${file}"))" >&2
    echo "mismatch"
    return 0
}

# --- 自己テスト ----------------------------------------------------------------------
#
# 入力の組み合わせを網羅して判定を確かめる。実レジストリも GitHub API も要らない。

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

    # 判定 1 語と終了コードをまとめて確かめる。
    expect() {
        local attempt="$1" tag_state="$2" external="$3" marker="$4" expected="$5" expected_code="$6"
        local actual code=0
        actual="$(cmd_decide "${attempt}" "${tag_state}" "${external}" "${marker}" 2>/dev/null)" || code=$?
        check "$([ "${actual}" = "${expected}" ] && [ "${code}" = "${expected_code}" ] && echo 0 || echo 1)" \
            "試行 ${attempt} / tag ${tag_state} / 外部 ${external} / 印 ${marker} -> ${expected} (exit ${expected_code})" \
            "${actual} (exit ${code})"
    }

    echo "[外部状態が無い]"
    expect 1 absent none absent proceed 0
    expect 2 absent none absent proceed 0
    expect 2 absent none match proceed 0

    echo "[外部状態がある]"
    expect 1 absent present absent fail-new-dispatch 1
    expect 1 absent present match fail-new-dispatch 1
    expect 2 absent present match resume 0
    expect 7 absent present match resume 0

    echo "[外部状態があるが、この run 由来だと示せない]"
    # ここが本題。試行 2 回目という条件だけでは resume を通さない
    expect 2 absent present absent fail-no-marker 1
    expect 7 absent present absent fail-no-marker 1
    expect 2 absent present mismatch fail-marker-mismatch 1
    expect 7 absent present mismatch fail-marker-mismatch 1

    echo "[monorepo の tag]"
    expect 1 same-commit present absent skip-all 0
    expect 1 same-commit none absent skip-all 0
    expect 2 same-commit present absent skip-all 0
    expect 1 other-commit none absent fail-tag-mismatch 1
    expect 2 other-commit present match fail-tag-mismatch 1

    echo "[案内]"
    local message
    message="$(cmd_decide 1 absent present absent 2>&1 >/dev/null)" || true
    check "$(if [ "${message#*再実行}" != "${message}" ]; then echo 0; else echo 1; fi)" \
        "新規 dispatch の失敗は再実行を案内する" "${message}"
    check "$(if [ "${message#*::error::}" != "${message}" ]; then echo 1; else echo 0; fi)" \
        "案内は診断注釈ではなく説明として出す" "${message}"
    message="$(cmd_decide 2 absent present absent 2>&1 >/dev/null)" || true
    check "$(if [ "${message#*部分 publish を行った run}" != "${message}" ]; then echo 0; else echo 1; fi)" \
        "印の無い再試行の失敗は部分 publish を行った run の再実行を案内する" "${message}"

    echo "[入力の誤り]"
    check "$(if ( cmd_decide 0 absent none absent > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "試行回数 0 は失敗する"
    check "$(if ( cmd_decide x absent none absent > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "試行回数が数字でなければ失敗する"
    check "$(if ( cmd_decide 1 unknown none absent > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "知らない tag の状態は失敗する"
    check "$(if ( cmd_decide 1 absent unknown absent > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "知らない外部状態は失敗する"
    check "$(if ( cmd_decide 1 absent none unknown > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "知らない marker の状態は失敗する"

    echo "[印の読み書き]"
    local marker="${work}/deep/marker.txt" state
    state="$(cmd_marker_state "${marker}" 0.1.0 abc123 42 2>/dev/null)"
    check "$([ "${state}" = "absent" ] && echo 0 || echo 1)" "書く前の印は absent" "${state}"

    cmd_marker_write "${marker}" 0.1.0 abc123 42 2>/dev/null
    state="$(cmd_marker_state "${marker}" 0.1.0 abc123 42 2>/dev/null)"
    check "$([ "${state}" = "match" ] && echo 0 || echo 1)" "同じ version・commit・run なら match" "${state}"

    state="$(cmd_marker_state "${marker}" 0.2.0 abc123 42 2>/dev/null)"
    check "$([ "${state}" = "mismatch" ] && echo 0 || echo 1)" "version が違えば mismatch" "${state}"
    state="$(cmd_marker_state "${marker}" 0.1.0 def456 42 2>/dev/null)"
    check "$([ "${state}" = "mismatch" ] && echo 0 || echo 1)" "commit が違えば mismatch" "${state}"
    state="$(cmd_marker_state "${marker}" 0.1.0 abc123 43 2>/dev/null)"
    check "$([ "${state}" = "mismatch" ] && echo 0 || echo 1)" "run が違えば mismatch" "${state}"

    printf '\n\n' > "${marker}"
    state="$(cmd_marker_state "${marker}" 0.1.0 abc123 42 2>/dev/null)"
    check "$([ "${state}" = "absent" ] && echo 0 || echo 1)" "空白だけの印は absent" "${state}"

    printf 'version=0.1.0\ncommit=abc123\nrun=42\n\n' > "${marker}"
    state="$(cmd_marker_state "${marker}" 0.1.0 abc123 42 2>/dev/null)"
    check "$([ "${state}" = "match" ] && echo 0 || echo 1)" "末尾に空行が増えても match" "${state}"

    echo "[印と判定をつないだ経路]"
    # 拒否された試行 1 回目は印を残さないので、その run の再実行は止まる
    local decision code=0
    decision="$(cmd_decide 1 absent present "$(cmd_marker_state "${work}/none.txt" 0.1.0 abc123 42 2>/dev/null)" 2>/dev/null)" || code=$?
    check "$([ "${decision}" = "fail-new-dispatch" ] && [ "${code}" = "1" ] && echo 0 || echo 1)" \
        "外部状態ありの試行 1 回目は拒否され、印は書かれない" "${decision}"
    check "$([ ! -f "${work}/none.txt" ] && echo 0 || echo 1)" "拒否された試行は印を残さない"
    code=0
    decision="$(cmd_decide 2 absent present "$(cmd_marker_state "${work}/none.txt" 0.1.0 abc123 42 2>/dev/null)" 2>/dev/null)" || code=$?
    check "$([ "${decision}" = "fail-no-marker" ] && [ "${code}" = "1" ] && echo 0 || echo 1)" \
        "その run の再実行 (試行 2 回目) も止まる" "${decision}"

    # 正規の経路: 外部状態が無い試行 1 回目が印を書き、その run の再実行だけが続きを埋める
    local proper="${work}/proper.txt"
    decision="$(cmd_decide 1 absent none "$(cmd_marker_state "${proper}" 0.1.0 abc123 99 2>/dev/null)" 2>/dev/null)"
    check "$([ "${decision}" = "proceed" ] && echo 0 || echo 1)" "外部状態が無い試行 1 回目は proceed" "${decision}"
    cmd_marker_write "${proper}" 0.1.0 abc123 99 2>/dev/null
    decision="$(cmd_decide 2 absent present "$(cmd_marker_state "${proper}" 0.1.0 abc123 99 2>/dev/null)" 2>/dev/null)"
    check "$([ "${decision}" = "resume" ] && echo 0 || echo 1)" "その run の再実行だけが resume" "${decision}"
    # 別の run が同じ印を拾っても通らない
    code=0
    decision="$(cmd_decide 2 absent present "$(cmd_marker_state "${proper}" 0.1.0 abc123 100 2>/dev/null)" 2>/dev/null)" || code=$?
    check "$([ "${decision}" = "fail-marker-mismatch" ] && [ "${code}" = "1" ] && echo 0 || echo 1)" \
        "別の run から拾った印では resume にならない" "${decision}"
    code=0
    decision="$(cmd_decide 2 absent present "$(cmd_marker_state "${proper}" 0.1.0 zzz999 99 2>/dev/null)" 2>/dev/null)" || code=$?
    check "$([ "${decision}" = "fail-marker-mismatch" ] && [ "${code}" = "1" ] && echo 0 || echo 1)" \
        "別の commit で起動した再試行では resume にならない" "${decision}"

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
    if [ $# -lt 1 ]; then
        usage
        exit 2
    fi

    local command="$1"
    shift
    case "${command}" in
        decide)
            [ $# -eq 4 ] || { usage; exit 2; }
            cmd_decide "$@"
            ;;
        marker-write)
            [ $# -eq 4 ] || { usage; exit 2; }
            cmd_marker_write "$@"
            ;;
        marker-state)
            [ $# -eq 4 ] || { usage; exit 2; }
            cmd_marker_state "$@"
            ;;
        *)
            usage
            exit 2
            ;;
    esac
}

main "$@"
