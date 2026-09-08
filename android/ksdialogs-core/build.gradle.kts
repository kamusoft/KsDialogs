import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Android Native のライブラリモジュール。
// AGP 9 以降は Kotlin サポートが AGP に内蔵されたため org.jetbrains.kotlin.android は適用しない。
plugins {
    alias(libs.plugins.androidLibrary)
    // Maven Central への発行。共通の発行設定 (variant / 署名 / POM の共通部) はルート build.gradle.kts が持ち、
    // ここには artifact 固有の POM の name / description だけを置く
    alias(libs.plugins.mavenPublish)
}

// Maven 座標は View 系本体を ksdialogs-core、Compose 系を ksdialogs とする写像に従う。
// group と version は全モジュール共通の事項なのでルート build.gradle.kts が一括で設定する。
mavenPublishing {
    pom {
        name.set("KsDialogs Core")
        description.set(
            "The Compose-free core of KsDialogs for Android: typed dialogs, loading indicators, " +
                "and toasts built with Android Views. Use this artifact directly only when you " +
                "do not use Jetpack Compose.",
        )
    }
}

android {
    namespace = "jp.kamusoft.ksdialogs"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // 最低対象 OS は Android 7.0 (API 24) (cross/ADR-0002)
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("androidTest") {
            // レイアウト共通ケース表 (core/ADR-0009) は両 Native 実装が参照する唯一の正。
            // 写しを作らず、リポジトリルートの置き場を instrumented test の asset として直接運ぶ
            // パスはこのモジュールから見た相対位置 (android/ksdialogs-core → リポジトリルート)
            assets.directories.add("../../core/layout-spec")
            // ケース表の読み込みと期待 rect の照合は :ksdialogs の androidTest とも共通なので、
            // どちらのモジュールにも属さない置き場から双方が同じ物を取り込む
            kotlin.directories.add("../layout-case-fixtures/kotlin")
        }
    }

    testOptions {
        unitTests {
            // ユニットテストで Android フレームワークの型 (View / Dialog 等) を素の JVM 上で扱えるようにする。
            // 実際の描画・提示は行われないため、提示の実挙動は実環境での確認が受け持つ
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    // AGP 内蔵 Kotlin の設定は android ブロック配下の kotlin ブロックで行う
    kotlin {
        // 公開ライブラリなので、公開面の可視性と戻り値型の明示を必須にする
        explicitApi()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // suspend な show の UI スレッドマーシャリングに Android の Main ディスパッチャを使う
    implementation(libs.kotlinx.coroutines.android)

    // 公開 API の overlayColor に付けた @ColorInt を利用者側の lint に見せるため、api スコープで公開する
    // (implementation だと利用者のコンパイル classpath に注釈が乗らず、誤用警告が働かない)
    api(libs.androidx.annotation)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // 実 View のレイアウト結果を実機・エミュレータで測る検証は instrumented test で行う
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

// 本体は宣言的 UI (Compose) に依存しない (android/ADR-0001)。View 系だけを使う消費者 — 特に
// バインディング経由で本体を取り込む MAUI Android — に compose-ui の推移的依存を持ち込まない
// ための境界であり、推移的な混入も含めてビルドで固定する。
val declarativeUiGroups = listOf("androidx.compose", "org.jetbrains.compose")

val verifyNoDeclarativeUiDependency = tasks.register("verifyNoDeclarativeUiDependency") {
    group = "verification"
    description = "消費者へ配られる依存グラフに Compose 系 artifact が混ざっていないことを確かめる"

    // 検査対象は消費者へ配られる classpath だけ。テスト専用の classpath は対象外
    val classpaths = listOf(
        "debugCompileClasspath",
        "debugRuntimeClasspath",
        "releaseCompileClasspath",
        "releaseRuntimeClasspath",
    ).associateWith { name ->
        configurations.named(name).flatMap { it.incoming.resolutionResult.rootComponent }
    }

    doLast {
        classpaths.forEach { (name, rootComponent) ->
            val offenders = sortedSetOf<String>()
            val visited = mutableSetOf<ComponentIdentifier>()

            fun visit(component: ResolvedComponentResult) {
                if (!visited.add(component.id)) {
                    return
                }
                val id = component.id
                if (id is ModuleComponentIdentifier &&
                    declarativeUiGroups.any { id.group == it || id.group.startsWith("$it.") }
                ) {
                    offenders += "${id.group}:${id.module}"
                }
                component.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .forEach { visit(it.selected) }
            }

            visit(rootComponent.get())
            check(offenders.isEmpty()) {
                "$name に Compose 系 artifact が混ざっています: ${offenders.joinToString()}"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyNoDeclarativeUiDependency)
}

// このリポジトリの android の全件テストは `./gradlew test` で回し、`check` は経由しない。
// `check` だけに結線すると検査が日常の実行から漏れて混入を黙って通すため、`test` にも結線する。
// `test` は variant の確定後に作られるので、遅延評価の live collection 経由で拾う
tasks.matching { it.name == "test" }.configureEach {
    dependsOn(verifyNoDeclarativeUiDependency)
}
