import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Sample の共有モジュール。公開 product (KMP facade) を利用者アプリと同じ側から消費する (cross/ADR-0006)。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "jp.kamusoft.ksdialogs.samples.kmp"
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
        // iosApp が参照する成果物。共有 Presenter と ViewModel はこの framework 越しに見える
        iosTarget.binaries.framework {
            baseName = "SampleShared"
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
            // 公開 product。composite build により、公開済み成果物ではなくローカルの kmp/ ビルドへ解決される。
            // ViewModel 契約が公開面の型そのものになるため、利用者へも見える依存として公開する
            api("jp.kamusoft:ksdialogs-kmp:${libs.versions.ksdialogs.get()}")
            // 進捗を段階的に見せるための待ちに使う。利用者アプリと同じ側から足す依存
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
        }
    }
}
