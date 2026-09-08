import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 公開 API 形状の検証だけを持つモジュール。
//
// ライブラリを **利用者と同じ側から** 参照する (project 依存は friend path にならないため、
// internal な宣言は見えない)。テストソースセットからの検証では test friend path を通ってしまい、
// 公開すべき型が誤って internal になっても気づけないため、この境界を別モジュールとして分けている。
//
// 正の検証は main ソースセットにあり、既定のビルドで常にコンパイルされる。
// 負の検証は禁止形状ごとに別のソースセットへ分け、対応するプロパティを付けたときだけ加わる
// (そのビルドが失敗することが期待結果。1回のビルドに1つの誤りしか入らないので個別に示せる)。
plugins {
    alias(libs.plugins.androidLibrary)
    // 宣言的 UI の中身を書く利用者と同じ構成にするため、この検査モジュールも Compose を書ける状態にする
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "jp.kamusoft.ksdialogs.apicheck"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            negativeCheckSourceDirectories.forEach { (property, directory) ->
                if (providers.gradleProperty(property).isPresent) {
                    kotlin.directories.add(directory)
                }
            }
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // 利用者と同じ経路での参照。friend path を持たないため internal な宣言は見えない
    implementation(project(":ksdialogs-core"))
    // 宣言的 UI で中身を書く消費者が追加する配布物。こちらも利用者と同じ経路で参照する
    implementation(project(":ksdialogs"))
}

/** 禁止形状ごとの検証プロパティと、そのときだけ加えるソースの置き場。 */
val negativeCheckSourceDirectories
    get() = mapOf(
        "ksdialogs.negativeCheck.vmAttribute" to "src/negativeCheckVmAttribute/kotlin",
        "ksdialogs.negativeCheck.showOptions" to "src/negativeCheckShowOptions/kotlin",
        "ksdialogs.negativeCheck.resultType" to "src/negativeCheckResultType/kotlin",
        "ksdialogs.negativeCheck.notifierValue" to "src/negativeCheckNotifierValue/kotlin",
        "ksdialogs.negativeCheck.showTransition" to "src/negativeCheckShowTransition/kotlin",
        "ksdialogs.negativeCheck.optionsTransition" to "src/negativeCheckOptionsTransition/kotlin",
        "ksdialogs.negativeCheck.noneArguments" to "src/negativeCheckNoneArguments/kotlin",
        "ksdialogs.negativeCheck.loadingShowStyle" to "src/negativeCheckLoadingShowStyle/kotlin",
        "ksdialogs.negativeCheck.loadingShowOptions" to "src/negativeCheckLoadingShowOptions/kotlin",
        "ksdialogs.negativeCheck.toastHide" to "src/negativeCheckToastHide/kotlin",
        "ksdialogs.negativeCheck.toastShowResult" to "src/negativeCheckToastShowResult/kotlin",
        "ksdialogs.negativeCheck.toastShowStyle" to "src/negativeCheckToastShowStyle/kotlin",
        "ksdialogs.negativeCheck.toastOptions" to "src/negativeCheckToastOptions/kotlin",
        "ksdialogs.negativeCheck.toastComposeFromCore" to "src/negativeCheckToastComposeFromCore/kotlin",
        "ksdialogs.negativeCheck.legacyContractName" to "src/negativeCheckLegacyContractName/kotlin",
    )
