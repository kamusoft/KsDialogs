#!/bin/bash
# 配信リポジトリの同名 tag と、今回のスナップショットの内容の照合。
#
# 使い方:
#   scripts/release/check-distribution-tag.sh <配信リポジトリの作業コピー> <version>
#   scripts/release/check-distribution-tag.sh --selftest
#
# 作業コピーには scripts/spm-snapshot/sync-snapshot.sh を適用済みであることを前提とする
# (スナップショットの生成そのものはこのスクリプトの責務ではない)。
#
# 判定は次の 3 通りで、結果を標準出力へ 1 語で出す。説明とエラーは標準エラーへ出す。
#
#   absent   同名の tag が無い (これから作れる)
#   match    同名の tag があり、内容が今回のスナップショットと同一 (作成を飛ばせる)
#   (失敗)   同名の tag があり、内容が異なる。exit 1
#
# 内容が異なる tag は上書きできないため、公開の途中でこれに当たると iOS だけ利用者から
# 解決できない状態が残る。呼び出し側は、不可逆な公開に入る前と tag を作る直前の両方で
# この検査を通す。
#
# 判定は remote 側の tag を正とする。手元の tag は古くなり得るので、有無は remote へ
# 直接聞き、手元の同名 tag は remote の状態に合わせて取り直す (または消す)。
#
# 未追跡ファイルを含めたツリーを比較するため、作業コピーの index を書き換える
# (`git add -A`)。呼び出し側は index と手元の tag の状態に依存しないこと。

set -euo pipefail

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <配信リポジトリの作業コピー> <version>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

cmd_check() {
    local work="$1" version="$2"

    [ -d "${work}/.git" ] || fail "配信リポジトリの作業コピーではありません: ${work}"

    # 判定の正は常に remote 側の tag とする。手元の tag は clone / fetch の時点で古くなり、
    # remote から削除された tag もローカルには残り続けるため、有無は remote へ直接聞く。
    local remote_refs
    remote_refs="$(git -C "${work}" ls-remote --tags origin "refs/tags/${version}")"

    if [ -z "${remote_refs}" ]; then
        # remote に無いのに手元に残っている tag は、呼び出し側の `git tag` を重複で失敗させる。
        # remote に合わせて消しておく。
        if git -C "${work}" rev-parse -q --verify "refs/tags/${version}" > /dev/null; then
            git -C "${work}" tag -d "${version}" > /dev/null
            echo "remote から削除された tag ${version} を手元からも消した" >&2
        fi
        echo "配信リポジトリに tag ${version} は無い" >&2
        echo "absent"
        return 0
    fi

    # remote にある tag が指すものを手元へ取り直す (別の場所へ移されていれば上書きする)。
    git -C "${work}" fetch --quiet --force origin "refs/tags/${version}:refs/tags/${version}"

    # スナップショットは未追跡ファイルを含むので、index へ載せてからツリーを取る。
    git -C "${work}" add -A
    local snapshot_tree tag_tree
    snapshot_tree="$(git -C "${work}" write-tree)"
    tag_tree="$(git -C "${work}" rev-parse "refs/tags/${version}^{tree}")"

    if [ "${snapshot_tree}" != "${tag_tree}" ]; then
        git -C "${work}" diff --cached --stat "refs/tags/${version}" >&2 || true
        fail "配信リポジトリの tag ${version} が今回のスナップショットと異なる内容を指している"
    fi

    echo "配信リポジトリの tag ${version} は今回のスナップショットと同一" >&2
    echo "match"
}

# --- 自己テスト ----------------------------------------------------------------------
#
# 手元に origin 役の裸リポジトリと作業コピーを作り、tag の有無と内容の一致 / 不一致を
# 一通り試す。ネットワークへは出ない。

selftest() {
    SELFTEST_WORK="$(mktemp -d)"
    trap 'rm -rf "${SELFTEST_WORK}"' EXIT
    local root="${SELFTEST_WORK}"

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

    # git の呼び出しに作者情報と既定ブランチ名を与える (実行環境の設定に依存させない)。
    local -a git_env=(
        GIT_AUTHOR_NAME=selftest GIT_AUTHOR_EMAIL=selftest@invalid
        GIT_COMMITTER_NAME=selftest GIT_COMMITTER_EMAIL=selftest@invalid
    )
    run_git() {
        env "${git_env[@]}" git "$@"
    }

    # origin 役の裸リポジトリと、そこから clone した作業コピーを組み立てる。
    local origin="${root}/origin.git" work="${root}/work"
    run_git init --quiet --bare --initial-branch=main "${origin}"

    local seed="${root}/seed"
    mkdir -p "${seed}"
    run_git init --quiet --initial-branch=main "${seed}"
    echo "package" > "${seed}/Package.swift"
    run_git -C "${seed}" add -A
    run_git -C "${seed}" commit --quiet -m "seed"
    run_git -C "${seed}" remote add origin "${origin}"
    run_git -C "${seed}" push --quiet origin main

    run_git clone --quiet "${origin}" "${work}"

    local result
    echo "[tag が無い]"
    result="$(cmd_check "${work}" 1.0.0 2>/dev/null)" || result="失敗"
    check "$([ "${result}" = "absent" ] && echo 0 || echo 1)" \
        "remote に tag が無ければ absent" "${result}"

    echo "[tag の内容が一致する]"
    # 作業コピーの現在のツリーで tag を作り、remote へ送る。
    run_git -C "${work}" tag 1.0.0
    run_git -C "${work}" push --quiet origin refs/tags/1.0.0
    result="$(cmd_check "${work}" 1.0.0 2>/dev/null)" || result="失敗"
    check "$([ "${result}" = "match" ] && echo 0 || echo 1)" \
        "内容が同じ tag は match" "${result}"

    echo "[未追跡ファイルもツリーに含める]"
    # スナップショットは未追跡のまま置かれるので、追加したファイルは差異として出る。
    echo "added" > "${work}/Added.swift"
    check "$(if ( cmd_check "${work}" 1.0.0 > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "未追跡ファイルが増えていれば失敗する"
    local output
    output="$(cmd_check "${work}" 1.0.0 2>&1)" || true
    check "$(if [ "${output#*1.0.0}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "食い違った version を出力する" "${output}"

    echo "[内容が違う tag]"
    rm -f "${work}/Added.swift"
    # remote 側の tag だけを別の内容へ動かす。手元の tag は古いままにしておき、
    # 判定が remote を正としていることを確かめる。
    local other="${root}/other"
    run_git clone --quiet "${origin}" "${other}"
    echo "changed" > "${other}/Package.swift"
    run_git -C "${other}" add -A
    run_git -C "${other}" commit --quiet -m "changed"
    run_git -C "${other}" tag -f 1.0.0 > /dev/null
    run_git -C "${other}" push --quiet --force origin refs/tags/1.0.0
    check "$(if ( cmd_check "${work}" 1.0.0 > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "remote の tag が別の内容を指していれば失敗する (手元の tag が古くても)"

    echo "[remote から消えた tag]"
    run_git -C "${other}" push --quiet origin ":refs/tags/1.0.0"
    result="$(cmd_check "${work}" 1.0.0 2>/dev/null)" || result="失敗"
    check "$([ "${result}" = "absent" ] && echo 0 || echo 1)" \
        "remote から消えていれば absent" "${result}"
    check "$(if run_git -C "${work}" rev-parse -q --verify refs/tags/1.0.0 > /dev/null; then echo 1; else echo 0; fi)" \
        "手元に残っていた同名 tag は消す"

    echo "[作業コピーの指定誤り]"
    check "$(if ( cmd_check "${root}/absent" 1.0.0 > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "作業コピーが無ければ失敗する"
    mkdir -p "${root}/not-a-repo"
    check "$(if ( cmd_check "${root}/not-a-repo" 1.0.0 > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "git の作業コピーでなければ失敗する"

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
