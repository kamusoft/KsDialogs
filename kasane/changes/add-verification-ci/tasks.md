# Tasks: add-verification-ci

翻案元: `../KsSettingsView/.github/workflows/` (ci.yml / verify-ios.yml / verify-android.yml / verify-maui.yml、ADR-0028 反映後)。コピーして固有値 (scheme 名・csproj パス・job 名) を差し替え、KsDialogs 固有の job (android-instrumented / kmp) と手順 (橋渡しテスト・metadata compile・scenario-id-coverage) を足す。workflow の書き方は github-workflow-skill を参照する。

## 1. toolchain 固定

- [x] 1.1 repo 直下に `global.json` を新設する — SDK 10.0.300 / workload set 10.0.300.3。手元で `dotnet --version` が repo の設定を拾い、`maui/` の facade が既定の Xcode 26.5 で `net10.0-ios` ビルドできることを確認する (→ Requirement: ツールチェーンの再現性 / Scenario: 手元のビルドが repo の設定で固定される)
- [x] 1.2 `Microsoft.Maui.Controls` を 10.0.70 に固定する — facade / Tests / ApiSurfaceCheck の 3 csproj。宣言の一元化 (`maui/Directory.Packages.props` の central package management) を採るかは KsSettingsView を参考に決め、`samples/maui` は `MauiVersion` 直書きのまま。`dotnet test` 155 件と ApiSurfaceCheck のビルドが通ること、`samples/maui` の `net10.0-ios` / `net10.0-android` ビルドが手元で通ること (Sample の `MauiVersion` は変えない。解決版が推移的に 10.0.70 に上がる警告の有無を記録) を確認する (→ Requirement: ツールチェーンの再現性)

## 2. platform 別 reusable workflow

- [x] 2.1 `verify-ios.yml` — `workflow_call`、`macos-26`、Xcode 26.5 を変数 + `DEVELOPER_DIR` で選択 (無ければ一覧を出して失敗)、最新ランタイムの iPhone Simulator を UDID で選び `xcodebuild test -scheme KsDialogs`、件数検査は Swift Testing (`Test run with N tests`) と XCTest (`Executed N tests`) の 2 系統を合算 (合算 0 件・両系統の件数行なしで fail、summary へ) (→ Requirement: iOS の検証 / platform workflow の再利用契約 / ツールチェーンの再現性)
- [x] 2.2 `verify-android.yml` — `workflow_call`、`ubuntu-24.04`、Temurin 17、Gradle 依存のみキャッシュ、`./gradlew test` (現構成で起動するのは `testDebugUnitTest` のみ。release は契約に含めない)。件数検査は `android/settings.gradle.kts` の include のうち `src/test` を持つ module を導出 (現行は `:ksdialogs` の 1 module。翻案元の「全 include × debug / release を期待」は `:ksdialogs-compose` (instrumented のみ)・`:api-surface-check` (テストなし)・release 不在で即 fail するため変える)、導出が空なら fail、module ごとに XML 欠落 / 0 件で fail、合計を summary へ。本体の Compose 非依存検査 (`verifyNoDeclarativeUiDependency`) が同じ `test` 実行に乗ることをログで確認する (→ Requirement: Android の検証と実行件数の担保)
- [x] 2.3 `verify-android-instrumented.yml` — `workflow_call`、`ubuntu-24.04`、KVM を有効化した API 36 の Emulator 1 台 (system image・action の選定は実装時。action は SHA 固定)、`./gradlew connectedDebugAndroidTest`。件数検査は `src/androidTest` を持つ module ごと (現行 `:ksdialogs` / `:ksdialogs-compose`、`build/outputs/androidTest-results/connected/**/TEST-*.xml`)、導出が空なら fail、XML 欠落 / 実行数 (tests − skipped) 0 で fail、tests / skipped / failures を分けて summary へ (→ Requirement: Android instrumented の検証)
- [x] 2.4 `verify-kmp.yml` — `workflow_call`、`macos-26`、Xcode 26.5 選択、Temurin 17、Gradle 依存のみキャッシュ (キャッシュキーは `android/` と `kmp/` の両方のビルド定義から)、`kmp/local.properties` の生成 (`sdk.dir` = ランナーの `ANDROID_HOME`)、`./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` を 1 回で。件数検査はターゲットごと (`testAndroidHostTest` / `iosSimulatorArm64Test` の結果 XML)、XML 欠落 / 0 件で fail、summary へ (→ Requirement: KMP の検証 / platform workflow の再利用契約)
- [x] 2.5 `verify-maui.yml` — `workflow_call`、`macos-26`、Xcode 26.5 選択、Temurin 17、`setup-dotnet` (`global-json-file: global.json`) + `dotnet workload install maui`、NuGet キャッシュ。facade の `dotnet test` (TRX 件数検査)、binding 2 本と facade の `net10.0-ios` / `net10.0-android` ビルド (→ Requirement: MAUI の検証 / ツールチェーンの再現性)
- [x] 2.6 maui job に橋渡しテスト 2 本を足す — `maui/android/native` で `./gradlew :ksdialogs-maui-bridge:test` (結果 XML の件数検査)、`maui/macios/native` で `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge` を Simulator destination で (件数は ios と同じ 2 系統合算)。それぞれ 0 件で fail、summary へ (→ Requirement: MAUI の検証 / Scenario: 0 件実行の検出)

## 3. CI 入口と lint

- [x] 3.1 `ci.yml` — `push: branches: [develop]` と `pull_request: branches: [main]` (`paths-ignore` は使わない)。軽量な変更検出 job (`changes`、ubuntu、checkout + `git diff --name-only` を `github.event.before`..`HEAD` で。before が空 / 到達不能なら「ソース変更あり」に倒す) が「`kasane/**`・`.github/ISSUE_TEMPLATE/**`・`.github/CONTRIBUTING*.md` だけの変更か」を出力し、本体検証 5 job は `needs: changes` + `if: github.event_name == 'pull_request' || needs.changes.outputs.source == 'true'` で条件付け。本体検証 5 job を `uses:` で呼び、job 名 / 呼ばれた側 job 名を `ios / verify`・`android / verify`・`android-instrumented / verify`・`kmp / verify`・`maui / verify` に固定。concurrency は `ci-<workflow>-<PR 番号 or ref>` で cancel-in-progress。`permissions: contents: read` (→ Requirement: CI の起動条件 / platform workflow の再利用契約)
- [x] 3.2 lint job — `ubuntu-24.04`。`main` 宛て PR の head 制限 (自リポジトリの `develop` のみ)、gitleaks (版 + SHA-256 固定、`git archive` 展開ディレクトリに `dir` モード、展開数 < 追跡数で fail)、`local-path-lint.py`、`identity-lint.py`、`comment-policy-lint.py`、`scenario-id-coverage.py` (→ Requirement: lint の検証 / main 宛て PR の head 制限)
- [x] 3.3 全 workflow で外部 action の参照を commit SHA + version コメントで固定し、`permissions: contents: read` を明示する (→ Requirement: ツールチェーンの再現性)

## 4. handbook の追随

- [x] 4.1 `kasane/handbook/cross/local-development-setup.md` — repo 直下の `global.json` で SDK / workload set が固定されること、`DEVELOPER_DIR` の付け替えが既定では不要になったことを反映する (→ Scenario: 手元のビルドが repo の設定で固定される)
- [x] 4.2 `kasane/handbook/cross/test-execution.md` — CI が回す範囲 (5 job + lint) と手元に残る範囲 (API 29 の instrumented・負のコンパイル検証) を明記する (→ Requirement: Android instrumented の検証 / KMP の検証)

## 5. 検証 (Scenario の実機確認)

- [x] 5.1 `develop` へ push して lint + 5 job が起動・成功することを確認し、各 job の所要時間を記録して timeout を実測に合わせる (→ Scenario: develop への push で本体検証と lint が起動する)
- [ ] 5.2 `kasane/` 配下だけの commit を push して lint だけが走り本体検証 5 job がスキップされること、`kasane/` とソースを混ぜた commit で全 job が走ること、連続 push で古い実行が打ち切られることを確認する (→ Scenario: 記録だけの push では lint だけが走る / 記録とソースが混ざった push では全 job が走る / 連続する push で古い実行が打ち切られる)
- [x] 5.3 件数検査の負ケースを job ごとにステップ単体で確認する — ios / maui の iOS 橋渡し (Swift Testing のみ・XCTest のみ・混在・両方 0 件・件数行欠落)、maui facade (TRX 0 件)、android / kmp (結果 XML 欠落・0 件・導出集合が空)、android-instrumented (XML 欠落・全件 skip) (→ Scenario: 各 job の 0 件実行の検出 / Swift Testing だけの構成で件数が取れる)
- [x] 5.8 android job で本体に Compose 依存を一時的に足し、依存グラフ検査で job が失敗することを確認する (→ Scenario: 依存グラフ違反の検出)
- [x] 5.4 kmp job で metadata compile だけが失敗する状態 (iosMain の override に `@Throws` を付けた一時変更等) を作り、job が失敗することを確認する (→ Scenario: metadata compile の失敗を検出する)
- [x] 5.5 lint の負ケースを 5 検査それぞれで確認する (gitleaks は検証用ダミー文字列、identity-lint は `maui/macios/native` 配下の識別子、scenario-id-coverage は仕様だけにある ID)。secret scan の展開数不足で fail することもステップ単体で確認する (→ Scenario: 違反の検出 / ソースルート配下の識別子検出 / secret scan の空振り検出)
- [x] 5.6 `main` 宛て PR の head 制限は `main` が無いため実 PR で確認できない — lint job のステップを `github.head_ref` 相当の環境変数を差し替えて単体実行し、`develop` 以外と fork で失敗することを確認する。pull_request トリガー・status check 名・head 制限の実動確認は phase-9 の初回リリース PR で行う (proposal Non-Goals、phase-9 agenda に申し送り済み) (→ Scenario: develop 以外からの PR は失敗する / main 宛ての PR で起動する / status check 名が固定される)
- [x] 5.7 Xcode 選択ステップに存在しない版を与えて失敗と一覧出力を確認する (→ Scenario: 指定した Xcode が無ければ失敗する)

## 備考

- 所要時間が許容できない場合の最適化 (workload / SDK キャッシュ等) は別 change (proposal Non-Goals)
- `Microsoft.Maui.Controls` 10.0.70 / workload set 10.0.300.3 で facade・binding・テストが通らない場合は実装を止めて報告する (proposal Impact)
- `main` の branch protection と消費者検証 job は phase-9 / phase-8 (proposal Non-Goals)
