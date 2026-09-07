rootProject.name = "KsDialogsMauiBridge"

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

    // Kotlin / AGP のバージョン整合を保つため、バージョンカタログは android/ のファイルを共有する (cross/ADR-0004)
    versionCatalogs {
        create("libs") {
            from(files("../../../android/gradle/libs.versions.toml"))
        }
    }
}

// jp.kamusoft:ksdialogs への依存を、公開済み成果物ではなくローカルの Android ビルドへ解決する (cross/ADR-0004)
includeBuild("../../../android")

include(":ksdialogs-maui-bridge")
