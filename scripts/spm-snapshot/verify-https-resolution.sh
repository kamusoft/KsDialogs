#!/bin/bash
# SwiftPM 配信リポジトリ (KsDialogs-SPM) を https URL から解決できることを、検証用の
# prerelease tag と一時消費者パッケージで確かめる。
#
# 使い方:
#   scripts/spm-snapshot/verify-https-resolution.sh <配信リポジトリの作業コピー> <検証用 tag> [ログ出力先]
#
#   <検証用 tag>   : X.Y.Z-alpha.N 形式のみ受け付ける (リリース用 tag を誤って扱わないため)
#   [ログ出力先]   : 実行ログの保存先ファイル。省略すると一時ディレクトリに作り、末尾に場所を表示する
#
# 手順:
#   1. 配信リポジトリの作業コピーと tag 名を検証する (下記の事前検証)
#   2. 検証用 tag を作業コピーに作り、origin へ push する
#   3. リポジトリ外の一時ディレクトリに消費者パッケージを生成し、https URL + tag の exact 指定で
#      依存を解決して、product KsDialogs の公開型を参照するコードを iOS Simulator 向けにビルドする
#   4. 成否を問わず、この手順が作った tag を local と remote の両方から削除する
#
# 事前検証 (1 つでも満たさない場合は tag を作らずに異常終了する):
#   - 作業コピーが (シンボリックリンクを解決した上で) git top-level ディレクトリそのものである
#   - 作業コピーの origin remote URL が配信リポジトリを指す
#   - 作業コピーが commit を 1 つ以上持つ
#   - tag 名が X.Y.Z-alpha.N 形式である
#   - 同名の tag が local にも remote にも存在しない
#   - 作業コピーの HEAD が origin の既定ブランチに含まれる (= スナップショットが remote に届いている)
#
# 最後の 1 つを見ないと、スナップショット本体の push が抜けたまま tag だけを送っても検証が
# 通ってしまう (tag の push は tag が指すオブジェクトを一緒に送るため)。後始末で tag を消すと
# 配信リポジトリには何も残らず、ログには成功だけが残る。
#
# 後始末が触るのは、この手順が作った tag ただ 1 つに限る。事前検証で同名 tag の不在を確かめて
# いるため、既存の tag を巻き添えで消すことはない。後始末は tag の作成・push に「着手した」
# 時点を見て動くため、コマンドが終了コードを返さずに中断された場合 (remote が tag を受領した
# 直後の切断など) でも削除を試みる。成否の判定には削除コマンドの終了コードではなく、削除後に
# 照会した remote と local の tag 一覧の実体を使い、照会そのものができない場合は「消えたことを
# 確認できていない」として失敗させる。
#
# commit / push (スナップショット本体) はこのスクリプトの責務ではない。作業コピーには
# 同期済み・commit 済みの内容が入っている前提で実行する。

set -euo pipefail

# 配信リポジトリの識別子。リポジトリを rename した場合はこの定数を追随させる。
readonly DISTRIBUTION_REPO="kamusoft/KsDialogs-SPM"
readonly DISTRIBUTION_URL="https://github.com/${DISTRIBUTION_REPO}"

# 一時消費者パッケージの名前。依存の product 名 (KsDialogs) と衝突しない名前にする。
readonly CONSUMER_NAME="KsDialogsSpmConsumer"

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <配信リポジトリの作業コピー> <検証用 tag> [ログ出力先]" >&2
}

fail() {
    echo "エラー: $1" >&2
    exit 1
}

if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    usage
    exit 1
fi

readonly DESTINATION_ARG="$1"
readonly TAG="$2"

# ログ出力先を決めて、本体を tee へのパイプライン越しに実行し直す。
#
# `exec > >(tee -a ...)` で同じことをすると tee が非同期の子プロセスになり、本体が exit した
# 時点で最後の出力 (後始末の記録) を書き切っている保証がない。パイプラインなら shell が tee の
# 終了まで待つため、ログの末尾が欠けない。本体側は環境変数の有無で見分けて二重に包まない。
if [ -z "${VERIFY_HTTPS_RESOLUTION_LOGGING:-}" ]; then
    if [ $# -eq 3 ]; then
        LOG_PATH="$3"
        log_dir="$(dirname "${LOG_PATH}")"
        [ -d "${log_dir}" ] || fail "ログ出力先のディレクトリがありません: ${log_dir}"
    else
        LOG_PATH="$(mktemp -d)/verify-https-resolution.log"
    fi
    : > "${LOG_PATH}" || fail "ログを書き出せません: ${LOG_PATH}"

    set +e
    VERIFY_HTTPS_RESOLUTION_LOGGING=1 bash "${BASH_SOURCE[0]}" "$1" "$2" "${LOG_PATH}" 2>&1 \
        | tee -a "${LOG_PATH}"
    body_status="${PIPESTATUS[0]}"
    set -e
    exit "${body_status}"
fi

# ここから本体。内部フラグが外から立てられて直接呼ばれた場合はログ出力先が無いので止める
[ $# -eq 3 ] || fail "内部フラグ VERIFY_HTTPS_RESOLUTION_LOGGING が設定されています。解除してから実行します"
readonly LOG_PATH="$3"

echo "== https 解決検証 =="
echo "配信リポジトリ: ${DISTRIBUTION_REPO}"
echo "検証用 tag: ${TAG}"

# --- 事前検証: 作業コピー ----------------------------------------------------------

[ -d "${DESTINATION_ARG}" ] || fail "作業コピーがディレクトリではありません: ${DESTINATION_ARG}"

DESTINATION="$(cd "${DESTINATION_ARG}" && pwd -P)"
readonly DESTINATION

destination_toplevel="$(git -C "${DESTINATION}" rev-parse --show-toplevel 2>/dev/null || true)"
[ -n "${destination_toplevel}" ] || fail "作業コピーが git リポジトリではありません: ${DESTINATION}"
destination_toplevel="$(cd "${destination_toplevel}" && pwd -P)"
if [ "${destination_toplevel}" != "${DESTINATION}" ]; then
    fail "作業コピーが git top-level ディレクトリではありません (top-level: ${destination_toplevel})"
fi

origin_url="$(git -C "${DESTINATION}" remote get-url origin 2>/dev/null || true)"
[ -n "${origin_url}" ] || fail "作業コピーに origin remote がありません: ${DESTINATION}"

# 末尾の `.git` とスラッシュの表記ゆれだけを吸収し、残りは受理する URL 形式を列挙して照合する
# (部分一致で判定すると `.../evil/kamusoft/KsDialogs-SPM` のような URL も通ってしまう)。
normalized_origin="${origin_url%/}"
normalized_origin="${normalized_origin%.git}"
case "${normalized_origin}" in
    "https://github.com/${DISTRIBUTION_REPO}") ;;
    "ssh://git@github.com/${DISTRIBUTION_REPO}") ;;
    "git@github.com:${DISTRIBUTION_REPO}") ;;
    *) fail "作業コピーの origin が配信リポジトリ (${DISTRIBUTION_REPO}) を指していません: ${origin_url}" ;;
esac

git -C "${DESTINATION}" rev-parse --verify --quiet HEAD > /dev/null \
    || fail "作業コピーに commit がありません (スナップショットを commit してから実行します)"

# --- 事前検証: tag 名と同名 tag の不在 ---------------------------------------------

if ! [[ "${TAG}" =~ ^[0-9]+\.[0-9]+\.[0-9]+-alpha\.[0-9]+$ ]]; then
    fail "検証用 tag は X.Y.Z-alpha.N 形式である必要があります: ${TAG}"
fi

if git -C "${DESTINATION}" rev-parse --verify --quiet "refs/tags/${TAG}" > /dev/null; then
    fail "同名の tag が作業コピーに既にあります (後始末で他人の tag を消さないため中止します): ${TAG}"
fi

echo "-- remote の tag 一覧 (実行前) --"
tags_remote_before="$(git -C "${DESTINATION}" ls-remote --tags --refs origin | awk '{print $2}' | sort)"
echo "${tags_remote_before:-(なし)}"
if printf '%s\n' "${tags_remote_before}" | grep -qxF "refs/tags/${TAG}"; then
    fail "同名の tag が remote に既にあります (後始末で他人の tag を消さないため中止します): ${TAG}"
fi

echo "-- local の tag 一覧 (実行前) --"
tags_local_before="$(git -C "${DESTINATION}" tag | sort)"
echo "${tags_local_before:-(なし)}"

# --- 事前検証: スナップショットが remote に届いているか -----------------------------

# tag の push は tag が指すオブジェクトを一緒に送るため、スナップショット本体を push し忘れて
# いても https 解決とビルドは成功してしまう。検証したいのは「配信リポジトリに置かれた
# スナップショット」なので、HEAD が remote の既定ブランチに含まれることをここで確かめる。
echo "-- 事前検証: スナップショットの remote 到達 --"
# `--no-tags` で tag の自動追従を止める。追従を許すと remote にある別の tag が local に増え、
# 後始末で実行前の local tag 一覧と突き合わせる判定が偽の失敗を出す。
git -C "${DESTINATION}" fetch --quiet --no-tags origin \
    || fail "origin から fetch できません (ネットワークと remote の設定を確認します)"

default_branch_ref="$(git -C "${DESTINATION}" ls-remote --symref origin HEAD \
    | awk '$1 == "ref:" { print $2; exit }')" \
    || fail "origin の既定ブランチを照会できません"
[ -n "${default_branch_ref}" ] \
    || fail "origin の既定ブランチを特定できません (スナップショットを push してから実行します)"
readonly DEFAULT_BRANCH="${default_branch_ref#refs/heads/}"
readonly DEFAULT_BRANCH_TRACKING="refs/remotes/origin/${DEFAULT_BRANCH}"

git -C "${DESTINATION}" rev-parse --verify --quiet "${DEFAULT_BRANCH_TRACKING}" > /dev/null \
    || fail "origin/${DEFAULT_BRANCH} が作業コピーにありません (スナップショットを push してから実行します)"

if ! git -C "${DESTINATION}" merge-base --is-ancestor HEAD "${DEFAULT_BRANCH_TRACKING}"; then
    fail "作業コピーの HEAD が origin/${DEFAULT_BRANCH} に含まれていません (スナップショットを push してから実行します)"
fi
echo "HEAD は origin/${DEFAULT_BRANCH} に含まれる"

# --- 後始末 ------------------------------------------------------------------------

# tag の作成・push は「着手した」時点で記録する。コマンドの戻りを待って記録すると、remote が
# tag を受領した直後に接続が切れた場合や、push の途中で中断された場合に、後始末が削除を試みない
# 窓ができる。事前検証で同名 tag の不在を確かめているため、着手しただけで削除に回っても
# 巻き添えは起きない。
tag_creation_started="no"
tag_push_started="no"
WORK_ROOT=""

cleanup() {
    local exit_code=$?
    set +e
    echo "-- 後始末 --"

    if [ "${tag_push_started}" = "yes" ]; then
        echo "remote の ${TAG} を削除します"
        if git -C "${DESTINATION}" push --delete origin "refs/tags/${TAG}"; then
            echo "remote の削除コマンドは成功"
        else
            # push が remote に届いていなければ「そんな ref は無い」で失敗する。ここでは
            # 成否を決めず、後段の照会で remote の実体を確かめる
            echo "警告: remote の削除コマンドが非ゼロ終了しました (この後の照会で状態を確かめます)" >&2
        fi
    fi
    if [ "${tag_creation_started}" = "yes" ]; then
        echo "local の ${TAG} を削除します"
        if git -C "${DESTINATION}" tag --delete "${TAG}"; then
            echo "local の削除コマンドは成功"
        else
            echo "警告: local の削除コマンドが非ゼロ終了しました (この後の照会で状態を確かめます)" >&2
        fi
    fi
    if [ -n "${WORK_ROOT}" ] && [ -d "${WORK_ROOT}" ]; then
        rm -rf "${WORK_ROOT}"
    fi

    # 後始末の成否は削除コマンドの終了コードではなく、削除後に照会した tag 一覧の実体で判定する。
    # 照会自体ができない場合は「消えたことを確認できていない」ため、失敗として扱う。
    local cleanup_verified="yes"
    local tags_remote_after tags_local_after remote_query_status local_query_status

    echo "-- remote の tag 一覧 (実行後) --"
    tags_remote_after="$(git -C "${DESTINATION}" ls-remote --tags --refs origin | awk '{print $2}' | sort)"
    remote_query_status=$?
    if [ "${remote_query_status}" -ne 0 ]; then
        cleanup_verified="no"
        echo "エラー: remote の tag 一覧を照会できません。${TAG} が remote に残っていないか手で確認してください" >&2
    else
        echo "${tags_remote_after:-(なし)}"
        if printf '%s\n' "${tags_remote_after}" | grep -qxF "refs/tags/${TAG}"; then
            cleanup_verified="no"
            echo "エラー: 検証用 tag が remote に残っています: ${TAG}" >&2
        elif [ "${tags_remote_before}" != "${tags_remote_after}" ]; then
            cleanup_verified="no"
            echo "エラー: remote の tag 一覧が実行前と一致しません。手で確認してください" >&2
        fi
    fi

    echo "-- local の tag 一覧 (実行後) --"
    tags_local_after="$(git -C "${DESTINATION}" tag | sort)"
    local_query_status=$?
    if [ "${local_query_status}" -ne 0 ]; then
        cleanup_verified="no"
        echo "エラー: local の tag 一覧を照会できません。${TAG} が作業コピーに残っていないか手で確認してください" >&2
    else
        echo "${tags_local_after:-(なし)}"
        if printf '%s\n' "${tags_local_after}" | grep -qxF "${TAG}"; then
            cleanup_verified="no"
            echo "エラー: 検証用 tag が作業コピーに残っています: ${TAG}" >&2
        elif [ "${tags_local_before}" != "${tags_local_after}" ]; then
            cleanup_verified="no"
            echo "エラー: local の tag 一覧が実行前と一致しません。手で確認してください" >&2
        fi
    fi

    if [ "${cleanup_verified}" = "yes" ]; then
        echo "tag 一覧は実行前と同一 (検証用 tag は残らず、他の tag にも触れていない)"
    elif [ "${exit_code}" -eq 0 ]; then
        exit_code=1
    fi

    if [ "${exit_code}" -eq 0 ]; then
        echo "結果: 成功"
    else
        echo "結果: 失敗 (exit ${exit_code})"
    fi
    echo "ログ: ${LOG_PATH}"
    exit "${exit_code}"
}
trap cleanup EXIT

# --- 検証用 tag の作成と push -------------------------------------------------------

echo "-- 検証用 tag の作成 --"
tag_creation_started="yes"
git -C "${DESTINATION}" tag "${TAG}"
tag_push_started="yes"
git -C "${DESTINATION}" push origin "refs/tags/${TAG}"

# --- 一時消費者パッケージの生成とビルド ---------------------------------------------

WORK_ROOT="$(mktemp -d)"
readonly CONSUMER_DIR="${WORK_ROOT}/${CONSUMER_NAME}"
mkdir -p "${CONSUMER_DIR}/Sources/${CONSUMER_NAME}"

cat > "${CONSUMER_DIR}/Package.swift" <<MANIFEST
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "${CONSUMER_NAME}",
    platforms: [.iOS(.v17)],
    products: [.library(name: "${CONSUMER_NAME}", targets: ["${CONSUMER_NAME}"])],
    dependencies: [
        .package(url: "${DISTRIBUTION_URL}", exact: "${TAG}")
    ],
    targets: [
        .target(
            name: "${CONSUMER_NAME}",
            dependencies: [.product(name: "KsDialogs", package: "KsDialogs-SPM")])
    ]
)
MANIFEST

cat > "${CONSUMER_DIR}/Sources/${CONSUMER_NAME}/Consumer.swift" <<'SOURCE'
import KsDialogs

// 公開型を参照して product の配線まで確かめる (import だけでは product のリンクを検証できない)。
public enum ConsumerCheck {
    public static let options: Any.Type = DialogOptions.self
    public static let alignment: Any.Type = DialogAlignment.self
}
SOURCE

echo "-- 一時消費者パッケージ --"
echo "場所: ${CONSUMER_DIR}"
cat "${CONSUMER_DIR}/Package.swift"

echo "-- iOS Simulator 向けビルド --"
(
    cd "${CONSUMER_DIR}"
    xcodebuild build \
        -scheme "${CONSUMER_NAME}" \
        -destination 'generic/platform=iOS Simulator' \
        -derivedDataPath "${WORK_ROOT}/DerivedData"
)

echo "-- Package.resolved --"
cat "${CONSUMER_DIR}/Package.resolved" 2>/dev/null \
    || cat "${CONSUMER_DIR}/.swiftpm/xcode/package.xcworkspace/xcshareddata/swiftpm/Package.resolved" 2>/dev/null \
    || echo "(Package.resolved が見つかりません)"
