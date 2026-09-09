import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 宣言的 UI (Jetpack Compose) でダイアログの中身を書く利用者と同じ形の消費者。
// 配布物への依存は公開座標 1 行だけで、本体ソースへの参照は持たない。
// View 系の本体 (jp.kamusoft:ksdialogs-core) は公開依存として推移的に付いてくる。
//
// 利用者向けドキュメントの Android 最小例 (../readme-example) を、-core 単独の消費者と
// 同じソースディレクトリからコンパイル対象に含める。

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ksdialogsVersion: String = providers.gradleProperty("ksdialogs.version").get()

android {
    namespace = "jp.kamusoft.ksdialogs.verification.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.kamusoft.ksdialogs.verification.android"
        // 最低対象 OS はライブラリ本体と揃える (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        named("release") {
            // 縮小・難読化は行わない。R8 が走らないため、配布物の consumer ProGuard ルールの
            // 不足はこの検証では検出できない (見るのはビルドの成立まで)。
            // signingConfig を割り当てないため release の出力は未署名 APK になる。
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
    // 利用者が書くのと同じ 1 行。参照先は settings.gradle.kts の exclusiveContent が決める
    implementation("jp.kamusoft:ksdialogs:$ksdialogsVersion")

    // 中身の composable を組み立てるための Compose 基盤 (版は本体と共有するカタログで整合させる)
    implementation("androidx.compose.foundation:foundation:${libs.versions.androidx.compose.get()}")
    implementation(libs.androidx.activity)
}
