# Tasks: add-native-distribution

## 1. Android 座標のリネームと module 改名

- [x] 1.1 `git mv` で `android/ksdialogs` → `android/ksdialogs-core`、`android/ksdialogs-compose` → `android/ksdialogs` に改名し、`settings.gradle.kts` の include・`api-surface-check` の project 参照・`layout-case-fixtures/README.md` の説明・各 build.gradle.kts のコメントを追随する (design Decision 5) (→ Requirement: Android module の座標と構成)
- [x] 1.2 改名前に module ごとのユニットテスト件数・instrumented test 件数 (294 / 39) を記録し、改名後に `./gradlew test --rerun-tasks`、`:api-surface-check` の肯定ケースと各 `ksdialogs.negativeCheck.*` プロパティの否定ケース、API 36 エミュレータでの `connectedDebugAndroidTest` を実行して件数一致・全件成功・否定ケースの同一診断を確認する (handbook cross/test-execution.md) (→ Scenario: ユニットテストと API 形状検査の無改変実行 / instrumented test の無改変実行)
- [x] 1.3 検証 CI (`verify-android.yml` / `verify-maui.yml` / `ci.yml`) と handbook (`diagnostic-message-language.md` の検査パス・`test-execution.md` の module 名・`local-development-setup.md` の Sample 参照方式) を grep で洗い出し、改名後のパス・座標・依存 1 行へ追随する (規約の内容は変えない) (→ Requirement: Android module の座標と構成)
- [x] 1.4 `.agents/skills/docs-refresh/SKILL.md` の識別子表 (6-⑧) と Maven 配布座標の説明を新座標 (`ksdialogs-core` = View 系本体 / `ksdialogs` = Compose 系) へ更新する (→ Requirement: Android module の座標と構成)

## 2. version の単一ソースと注入

- [x] 2.1 カタログの `ksdialogs` を `0.1.0-SNAPSHOT` に改め、`android/build.gradle.kts` を新設して `-Pversion=` 注入を優先する導出式で subprojects の `group` / `version` を一括設定し、注入値の形式検査 (`X.Y.Z` / `X.Y.Z-{alpha|beta|rc}.N` 以外は失敗) を置き、各 module の直書きを削除する (design Decision 3) (→ Requirement: version の単一ソースと注入)
- [x] 2.2 `-Pversion=` なし / `-Pversion=0.1.0-beta.1` の両方で `publishToMavenLocal` して座標の version と `ksdialogs` POM の依存 version を確認し、空文字・`v1.0.0`・`1.0.0-pre` で設定が失敗することを確認する (→ Scenario: 注入なしの開発既定値 / リリース version の注入 / 不正な注入値の拒否)

## 3. 発行設定

- [x] 3.1 `com.vanniktech.maven.publish` 0.37.0 をカタログに宣言し `:ksdialogs-core` / `:ksdialogs` にのみ適用する。ルート subprojects の `plugins.withId` で共通設定 (release 単一 variant + sources jar + 空 javadoc jar / `publishToMavenCentral()` / `signAllPublications()` / 署名必須の鍵有無連動 / POM 共通部 / SNAPSHOT ガード) を 1 回だけ書く (design Decision 4) (→ Requirement: Maven 発行物の座標と内容 / 発行物の署名 / version の単一ソースと注入)
- [x] 3.2 各 module に POM の name / description (agenda 決定事項の文言) を書く (→ Requirement: Maven 発行物の座標と内容)
- [x] 3.3 SNAPSHOT のまま Central 向けタスクを実行して失敗すること、`publishToMavenLocal` が通ることを確認する (→ Scenario: SNAPSHOT の Central 発行拒否)
- [x] 3.4 `:api-surface-check` に発行タスクが生えていないことを確認する (→ Scenario: api-surface-check の非公開)
- [x] 3.5 鍵なしの `publishToMavenLocal` で `.asc` が生成されず成功すること、一時生成した検証用鍵を `signingInMemoryKey` 系プロパティで渡した `publishToMavenLocal` で両 module の全 publication に `.asc` が生成されることを確認し、鍵は破棄する (→ Scenario: 鍵なしのローカル発行 / 鍵ありのローカル発行)

## 4. 発行検証

- [x] 4.1 `publishToMavenLocal` の発行物を検証し証跡を evidence/ に残す: aar / sources jar (公開パッケージの `.kt` 含有) / 空 javadoc jar / POM の内容 / POM と `.module` の compile・runtime スコープ (Requirement の列挙との一致、テスト専用ライブラリの不在) (→ Scenario: ローカル発行での発行物検証 / 発行メタデータの依存スコープ)
- [x] 4.2 release aar の公開シグネチャを javap で走査し、外部の非プラットフォーム型の提供 artifact が compile スコープと一致することを確認して evidence/ に残す (design Decision 6) (→ Scenario: release aar の公開シグネチャ検算)

## 5. monorepo 内消費者の追随

- [x] 5.1 `samples/android` / `samples/kmp` の dependencySubstitution を新座標 2 本に更新し、アプリの直接依存を `jp.kamusoft:ksdialogs` 1 行に減らす。両 Sample の Android アプリをビルドして、本体が推移的依存で置換されること・置換の実効 (本体ソースの変更が反映されること) を確認する (design Decision 7) (→ Scenario: Sample のビルドとソース参照)
- [x] 5.2 `kmp/ksdialogs-kmp` と `maui/android/native/ksdialogs-maui-bridge` の本体依存を `jp.kamusoft:ksdialogs-core` に更新し、`kmp/` のテストと bridge の `assembleRelease` の成功を確認する (→ Scenario: KMP と MAUI bridge のビルド)
- [x] 5.3 `maui/android/KsDialogs.Binding.Android.csproj` の aar パスと gradlew task 名を追随し、binding をビルドして成功を確認する (→ Scenario: MAUI binding のビルド)

## 6. SwiftPM 同期スクリプト

- [x] 6.1 `scripts/spm-snapshot/sync-snapshot.sh` を翻案元から持ち込む (配信リポジトリ名 `kamusoft/KsDialogs-SPM` とコメントを差し替え): 破壊的操作前の全件検証・`.git/` 以外の除去 (`rm -rf`。オーナー裁定 2026-09-08、design Decision 1)・ホワイトリスト 5 点配置・git 非操作・冪等 (design Decision 1) (→ Requirement: スナップショット同期スクリプト)
- [x] 6.2 `README.template.md` を翻案元から名前差し替えで持ち込み、配信リポジトリの現行 README と差分がないことを確認する (→ Requirement: 誘導 README の整合)
- [x] 6.3 スクリプトのテスト (`sync-snapshot-test.sh`) を持ち込み、6 Scenario (5 点配置 / 列挙外除去 / 冪等性 / 誤指定拒否 / コピー元不足時の無変更 / git 非操作) が通ることを確認する (→ Requirement: スナップショット同期スクリプト)
- [x] 6.4 検証 CI (`.github/workflows/ci.yml`) の lint job に `scripts/spm-snapshot/sync-snapshot-test.sh` を実行する step を足し、handbook cross/verification-ci.md の lint job の検査一覧を追随する (cross/ADR-0017 の amends は蒸留時) (→ Requirement: スナップショット同期スクリプト)

## 7. 配信リポジトリへの初回 push と解決確認

- [ ] 7.1 検証手順を「tag 作成後は成否を問わず local / remote の検証用 tag を削除する後始末」を組み込んだ形 (`trap` 相当) で用意し、同期スクリプトの成果物を手動で commit・push して検証用 prerelease tag (`X.Y.Z-alpha.N`) を打つ (design Decision 2) (→ Scenario: 実リモートからの依存解決とビルド)
- [ ] 7.2 一時消費者プロジェクト (リポジトリ外) から https URL + tag の exact 指定で依存解決し、`KsDialogs` の公開型を参照するコードを iOS Simulator 向けにビルドして成功を確認する (design Decision 8) (→ Scenario: 実リモートからの依存解決とビルド)
- [ ] 7.3 後始末で検証用 tag が remote と作業コピーの両方から消え、他の tag に触れていないことを確認する。証跡 (解決ログ・後始末の記録) を evidence/ に保存する (→ Scenario: 検証用 tag の後始末 (成功時) / 検証用 tag の後始末 (失敗時))
- [x] 7.4 `ios/` の全テストを iOS Simulator destination で実行し、1 件以上が成功することを確認する (`ios/Package.swift` 無変更の確認を兼ねる) (→ Scenario: iOS package のテスト維持)
- [x] 7.5 `samples/ios` と maui の iOS binding を iOS Simulator 向けにビルドし、Local Swift Package 参照のまま成功することを確認する (→ Scenario: iOS Sample と binding のビルド)
