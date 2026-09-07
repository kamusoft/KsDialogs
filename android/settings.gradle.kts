rootProject.name = "KsDialogsAndroid"

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
}

include(":ksdialogs")

// 宣言的 UI (Jetpack Compose) で中身を書く消費者だけが追加する配布物。
// 本体 :ksdialogs に Compose の依存を持ち込まないため、別モジュールに分けている
include(":ksdialogs-compose")

// 公開 API 形状の検証を、利用者と同じ側 (friend path を持たない参照) から行うモジュール
include(":api-surface-check")
