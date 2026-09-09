import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP 消費者の Android アプリ。共有モジュールだけに依存し、View の登録は
// 推移的に届く Android Native API に対して行う。配布物の座標は 1 つも直接書かない。

plugins {
    // 版はルートのプラグイン classpath (共有バージョンカタログの agp) に従うため指定しない
    id("com.android.application")
}

android {
    namespace = "jp.kamusoft.ksdialogs.verification.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.kamusoft.ksdialogs.verification.kmp"
        // 最低対象 OS はライブラリ本体と揃える (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // 共有モジュール。KMP facade と Android Native ライブラリは推移参照で付いてくる
    implementation(project(":shared"))
}
