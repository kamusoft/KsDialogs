rootProject.name = "KsDialogsSampleAndroid"

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

    // Sample だけ解決版が上がるのを避けるため、バージョンカタログは Android Native ビルドのものを共有する
    versionCatalogs {
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

// 公開座標 jp.kamusoft:ksdialogs への依存を、公開済み成果物ではなくローカルの Android ビルドへ解決する。
// AGP のライブラリモジュールは Maven publication を生成せず自動置換が発火しないため、
// 利用側であるこの Sample で GAV → included build の project への置換を明示する (cross/ADR-0006)。
includeBuild("../../android") {
    dependencySubstitution {
        substitute(module("jp.kamusoft:ksdialogs")).using(project(":ksdialogs"))
        // 宣言的 UI (Jetpack Compose) で中身を書く消費者だけが足す配布物も同じ扱いにする
        substitute(module("jp.kamusoft:ksdialogs-compose")).using(project(":ksdialogs-compose"))
    }
}

include(":app")
