import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 共有コードの公開 API 形状の検証だけを持つモジュール。
//
// KMP 公開面を **利用者と同じ側から** 参照する (project 依存は friend path にならないため、
// internal な宣言は見えない)。commonTest からの検証では test friend path を通ってしまい、
// 公開すべき型が誤って internal になっても気づけないため、この境界を別モジュールとして分けている。
//
// 正の検証は commonMain にあり、既定のビルドで常にコンパイルされる。
// 負の検証は禁止形状ごとに別のソースへ分け、対応するプロパティを付けたときだけ加わる
// (そのビルドが失敗することが期待結果)。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "jp.kamusoft.ksdialogs.kmp.apicheck"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // 公開面は共有コードで決まるため、確認は Kotlin/Native 側 1 ターゲットで足りる
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            negativeCheckSourceDirectories.forEach { (property, directory) ->
                if (providers.gradleProperty(property).isPresent) {
                    kotlin.srcDir(directory)
                }
            }
            dependencies {
                // 利用者と同じ経路での参照。friend path を持たないため internal な宣言は見えない
                implementation(project(":ksdialogs-kmp"))
            }
        }
    }
}

/** 禁止形状ごとの検証プロパティと、そのときだけ加えるソースの置き場。 */
val negativeCheckSourceDirectories
    get() = mapOf(
        "ksdialogs.negativeCheck.optionsType" to "src/negativeCheckOptionsType/kotlin",
        "ksdialogs.negativeCheck.showOptions" to "src/negativeCheckShowOptions/kotlin",
        "ksdialogs.negativeCheck.resultType" to "src/negativeCheckResultType/kotlin",
        "ksdialogs.negativeCheck.loadingStyleType" to "src/negativeCheckLoadingStyleType/kotlin",
        "ksdialogs.negativeCheck.loadingStyleProperty" to "src/negativeCheckLoadingStyleProperty/kotlin",
        "ksdialogs.negativeCheck.toastStyleType" to "src/negativeCheckToastStyleType/kotlin",
        "ksdialogs.negativeCheck.toastStyleProperty" to "src/negativeCheckToastStyleProperty/kotlin",
        "ksdialogs.negativeCheck.toastRegistration" to "src/negativeCheckToastRegistration/kotlin",
        "ksdialogs.negativeCheck.loadingRegistration" to "src/negativeCheckLoadingRegistration/kotlin",
        "ksdialogs.negativeCheck.toastHide" to "src/negativeCheckToastHide/kotlin",
        "ksdialogs.negativeCheck.toastShowResult" to "src/negativeCheckToastShowResult/kotlin",
        "ksdialogs.negativeCheck.legacyContractName" to "src/negativeCheckLegacyContractName/kotlin",
    )
