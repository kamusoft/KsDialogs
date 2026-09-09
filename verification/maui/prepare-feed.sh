#!/bin/bash
# .NET MAUI 消費者検証のフィード準備。
#
# facade (KsDialogs.Maui) と binding 2 件を指定 version で pack し、作業ディレクトリ内の
# ローカルフォルダフィードへ置く。
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>] [--work <dir>]
#
# 標準出力の最終行に、準備したフォルダフィードの絶対パスを出す。
# smoke では nuget.org を参照するため何も準備せず、参照先も出力しない。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="maui"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

ksv_parse_args "$@"

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (nuget.org) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

work="$(ksv_prepare_work "${KSV_WORK}" "maui")"
feed="$(ksv_reset_dir "${work}" feed)"

# Android binding の pack は aar を作るために本体の Gradle ビルドを呼ぶ。その実行 JVM を
# 明示できるよう、JAVA_HOME があれば .NET Android SDK へ渡す (CI の JDK 選択に追随する)。
pack_args=()
if [ -n "${JAVA_HOME:-}" ]; then
    pack_args+=("-p:JavaSdkDirectory=${JAVA_HOME}")
fi

projects=(
    "maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj"
    "maui/android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj"
    "maui/KsDialogs.Maui/KsDialogs.Maui.csproj"
)
for project in "${projects[@]}"; do
    dotnet pack "${REPO_ROOT}/${project}" \
        -c Release \
        -p:Version="${KSV_VERSION}" \
        -o "${feed}" \
        ${pack_args[@]+"${pack_args[@]}"}
done

for package in KsDialogs.Maui KsDialogs.Binding.iOS KsDialogs.Binding.Android; do
    [ -f "${feed}/${package}.${KSV_VERSION}.nupkg" ] \
        || ksv_fail "pack されていません: ${package}.${KSV_VERSION}.nupkg"
done

echo "${feed}"
