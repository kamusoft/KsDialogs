#!/bin/bash
# Android 消費者検証のフィード準備。
#
# dry-run の参照先となる作業ディレクトリ内のローカル Maven リポジトリへ、本体ライブラリ
# (jp.kamusoft:ksdialogs-core / jp.kamusoft:ksdialogs) を指定 version で発行する。
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>] [--work <dir>]
#
# 標準出力の最終行に、準備した参照先 (ローカル Maven リポジトリ) の絶対パスを出す。
# smoke では Maven Central を参照するため何も準備せず、参照先も出力しない。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="android"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"
# shellcheck source=../lib/android-sdk.sh
. "${REPO_ROOT}/verification/lib/android-sdk.sh"
# shellcheck source=../lib/gradle-publish.sh
. "${REPO_ROOT}/verification/lib/gradle-publish.sh"

ksv_parse_args "$@"
# SDK が無ければ発行の前に落とす (本体ビルドを走らせてから分かるより早く理由が出る)。
ksv_ensure_android_home

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (Maven Central) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

work="$(ksv_prepare_work "${KSV_WORK}" "android")"
maven="$(ksv_reset_dir "${work}" maven)"

ksv_publish_android "${maven}" "${KSV_VERSION}"

echo "${maven}"
