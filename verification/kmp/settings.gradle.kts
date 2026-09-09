// KsDialogs KMP 消費者検証 — Gradle settings
//
// 利用者と同じ経路 (公開座標 1 行の依存) で KMP の配布物を解決する消費者。
// 本体ソースは参照しない (composite build も dependencySubstitution も持たない)。
//
// `jp.kamusoft` は mode に応じて、ローカル参照先 (dry-run) か mavenCentral (smoke) の
// どちらか一方へ exclusiveContent で排他的に割り当てる。filter を持たない他の repository も
// 同じ group を検索してしまうため、排他割り当てにして参照先に無い version を必ず解決失敗にする。
//
// 受け取る Gradle プロパティ:
//   ksdialogs.mode       dry-run (既定) | smoke
//   ksdialogs.reference  dry-run の参照先 (ローカル Maven リポジトリのパス)。必須
//   ksdialogs.version    解決する version (:shared の build.gradle.kts が読む)

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

    // AGP / Kotlin の版は本体ビルドのバージョンカタログをそのまま共有する。
    // 消費者の Kotlin Gradle Plugin が配布物のサポート範囲内であることも、この共有が保証する。
    //
    // このプロジェクトは作業ディレクトリへコピーしてからビルドするため、リポジトリ内の相対位置は
    // コピー先で成り立たない。コピーの中で走らせるときは ksdialogs.catalog に本体のカタログの
    // パスを渡す (既定はリポジトリ内での相対パス)。
    versionCatalogs {
        create("libs") {
            val catalog = settings.providers.gradleProperty("ksdialogs.catalog").orNull
                ?: "../../android/gradle/libs.versions.toml"
            from(files(catalog))
        }
    }
}

rootProject.name = "VerificationKmp"

include(":shared")
include(":androidApp")
