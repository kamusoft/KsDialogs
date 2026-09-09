import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP 公開面のモジュール。Android ターゲットは Android Native ライブラリへ委譲する (core/ADR-0001)。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9 以降、KMP モジュールの Android ターゲットは com.android.library ではなくこのプラグインで構成する
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    // Maven Central への発行。この KMP ビルドで発行するモジュールはここだけなので、
    // 発行設定はルートに共通化せずこのファイルで完結させる
    alias(libs.plugins.mavenPublish)
}

// Maven 座標は公開識別子の写像表 (cross/ADR-0005) に従う。
// version はルートビルドファイルが導出した 1 つの値を使う (`-Pversion=` の注入値、無ければカタログの既定値)
group = "jp.kamusoft"
version = rootProject.extra["ksdialogsVersion"] as String

// version 由来の分岐に使う。開発中の既定は SNAPSHOT で、リリース版は注入されたときだけ現れる
val isSnapshotVersion = version.toString().endsWith("-SNAPSHOT")

mavenPublishing {
    // KMP の全ターゲットの publication (root + Android + iOS 3 ターゲット) を発行し、sources jar を
    // 同梱する。javadoc jar は Maven Central の必須要件を満たすためだけの空 jar とする。
    // IDE の KDoc 表示は sources jar が担うため javadoc の実体は要らず、
    // 利用者向けドキュメントは skills/ と README が担う。
    // Android の発行 variant は指定しない — AGP の KMP ライブラリプラグインでは使われない
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources(),
        ),
    )

    // Sonatype Central Portal へ発行する。認証は
    // `ORG_GRADLE_PROJECT_mavenCentralUsername` / `mavenCentralPassword` で渡す
    publishToMavenCentral()

    // Central の必須要件。署名鍵は `ORG_GRADLE_PROJECT_signingInMemoryKey` 系で渡す。
    // 署名の必須/任意は下の signing ブロックで鍵の有無に連動させる
    signAllPublications()

    pom {
        name.set("KsDialogs KMP")
        description.set(
            "The Kotlin Multiplatform facade of KsDialogs: shared-code contracts for typed " +
                "dialogs, loading indicators, and toasts that delegate to the native iOS and " +
                "Android libraries. The iOS app links the KsDialogs Swift package alongside " +
                "this artifact.",
        )

        // name / description 以外は Android Native の配布物の POM と同じ値。
        // 別のビルドルートなので共有はせず、同じ内容をここにも書く (cross/ADR-0004)
        inceptionYear.set("2026")
        url.set("https://github.com/kamusoft/KsDialogs")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("kamusoft")
                name.set("kamusoft")
                url.set("https://github.com/kamusoft")
            }
        }

        scm {
            url.set("https://github.com/kamusoft/KsDialogs")
            connection.set("scm:git:https://github.com/kamusoft/KsDialogs.git")
            developerConnection.set("scm:git:ssh://git@github.com/kamusoft/KsDialogs.git")
        }
    }
}

// 署名鍵が渡されていない発行 (発行物を手元で確かめる `publishToMavenLocal`) は、
// SNAPSHOT 以外の version でも Sign タスクを skip して未署名のまま通す。
// 本番発行で鍵の指定が抜けていても Sonatype Central Portal が未署名を拒否するため静かには通らない。
// (signing プラグインは発行プラグインが適用するため、型付きアクセサではなく拡張経由で設定する)
plugins.withId("signing") {
    extensions.configure<SigningExtension> {
        setRequired(providers.gradleProperty("signingInMemoryKey").isPresent)
    }
}

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
    //
    // 参照の種別は version から導出する。開発中 (SNAPSHOT) は monorepo 内の ios/ をそのまま指し、
    // `../ios` のライブ編集が即座に効く。リリース版の version が注入されたときだけ、配信リポジトリを
    // 同じ version の tag で厳密指定するリモート参照になる。
    // 明示のモード切替スイッチを置かないのは、リリース版でローカル参照を選べる余地を残すと、
    // 発行者マシンの絶対パスを載せた成果物が警告なしに出来上がるため。
    // URL だけは上書きできる — 配信リポジトリ相当のローカル clone を指して、
    // 公開前にリモート参照の解決を確かめるための口 (SNAPSHOT では参照に URL が無いので無視される)
    val swiftPackageUrl = providers.gradleProperty("ksdialogs.swiftPackageUrl")
        .orElse("https://github.com/kamusoft/KsDialogs-SPM")
    swiftPMDependencies {
        // 配布物の最低対象 OS は iOS 17 (cross/ADR-0002)。
        // 参照の種別によらず metadata に載るよう、分岐の外で宣言する
        iosMinimumDeploymentTarget.set("17.0")
        if (isSnapshotVersion) {
            localSwiftPackage(rootProject.layout.projectDirectory.dir("../ios"), listOf("KsDialogs"))
        } else {
            swiftPackage(
                url(swiftPackageUrl.get()),
                exact(version.toString()),
                listOf(product("KsDialogs")),
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            // ViewModel 契約が Native ライブラリの型そのものになるため、利用者へも見える依存として公開する。
            // composite build により、公開済み成果物ではなくローカルの android/ ビルドへ解決される (cross/ADR-0004)
            api("jp.kamusoft:ksdialogs-core:$version")
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
