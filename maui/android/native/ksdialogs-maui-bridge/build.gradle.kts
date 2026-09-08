import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// MAUI 形態のための互換面モジュール。
// AGP 9 以降は Kotlin サポートが AGP に内蔵されたため org.jetbrains.kotlin.android は適用しない。
plugins {
    alias(libs.plugins.androidLibrary)
}

group = "jp.kamusoft"
version = libs.versions.ksdialogs.get()

android {
    namespace = "jp.kamusoft.ksdialogs.maui"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // 最低対象 OS は Android 7.0 (API 24) (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            // ユニットテストで Android フレームワークの型 (View 等) を素の JVM 上で扱えるようにする
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    // AGP 内蔵 Kotlin の設定は android ブロック配下の kotlin ブロックで行う
    kotlin {
        // 束縛される面なので、公開面の可視性と戻り値型の明示を必須にする
        explicitApi()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // ViewModel 契約と結果報告口が公開面に現れるため、利用者へも見える依存として公開する。
    // composite build により、公開済み成果物ではなくローカルの android/ ビルドへ解決される (cross/ADR-0004)
    api("jp.kamusoft:ksdialogs-core:${libs.versions.ksdialogs.get()}")

    // suspend な show の呼び出しに Android の Main ディスパッチャを使う
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
