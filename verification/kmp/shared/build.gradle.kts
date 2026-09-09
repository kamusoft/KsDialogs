import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP 利用者と同じ形の共有モジュール。commonMain の依存は公開座標 1 行だけで、
// iOS ホストが要る Swift package 参照は配布物の発行 metadata から推移的に届く。
//
// 利用者向けドキュメントの KMP 最小例 (src/commonMain/kotlin) をコンパイル対象に含める。

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

val ksdialogsVersion: String = providers.gradleProperty("ksdialogs.version").get()

kotlin {
    android {
        namespace = "jp.kamusoft.ksdialogs.verification.kmp.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        // 最低対象 OS はライブラリ本体と揃える (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Kotlin/Native の既定より高い最低対象 OS (iOS 17) をリンク時に固定する (cross/ADR-0002)
    val iosDeploymentTarget = listOf(
        "osVersionMin.ios_arm64=17.0",
        "osVersionMin.ios_simulator_arm64=17.0",
        "osVersionMin.ios_x64=17.0",
    ).joinToString(";")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        // iosApp が参照する成果物。共有コードの ViewModel はこの framework 越しに見える
        iosTarget.binaries.framework {
            baseName = "VerificationShared"
            // KMP facade と iOS Native ライブラリの実体はアプリ側でリンクされるため、
            // この framework は未解決の参照を残したまま配る静的形式にする
            isStatic = true
        }
        iosTarget.binaries.all {
            freeCompilerArgs += "-Xoverride-konan-properties=$iosDeploymentTarget"
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 利用者が書くのと同じ 1 行。ViewModel 契約が公開面の型そのものになるため、
            // 利用者へも見える依存として公開する
            api("jp.kamusoft:ksdialogs-kmp:$ksdialogsVersion")
        }
    }
}
