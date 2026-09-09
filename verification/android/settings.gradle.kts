// KsDialogs Android 消費者検証 — Gradle settings
//
// 利用者と同じ経路 (公開座標 1 行の依存) で配布物を解決する消費者アプリ 2 つを持つビルド。
// 本体ソースは参照しない (composite build も dependencySubstitution も持たない)。
//
// `jp.kamusoft` は mode に応じて、ローカル参照先 (dry-run) か mavenCentral (smoke) の
// どちらか一方へ exclusiveContent で排他的に割り当てる。repository-level の content filter は
// 「この repository はこの group を含みうる」の宣言にすぎず、filter を持たない他の repository も
// 同じ group を検索するため、参照先に無いときに公開済みの版へ静かにフォールバックする。
// exclusiveContent なら参照先に無い version は必ず解決失敗になる。
//
// 受け取る Gradle プロパティ:
//   ksdialogs.mode       dry-run (既定) | smoke
//   ksdialogs.reference  dry-run の参照先 (ローカル Maven リポジトリのパス)。必須
//   ksdialogs.version    解決する version (:app / :app-core の build.gradle.kts が読む)

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val ksdialogsMode: String =
    settings.providers.gradleProperty("ksdialogs.mode").orNull ?: "dry-run"

require(ksdialogsMode == "dry-run" || ksdialogsMode == "smoke") {
    "ksdialogs.mode は dry-run か smoke のいずれかです: $ksdialogsMode"
}

val ksdialogsReference: String? =
    settings.providers.gradleProperty("ksdialogs.reference").orNull

if (ksdialogsMode == "dry-run") {
    require(!ksdialogsReference.isNullOrBlank()) {
        "dry-run では ksdialogs.reference にローカル参照先のパスが要ります"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                if (ksdialogsMode == "smoke") {
                    mavenCentral()
                } else {
                    maven {
                        name = "verificationFeed"
                        url = uri(ksdialogsReference!!)
                    }
                }
            }
            filter { includeGroup("jp.kamusoft") }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }

    // AGP / Kotlin / Compose の版は本体ビルドのバージョンカタログをそのまま共有し、
    // 消費者側で二重に宣言しない (Sample と同じ)。消費者の Kotlin 版が本体と一致することも
    // この共有が保証する。
    versionCatalogs {
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "ksdialogs-verification-android"

include(":app")
include(":app-core")
