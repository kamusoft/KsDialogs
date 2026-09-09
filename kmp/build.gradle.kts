// KsDialogs KMP — ルートプロジェクトのビルドファイル。
//
// 発行の配線そのもの (プラグインの適用・POM・署名) は、発行する 1 モジュール `:ksdialogs-kmp` が
// 自分の build.gradle.kts に持つ。ここが持つのは version の導出と、SNAPSHOT のあいだ
// Maven Central 向けタスクを止めるガードだけで、発行プラグインの型は参照しない。

plugins {
    // Kotlin Multiplatform プラグインをこのビルドの plugin classpath に 1 回だけ載せるための宣言。
    // 適用は各モジュールが個別に行う。
    //
    // Gradle はモジュールごとの plugin 集合が一致するときだけ classloader を共有する。
    // 発行プラグインを `:ksdialogs-kmp` にだけ適用すると `:api-surface-check` と集合がずれ、
    // Kotlin Multiplatform プラグインのクラスが 2 つの classloader に別々に読み込まれて、
    // 両モジュールが共有する Kotlin/Native ツールチェーンの build service が型不一致で作れなくなる。
    // ルートで先に宣言しておくと親スコープの 1 つの classloader を両モジュールが使う
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// リリース時は `-Pversion=` で version を注入する (cross/ADR-0009)。注入があればそれを優先し、無ければ
// カタログの開発用既定値 (SNAPSHOT) を使う。android/ のルートビルドファイルと同じ導出式で、
// KMP artifact の version・本体 `jp.kamusoft:ksdialogs-core` への依存版・Swift パッケージ参照の
// exact がすべてこの 1 つの値から出るため、形態をまたいだ版の突き合わせを手で揃える箇所が無い
val catalogVersion = libs.versions.ksdialogs.get()
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

// 発行するモジュールがここから読んで自分の Maven 座標と依存版に使う。
// 公開 API 形状の検査モジュール `:api-surface-check` は発行しないため座標を持たせない
extra["ksdialogsVersion"] = ksDialogsVersion

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
// 判定にタスク名だけを使うのは、ルートで発行プラグインの型を参照せずに済ませるため。
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
val snapshotCentralPublishRefusal = "Refusing to publish a SNAPSHOT version to Maven Central: " +
    "$ksDialogsVersion. リリース版の version は -Pversion=<version> で注入する。" +
    "ローカルでの発行物確認には publishToMavenLocal を使う。"

if (ksDialogsVersion.endsWith("-SNAPSHOT")) {
    // 要求されたタスク名はプロジェクトパス付き (`:ksdialogs-kmp:publishToMavenCentral`) で
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
