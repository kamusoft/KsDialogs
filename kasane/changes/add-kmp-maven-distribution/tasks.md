# Tasks: add-kmp-maven-distribution

前提: phase-5 の change (add-native-distribution) の実装が完了していること (カタログ `ksdialogs` = `0.1.0-SNAPSHOT`、`android/build.gradle.kts` の導出式、`kmp/` の本体依存座標 `ksdialogs-core`)。発行検証はケースごとに空の一時 Maven local repository (`-Dmaven.repo.local=<scratch>/case-N`) へ行い、確認後に trash で片付ける (design Decision 6)。

## 1. version の導出式 (kmp/build.gradle.kts 新設)

- [ ] 1.1 ルート `kmp/build.gradle.kts` を新設し、`-Pversion=` 注入を優先する導出式・注入値の形式検査 (`X.Y.Z` / `X.Y.Z-{alpha|beta|rc}.N` 以外は失敗)・SNAPSHOT 中の Central 向けタスク (名前に `MavenCentral` を含み `dropMavenCentralDeployment` を除く) を `doFirst` で診断 `Refusing to publish a SNAPSHOT version to Maven Central: <version>` を出して失敗させるガードを、タスク名だけで (プラグインの型を参照せず) 書く。`android/build.gradle.kts` との対応をコメントで示す。`:ksdialogs-kmp` の `group` / `version` と `androidMain` の `ksdialogs-core` 依存版をこの値から取り、直書き `"0.1.0"` とカタログ直接参照を消す (design Decision 2) (→ Requirement: version の単一ソースと注入 (kmp))
- [ ] 1.2 `-Pversion=` なし / `-Pversion=0.1.0-beta.1` で `publishToMavenLocal` して version と android publication の POM の依存版を確認し、空文字・`' 1.0.0 '`・`v1.0.0`・`1.0.0-pre` で同じ診断で設定が失敗することを確認する (→ Scenario: 注入なしの開発既定値 / 不正な注入値の拒否)
- [ ] 1.3 SNAPSHOT のまま `--offline` で `publishToMavenCentral` と名前に `MavenCentral` を含む各発行タスクを実行し、SNAPSHOT 固有の診断で失敗すること (ネットワーク・認証を理由としない)、`dropMavenCentralDeployment` にガードが無いこと、`publishToMavenLocal` が通ることを確認する (→ Scenario: SNAPSHOT の Central 発行拒否)

## 2. 発行設定 (module 側で完結)

- [ ] 2.1 `kmp/ksdialogs-kmp/build.gradle.kts` に `com.vanniktech.maven.publish` 0.37.0 を適用し、`KotlinMultiplatform(JavadocJar.Empty(), sourcesJar = true)` (`androidVariantsToPublish` は既定値のまま)・`publishToMavenCentral()`・鍵の有無に連動する `signAllPublications()`・POM (共通部は `android/build.gradle.kts` と同じ値、name `KsDialogs KMP` と design Decision 1 の description) を書く。ルートには vanniktech / KGP の型参照も `apply false` も足さない。`:api-surface-check` には適用しない (design Decision 1) (→ Requirement: KMP 発行物の座標と内容 / KMP 発行物の署名)
- [ ] 2.2 `:api-surface-check` に発行タスクが生えていないことを確認する (→ Scenario: api-surface-check の非公開)
- [ ] 2.3 鍵なしの `publishToMavenLocal` (空 repository) で `.asc` が 1 つも生成されず成功すること、一時生成した検証用鍵を `signingInMemoryKey` 系プロパティで渡した `publishToMavenLocal` (別の空 repository) で root と 4 ターゲット publication の全成果物と `swiftpm-metadata.json` に `.asc` が生成されることを確認し、鍵は破棄する (→ Scenario: 鍵なしのローカル発行 / 鍵ありのローカル発行)

## 3. Swift 参照の version 導出

- [ ] 3.1 `swiftPMDependencies` の宣言を version で分岐する: SNAPSHOT なら `localSwiftPackage(../ios)`、それ以外は `swiftPackage(url(<ksdialogs.swiftPackageUrl> または既定の配信リポジトリ URL), exact(<version>), product KsDialogs)`。SNAPSHOT ではプロパティを無視する。`iosMinimumDeploymentTarget.set("17.0")` は分岐の外に置く (design Decision 3) (→ Requirement: Swift 参照の version 導出)
- [ ] 3.2 SNAPSHOT で発行した root の `swiftpm-metadata.json` がローカル参照で deployment target `17.0` を持つこと、`samples/kmp` の `androidApp` と iOS の shared framework をビルドしても `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` に差分が出ないことを確認する (→ Scenario: 開発既定のローカル参照)
- [ ] 3.3 `-Pversion=0.1.0-beta.1` で発行した `swiftpm-metadata.json` が `https://github.com/kamusoft/KsDialogs-SPM` + exact `0.1.0-beta.1` + deployment target `17.0` のリモート参照で、発行者環境のローカルパスを含まないことを確認する (→ Scenario: リリース版の配信リポジトリ参照)
- [ ] 3.4 配信リポジトリ配置のローカル clone (`scripts/spm-snapshot/sync-snapshot.sh` で同期し commit + tag `0.1.0-beta.1`、push しない) を用意し、`-Pksdialogs.swiftPackageUrl=file:///<clone>` で発行した `swiftpm-metadata.json` が `file://` URL + exact + deployment target `17.0` のリモート参照であることを確認する。clone は確認後に trash で片付ける (→ Scenario: URL の上書き)

## 4. `@Throws` の回帰検査

- [ ] 4.1 `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt` を追加する: `KsDialog` / `KsLoading` / `KsToast` の Java クラスの宣言メソッド (synthetic / bridge / `DefaultImpls` 由来を除く) を反射で列挙し、4 経路の `exceptionTypes` に `DialogException` があること・4 経路が過不足なく見つかること・それ以外の宣言メソッドの `exceptionTypes` が空であることを検査する。失敗メッセージにメソッド名を含める (design Decision 4) (→ Requirement: Swift 境界の失敗経路の宣言検査)
- [ ] 4.2 `./gradlew testAndroidHostTest` で新テストが通ること、4 経路の 1 つから `@Throws` を外すと当該メソッド名を示して失敗すること、非対象メソッド 1 つ (`KsLoading.setMessage`) に `@Throws(DialogException::class)` を付けると当該メソッド名を示して失敗することを確認し、いずれも復元後に `git diff` が空であることを確認する (→ Scenario: 失敗しうる 4 経路の宣言 / 失敗しない経路に宣言が無いこと / 宣言が落ちたときの検出 / 宣言が余計に付いたときの検出)

## 5. Sample の追随

- [ ] 5.1 `samples/kmp/shared/build.gradle.kts` の `ksdialogs-kmp` の版をカタログ `ksdialogs` から取る形にし、`samples/kmp/settings.gradle.kts` の置換コメントを「公開版への無音フォールバックを避けるため常にローカルソースへ置換する」という現在形の説明に改める (design Decision 5) (→ Requirement: Sample の依存版)
- [ ] 5.2 `androidApp` の `assembleDebug` が成功すること、`kmp/ksdialogs-kmp/src/commonMain` に意図的なコンパイルエラーを一時注入すると失敗すること、復元後に `git diff` が空であることを確認する (→ Scenario: Sample のビルドとソース参照)

## 6. 発行検証と証跡

- [ ] 6.1 `-Pversion=0.1.0-beta.1` の `publishToMavenLocal` (空 repository) の発行物を検証し証跡を `evidence/` に残す: root と 4 ターゲット publication の配置 (design Decision 1 の表。root の `swiftpm-metadata.json` を含む)、空 javadoc jar、各 sources jar の代表 `.kt`、5 つの POM の共通項目と name / description。SwiftPM 連携メタデータが欠けていれば実装を止めてユーザーに諮る (design Risks) (→ Scenario: ローカル発行での発行物検証)
- [ ] 6.2 同じ発行物の `.module` を検査して証跡に残す: root のターゲット委譲 variant と SwiftPM 連携 variant、Android API variant の `ksdialogs-core:0.1.0-beta.1`、iOS 各 variant の klib / cinterop klib 参照と `kotlinx-coroutines-core`、テスト専用ライブラリの不在 (design Decision 6) (→ Scenario: Gradle Module Metadata の内容)
- [ ] 6.3 同じ `-Pversion=0.1.0-beta.1` で `android/` も同じ空 repository へ `publishToMavenLocal` し、kmp の android publication の POM の依存版と `ksdialogs-core` artifact の version が同一文字列であること、両 POM の共通部 (url / license / developers / scm / inceptionYear) が同一であることを確認して証跡に残す (→ Scenario: リリース version の注入と android/ との一致)

## 7. 規範の追随と最終確認

- [ ] 7.1 検証 CI と同じ `./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata --rerun-tasks` を `kmp/` で実行して全件成功を確認し、`testAndroidHostTest` / `iosSimulatorArm64Test` の件数を記録する。`kasane/handbook/cross/test-execution.md` の kmp/ の件数記録 (件数と日付) を新テスト追加後の値に更新する (→ Requirement: Swift 境界の失敗経路の宣言検査)
