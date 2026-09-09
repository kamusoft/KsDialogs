#!/bin/bash
# KMP 消費者のビルドと依存検査。
#
# このディレクトリを作業ディレクトリへコピーし、コピーの中で次を順に通す。
# 前段が失敗すれば後段には進まない。
#
#   1. Android アプリの release variant
#   2. 合成 package の再生成 (発行 metadata から) と、共有モジュールの iOS Simulator 向け
#      Release framework のリンク
#   3. iOS アプリの iOS Simulator 向け Release ビルド (合成 package と登録 API の
#      ローカル Swift package をリンク)
#
# 追跡している合成 package と VerificationApp/Package.swift は Swift 参照が配信リポジトリの
# https URL になる形の非解決 fixture であり、mode で変わる参照はコピーの中でだけ書き換える。
# リポジトリ内の verification/kmp/ はこのスクリプトの実行で変化しない。
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <local maven repository>] [--work <dir>]
#
# --reference を与えると、その参照先を本体の Android ライブラリの発行物として使い、
# その発行だけを飛ばす (KMP の発行とスナップショット clone の tag は行う)。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="kmp"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"
# shellcheck source=../lib/android-sdk.sh
. "${REPO_ROOT}/verification/lib/android-sdk.sh"

readonly DISTRIBUTION_URL="https://github.com/kamusoft/KsDialogs-SPM"
readonly CENTRAL_URL="https://repo.maven.apache.org/maven2"
# 共有モジュールの iOS ターゲットと、その依存解決を見る configuration。
readonly IOS_TARGETS="iosArm64 iosSimulatorArm64 iosX64"

ksv_parse_args "$@"
ksv_ensure_android_home

work="$(ksv_prepare_work "${KSV_WORK}" "kmp")"

reference=""
if [ "${KSV_MODE}" = "dry-run" ]; then
    prepare_args=(--mode "${KSV_MODE}" --version "${KSV_VERSION}" --work "${work}")
    if [ -n "${KSV_REFERENCE}" ]; then
        prepare_args+=(--reference "${KSV_REFERENCE}")
    fi
    # フィード準備の出力はコマンド置換で最終行 (参照先) だけを取るため、そのままでは準備の
    # ログが残らず失敗理由が見えない。標準エラーにも流して CI のログに残す。
    prepared="$("${SCRIPT_DIR}/prepare-feed.sh" "${prepare_args[@]}" | tee /dev/stderr | tail -n 2)"
    swift_package_url="$(printf '%s\n' "${prepared}" | head -n 1)"
    reference="$(printf '%s\n' "${prepared}" | tail -n 1)"
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    dependency=".package(url: \"${swift_package_url}\", exact: \"${KSV_VERSION}\")"
else
    swift_package_url="${DISTRIBUTION_URL}"
    dependency=".package(url: \"${DISTRIBUTION_URL}\", exact: \"${KSV_VERSION}\")"
fi

# 作業コピー。生成物 (build / .gradle / .kotlin) は持ち込まず、追跡している内容だけを写す。
# 合成 package の subpackage は 2 段目の再生成が作り直すため写さない。subpackage の
# ディレクトリ名は version を含むので、追跡している fixture の version と実行の version が
# 違うと、写した方が残って subpackage が 2 つになる。
consumer="$(ksv_reset_dir "${work}" consumer)"
rsync -a \
    --exclude 'build/' \
    --exclude '.gradle/' \
    --exclude '.kotlin/' \
    --exclude 'iosApp/KotlinMultiplatformLinkedPackage/subpackages/' \
    "${SCRIPT_DIR}/" "${consumer}/"

gradle_args=(
    "--console=plain"
    "-Pksdialogs.mode=${KSV_MODE}"
    "-Pksdialogs.version=${KSV_VERSION}"
    # 作業コピーからはリポジトリ内の相対位置が成り立たないため、共有カタログの位置を渡す。
    "-Pksdialogs.catalog=${REPO_ROOT}/android/gradle/libs.versions.toml"
)
if [ "${KSV_MODE}" = "dry-run" ]; then
    gradle_args+=("-Pksdialogs.reference=${reference}")
fi

# iOS ホスト側のローカル Swift package のマニフェストを mode に応じて作り直す。
python3 "${REPO_ROOT}/verification/lib/render-template.py" \
    --template "${consumer}/iosApp/VerificationApp/Package.swift.template" \
    --output "${consumer}/iosApp/VerificationApp/Package.swift" \
    --dependency "${dependency}"

echo "==== 1 段目: Android アプリの Release ビルド ===="
(cd "${consumer}" && ./gradlew "${gradle_args[@]}" :androidApp:assembleRelease)

echo "==== 2 段目: 合成 package の再生成と共有モジュールの framework リンク ===="
# 合成 package を発行 metadata から作り直す。リンクのタスクはこの再生成を行わないため、
# Xcode project の統合タスク経由で先に走らせる。
(cd "${consumer}" && XCODEPROJ_PATH="${consumer}/iosApp/VerificationKmp.xcodeproj" \
    ./gradlew "${gradle_args[@]}" :shared:integrateLinkagePackage)
(cd "${consumer}" && ./gradlew "${gradle_args[@]}" :shared:linkReleaseFrameworkIosSimulatorArm64)

echo "==== 3 段目: iOS アプリの Release ビルド ===="
xcodebuild_log="${work}/xcodebuild.log"
# Xcode のビルドフェーズが呼ぶ Gradle へ、mode と参照先を環境変数で渡す。
export KSDIALOGS_MODE="${KSV_MODE}"
export KSDIALOGS_REFERENCE="${reference}"
export KSDIALOGS_VERSION="${KSV_VERSION}"
export KSDIALOGS_CATALOG="${REPO_ROOT}/android/gradle/libs.versions.toml"
xcodebuild \
    -project "${consumer}/iosApp/VerificationKmp.xcodeproj" \
    -scheme VerificationKmp \
    -configuration Release \
    -destination 'generic/platform=iOS Simulator' \
    -derivedDataPath "${work}/DerivedData" \
    CODE_SIGNING_ALLOWED=NO \
    build 2>&1 | tee "${xcodebuild_log}"

echo "==== 依存検査 ===="
trees="$(ksv_reset_dir "${work}" trees)"

android_tree="${trees}/androidApp.txt"
if ! (cd "${consumer}" && ./gradlew "${gradle_args[@]}" -q \
    :androidApp:dependencies --configuration releaseRuntimeClasspath) > "${android_tree}"; then
    cat "${android_tree}" >&2
    ksv_fail "依存ツリーの取得に失敗しました (:androidApp:dependencies)"
fi

check_args=(
    --expected-version "${KSV_VERSION}"
    --expected-url "${swift_package_url}"
    --android-tree "${android_tree}"
    --linkage-package "${consumer}/iosApp/KotlinMultiplatformLinkedPackage"
    --local-manifest "${consumer}/iosApp/VerificationApp/Package.swift"
    --xcodebuild-log "${xcodebuild_log}"
)

for target in ${IOS_TARGETS}; do
    tree="${trees}/${target}.txt"
    if ! (cd "${consumer}" && ./gradlew "${gradle_args[@]}" -q \
        :shared:dependencies --configuration "${target}CompileKlibraries") > "${tree}"; then
        cat "${tree}" >&2
        ksv_fail "依存ツリーの取得に失敗しました (:shared:dependencies ${target})"
    fi
    check_args+=(--ios-tree "${target}=${tree}")
done

if [ "${KSV_MODE}" = "dry-run" ]; then
    check_args+=(--metadata "${reference}/jp/kamusoft/ksdialogs-kmp/${KSV_VERSION}/ksdialogs-kmp-${KSV_VERSION}-swiftpm-metadata.json")
fi

python3 "${SCRIPT_DIR}/check-dependencies.py" "${check_args[@]}" | ksv_evidence "解決版と取得元"

if [ "${KSV_MODE}" = "dry-run" ]; then
    echo "${reference}"
else
    echo "${CENTRAL_URL}"
fi
