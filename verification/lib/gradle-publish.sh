# shellcheck shell=bash
# ローカル Maven リポジトリへの発行 (Android と KMP の消費者検証が共有する)。
#
# 発行先は ~/.m2/repository ではなく作業ディレクトリ内のリポジトリにする。前回の実行や
# 別の作業の残留物が解決に混ざると、参照先に無い version が解決できてしまい「ローカル参照先
# からのみ取得される」ことを確かめられなくなる。位置の指定はシステムプロパティ
# maven.repo.local で行う (Maven と同じ規則で mavenLocal の位置が決まる)。
#
# source する側は REPO_ROOT と verification-args.sh (ksv_fail) を先に用意しておく。

# 本体の Android ライブラリ (jp.kamusoft:ksdialogs-core / jp.kamusoft:ksdialogs) を
# 指定 version で発行する。
#
#   ksv_publish_android <ローカル Maven リポジトリ> <version>
ksv_publish_android() {
    local maven="$1"
    local version="$2"

    (cd "${REPO_ROOT}/android" && ./gradlew --console=plain \
        -Pversion="${version}" \
        "-Dmaven.repo.local=${maven}" \
        publishToMavenLocal)

    ksv_require_maven_artifact "${maven}" "ksdialogs-core" "${version}"
    ksv_require_maven_artifact "${maven}" "ksdialogs" "${version}"
}

# KMP の配布物 (5 publication) を、Swift 参照の URL を上書きして指定 version で発行する。
#
#   ksv_publish_kmp <ローカル Maven リポジトリ> <version> <Swift 参照の URL>
#
# 発行 metadata に載る Swift 参照はリリース版では配信リポジトリの https になるが、
# 検証用の version の tag は公開されないため、tag を打ったローカル clone の file:// URL へ
# 上書きして発行する。
#
# この発行は本体側の合成 Swift マニフェストを発行先の URL で書き換えるため、発行の前に
# 2 本が HEAD と同じであることを確かめ、発行の成否によらず終了までに追跡状態へ戻す。
ksv_publish_kmp() {
    local maven="$1"
    local version="$2"
    local swift_package_url="$3"

    ksv_require_pristine_swiftpm_locks
    ksv_arm_swiftpm_lock_restore

    (cd "${REPO_ROOT}/kmp" && ./gradlew --console=plain \
        -Pversion="${version}" \
        "-Pksdialogs.swiftPackageUrl=${swift_package_url}" \
        "-Dmaven.repo.local=${maven}" \
        publishToMavenLocal)

    local artifact
    for artifact in \
        ksdialogs-kmp \
        ksdialogs-kmp-android \
        ksdialogs-kmp-iosarm64 \
        ksdialogs-kmp-iossimulatorarm64 \
        ksdialogs-kmp-iosx64
    do
        ksv_require_maven_artifact "${maven}" "${artifact}" "${version}"
    done

    ksv_disarm_swiftpm_lock_restore
}

# kmp/ の発行が書き換える本体側の合成 Swift マニフェスト。
KSV_SWIFTPM_LOCKS=(
    "kmp/.swiftpm-locks/default/swiftImport/subpackages/_ksdialogs-kmp/Package.swift"
    "kmp/.swiftpm-locks/default/swiftImport/subpackages/_ksdialogs_kmp/Package.swift"
)

# 発行を始めてよい状態か (2 本を HEAD から復元できるか) を確かめる。
#
# 復元は HEAD の内容で上書きするため、実行前から手が入っていた変更を巻き添えで捨てる。
# 消費者検証は本体の作業ツリーを預かる立場ではないので、変更があれば書き換える前に止める。
# Git 管理下でなければ復元手段が無く、書き換えたまま終わるので同じく止める。
ksv_require_pristine_swiftpm_locks() {
    git -C "${REPO_ROOT}" rev-parse --is-inside-work-tree >/dev/null 2>&1 \
        || ksv_fail "Git 管理下ではないため、発行で書き換わる合成 Swift マニフェストを戻せません: ${REPO_ROOT}"

    local lock
    for lock in "${KSV_SWIFTPM_LOCKS[@]}"; do
        git -C "${REPO_ROOT}" ls-files --error-unmatch "${lock}" >/dev/null 2>&1 \
            || ksv_fail "合成 Swift マニフェストが追跡されていません (復元先が分かりません): ${lock}"
    done

    # HEAD との比較なので、作業ツリーの変更と index に載せた変更の両方を検出する。
    git -C "${REPO_ROOT}" diff --quiet HEAD -- "${KSV_SWIFTPM_LOCKS[@]}" \
        || ksv_fail "合成 Swift マニフェストに HEAD からの変更があります。発行はこの 2 本を書き換えて HEAD の内容へ戻すため、変更を退避または commit してから実行してください: ${KSV_SWIFTPM_LOCKS[*]}"
}

# 復元を EXIT に仕掛ける。発行が途中で落ちても、書き換わった 2 本を残さない。
ksv_arm_swiftpm_lock_restore() {
    # 呼び出し元が張った EXIT trap を潰さないよう、その本体を控えて後段で実行する。
    # trap -p の出力は trap -- '<本体>' EXIT の形で、本体はシェルの引用形式になっている。
    # 引用符の復号は文字列置換ではなく eval に任せ、本体に ' が含まれていても原文のまま控える。
    local declared
    declared="$(trap -p EXIT)"
    KSV_PREVIOUS_EXIT_TRAP=""
    if [ -n "${declared}" ]; then
        local -a declared_words=()
        eval "declared_words=(${declared#trap -- })"
        KSV_PREVIOUS_EXIT_TRAP="${declared_words[0]}"
    fi

    trap 'ksv_restore_swiftpm_locks; if [ -n "${KSV_PREVIOUS_EXIT_TRAP:-}" ]; then eval "${KSV_PREVIOUS_EXIT_TRAP}"; fi' EXIT
}

# 成功経路の復元。EXIT を待たずに戻し、控えた trap を戻して仕掛けを解く。
ksv_disarm_swiftpm_lock_restore() {
    trap - EXIT
    if [ -n "${KSV_PREVIOUS_EXIT_TRAP:-}" ]; then
        # 控えた本体をそのまま trap へ戻すため、ここは展開してから渡すのが意図どおり。
        # shellcheck disable=SC2064
        trap "${KSV_PREVIOUS_EXIT_TRAP}" EXIT
    fi
    KSV_PREVIOUS_EXIT_TRAP=""

    ksv_restore_swiftpm_locks
}

# 本体側の合成 Swift マニフェストを追跡状態へ戻す。
#
# kmp/ の発行は Swift 参照の URL をこの 2 本へ書き戻すため、手元で実行すると作業ツリーに
# 発行先の絶対パスが残る。消費者検証は本体の追跡物を書き換えないので、発行のたびに戻す。
# index に載った内容ではなく HEAD の内容で戻す (発行前の検査で両者の一致は確かめてある)。
ksv_restore_swiftpm_locks() {
    git -C "${REPO_ROOT}" checkout HEAD -- "${KSV_SWIFTPM_LOCKS[@]}"
    echo "本体側の合成 Swift マニフェスト ${#KSV_SWIFTPM_LOCKS[@]} 本を追跡状態へ戻した"
}

# 発行された座標が参照先に置かれたことを確かめる。座標がずれたまま消費者ビルドへ進むと、
# 解決できた事実が「要求した版を解決できた」証拠にならなくなる。
ksv_require_maven_artifact() {
    local maven="$1"
    local artifact="$2"
    local version="$3"

    local published="${maven}/jp/kamusoft/${artifact}/${version}"
    [ -f "${published}/${artifact}-${version}.pom" ] \
        || ksv_fail "要求した version が発行されていません: jp.kamusoft:${artifact}:${version}"
}
