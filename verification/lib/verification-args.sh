# shellcheck shell=bash
# 消費者検証スクリプトが共有する引数解釈。
#
# 各形態の prepare-feed.sh / build-consumer.sh から source して使う。
# source する側は、あらかじめ次を定義しておく。
#
#   KSV_PLATFORM        形態名 (ios / android / maui / kmp)。使い方の表示と証跡の見出しに使う
#   KSV_DEFAULT_VERSION 省略可。dry-run で version 指定が無いときの既定値を上書きする。
#                       version を持たない形態 (iOS の dry-run は path 参照) は空文字を入れる。
#                       未定義なら下の検証用の合成 version を使う
#
# ksv_parse_args "$@" を呼ぶと、次の変数が設定される。
#
#   KSV_MODE       dry-run | smoke
#   KSV_VERSION    解決後の version (iOS の dry-run では空)
#   KSV_REFERENCE  --reference で与えられた準備済み参照先 (未指定なら空。dry-run 専用)
#   KSV_WORK       作業ディレクトリ (未指定なら形態ごとの既定)
#
# 許可値以外の mode、smoke で version が無い場合、smoke に --reference を与えた場合は、
# フィード準備や依存解決へ進む前にこの関数が異常終了する。

# dry-run で version の指定が無いときに 4 形態へ流す検証用の合成 version。
# 本体が注入値として受け付ける形式 (X.Y.Z または X.Y.Z-{alpha|beta|rc}.N) に適合し、
# リリースしない値なので実在の配布物と衝突しない。値の宣言はこの 1 か所だけで、
# 形態ごとの既定値は持たない (同じ文字列が 4 形態に流れることを検証の前提にしている)。
KSV_SYNTHETIC_VERSION="0.0.0-alpha.0"

ksv_fail() {
    echo "エラー: $1" >&2
    exit 1
}

# 解決結果の証跡を標準出力へ出し、CI では job summary にも残す。
# 本文は標準入力から受け取る。
#
#   printf '%s\n' "$resolved" | ksv_evidence "解決した依存"
ksv_evidence() {
    local title="$1"
    local body
    body="$(cat)"

    echo "==== ${title} ===="
    printf '%s\n' "${body}"

    if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
        {
            echo "### ${KSV_PLATFORM}: ${title}"
            echo
            echo '```'
            printf '%s\n' "${body}"
            echo '```'
            echo
        } >> "${GITHUB_STEP_SUMMARY}"
    fi
}

# 作業ディレクトリを決めて作り、絶対パスを標準出力へ返す。
#
#   work="$(ksv_prepare_work "${KSV_WORK}" "ios")"
#
# 指定が無ければ一時領域の下に形態ごとのディレクトリを作る。リポジトリの中は使わない
# (発行物・作業コピー・展開先が作業ツリーに残ると、追跡物との差分や lint 違反の元になる)。
ksv_prepare_work() {
    local requested="$1"
    local platform="$2"
    local work="${requested:-${TMPDIR:-/tmp}/ksdialogs-verification/${platform}}"

    mkdir -p "${work}"
    work="$(cd "${work}" && pwd -P)"

    case "${work}/" in
        "${REPO_ROOT}"/*)
            ksv_fail "作業ディレクトリをリポジトリの中に置けません: ${work}"
            ;;
    esac

    printf '%s\n' "${work}"
}

# 作業ディレクトリ直下の使い捨てディレクトリを空にして作り直し、絶対パスを標準出力へ返す。
#
#   feed="$(ksv_reset_dir "${work}" feed)"
#
# 前回の実行の残留物が参照先に混ざると「参照先に無い version は解決できない」ことを
# 確かめられなくなるため、実行のたびに空から作る。引数の誤指定で無関係な作業ツリーを
# 消さないよう、削除の前に次を全件検証する。
#
#   1. 作業ディレクトリが絶対パスの実在するディレクトリである
#   2. 名前が単一の階層である (パス区切り・`.`・`..` を含まない)
#   3. 対象がリポジトリの中でない
ksv_reset_dir() {
    local work="$1"
    local name="$2"

    case "${work}" in
        /*) ;;
        *) ksv_fail "作業ディレクトリが絶対パスではありません: ${work}" ;;
    esac
    [ -d "${work}" ] || ksv_fail "作業ディレクトリがありません: ${work}"

    case "${name}" in
        ""|.|..|*/*) ksv_fail "作り直すディレクトリ名が単一の階層ではありません: ${name}" ;;
    esac

    local target="${work}/${name}"
    case "${target}/" in
        "${REPO_ROOT}"/*)
            ksv_fail "リポジトリの中のディレクトリは作り直しません: ${target}"
            ;;
    esac

    # CI ランナーと利用者の環境に trash は無いため、成果物のスクリプト内の削除は rm で行う。
    rm -rf "${target}"
    mkdir -p "${target}"

    printf '%s\n' "${target}"
}

ksv_usage() {
    cat >&2 <<EOF
使い方: $(basename "$0") [オプション]

  --mode <dry-run|smoke>  参照先の選択 (既定: dry-run)
  --version <version>     解決する version (smoke では必須。dry-run の既定は ${KSV_SYNTHETIC_VERSION})
  --reference <dir>       準備済みの参照先。与えるとフィード準備を行わない (dry-run 専用)
  --work <dir>            作業ディレクトリ (既定は形態ごと)
EOF
}

ksv_parse_args() {
    KSV_MODE="dry-run"
    KSV_VERSION=""
    KSV_REFERENCE=""
    KSV_WORK=""

    while [ $# -gt 0 ]; do
        case "$1" in
            --mode)
                [ $# -ge 2 ] || ksv_fail "--mode に値がありません"
                KSV_MODE="$2"
                shift 2
                ;;
            --version)
                [ $# -ge 2 ] || ksv_fail "--version に値がありません"
                KSV_VERSION="$2"
                shift 2
                ;;
            --reference)
                [ $# -ge 2 ] || ksv_fail "--reference に値がありません"
                KSV_REFERENCE="$2"
                shift 2
                ;;
            --work)
                [ $# -ge 2 ] || ksv_fail "--work に値がありません"
                KSV_WORK="$2"
                shift 2
                ;;
            -h|--help)
                ksv_usage
                exit 0
                ;;
            *)
                ksv_usage
                ksv_fail "不明な引数: $1"
                ;;
        esac
    done

    case "${KSV_MODE}" in
        dry-run|smoke) ;;
        *)
            ksv_fail "mode は dry-run か smoke のいずれかです: ${KSV_MODE}"
            ;;
    esac

    if [ "${KSV_MODE}" = "smoke" ] && [ -z "${KSV_VERSION}" ]; then
        ksv_fail "smoke では --version が必須です"
    fi

    # smoke の参照先は公開レジストリで、準備済みの参照先を差し込む余地がない。
    # 黙って無視すると「渡した配布物を検証した」と誤解されるため、組み合わせ自体を拒む。
    if [ "${KSV_MODE}" = "smoke" ] && [ -n "${KSV_REFERENCE}" ]; then
        ksv_fail "smoke では --reference を指定できません (参照先は公開レジストリで、準備済みの配布物は使いません)"
    fi

    if [ -z "${KSV_VERSION}" ]; then
        KSV_VERSION="${KSV_DEFAULT_VERSION-${KSV_SYNTHETIC_VERSION}}"
    fi

    echo "platform=${KSV_PLATFORM} mode=${KSV_MODE} version=${KSV_VERSION:-(なし)}"
}
