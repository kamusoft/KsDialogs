#!/bin/bash
# KMP 消費者検証のフィード準備。
#
# dry-run の参照先を次の 3 段で作る。
#
#   1. 配信リポジトリと同じファイル配置のスナップショットを作業ディレクトリへ同期し、
#      commit して version と同名の tag を打つ (push はしない)
#   2. 本体の Android ライブラリを作業ディレクトリ内のローカル Maven リポジトリへ発行する
#      (--reference が与えられていれば、その内容を参照先にしてこの段を飛ばす)
#   3. KMP の 5 publication を同じリポジトリへ発行する。発行 metadata の Swift 参照は
#      1 の clone を指す file:// URL + exact で、消費者の iOS ホスト側と同じ URL になる
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>]
#                   [--reference <local maven repository>] [--work <dir>]
#
# 標準出力の最終行に、参照先 (ローカル Maven リポジトリ) の絶対パスを出す。
# その 1 つ前の行に Swift 参照の file:// URL を出す。
# smoke では公開レジストリを参照するため何も準備せず、参照先も出力しない。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="kmp"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"
# shellcheck source=../lib/android-sdk.sh
. "${REPO_ROOT}/verification/lib/android-sdk.sh"
# shellcheck source=../lib/gradle-publish.sh
. "${REPO_ROOT}/verification/lib/gradle-publish.sh"

readonly DISTRIBUTION_REMOTE="https://github.com/kamusoft/KsDialogs-SPM.git"
# スナップショットの clone に打つ commit の作者。手元の git 設定に依存させない。
readonly SNAPSHOT_AUTHOR_NAME="ksdialogs-verification"
readonly SNAPSHOT_AUTHOR_EMAIL="verification@localhost"

ksv_parse_args "$@"
# SDK が無ければ発行の前に落とす (本体ビルドを走らせてから分かるより早く理由が出る)。
ksv_ensure_android_home

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (Maven Central と配信リポジトリの tag) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

# 3 段目の発行が本体側の合成 Swift マニフェストを書き換えるため、戻せる状態かをここで
# 確かめる (1・2 段を走らせてから分かるより早く理由が出る。発行の直前でも同じ検査を行う)。
ksv_require_pristine_swiftpm_locks

work="$(ksv_prepare_work "${KSV_WORK}" "kmp")"

# 1. スナップショットの clone に tag を打つ
snapshot="${work}/KsDialogs-SPM"
mkdir -p "${snapshot}"

if [ ! -d "${snapshot}/.git" ]; then
    git -C "${snapshot}" init --quiet
fi

if git -C "${snapshot}" remote get-url origin >/dev/null 2>&1; then
    git -C "${snapshot}" remote set-url origin "${DISTRIBUTION_REMOTE}"
else
    git -C "${snapshot}" remote add origin "${DISTRIBUTION_REMOTE}"
fi

"${REPO_ROOT}/scripts/spm-snapshot/sync-snapshot.sh" "${snapshot}"

git -C "${snapshot}" add -A
git -C "${snapshot}" \
    -c "user.name=${SNAPSHOT_AUTHOR_NAME}" \
    -c "user.email=${SNAPSHOT_AUTHOR_EMAIL}" \
    commit --quiet --allow-empty -m "snapshot ${KSV_VERSION}"
# 同じ作業ディレクトリで version を変えて回せるよう、既存の tag は打ち直す。
git -C "${snapshot}" tag -f "${KSV_VERSION}" >/dev/null

swift_package_url="file://${snapshot}"

# 2. Android ライブラリの発行 (準備済みの参照先が与えられていれば飛ばす)
if [ -n "${KSV_REFERENCE}" ]; then
    [ -d "${KSV_REFERENCE}" ] || ksv_fail "参照先がディレクトリではありません: ${KSV_REFERENCE}"
    maven="$(cd "${KSV_REFERENCE}" && pwd -P)"
    echo "準備済みの参照先を使うため、本体の Android ライブラリの発行は行わない: ${maven}"
    ksv_require_maven_artifact "${maven}" "ksdialogs-core" "${KSV_VERSION}"
    ksv_require_maven_artifact "${maven}" "ksdialogs" "${KSV_VERSION}"
else
    maven="$(ksv_reset_dir "${work}" maven)"
    ksv_publish_android "${maven}" "${KSV_VERSION}"
fi

# 3. KMP の発行 (準備済みの参照先が与えられていても行う。Swift 参照の URL は
#    公開されていない tag を指せないため、この場で file:// へ上書きして発行する)
ksv_publish_kmp "${maven}" "${KSV_VERSION}" "${swift_package_url}"

echo "${swift_package_url}"
echo "${maven}"
