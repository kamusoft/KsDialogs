# Design: add-kmp-maven-distribution

## Context

`kmp/` は Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.7.0 の KMP ビルドで、Android ターゲットは `com.android.kotlin.multiplatform.library`、iOS は 3 ターゲットの static framework、iOS Native への委譲は KGP の SwiftPM import (`swiftPMDependencies`、Alpha) で `localSwiftPackage(../ios)` を宣言している。`includeBuild("../android")` で本体 `jp.kamusoft:ksdialogs-core` をローカル解決し、バージョンカタログは `android/gradle/libs.versions.toml` を共有する。発行の配線は無く version は直書き。

phase-5 (add-native-distribution) が android/ 側に vanniktech 0.37.0 の発行設定と version の導出式 (`android/build.gradle.kts`) を入れ、`kmp/` の本体依存座標を `ksdialogs-core` に追随させる。本変更はその上に、フェーズ議論 (phase-7 agenda 決定事項 A1〜A6・B) で確定した KMP の発行を積む。PoC (library-foundation phase-10 `artifacts/poc-swiftpm-remote-distribution.md`) で、リモート参照時の Kotlin SwiftPM 連携は消費者側まで成立 (4 項目) し、`localSwiftPackage` のまま発行すると発行者の絶対パスが metadata に乗ることが実測済み。

## Goals / Non-Goals

Goals: proposal.md の What Changes。Non-Goals: proposal.md の Non-Goals。

## Decisions

### Decision 1: vanniktech を `:ksdialogs-kmp` に適用し、composite build は維持する

**採用案:** `com.vanniktech.maven.publish` 0.37.0 (phase-5 でカタログに宣言済み) を `:ksdialogs-kmp` にだけ適用し、発行設定は module 側 (`kmp/ksdialogs-kmp/build.gradle.kts`) で完結させる: `mavenPublishing { configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), sourcesJar = true)) ; publishToMavenCentral() ; signAllPublications() (鍵があるときのみ) ; pom { ... } }`。`androidVariantsToPublish` は引数を書かず既定値のままにする (AGP の KMP ライブラリプラグイン `com.android.kotlin.multiplatform.library` では variant 指定は使われない — vanniktech 公式の指定)。ルート `kmp/build.gradle.kts` (新設) は version の導出と SNAPSHOT ガード (Decision 2) だけを持ち、vanniktech / KGP の型を参照しない (ルートの plugin classpath に `apply false` を足す必要を作らない)。`includeBuild("../android")` と `androidMain` の `api("jp.kamusoft:ksdialogs-core:<version>")` はそのまま。POM の共通部 (url `https://github.com/kamusoft/KsDialogs` / MIT license / developers / scm / inceptionYear 2026) は phase-5 の `android/build.gradle.kts` と同じ値を module 側に書き、対応をコメントで示す。name は `KsDialogs KMP`、description は次の文言。

> The Kotlin Multiplatform facade of KsDialogs: shared-code contracts for typed dialogs, loading indicators, and toasts that delegate to the native iOS and Android libraries. The iOS app links the KsDialogs Swift package alongside this artifact.

発行物の配置 (KGP 2.4.10 の publication モデル。`SerializeSwiftPMDependenciesMetadataKt` が SwiftPM 連携メタデータを `KotlinMultiplatformExtension.adhocSoftwareComponent` (root の `kotlinMultiplatform` publication) に classifier `swiftpm-metadata` で登録することを KGP の class から確認):

| publication | 成果物 |
|---|---|
| root `ksdialogs-kmp` | metadata jar (commonMain)・POM・`.module`・sources jar・空 javadoc jar・`ksdialogs-kmp-<version>-swiftpm-metadata.json` |
| `ksdialogs-kmp-android` | release aar・POM・`.module`・sources jar・空 javadoc jar |
| `ksdialogs-kmp-iosarm64` / `-iossimulatorarm64` / `-iosx64` | 本体 klib・cinterop klib (`*-cinterop-swiftPMImport.klib`)・POM・`.module`・sources jar・空 javadoc jar |

**理由:** composite build の置換は解決時にだけ効き、発行される POM には宣言どおりの座標が載る (PoC 項目 1 で Maven 1 点から Android Native が推移的に揃った実測がこの挙動)。vanniktech は KGP が作った既存 publication (kotlinMultiplatform / android / iosArm64 / iosSimulatorArm64 / iosX64) を走査して POM・署名・javadoc jar を付けるだけで作り直さない (プラグインの `Platform.kt` で確認)。署名・Central upload・SNAPSHOT ガードを android/ と同じ形にでき、release CI が翻案元と同じ手順で書ける。POM 共通部は kmp/ が別ビルドルート (cross/ADR-0004: monorepo ルートに共通ビルドファイルを置かない) のため複製する。発行設定を module 側に閉じるのは、ルートで `MavenPublishBaseExtension` や `KotlinMultiplatform` を型付き参照すると対応プラグインをルートの plugin classpath に載せる必要が生じ、配置を誤るとルートスクリプトのコンパイルで止まるため (相方レビュー spec-001 の指摘)。

**代替案:**
- **A: PoC どおり素の `maven-publish`** — SwiftPM 連携メタデータの発行は実測済みで確実だが、署名と Central Portal への upload を kmp だけ別実装することになり release CI の手順が分岐する。却下 (agenda A1)。ただし Decision 6 の検算で SwiftPM メタデータが欠けた場合はこの案へ戻す判断をユーザーに仰ぐ
- **B: POM 共通部を android/ 側から共有する** — ビルドルート横断の共有物を増やす。数行の複製で済み、URL 変更時の取り残しは Decision 6 の検算 (両 POM の比較) で拾う。却下
- **C: 発行の共通設定をルートの `plugins.withId("com.vanniktech.maven.publish")` に置く (phase-5 の android/ と同じ形)** — 発行するのが 1 module だけの kmp/ では共通化の利得が無く、ルートに vanniktech / KGP の plugin classpath 宣言が要る。却下

### Decision 2: version の導出式は kmp/build.gradle.kts にも同じものを書く

**採用案:** `kmp/build.gradle.kts` に phase-5 design Decision 3 と同じ式を書く: `providers.gradleProperty("version").orNull` があればそれ、無ければ `libs.versions.ksdialogs.get()` (`0.1.0-SNAPSHOT`)。注入値は `^\d+\.\d+\.\d+(-(alpha|beta|rc)\.\d+)?$` に限り、空文字・空白・形式外はビルド設定を失敗させる。`:ksdialogs-kmp` の `group = "jp.kamusoft"` / `version` と、`androidMain` の `api("jp.kamusoft:ksdialogs-core:$version")` の版をこの値から取る (カタログ `ksdialogs` の直接参照と直書き `"0.1.0"` を廃止)。version が `-SNAPSHOT` で終わるとき、名前に `MavenCentral` を含むタスク (`dropMavenCentralDeployment` を除く) を `doFirst` で SNAPSHOT 固有の診断 (`Refusing to publish a SNAPSHOT version to Maven Central: <version>`) を出して失敗させる。ガードはルートの `subprojects { tasks.configureEach { ... } }` でタスク名だけで掛け、プラグインの型を参照しない。`api-surface-check` は `group` / `version` を持たない (発行しない)。

**理由:** kmp/ の version・本体依存版・Decision 3 の Swift 参照導出が同じ 1 つの値から出るため、lockstep (cross/ADR-0009) を手で揃える箇所が無い。別ビルドルートで式は数行、ずれは Decision 6 の検算 (android/ と kmp/ を同じ `-Pversion=` で発行し、kmp POM の依存版と android artifact の version が同一文字列) で機械的に検出できる。

**代替案:**
- **A: `android/gradle/` 配下の共有スクリプトを両ルートから `apply(from=)`** — phase-5 の design 改訂が要り、kts の `apply(from=)` は型安全でなく制約が多い。却下 (agenda A5)
- **B: 規約プラグイン用 included build (`build-logic`)** — 3 行の式に対して過剰で、ビルドルートが実質 1 つ増える。却下

### Decision 3: Swift 参照は version から導出し、URL だけプロパティで上書きできる

**採用案:** `swiftPMDependencies { }` の宣言を version で分岐する。

```kotlin
// kmp/ksdialogs-kmp/build.gradle.kts (要旨)
val swiftPackageUrl = providers.gradleProperty("ksdialogs.swiftPackageUrl")
    .orElse("https://github.com/kamusoft/KsDialogs-SPM")
swiftPMDependencies {
    iosMinimumDeploymentTarget.set("17.0")
    if (isSnapshot) {
        localSwiftPackage(rootProject.layout.projectDirectory.dir("../ios"), listOf("KsDialogs"))
    } else {
        swiftPackage(url(swiftPackageUrl.get()), exact(version.toString()), listOf(product("KsDialogs")))
    }
}
```

- SNAPSHOT (開発既定) では local 参照。URL プロパティは無視する (local 参照に URL は無い)
- リリース版 (注入時) では remote 参照。exact は version と同じ値で、上書き手段を持たない
- URL プロパティ `ksdialogs.swiftPackageUrl` は dry-run (phase-8) がスナップショットのローカル clone (commit + tag 済み) の `file://` URL を渡すためのもの。`file://` が KGP の `swiftPackage(url(...))` に通ることは PoC (KGP 2.4.10、`file://` の bare clone + tag) で実測済み。公式記述は無いため Decision 6 で再確認する
- 専用のモード切替スイッチは持たない
- `iosMinimumDeploymentTarget.set("17.0")` は分岐の外で常に宣言し、local / remote のどちらでも metadata に `17.0` が乗る (cross/ADR-0009 が配布物の最低 OS 担保の正としている値。分岐の組み替えで落ちないよう受け入れ条件に入れる)

**理由:** リリース版で local 参照を選べない形にすることで、`localSwiftPackage` のまま発行して発行者の絶対パスが伝播する事故 (PoC 判明事実 1、警告も出ない) を構造的に防ぐ。exact の値が version と同じ式から出るので、KMP artifact x.y.z → SPM tag x.y.z が機械的に揃う。日常開発は SNAPSHOT のまま local 参照で、`../ios` のライブ編集も Sample の `KotlinMultiplatformLinkedPackage/` の中身も変わらない (agenda A3 の解消)。cross/ADR-0008 (proposed) の Consequences に 2026-09-08 追記済み。

**代替案:**
- **A: Gradle プロパティで local / remote を明示切替** — フラグ忘れが残り、publish 時に local なら失敗させるガードを別途書くことになる。却下 (agenda A2)
- **B: version 導出のみ、URL 上書きなし** — dry-run のたびに配信リポジトリへ一時 tag を push し検証後に削除する運用になる。却下 (agenda A2)

既知の限界: SNAPSHOT を `publishToMavenLocal` した成果物の metadata には local 参照 (発行者の絶対パス) が乗り、同一マシンでだけ動く。SNAPSHOT の消費はリポジトリ内 Sample の composite build が担い、リポジトリ外の検証はリリース版で行う (文書化は docs-refresh)。

### Decision 4: `@Throws` の回帰検査は androidHostTest の反射テスト

**採用案:** `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt` を追加する。公開 interface `KsDialog` / `KsLoading` / `KsToast` (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialog.kt` / `KsLoading.kt` / `KsToast.kt`) の Java クラスから宣言メソッドを反射で列挙し、synthetic / bridge と `DefaultImpls` 由来を除いた公開メソッドについて:

- 肯定: `KsDialog.show(DialogViewModel, DialogPlacement?)`・`KsLoading.show(LoadingViewModel, DialogPlacement?)`・`KsLoading.start(LoadingViewModel, DialogPlacement?, action)`・`KsToast.show(ToastViewModel, Int?, DialogPlacement?)` の 4 経路 (現状の `@Throws` 付与箇所: `KsDialog.kt:32` / `KsLoading.kt:50` / `KsLoading.kt:120` / `KsToast.kt:58`) の `exceptionTypes` に `DialogException` が含まれる
- 否定: 上記以外の公開メソッド (message 引数の overload・型指定 show / start・`hide`・`setMessage`・registry 等) の `exceptionTypes` が空である
- 4 経路の特定はメソッド名と引数型で行い、名前を列挙して素通りしないよう、列挙した 4 経路が実際に見つかったこと (件数一致) も検査する
- 検査の範囲は **この 3 interface の宣言メソッド**に限る (registry 型や ViewModel 契約など他の公開型は対象外。Swift から呼ばれて失敗しうる入口が 3 interface に集約されているため)。否定側は 4 経路以外の全宣言メソッドを対象にする
- 退行検出の確認は肯定側 (4 経路の 1 つから外す) と否定側 (非対象メソッドの 1 つに一時的に付ける) の両方を一度ずつ行い、復元後に `git diff` が空であることを確認する

**理由:** `@Throws` は commonMain の宣言 1 つで、JVM では throws 節、Native では Swift の `throws` に変換される。JVM 側の throws 節の消失は共通宣言の欠落と同値 (これらの interface は expect/actual で分かれていない)。ホスト JVM で走り、本体検証 CI (`kmp / verify` の `testAndroidHostTest`) の件数検査にそのまま乗る。既存の ObjC ヘッダ検査は suspend 関数の completionHandler が `@Throws` の有無に関わらず `NSError` を持つため (生成ヘッダで `setMessage` と `show` が同形) 4 経路中 1 経路しか区別できない。

**代替案:**
- **A: `ObjCApiSurfaceTests` (ヘッダ検査) に足す** — 非 suspend の `KsToast.show` の `error:` 引数しか区別できない。却下 (agenda B)
- **B: `api-surface-check` に組み込む** — コンパイル検査は `@Throws` の有無に反応しない。却下 (agenda B)
- **C: 検査なし (archive の evidence のみ)** — 退行が利用者の Swift 側の abort でしか分からない。却下 (agenda B)

### Decision 5: Sample の依存版はカタログ値に揃える

**採用案:** `samples/kmp/shared/build.gradle.kts` の `api("jp.kamusoft:ksdialogs-kmp:0.1.0")` を `api("jp.kamusoft:ksdialogs-kmp:${libs.versions.ksdialogs.get()}")` にする。`samples/kmp/settings.gradle.kts` の dependencySubstitution は phase-5 が新座標へ追随済みのものをそのまま使うが、そこにある「KMP facade は Maven publication を生成しないため置換を明示する」というコメントは本変更後に虚偽になるので、「公開版への無音フォールバックを避けるため常にローカルソースへ置換する」という現在形の説明に改める。置換の実効は、`kmp/` の commonMain に意図的なコンパイルエラーを一時注入して Sample のビルドが失敗すること (置換が効かなければ公開版 / キャッシュで成功してしまう) で確かめ、復元後に `git diff` が空であることを確認する。

**理由:** 置換は座標で行われるため版の値に挙動は左右されないが、直書きが残ると「版はカタログ 1 箇所」が崩れ、リリース後に古い版が Sample に残り続ける。

**代替案:**
- **A: 現状の直書きを残す** — 実害は無いが版の置き場が 2 つになる。却下

### Decision 6: 発行検証は `publishToMavenLocal` の 4 通りと android/ との照合

**採用案:** 次を実行し、証跡を `evidence/` に残す (生ログは sanitize 済み抜粋のみ — config `lint.exclude` の運用)。各ケースは**空の一時 Maven local repository** (`-Dmaven.repo.local=<scratch>/case-N`、開始時に空であることを確認) へ発行し、前回の `.asc`・JSON・klib が残って偽の存在確認や偽の署名検出を生まないようにする。一時 repository は確認後に trash で片付ける。

| # | 実行 | 検算 |
|---|---|---|
| 1 | `-Pversion=` なし | version `0.1.0-SNAPSHOT`、root の `swiftpm-metadata.json` が Local 参照 (`../ios` 相当)・deployment target `17.0` |
| 2 | `-Pversion=0.1.0-beta.1` | version・依存版 `0.1.0-beta.1`、`swiftpm-metadata.json` が Remote `https://github.com/kamusoft/KsDialogs-SPM` + Exact `0.1.0-beta.1`・deployment target `17.0`、ローカルパスを含まない |
| 3 | `-Pversion=0.1.0-beta.1 -Pksdialogs.swiftPackageUrl=file:///<一時 clone>` | `swiftpm-metadata.json` が Remote `file://...` + Exact `0.1.0-beta.1` (`file://` が通ること) |
| 4 | #2 と同じ `-Pversion=` で android/ も同じ repository へ発行 | kmp の android publication の POM の `ksdialogs-core` 依存版 = android artifact の version (同一文字列)、両 POM の共通部が同一 |

#2 で発行物の構成 (Decision 1 の表) を確認し、Gradle Module Metadata の内容も検査する: root `.module` に各ターゲット publication への `available-at` 相当の variant と、`swiftpm-metadata.json` を artifact に持つ SwiftPM 連携 variant があること / android `.module` の API variant が `jp.kamusoft:ksdialogs-core` (同版) を持つこと / iOS 各 `.module` の variant が本体 klib と cinterop klib を参照し `kotlinx-coroutines-core` を依存に持つこと / いずれの `.module` にもテスト専用ライブラリ (kotlin-test / kotlinx-coroutines-test) が無いこと。5 publication すべての POM の共通項目と、各 sources jar に対応 source set の代表 `.kt` (root: `KsDialog.kt`、android: `androidMain` の actual、iOS: `iosMain` の actual) が入っていることも見る。SNAPSHOT のまま Central 向けタスクが `--offline` で SNAPSHOT 固有の診断を出して失敗すること、`:api-surface-check` に発行タスクが無いこと、鍵なし / 一時鍵ありの署名挙動 (phase-5 と同じ手順、`swiftpm-metadata.json.asc` を含む) も同じ枠で確認する。

**理由:** vanniktech 経由で KGP の SwiftPM 連携メタデータが発行物に載ることは公式に明記が無く (リスク ①)、`file://` の可否も公式記述が無い (リスク ③)。設計の前提 2 つを実装内で実物で確かめる。PoC は素の `maven-publish` の出力を実証したもので、vanniktech を加えた今回の実物は保証しないため `.module` の中身まで見る。消費者側の実解決は phase-8 の責務 (agenda A6)。

**代替案:**
- **A: リポジトリ外の一時 KMP 消費者で解決・framework リンクまで確認する** — phase-8 の `verification/kmp/` と重複し、xcodeproj と linkage package の準備が本変更の範囲を超える。却下
- **B: 発行物の存在確認だけ (metadata の中身を見ない)** — リスク ①③ が実装完了時点で未確認のまま phase-8 に流れる。却下
- **C: 共用の `~/.m2` へ発行し GAV を事前削除する** — 削除漏れで前回成果物が残り得る。却下

## Risks / Trade-offs

- vanniktech が KMP publication に SwiftPM メタデータを残さない場合 (リスク ①): Decision 6 #1 で判明する。素の `maven-publish` へ戻す (Decision 1 代替案 A) か、vanniktech の版を上げるかをユーザーに諮る
- `file://` URL が KGP の `url(...)` で拒否される場合 (リスク ③): Decision 6 #3 で判明する。dry-run の形 (agenda C1) の見直しになるため phase-8 の agenda に申し送る
- 導出式の複製 (Decision 2) は android/ と乖離しうる。Decision 6 #4 と、release CI の再ビルド同一性検査 (phase-9) が拾う
- vanniktech 0.37.0 の検証済み範囲 (Kotlin 2.4.0 / AGP 9.2.1) を手元の版が少し超える点は phase-5 と同じ

## Migration Plan

- phase-5 の change (add-native-distribution) の実装完了後に着手する (カタログの SNAPSHOT 化・`android/build.gradle.kts`・`kmp/` の依存座標 `ksdialogs-core` が前提)
- 実装順: `kmp/build.gradle.kts` 新設 (Decision 2) → 発行設定 (Decision 1) → Swift 参照導出 (Decision 3) → テスト (Decision 4) → Sample (Decision 5) → 発行検証 (Decision 6) → handbook の件数追随
- 既存の開発手順 (`cd kmp && ./gradlew allTests`) は変わらない。`kmp/local.properties` の要件も同じ

## Open Questions

なし。

## ADR 候補

- Decision 3 (Swift 参照の version 導出と URL 上書き): phase-8 / 9 の workflow が前提にする機構で、cross/ADR-0008 (proposed) の Consequences に 2026-09-08 追記済み。蒸留時に 0008 の accepted 昇格と一緒に確定する
- Decision 2 (version の導出式): phase-5 design Decision 3 と同じ決定。cross/ADR-0009 へ溶かすか新規かは phase-5 の蒸留時の判断に従う
- Kotlin サポート範囲の宣言 (agenda A4、同 minor 2.4.x): cross/ADR-0008 の Consequences「サポートする Kotlin 範囲を文書で宣言する」の具体化。本変更にコードは無いため蒸留時に 0008 の同じ段落へ具体形を書き足す候補
- Decision 1 / 4 / 5 / 6: なし (翻案元の踏襲、またはテスト・検証手順で可逆)
