#!/bin/bash
# 公開レジストリへの反映待ち。
#
# 使い方:
#   scripts/release/wait-for-registries.sh <version>
#   scripts/release/wait-for-registries.sh --selftest
#
# publish 直後は、Maven Central も nuget.org も配信元へ同期されるまで数分から数十分かかる。
# 反映前に消費者検証 (smoke) を走らせると、利用者から解決できないのか単に間に合っていない
# だけなのかが区別できないため、次の 10 件すべてが取得可能になるまで待ってから先へ進める。
#
#   Maven Central  Android 2 座標 (ksdialogs-core / ksdialogs) と
#                  KMP 5 publication (ksdialogs-kmp root / -android / iOS 3 ターゲット) の pom
#   nuget.org      KsDialogs.Maui / KsDialogs.Binding.iOS /
#                  KsDialogs.Binding.Android の 3 Package ID
#
# KMP は root の pom だけを待つと、ターゲット別 publication が未反映のまま smoke へ進んで
# しまう。利用者のビルドはターゲット別 publication まで解決するため、待つ対象も同じ粒度に
# 揃える (この待ちの判別力は、待ち対象と利用者が解決する対象が一致することで決まる)。
# 配信リポジトリの tag は publish 段で push して存在を検査済みなので待ち対象に入れない。
#
# nuget.org は flat container の index.json に含まれる version の一覧で判定する。index の
# version は小文字へ正規化されるため、比較も小文字で行う。
#
# 待機の間隔と上限は環境変数で上書きできる (テストと運用の調整用):
#   KSR_POLL_INTERVAL_SECONDS  ポーリング間隔 (既定 30)
#   KSR_POLL_TIMEOUT_SECONDS   上限 (既定 2700 = 45 分)
#
# ネットワークへ出るのは実行本番だけで、--selftest は取得関数をモックへ差し替えて
# 待ち対象の組み立てと決着の判定だけを検査する。

set -euo pipefail

readonly MAVEN_CENTRAL_BASE_URL="https://repo1.maven.org/maven2"
readonly MAVEN_GROUP_PATH="jp/kamusoft"

# Maven Central 側の待ち対象。Android 枠 2 件と KMP 枠 5 件。
readonly MAVEN_ARTIFACT_IDS=(
    "ksdialogs-core"
    "ksdialogs"
    "ksdialogs-kmp"
    "ksdialogs-kmp-android"
    "ksdialogs-kmp-iosarm64"
    "ksdialogs-kmp-iossimulatorarm64"
    "ksdialogs-kmp-iosx64"
)

readonly NUGET_FLAT_CONTAINER_URL="https://api.nuget.org/v3-flatcontainer"
# flat container の URL は Package ID を小文字にしたものを使う。
readonly NUGET_PACKAGE_IDS=(
    "ksdialogs.maui"
    "ksdialogs.binding.ios"
    "ksdialogs.binding.android"
)

# 待ち対象の総数。増減を無音で見逃さないよう、実行の冒頭に出す。
readonly TOTAL_TARGETS=$(( ${#MAVEN_ARTIFACT_IDS[@]} + ${#NUGET_PACKAGE_IDS[@]} ))

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <version>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# --- URL の組み立て ----------------------------------------------------------------

maven_pom_url() {
    local artifact_id="$1" version="$2"
    echo "${MAVEN_CENTRAL_BASE_URL}/${MAVEN_GROUP_PATH}/${artifact_id}/${version}/${artifact_id}-${version}.pom"
}

nuget_index_url() {
    echo "${NUGET_FLAT_CONTAINER_URL}/$1/index.json"
}

# --- 取得 -----------------------------------------------------------------------------
#
# 自己テストはこの 2 つだけを差し替える。URL の組み立てと決着の判定は差し替えの外にある。

# URL への HEAD のステータスコードを 1 行で返す。到達できなければ 000。
http_head_status() {
    curl --silent --show-error --location --head --output /dev/null \
        --connect-timeout 30 --max-time 120 --write-out '%{http_code}' "$1" || echo "000"
}

# URL の本文を返す。取得できなければ空。
http_get_body() {
    curl --silent --show-error --location \
        --connect-timeout 30 --max-time 120 "$1" || echo ""
}

# --- 反映の判定 ------------------------------------------------------------------------

# Maven Central に当該座標・当該 version の pom があれば 0。
maven_central_ready() {
    local artifact_id="$1" version="$2"
    [ "$(http_head_status "$(maven_pom_url "${artifact_id}" "${version}")")" = "200" ]
}

# nuget.org の当該 Package ID の index に当該 version が含まれれば 0。
nuget_ready() {
    local package_id="$1" version="$2"
    local body
    body="$(http_get_body "$(nuget_index_url "${package_id}")")"
    [ -n "${body}" ] || return 1
    printf '%s' "${body}" | KSR_WANTED_VERSION="${version}" python3 -c '
import json
import os
import sys

wanted = os.environ["KSR_WANTED_VERSION"].lower()
try:
    payload = json.load(sys.stdin)
except ValueError:
    sys.exit(1)
versions = payload.get("versions")
if not isinstance(versions, list):
    sys.exit(1)
sys.exit(0 if wanted in [str(v).lower() for v in versions] else 1)
'
}

# 10 件すべてが取得可能になるまで待つ。取得できたものは以後照会しない。
wait_for_all() {
    local version="$1"
    local interval="${KSR_POLL_INTERVAL_SECONDS:-30}"
    local timeout="${KSR_POLL_TIMEOUT_SECONDS:-2700}"
    local deadline=$(( SECONDS + timeout ))

    echo "反映を待ちます (version ${version}、対象 ${TOTAL_TARGETS} 件、${interval} 秒間隔、上限 ${timeout} 秒)"

    local -a maven_done nuget_done
    local index
    for index in "${!MAVEN_ARTIFACT_IDS[@]}"; do maven_done[index]=0; done
    for index in "${!NUGET_PACKAGE_IDS[@]}"; do nuget_done[index]=0; done

    local -a pending
    local artifact_id package_id
    while :; do
        pending=()

        for index in "${!MAVEN_ARTIFACT_IDS[@]}"; do
            [ "${maven_done[index]}" -eq 0 ] || continue
            artifact_id="${MAVEN_ARTIFACT_IDS[index]}"
            if maven_central_ready "${artifact_id}" "${version}"; then
                maven_done[index]=1
                echo "Maven Central に反映されました: ${artifact_id}:${version}"
            else
                pending+=("maven/${artifact_id}")
            fi
        done

        for index in "${!NUGET_PACKAGE_IDS[@]}"; do
            [ "${nuget_done[index]}" -eq 0 ] || continue
            package_id="${NUGET_PACKAGE_IDS[index]}"
            if nuget_ready "${package_id}" "${version}"; then
                nuget_done[index]=1
                echo "nuget.org に反映されました: ${package_id} ${version}"
            else
                pending+=("nuget/${package_id}")
            fi
        done

        if [ "${#pending[@]}" -eq 0 ]; then
            echo "${TOTAL_TARGETS} 件すべてが取得可能になりました: ${version}"
            return 0
        fi

        if [ "${SECONDS}" -ge "${deadline}" ]; then
            fail "反映を待ちきれませんでした (上限 ${timeout} 秒、未反映 ${#pending[@]} 件: ${pending[*]})"
        fi

        echo "待機中 (未反映 ${#pending[@]} 件: ${pending[*]})"
        sleep "${interval}"
    done
}

# --- 自己テスト ----------------------------------------------------------------------
#
# http_head_status / http_get_body をモックへ差し替え、待ち対象の組み立てと決着の判定を
# 検査する。ネットワークへは出ない。

selftest() {
    SELFTEST_WORK="$(mktemp -d)"
    trap 'rm -rf "${SELFTEST_WORK}"' EXIT
    local work="${SELFTEST_WORK}"

    MOCK_READY="${work}/ready"
    MOCK_CALLS="${work}/calls"
    : > "${MOCK_READY}"
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

    # 反映済みとして扱う URL の一覧を仕込む。
    arrange() {
        : > "${MOCK_CALLS}"
        printf '%s\n' "$@" > "${MOCK_READY}"
    }

    # 仕込んだ一覧に URL があれば反映済みとして応答する。
    http_head_status() {
        echo "$1" >> "${MOCK_CALLS}"
        if grep -Fxq "$1" "${MOCK_READY}"; then echo "200"; else echo "404"; fi
    }

    http_get_body() {
        echo "$1" >> "${MOCK_CALLS}"
        if grep -Fxq "$1" "${MOCK_READY}"; then
            echo '{"versions":["0.0.1","1.2.3-BETA.4"]}'
        else
            echo '{"versions":["0.0.1"]}'
        fi
    }

    # 全対象の URL を 1 行 1 件で並べる。
    all_urls() {
        local id
        for id in "${MAVEN_ARTIFACT_IDS[@]}"; do maven_pom_url "${id}" "$1"; done
        for id in "${NUGET_PACKAGE_IDS[@]}"; do nuget_index_url "${id}"; done
    }

    echo "[待ち対象]"
    check "$([ "${TOTAL_TARGETS}" = "10" ] && echo 0 || echo 1)" \
        "待ち対象は Maven 7 件 + NuGet 3 件の 10 件" "${TOTAL_TARGETS}"
    check "$([ "$(maven_pom_url ksdialogs-kmp-iosarm64 1.2.3)" = "https://repo1.maven.org/maven2/jp/kamusoft/ksdialogs-kmp-iosarm64/1.2.3/ksdialogs-kmp-iosarm64-1.2.3.pom" ] && echo 0 || echo 1)" \
        "Maven の pom の URL" "$(maven_pom_url ksdialogs-kmp-iosarm64 1.2.3)"
    check "$([ "$(nuget_index_url ksdialogs.maui)" = "https://api.nuget.org/v3-flatcontainer/ksdialogs.maui/index.json" ] && echo 0 || echo 1)" \
        "nuget.org の index の URL" "$(nuget_index_url ksdialogs.maui)"

    echo "[全件が反映済み]"
    local urls
    urls="$(all_urls 1.2.3-beta.4)"
    # shellcheck disable=SC2046 # 1 行 1 URL を個別の引数として渡す
    arrange $(printf '%s ' ${urls})
    local output
    output="$(KSR_POLL_INTERVAL_SECONDS=0 wait_for_all 1.2.3-beta.4)"
    check "$(if [ "${output#*10 件すべて}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "10 件すべてが反映済みなら 1 巡で抜ける" "${output}"
    check "$([ "$(sort -u "${MOCK_CALLS}" | wc -l | tr -d ' ')" = "10" ] && echo 0 || echo 1)" \
        "照会した URL は 10 種" "$(wc -l < "${MOCK_CALLS}" | tr -d ' ')"

    echo "[版の大小文字]"
    check "$(if ( arrange "$(nuget_index_url ksdialogs.maui)"; nuget_ready ksdialogs.maui 1.2.3-beta.4 ); then echo 0; else echo 1; fi)" \
        "index の大文字表記でも同じ version として一致する"
    check "$(if ( arrange "$(nuget_index_url ksdialogs.maui)"; nuget_ready ksdialogs.maui 9.9.9 ); then echo 1; else echo 0; fi)" \
        "index に無い version は未反映"

    echo "[1 件でも未反映なら待つ]"
    local partial
    partial="$(all_urls 1.2.3-beta.4 | grep -v "ksdialogs-kmp-iosx64")"
    # shellcheck disable=SC2046 # 1 行 1 URL を個別の引数として渡す
    arrange $(printf '%s ' ${partial})
    output="$(KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=0 wait_for_all 1.2.3-beta.4 2>&1)" || true
    check "$(if [ "${output#*ksdialogs-kmp-iosx64}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "KMP のターゲット別 publication 1 件の未反映で待ち続ける" "${output}"
    check "$(if ( KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=0 wait_for_all 1.2.3-beta.4 > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "上限を過ぎれば失敗する"

    partial="$(all_urls 1.2.3-beta.4 | grep -v "ksdialogs.binding.ios")"
    # shellcheck disable=SC2046 # 1 行 1 URL を個別の引数として渡す
    arrange $(printf '%s ' ${partial})
    output="$(KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=0 wait_for_all 1.2.3-beta.4 2>&1)" || true
    check "$(if [ "${output#*ksdialogs.binding.ios}" != "${output}" ]; then echo 0; else echo 1; fi)" \
        "NuGet 1 件の未反映で待ち続ける" "${output}"

    if [ "${failures}" -eq 0 ]; then
        echo "失敗なし"
        return 0
    fi
    echo "失敗 ${failures} 件" >&2
    return 1
}

# --- 入口 ----------------------------------------------------------------------------

main() {
    if [ $# -ne 1 ]; then
        usage
        exit 2
    fi
    case "$1" in
        --selftest) selftest ;;
        -h|--help)  usage; exit 0 ;;
        -*)         usage; exit 2 ;;
        *)          wait_for_all "$1" ;;
    esac
}

main "$@"
