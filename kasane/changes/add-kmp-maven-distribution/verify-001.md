# 一致検証結果: add-kmp-maven-distribution (001 回目)

**日付**: 2026-09-09
**判定**: VALID
**対象**: 未コミットの作業ツリー (`kmp/build.gradle.kts` 新設・`kmp/ksdialogs-kmp/build.gradle.kts`・`kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt` 新設・`android/build.gradle.kts`・`samples/kmp/shared/build.gradle.kts`・`samples/kmp/settings.gradle.kts`・`samples/android/settings.gradle.kts`・`kasane/handbook/cross/test-execution.md`)
**デルタスペック**: `kasane/changes/add-kmp-maven-distribution/specs/kmp-maven-distribution/spec.md` (Requirement 5 / Scenario 11)、`kasane/changes/add-kmp-maven-distribution/specs/kmp-api-surface/spec.md` (Requirement 1 / Scenario 4)

## サマリー

Requirement 6 / Scenario 15 のすべてが「一致」または「deviation 記録済み」で、未記録の欠落・乖離は 0 件。tasks.md の 15 項目のチェックに虚偽はなく、足場 (proposal / design / specs) の逆流もない。テストは自分の手で再実行して全件成功を確認した (`kmp/` androidHostTest 78 件 / iosSimulatorArm64 75 件 = 153 件、failures 0)。

deviation.md に記録済みの差分は 7 件で、うち Scenario の字義に触れるのは 2 件 (「リリース版の配信リポジトリ参照」を root publication 単独発行で確認したこと、SNAPSHOT ガードの集約 `publish` 経路の残り)。どちらも「合意済み差分」として扱い ⚠️ とした。ただし後者は、対応する Scenario の WHEN が「名前に `MavenCentral` を含むタスク」に限定されており `publish` を含まないため、**Scenario の字義自体は満たしている** (実装が spec より広く捕まえた結果の記録)。

証跡が残っていない確認が 2 件ある (@Throws 検査の負のケース 2 種と Sample のコンパイルエラー注入)。いずれも Requirement の充足を実装の静的検査と別手段の実測で確認できたため ❌ とはしていない — 詳細は「観察事項」節。

## 対応表: specs/kmp-maven-distribution

### Requirement: version の単一ソースと注入 (kmp) [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 注入なしの開発既定値 | `kmp/build.gradle.kts:23-24`, `kmp/build.gradle.kts:31-39`, `kmp/build.gradle.kts:43`, `kmp/ksdialogs-kmp/build.gradle.kts:20`, `kmp/ksdialogs-kmp/build.gradle.kts:168` | `evidence/version-injection.txt` §1 (5 publication すべて `0.1.0-SNAPSHOT`、android publication の POM の `ksdialogs-core` 依存も同値) | ✅ 一致 |
| リリース version の注入と android/ との一致 | 同上 + `android/build.gradle.kts:36-44` (既存の導出式) | `evidence/android-version-alignment.txt` §1 (同一の空 repository へ両ビルドルートを `-Pversion=0.1.0-beta.1` で発行し、POM の依存版と発行された `ksdialogs-core` の version が同一文字列) | ✅ 一致 |
| 不正な注入値の拒否 | `kmp/build.gradle.kts:29`, `kmp/build.gradle.kts:34-37` | `evidence/version-injection.txt` §3 (5 値の受理/拒否表と診断文言)。**自分で再現**: `./gradlew help -Pversion=v1.0.0` → 設定段階で `-Pversion に指定できるのは X.Y.Z または X.Y.Z-{alpha\|beta\|rc}.N の形式だけです (指定値: "v1.0.0")` | ✅ 一致 |
| SNAPSHOT の Central 発行拒否 | `kmp/build.gradle.kts:68-87` (タスク名判定 + 2 段発火) | `evidence/snapshot-central-publish-guard.txt` §2 (実在する 10 の対象タスクすべてが SNAPSHOT 診断のみで失敗・`dropMavenCentralDeployment` は素通り・`publishToMavenLocal` は成功)。**自分で再現**: `--offline :ksdialogs-kmp:publishToMavenCentral` → `Refusing to publish a SNAPSHOT version to Maven Central: 0.1.0-SNAPSHOT.` のみ、`--offline :ksdialogs-kmp:dropMavenCentralDeployment` → タスク自身の `deploymentId` 未設定で失敗 (ガード例外ではない) | ⚠️ deviation 記録済み |

Requirement 本文の個別要求の照合:

- `group` = `jp.kamusoft` → `kmp/ksdialogs-kmp/build.gradle.kts:19` ✅
- `version` は `-Pversion=` 優先・無ければカタログ `ksdialogs` → `kmp/build.gradle.kts:23-39`、カタログ実体は `android/gradle/libs.versions.toml:15` (`0.1.0-SNAPSHOT`。`kmp/settings.gradle.kts:32` が同ファイルを読む) ✅
- `androidMain` の `ksdialogs-core` の版が同じ値 → `kmp/ksdialogs-kmp/build.gradle.kts:168` (`api("jp.kamusoft:ksdialogs-core:$version")`) ✅
- 形式検査 (`X.Y.Z` / `X.Y.Z-{alpha|beta|rc}.N`) と空文字・空白の拒否 → `kmp/build.gradle.kts:29` の正規表現は `matches` で全体一致のため前後空白を含む値も落ちる ✅
- 診断文言 `Refusing to publish a SNAPSHOT version to Maven Central: <version>` → `kmp/build.gradle.kts:71-73` ✅ (仕様どおり。`android/` 側は既存文言のまま — deviation の付随修正に記録)
- ローカル発行が妨げられない → `evidence/snapshot-central-publish-guard.txt` §2 末尾 ✅

**deviation との対応** (⚠️ の根拠): deviation.md 5 項目め — ガードを `doFirst` から「設定段階の `startParameter.taskNames` 照合 + `taskGraph.whenReady` の `allTasks` 照合」の 2 段へ変更したこと、および集約タスク `publish` 経由でのみ SNAPSHOT 診断と認証情報未解決が同時に報告されること。**補足**: この Scenario の WHEN は「`publishToMavenCentral`、および `publishAllPublicationsToMavenCentralRepository` 等の名前に `MavenCentral` を含むタスク」であり `publish` は対象集合に入らない。したがって Scenario の THEN (「ネットワーク不通や認証不足を理由としない」) は字義どおり満たされており、deviation は実装が spec より広く捕まえた分の保守的な記録として読める。

### Requirement: Swift 参照の version 導出 [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 開発既定のローカル参照 | `kmp/ksdialogs-kmp/build.gradle.kts:152-154` (`iosMinimumDeploymentTarget` は分岐の外、SNAPSHOT は `localSwiftPackage(../ios)`) | `evidence/swiftpm-reference-derivation.txt` §1 (`SwiftPMDependency.Local` + `absolutePath: <repo>/ios` + `iosDeploymentVersion: 17.0`、`samples/kmp` の `androidApp:assembleDebug` と `shared:linkDebugFrameworkIosSimulatorArm64` の後に `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の `git status` が空) | ✅ 一致 |
| リリース版の配信リポジトリ参照 | `kmp/ksdialogs-kmp/build.gradle.kts:147-148`, `kmp/ksdialogs-kmp/build.gradle.kts:156-160` | `evidence/swiftpm-reference-derivation.txt` §2 (`SwiftPMDependency.Remote` + `https://github.com/kamusoft/KsDialogs-SPM` + `Exact 0.1.0-beta.1` + `17.0`、ローカルパスの不在を全文走査で確認) | ⚠️ deviation 記録済み |
| URL の上書き | 同上 (`providers.gradleProperty("ksdialogs.swiftPackageUrl").orElse(既定 URL)`) | `evidence/swiftpm-reference-derivation.txt` §3 (`file://` URL + `Exact 0.1.0-beta.1` + `17.0`、cinterop klib 生成まで完走 = スキーマを理由に失敗しない) | ✅ 一致 |

Requirement 本文の個別要求の照合:

- 「exact の値を変える手段は持たない」→ `kmp/ksdialogs-kmp/build.gradle.kts:158` は `exact(version.toString())` のみで、exact を差し替えるプロパティは存在しない (`grep` で `ksdialogs.` 系プロパティは `ksdialogs.swiftPackageUrl` の 1 本のみ) ✅
- 「SNAPSHOT のときプロパティは無視される」→ `swiftPackageUrl.get()` の呼び出しは `else` 枝の内側 (`kmp/ksdialogs-kmp/build.gradle.kts:157`) のみ ✅ (review-002 が実測で追認)
- 「どちらの参照でも deployment target は `17.0`」→ `kmp/ksdialogs-kmp/build.gradle.kts:152` が分岐の外 ✅ (3 ケースの証跡すべてで `17.0`)
- 「SwiftPM 連携メタデータは root publication の成果物として発行される」→ `evidence/publish-to-maven-local.txt` §1 (`ksdialogs-kmp-0.1.0-beta.1-swiftpm-metadata.json` が root にある) ✅

**deviation との対応** (⚠️ の根拠): deviation.md 4 項目め — 既定 URL のケースは全 publication ではなく root publication 単独発行 (`publishKotlinMultiplatformPublicationToMavenLocal`) で確認したこと。原因は配信リポジトリに `0.1.0-beta.1` の tag が未 push で、cinterop klib 生成が Swift パッケージの実解決を要求するため。Scenario の THEN が問う対象 (root publication の SwiftPM 連携メタデータの内容) は満たされており、未実証なのは「全 publication の他成果物が URL に依存しないこと」で、これは phase-8 / phase-9 へ送られている。

### Requirement: KMP 発行物の座標と内容 [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| ローカル発行での発行物検証 | `kmp/ksdialogs-kmp/build.gradle.kts:12` (`alias(libs.plugins.mavenPublish)`), `:25-85` (`configure(KotlinMultiplatform(JavadocJar.Empty(), SourcesJar.Sources()))` / `publishToMavenCentral()` / POM) | `evidence/publish-to-maven-local.txt` §1-4 (5 publication の配置表・`-swiftpm-metadata.json` の存在・javadoc jar が 2 エントリのみ・sources jar の代表 `.kt`・5 POM の共通項目と name / description) | ✅ 一致 |
| Gradle Module Metadata の内容 | 同上 | `evidence/publish-to-maven-local.txt` §5 (root の 15 variant = ターゲット委譲 12 + `swiftPMDependenciesMetadataElements` + metadata/sources、Android API variant の `ksdialogs-core:0.1.0-beta.1`、iOS variant の本体 klib + cinterop klib + `kotlinx-coroutines-core`、`kotlin-test` / `coroutines-test` の grep ヒット 0) | ✅ 一致 |
| api-surface-check の非公開 | `kmp/ksdialogs-kmp/build.gradle.kts:12` (発行プラグインの適用はこのモジュールのみ)、`kmp/build.gradle.kts:16` (ルートは KGP の `apply false` だけ)、`kmp/build.gradle.kts:43` (座標はモジュール側が読む形) | `evidence/snapshot-central-publish-guard.txt` §3。**自分で再現**: `./gradlew :api-surface-check:tasks --all \| grep -i publish` → `prepareLintJarForPublish` のみ (AGP の lint jar 準備タスク。Maven 発行タスクなし) | ✅ 一致 |

Requirement 本文の個別要求の照合: 座標 5 件 (root + android + iosarm64 + iossimulatorarm64 + iosx64) の同一 version・配置・POM 共通項目・sources jar の内容・`.module` の 4 条件・テスト専用ライブラリの不在は、すべて `evidence/publish-to-maven-local.txt` の §1〜§5 と `evidence/android-version-alignment.txt` §2 (POM 共通部が `android/` 側 2 POM とも 1 文字違わないこと) で個別に押さえられている ✅

**注記** (deviation 記録済みの範囲): この検証は `-Pksdialogs.swiftPackageUrl=file://<tag 付きローカル clone>` を指した発行物に対するもので、公開 URL での全 publication 発行は未実証 (deviation.md 4 項目め、`evidence/publish-to-maven-local.txt:11-13` および `evidence/swiftpm-reference-derivation.txt` §2 に実証範囲が明記されている)。Scenario の GIVEN は「発行設定を導入した `kmp/`」で URL を指定していないため、Scenario 自体の充足は妨げられない。

### Requirement: KMP 発行物の署名 [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 鍵なしのローカル発行 | `kmp/ksdialogs-kmp/build.gradle.kts:44` (`signAllPublications()`), `:88-91` (`signing` 拡張の `setRequired` を `signingInMemoryKey` の有無に連動) | `evidence/publish-signing.txt` §1 (5 つの Sign タスクが SKIPPED、`.asc` 0 件、BUILD SUCCESSFUL) | ✅ 一致 |
| 鍵ありのローカル発行 | 同上 | `evidence/publish-signing.txt` §2 (成果物 33 件 / `.asc` 33 件の 1 対 1、publication ごとの内訳表、`-swiftpm-metadata.json.asc` の存在、`gpg --verify` の成功、鍵の破棄) | ✅ 一致 |

### Requirement: Sample の依存版 [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| Sample のビルドとソース参照 | `samples/kmp/shared/build.gradle.kts:49` (`api("jp.kamusoft:ksdialogs-kmp:${libs.versions.ksdialogs.get()}")`), `samples/kmp/settings.gradle.kts:41-44` (置換の維持), `samples/kmp/settings.gradle.kts:37-40` (説明の書き直し) | `evidence/swiftpm-reference-derivation.txt` §1 (`:androidApp:assembleDebug` の成功)。**自分で再現**: `samples/kmp` で `./gradlew :shared:dependencies` → 全 configuration で `jp.kamusoft:ksdialogs-kmp:0.1.0-SNAPSHOT -> project ':kmp:ksdialogs-kmp'`、その下に `jp.kamusoft:ksdialogs-core:0.1.0-SNAPSHOT -> project ':android:ksdialogs-core'` (カタログ値の宣言とローカルソースへの置換が実際に効いていることの直接確認) | ✅ 一致 |

Requirement 本文の個別要求の照合:

- 「版はバージョンカタログの `ksdialogs` から取る」→ `samples/kmp/shared/build.gradle.kts:49` (直書き `0.1.0` を撤去)。カタログの読み元は `samples/kmp/settings.gradle.kts:32` ✅
- 「ソース参照を維持し、`kmp/` の共有コードの変更が Sample のビルド結果を左右する」→ 置換は `samples/kmp/settings.gradle.kts:43` に残存。上記の依存解決ツリーで `project ':kmp:ksdialogs-kmp'` へ解決されていることを確認 ✅
- 「置換の説明は、KMP facade が発行設定を持つ状態でも成立する内容 (公開版への無音フォールバックを避けるため常にローカルソースへ置換する)」→ `samples/kmp/settings.gradle.kts:37-40` および `:47-48` が「Maven publication を生成しないため」から「公開済みの版へ無音でフォールバックするのを避けるため」へ書き換わっている ✅

## 対応表: specs/kmp-api-surface

### Requirement: Swift 境界の失敗経路の宣言検査 [ADDED]

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 失敗しうる 4 経路の宣言 | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialog.kt:32`, `KsLoading.kt:50`, `KsLoading.kt:120`, `KsToast.kt:58` | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt:89-118` (`失敗しうる 4 経路は Swift 境界へ届ける宣言を過不足なく持つ`。`matched.size == 1` の検査が「過不足なく見つかった」を担保)。**自分で実行**: `kmp/` で `./gradlew testAndroidHostTest` → BUILD SUCCESSFUL、`TEST-jp.kamusoft.ksdialogs.kmp.SwiftBoundaryThrowsTests.xml` を含む androidHostTest 78 件 / failures 0 | ⚠️ deviation 記録済み |
| 失敗しない経路に宣言が無いこと | 同上 (3 interface のそれ以外の宣言メソッド) | `SwiftBoundaryThrowsTests.kt:120-134` (`失敗しない経路は Swift 境界へ届ける宣言を持たない`。`declaredSurfaceMethods` が synthetic / bridge / `$default` を除外して 3 interface の宣言メソッドを列挙し、4 経路以外の `exceptionTypes` が空であることを検査) | ✅ 一致 |
| 宣言が落ちたときの検出 | `SwiftBoundaryThrowsTests.kt:110-116` (失敗メッセージが `method.describe()` = `<Interface>.<method>(引数型…)` を含む) | 実行記録は残っていない (tasks 4.2 が実施を主張)。実装の静的検査で充足を確認 — 4 経路のいずれかから `@Throws` が落ちれば `exceptionTypes` が空集合になり、期待集合との `assertEquals` が該当メソッド名入りで失敗する | ✅ 一致 (観察事項 1 参照) |
| 宣言が余計に付いたときの検出 | `SwiftBoundaryThrowsTests.kt:126-131` (同じく `method.describe()` を含む) | 同上。非対象メソッドに `@Throws` が付けば `exceptionTypes` が空でなくなり、`assertEquals(emptyList(), …)` が該当メソッド名入りで失敗する | ✅ 一致 (観察事項 1 参照) |

Requirement 本文の個別要求の照合:

- 「4 経路」の特定 → `SwiftBoundaryThrowsTests.kt:49-60` が interface・メソッド名・第 1 引数型の 3 つ組で特定しており、型指定 overload (`<VM : …> show` / `<VM, T> start`) や message 引数 overload と取り違えない ✅
- 「過不足なく見つかったことも検査する」→ `SwiftBoundaryThrowsTests.kt:98-104` の `assertEquals(1, matched.size, …)` ✅
- 「検査対象は 3 interface の宣言メソッドに限り、registry 型や ViewModel 契約は対象外」→ `SwiftBoundaryThrowsTests.kt:62-66` の `inspectedInterfaces` が 3 件のみ ✅

**deviation との対応** (⚠️ の根拠): deviation.md 7 項目め — spec は「throws 節に `DialogException` があること」だが、実装は期待例外型の**集合の完全一致** (suspend 3 経路 = `DialogException` + `CancellationException`、`KsToast.show` = `DialogException` のみ) へ拡張。commonMain の宣言 (`KsDialog.kt:32` / `KsLoading.kt:50` / `KsLoading.kt:120` は 2 型、`KsToast.kt:58` は 1 型) と一致しており、spec の要求を包含する。

## 追加検査

### tasks.md の虚偽チェック

15 項目すべて `[x]`。対応表と突き合わせて、実装または証跡の裏付けが無いものは無い。

| tasks | 裏付け |
|---|---|
| 1.1 | `kmp/build.gradle.kts` 新設 (実物を確認)。字面の `doFirst` は deviation 記録済みの 2 段ガードへ変更 |
| 1.2 / 1.3 | `evidence/version-injection.txt` / `evidence/snapshot-central-publish-guard.txt` + 自分の再現 |
| 2.1 | `kmp/ksdialogs-kmp/build.gradle.kts:12,25-85,88-91`。字面の `sourcesJar = true` は deviation 記録済みの `SourcesJar.Sources()` |
| 2.2 / 2.3 | `evidence/snapshot-central-publish-guard.txt` §3 + 自分の再現 / `evidence/publish-signing.txt` |
| 3.1〜3.4 | `kmp/ksdialogs-kmp/build.gradle.kts:147-161` / `evidence/swiftpm-reference-derivation.txt` §1〜§3 |
| 4.1 | `SwiftBoundaryThrowsTests.kt` (実物を確認、テスト実行で 2 件が緑) |
| 4.2 | 正のケースは自分で実行済み。負のケース 2 種の実行記録なし (観察事項 1) |
| 5.1 | `samples/kmp/shared/build.gradle.kts:49` / `samples/kmp/settings.gradle.kts:37-48` |
| 5.2 | 正のケースは `evidence/swiftpm-reference-derivation.txt` §1 + 自分の依存解決確認。負のケースの実行記録なし (観察事項 1) |
| 6.1〜6.3 | `evidence/publish-to-maven-local.txt` / `evidence/android-version-alignment.txt` |
| 7.1 | `evidence/kmp-test-counts.txt` + `kasane/handbook/cross/test-execution.md:24` が 153 件 (2026-09-09) へ更新済み。自分の実測 (androidHostTest 78 + iosSimulatorArm64 75 = 153) と一致 |

### 逆流検査 (足場アーティファクトの書き換え)

`git status --short kasane/changes/add-kmp-maven-distribution/specs design.md proposal.md` → 出力なし。足場は propose 時のコミット (`9ed3495`) 以降、実装期間中に一度も書き換わっていない。change ディレクトリで変更されているのは `tasks.md` (チェック更新のみ — diff は `[ ]` → `[x]` の 15 箇所だけで本文は無改変) と、新規追加の `deviation.md` / `evidence/` / `review-*.md` / `second-opinion-code-*.md`。

### 未記録乖離の洗い出し

作業ツリーの変更ファイル 8 本 (新規 2 本含む) をすべて Requirement または deviation の `[付随修正]` に対応づけた。対応の無い変更は無い。

| ファイル | 対応 |
|---|---|
| `kmp/build.gradle.kts` (新規) | Requirement: version の単一ソースと注入 (kmp) |
| `kmp/ksdialogs-kmp/build.gradle.kts` | Requirement: 座標と内容 / 署名 / Swift 参照の version 導出 / version の単一ソース |
| `kmp/ksdialogs-kmp/src/androidHostTest/.../SwiftBoundaryThrowsTests.kt` (新規) | Requirement: Swift 境界の失敗経路の宣言検査 |
| `samples/kmp/shared/build.gradle.kts` | Requirement: Sample の依存版 |
| `samples/kmp/settings.gradle.kts` | Requirement: Sample の依存版 |
| `android/build.gradle.kts` | deviation `[付随修正]` (SNAPSHOT ガードを同じ 2 段の形へ揃える) |
| `samples/android/settings.gradle.kts` | deviation `[付随修正]` (置換コメントの書き直し) |
| `kasane/handbook/cross/test-execution.md` | tasks 7.1 (件数記録の更新) |

deviation.md の 7 項目のうち、`[付随修正]` でない 5 項目 (ルートの KGP `apply false`・`sourcesJar` の書き方・group / version の与え方・「リリース版の配信リポジトリ参照」の確認範囲・ガードの 2 段化と `publish` 経路の残り) はいずれも対応表で ⚠️ または注記として反映済み。

### テストの実行

自分の手で再実行した (`kmp/`):

- `./gradlew testAndroidHostTest` → BUILD SUCCESSFUL。結果 XML の合算で **78 tests / 0 failures / 0 errors / 0 skipped**。`TEST-jp.kamusoft.ksdialogs.kmp.SwiftBoundaryThrowsTests.xml` が生成されている
- `iosSimulatorArm64Test` の結果 XML (18 ファイル) の合算 → **75 tests / 0 failures**
- 合計 153 件 / 0 failures。`kasane/handbook/cross/test-execution.md:24` の記載 (153 tests、iosSimulatorArm64 75 + androidHostTest 78、2026-09-09) と一致

## 観察事項 (判定には影響しない)

### 1. 負のケース 2 種の実行記録が `evidence/` に無い

tasks 4.2 (「`@Throws` を外す / 余計に付ける」→ 該当メソッド名を示して失敗) と tasks 5.2 (「commonMain にコンパイルエラーを注入」→ Sample のビルドが失敗) は `[x]` だが、`evidence/` にその実行の記録が無い (`evidence/swiftpm-reference-derivation.txt` §1 が持つのは正のケースのみ)。

❌ としなかった理由:

- 対応する Requirement の SHALL (「テストが該当するメソッド名を示して失敗する」「Sample はソース参照を維持する」) は実装の静的検査で充足を確認できる — 失敗メッセージが `method.describe()` を含むこと (`SwiftBoundaryThrowsTests.kt:113`, `:129`)、置換宣言が残存すること (`samples/kmp/settings.gradle.kts:43`)
- Sample のソース参照については、コードを触らずに済む別手段 (`./gradlew :shared:dependencies`) で `jp.kamusoft:ksdialogs-kmp:0.1.0-SNAPSHOT -> project ':kmp:ksdialogs-kmp'` を直接確認した。「置換が効かなければ公開版やキャッシュで成功してしまう」という Scenario の懸念は、この解決ツリーで塞がれている
- 本検証では制約によりソースを一時改変できないため、再実行による追認はしていない

見立て: 実装を直す必要は無い。将来同型の「一時的な破壊で検出を確かめる」確認を残すなら、証跡に 1 節を足すのが自然 (蒸留時の申し送り相当で、この change の再作業を要するものではない)。

### 2. review-002 の Minor は解消済み

review-002 (CHANGES_REQUESTED) の唯一の Minor は `evidence/publish-to-maven-local.txt:11-13` の主張が参照先の記録と食い違う点だったが、現在の同ファイル 11-13 行は「URL の値が他の成果物に影響しないことは root publication の POM / `.module` の突き合わせまでしか実証していない (klib / aar は既定 URL で発行が完了せず未確認。同証跡を参照)」となっており、`evidence/swiftpm-reference-derivation.txt` および `deviation.md` の記述と一致している。一致検証の観点では未記録乖離に当たらない。

### 3. deviation の 1 項目がオーナー承認待ちと明記されている

deviation.md 1 項目め (ルート `kmp/build.gradle.kts:16` の `alias(libs.plugins.kotlinMultiplatform) apply false`) は末尾に「オーナー承認待ち」と書かれている。一致検証では「記録済みの乖離」として ⚠️ 扱いとしたが、アーカイブ前にオーナーの合意を得る必要がある項目として呼び出し元に引き継ぐ。

## 判定

**VALID** — Requirement 6 / Scenario 15 のすべてが ✅ または ⚠️ (deviation 記録済み)。❌ は 0 件。tasks.md の虚偽なし、足場の逆流なし、テストは再実行して全件成功。
