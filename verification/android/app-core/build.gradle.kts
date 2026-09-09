import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// View 系の本体だけを使う利用者と同じ形の消費者。依存は公開座標 1 行で、
// 宣言的 UI (Jetpack Compose) の座標を 1 つも足さない。
// この module の release runtime classpath に androidx.compose が現れないことが、
// 本体の配布物が Compose を連れてこないことの確認になる (cross/ADR-0019)。
//
// コンパイル対象のソースは Compose 系の消費者と共有する (../readme-example)。

plugins {
    id("com.android.application")
}

val ksdialogsVersion: String = providers.gradleProperty("ksdialogs.version").get()

android {
    namespace = "jp.kamusoft.ksdialogs.verification.androidcore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.kamusoft.ksdialogs.verification.androidcore"
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

    sourceSets {
        named("main") {
            kotlin.srcDir("../readme-example")
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // 利用者が書くのと同じ 1 行。これ以外の配布物は足さない
    implementation("jp.kamusoft:ksdialogs-core:$ksdialogsVersion")
}
