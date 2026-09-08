import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP Sample の Android アプリ。共有 Presenter を呼び、View の登録だけを Android Native API で行う。
plugins {
    // バージョンはルートのプラグイン classpath (共有バージョンカタログの agp / kotlin) に従うため指定しない
    id("com.android.application")
    // 宣言的 UI (Jetpack Compose) で中身を書くために要る
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "jp.kamusoft.ksdialogs.samples.kmp.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.kamusoft.ksdialogs.samples.kmp.android"
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
    // 共有モジュール。公開 product (KMP facade) と Android Native ライブラリは推移参照で付いてくる
    implementation(project(":shared"))

    // Compose でダイアログの中身を書くための配布物。View 系の本体は推移参照で付いてくる
    implementation("jp.kamusoft:ksdialogs:${libs.versions.ksdialogs.get()}")

    // 中身の composable を組み立てるための Compose 基盤 (レイアウト・文字・押下)
    implementation("androidx.compose.foundation:foundation:${libs.versions.androidx.compose.get()}")

    // suspend な Presenter を呼ぶためのスコープに Android の Main ディスパッチャを使う
    implementation(libs.kotlinx.coroutines.android)

    // 戻る操作を予測型戻りジェスチャーの受け口 (ComponentActivity の OnBackPressedDispatcher) で受ける
    implementation(libs.androidx.activity)
}
