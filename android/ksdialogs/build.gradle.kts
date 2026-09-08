import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 宣言的 UI (Jetpack Compose) で中身を書くための配布物。
//
// 本体 :ksdialogs-core は Compose に依存しない。Compose でコンテンツを書く消費者だけがこのモジュールを追加し、
// View 系だけを使う消費者 (バインディング経由で本体を取り込む MAUI Android など) には
// compose-ui の推移的依存が届かない。
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    // Maven Central への発行。共通の発行設定 (variant / 署名 / POM の共通部) はルート build.gradle.kts が持ち、
    // ここには artifact 固有の POM の name / description だけを置く
    alias(libs.plugins.mavenPublish)
}

// Maven 座標は View 系本体を ksdialogs-core、Compose 系を ksdialogs とする写像に従う。
// group と version は全モジュール共通の事項なのでルート build.gradle.kts が一括で設定し、
// 本体との同版配布はその共通設定から自動的に成り立つ。
mavenPublishing {
    pom {
        name.set("KsDialogs")
        description.set(
            "A dialog UI library for Android that presents dialogs, loading indicators, and " +
                "toasts from anywhere in an application, with content written in Jetpack Compose.",
        )
    }
}

android {
    namespace = "jp.kamusoft.ksdialogs.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // 最低対象 OS は本体と揃える (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("androidTest") {
            // レイアウト共通ケース表 (core/ADR-0009) は両 Native 実装が参照する唯一の正。
            // 写しを作らず、リポジトリルートの置き場を instrumented test の asset として直接運ぶ
            assets.directories.add("../../core/layout-spec")
            // ケース表の読み込みと期待 rect の照合は :ksdialogs-core の androidTest とも共通なので、
            // どちらのモジュールにも属さない置き場から双方が同じ物を取り込む
            kotlin.directories.add("../layout-case-fixtures/kotlin")
        }
    }

    kotlin {
        // 公開ライブラリなので、公開面の可視性と戻り値型の明示を必須にする
        explicitApi()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // 公開面の型 (DialogViewRegistry / KsDialog / DialogNotifier ほか) がそのまま現れるため api で公開する
    api(project(":ksdialogs-core"))
    // 利用者が @Composable のコンテンツを書くため、runtime も公開面の一部として公開する
    api(libs.androidx.compose.runtime)

    // ホスティングの実装 (ComposeView) と、その動作に要る owner 2種
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.savedstate)

    // 宣言的 UI の中身が実際にどう置かれ、どう破棄されるかは実機・エミュレータでしか測れない
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    // 内容サイズを持つ中身を組み立てるために使う
    androidTestImplementation(libs.androidx.compose.foundation.layout)
}
