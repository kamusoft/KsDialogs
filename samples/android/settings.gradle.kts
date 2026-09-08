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

// 公開座標への依存を、公開済み成果物ではなくローカルの Android ビルドへ解決する。
// AGP のライブラリモジュールは Maven publication を生成せず自動置換が発火しないため、
// 利用側であるこの Sample で GAV → included build の project への置換を明示する (cross/ADR-0006)。
// 置換は本体と Compose 系の 2 座標とも書く。アプリが直接書く依存は Compose 系の 1 行だけだが、
// 座標の列挙は直接依存の数ではなく置換対象の網羅であり、本体が公開座標として現れる経路が
// できても公開済み成果物へ静かにフォールバックしないための安全装置になる。
includeBuild("../../android") {
    dependencySubstitution {
        // 宣言的 UI (Jetpack Compose) で中身を書く消費者が足す配布物
        substitute(module("jp.kamusoft:ksdialogs")).using(project(":ksdialogs"))
        // 上の配布物が公開依存として連れてくる本体 (View 系)
        substitute(module("jp.kamusoft:ksdialogs-core")).using(project(":ksdialogs-core"))
    }
}

include(":app")
