import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP 公開面のモジュール。Android ターゲットは Android Native ライブラリへ委譲する (core/ADR-0001)。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9 以降、KMP モジュールの Android ターゲットは com.android.library ではなくこのプラグインで構成する
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

// Maven 座標は公開識別子の写像表 (cross/ADR-0005) に従う。
// 発行の配線 (maven-publish) は行わず、座標の宣言のみを持つ。
group = "jp.kamusoft"
version = "0.1.0"

kotlin {
    // 公開ライブラリなので、公開面の可視性と戻り値型の明示を必須にする
    explicitApi()

    // Android ターゲットの設定は kotlin ブロック配下の android ブロックで行う
    android {
        namespace = "jp.kamusoft.ksdialogs.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        // 最低対象 OS は Android 7.0 (API 24) (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()

        // commonTest をホスト JVM 上で実行するために Android の unit test を有効化する
        withHostTestBuilder {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Kotlin/Native の既定より高い最低対象 OS (iOS 17) をリンク時に固定する (cross/ADR-0002)。
    // 既定値は konan の設定に埋め込まれているため、ターゲットごとに上書きする
    val iosDeploymentTarget = listOf(
        "osVersionMin.ios_arm64=17.0",
        "osVersionMin.ios_simulator_arm64=17.0",
        "osVersionMin.ios_x64=17.0"
    ).joinToString(";")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        // Swift / ObjC 利用者が参照する成果物。共有コードの契約はこの framework 越しに見える
        iosTarget.binaries.framework {
            baseName = "KsDialogsKmp"
            // iOS Native ライブラリ (Swift パッケージ) の実体はアプリ側でリンクされるため、
            // この framework は未解決の参照を残したまま配る静的形式にする (kmp/ADR-0002)
            isStatic = true
        }
        iosTarget.binaries.all {
            freeCompilerArgs += "-Xoverride-konan-properties=$iosDeploymentTarget"
        }
    }

    // iOS ターゲットは iOS Native ライブラリ (Swift パッケージ) の ObjC 互換面へ委譲する (kmp/ADR-0002)。
    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("17.0")
        localSwiftPackage(rootProject.layout.projectDirectory.dir("../ios"), listOf("KsDialogs"))
    }

    sourceSets {
        androidMain.dependencies {
            // ViewModel 契約が Native ライブラリの型そのものになるため、利用者へも見える依存として公開する。
            // composite build により、公開済み成果物ではなくローカルの android/ ビルドへ解決される (cross/ADR-0004)
            api("jp.kamusoft:ksdialogs-core:${libs.versions.ksdialogs.get()}")
        }
        iosMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
            }
        }
    }
}

// Swift / ObjC から見える面の検査 (ObjCApiSurfaceTests) は、生成された framework の ObjC ヘッダを読む。
// テストの実行体はビルドの出力位置を知らないため、ヘッダを作らせたうえでその場所を環境変数で渡す。
// シミュレータで走るテストへ環境変数を届けるには SIMCTL_CHILD_ の接頭辞が要る (接頭辞は取り除かれて届く)
tasks.named<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>(
    "iosSimulatorArm64Test",
) {
    val header = layout.buildDirectory
        .file("bin/iosSimulatorArm64/debugFramework/KsDialogsKmp.framework/Headers/KsDialogsKmp.h")
    dependsOn("linkDebugFrameworkIosSimulatorArm64")
    environment("SIMCTL_CHILD_KSDIALOGS_KMP_OBJC_HEADER", header.get().asFile.absolutePath)
}
