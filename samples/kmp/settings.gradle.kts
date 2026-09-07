rootProject.name = "KsDialogsSampleKmp"

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

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }

    // Sample だけ解決版が上がるのを避けるため、バージョンカタログは本体ビルドのものを共有する
    versionCatalogs {
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

// 公開座標 jp.kamusoft:ksdialogs-kmp への依存を、公開済み成果物ではなくローカルの KMP ビルドへ解決する。
// KMP facade は Maven publication を生成しないため、利用側であるこの Sample で
// GAV → included build の project への置換を明示する (cross/ADR-0006)。
includeBuild("../../kmp") {
    dependencySubstitution {
        substitute(module("jp.kamusoft:ksdialogs-kmp")).using(project(":ksdialogs-kmp"))
    }
}

// 宣言的 UI (Jetpack Compose) で中身を書く消費者だけが足す配布物も、同じくローカルの Android ビルドへ解決する。
// AGP のライブラリモジュールは Maven publication を生成せず自動置換が発火しないため、置換を明示する
includeBuild("../../android") {
    dependencySubstitution {
        substitute(module("jp.kamusoft:ksdialogs-compose")).using(project(":ksdialogs-compose"))
    }
}

include(":shared")
include(":androidApp")
