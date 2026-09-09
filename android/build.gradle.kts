import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.plugins.signing.SigningExtension

// KsDialogs Android Native — ルートプロジェクトのビルドファイル。
//
// 各モジュールのプラグインは自分の build.gradle.kts が `gradle/libs.versions.toml` の
// alias 経由で宣言する。ルートは Maven 座標のうち全モジュール共通の事項 (group / version) と、
// 発行モジュールに共通の発行設定だけを持つ。
plugins {
    // 発行プラグインの型 (AndroidSingleVariantLibrary ほか) をこのビルドスクリプトの
    // classpath に載せるためだけに宣言する。適用は発行する 2 モジュールが個別に行う。
    // 発行プラグインは Android Library プラグインのクラスを同じ classpath 上に要求するため、
    // AGP も同じくルートで宣言する (どちらもルートには適用しない)
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.mavenPublish) apply false
}

// カタログ由来の値は subprojects ブロックの外で解決する。ブロック内の `libs` は
// 対象サブプロジェクトの拡張として解決されるため参照できない。
val catalogVersion = libs.versions.ksdialogs.get()

// リリース時は `-Pversion=` で version を注入する (cross/ADR-0009)。注入があればそれを優先し、無ければ
// カタログの開発用既定値 (SNAPSHOT) を使う。この 1 つの導出式に集めることで、注入値・tag・
// Android の 2 artifact の version が同じ文字列のまま流れる (kmp/ も同じ式で自分の version と
// 本体依存版を導出するため、全形態が同じ version 文字列で配布される)。
val injectedVersion: String? = providers.gradleProperty("version").orNull

// 注入値として受け付ける形式。リリース版 `X.Y.Z` と prerelease `X.Y.Z-{alpha|beta|rc}.N` に限る。
// 空文字・空白を含む値・形式外の値をここで弾かないと、空文字が「注入あり」と解釈されて
// SNAPSHOT ガードを迂回し、開発版が発行版の顔で流れ得る
val releaseVersionPattern = Regex("""\d+\.\d+\.\d+(-(alpha|beta|rc)\.\d+)?""")

val ksDialogsVersion = if (injectedVersion == null) {
    catalogVersion
} else {
    require(releaseVersionPattern.matches(injectedVersion)) {
        "-Pversion に指定できるのは X.Y.Z または X.Y.Z-{alpha|beta|rc}.N の形式だけです " +
            "(指定値: \"$injectedVersion\")。注入しなければ開発用の既定値が使われます。"
    }
    injectedVersion
}

// SNAPSHOT を Sonatype Central へ発行しない (cross/ADR-0009)。
//
// 発行プラグインは version が `-SNAPSHOT` で終わるとき、mavenCentral リポジトリの URL を
// Central の snapshot リポジトリへ向ける。そのため認証情報がある環境で Central 向けタスクを
// 実行すると、開発版がそのまま公開される。SNAPSHOT のあいだは Central 向けタスクを失敗させ、
// リリース版の version を注入したときだけ通す。ローカル発行 (`publishToMavenLocal`) は
// 開発と発行物の検証に使うため妨げない。
//
// 対象はタスク名で判定する。個々の名前を列挙すると、プラグインが Central 向けタスクを
// 増やしたときに素通りし、名前を変えたときに列挙が死に名になって、どちらも無音で穴が開く。
// 名前に `MavenCentral` を含むタスクを一律で対象とし、除外は 1 つだけ挙げる —
// `dropMavenCentralDeployment` は誤って作った deployment を取り下げる後始末用で、
// 発行の経路ではないため止めない。
//
// 発火はタスクの実行前に済ませる。タスクの `doFirst` に置くと、Gradle が Central リポジトリの
// 認証情報を task graph の確定時に解決するため、認証情報の無い環境では「認証情報が足りない」で
// 先に落ちてガードに届かず、SNAPSHOT を止めているのか認証が無いだけなのかが診断から読めない。
// そこで 2 段で掛ける —
//   1. 設定段階: 実行を要求されたタスク名が対象なら、発行プラグインが認証情報を組み立てる前に落とす。
//      名前を直接指定した経路はここで止まり、失敗の理由が SNAPSHOT だけになる
//   2. task graph の確定時: 集約タスク (`publish`) 経由や名前の省略形で間接的に含まれた対象を捕まえる。
//      こちらが対象判定の正で、1. は診断を読みやすくするための前倒しにすぎない
val isCentralPublishTaskName: (String) -> Boolean = { taskName ->
    taskName.contains("MavenCentral") && taskName != "dropMavenCentralDeployment"
}
val snapshotCentralPublishRefusal = "SNAPSHOT ($ksDialogsVersion) は Maven Central へ発行しない。" +
    "リリース版の version は -Pversion=<version> で注入する。" +
    "ローカルでの発行物確認には publishToMavenLocal を使う。"

if (ksDialogsVersion.endsWith("-SNAPSHOT")) {
    // 要求されたタスク名はプロジェクトパス付き (`:ksdialogs-core:publishToMavenCentral`) で
    // 渡りうるため、最後のセグメントだけをタスク名として見る
    if (gradle.startParameter.taskNames.any { isCentralPublishTaskName(it.substringAfterLast(':')) }) {
        throw GradleException(snapshotCentralPublishRefusal)
    }

    gradle.taskGraph.whenReady {
        if (allTasks.any { isCentralPublishTaskName(it.name) }) {
            throw GradleException(snapshotCentralPublishRefusal)
        }
    }
}

subprojects {
    // Maven Central の groupId (cross/ADR-0005)
    group = "jp.kamusoft"
    version = ksDialogsVersion

    // 発行プラグインを適用したモジュールにだけ共通の発行設定を与える。共通部をモジュール側へ
    // 複製すると、URL や license を変えたときに片方が取り残されるため 1 箇所に集める。
    // モジュール側に置くのは artifact ごとに異なる POM の name / description だけ
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            // release variant のみを発行し、sources jar を同梱する。javadoc jar は Maven Central の
            // 必須要件を満たすためだけの空 jar とする。IDE の KDoc 表示は sources jar が担うため
            // javadoc の実体は要らず、利用者向けドキュメントは skills/ と README が担う
            configure(
                AndroidSingleVariantLibrary(
                    variant = "release",
                    sourcesJar = SourcesJar.Sources(),
                    javadocJar = JavadocJar.Empty(),
                ),
            )

            // Sonatype Central Portal へ発行する。認証は
            // `ORG_GRADLE_PROJECT_mavenCentralUsername` / `mavenCentralPassword` で渡す
            publishToMavenCentral()

            // Central の必須要件。署名鍵は `ORG_GRADLE_PROJECT_signingInMemoryKey` 系で渡す。
            // 署名の必須/任意は下の signing ブロックで鍵の有無に連動させる
            signAllPublications()

            pom {
                // name / description 以外は全 artifact で同一
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
        // (signing プラグインは maven-publish プラグインが適用するため、型付きアクセサではなく拡張経由で設定する)
        plugins.withId("signing") {
            extensions.configure<SigningExtension> {
                setRequired(providers.gradleProperty("signingInMemoryKey").isPresent)
            }
        }
    }
}
