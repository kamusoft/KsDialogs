import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Sample アプリのモジュール。利用者アプリと同じ側から公開 product を参照する (cross/ADR-0006)。
plugins {
    // バージョンは共有バージョンカタログの agp と一致させる (プラグイン宣言ではカタログを参照できないため直書き)
    id("com.android.application") version "9.3.0"
    // 宣言的 UI (Jetpack Compose) で中身を書くために要る。バージョンは共有バージョンカタログの kotlin と一致させる
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

android {
    namespace = "jp.kamusoft.ksdialogs.samples.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "jp.kamusoft.ksdialogs.samples.android"
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
    // 公開 product。Compose でダイアログの中身を書く消費者が足す 1 行で、View 系の本体
    // (jp.kamusoft:ksdialogs-core) は公開依存として推移的に付いてくる。
    // composite build により、公開済み成果物ではなくローカルの android/ ビルドへ解決される
    implementation("jp.kamusoft:ksdialogs:${libs.versions.ksdialogs.get()}")

    // 中身の composable を組み立てるための Compose 基盤 (レイアウト・文字・押下)
    implementation("androidx.compose.foundation:foundation:${libs.versions.androidx.compose.get()}")

    // suspend な show を呼ぶためのスコープに Android の Main ディスパッチャを使う
    implementation(libs.kotlinx.coroutines.android)

    // 戻る操作を予測型戻りジェスチャーの受け口 (ComponentActivity の OnBackPressedDispatcher) で受ける
    implementation(libs.androidx.activity)
}
