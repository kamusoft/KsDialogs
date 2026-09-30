# テストの実行結果と改名前の一覧との突き合わせ (tasks 1.4 / 2.4 / 3.2)

実行日: 2026-09-30。端末はこの作業専用に作成し、作業後に削除した (iOS Simulator: iPhone 17 / iOS 26.5、Android エミュレータ: API 36 google_apis arm64)。
基準の一覧は [baseline-test-names.md](baseline-test-names.md)。

## iOS (tasks 1.4)

コマンド: `cd ios && xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<作業専用 Simulator の UDID>' -resultBundlePath <作業用ディレクトリ>/ios-test.xcresult`

- Swift Testing: `Test run with 372 tests in 61 suites passed` / XCTest: `Executed 0 tests, with 0 failures` → 合計 372 tests / 0 failures、`** TEST SUCCEEDED **`
- 公開面の正の検査 `DialogCurrentPageCompileChecks.swift` (非 `@testable`) はこのテストビルドに同梱されてコンパイルされている
- テストごとの結果は `xcrun xcresulttool get test-results tests --path ios-test.xcresult` の出力から取り出した (Test Case 372 件、すべて `Passed`)

| # | 基準の一覧の関数名 | xcresult の識別子 | 結果 |
|---|---|---|---|
| 1 | `swiftUITabViewAndNavigationStackUseModifier()` | `DialogCurrentPageSwiftUITests/swiftUITabViewAndNavigationStackUseModifier()` | Passed |
| 2 | `swiftUITabViewUsesMarkerOfShownTabDuringSwitch()` | `DialogCurrentPageSwiftUITests/swiftUITabViewUsesMarkerOfShownTabDuringSwitch()` | Passed |
| 3 | `modifierWinsOverDefaultAndFallsBackAfterDetach()` | `DialogCurrentPageSwiftUITests/modifierWinsOverDefaultAndFallsBackAfterDetach()` | Passed |
| 4 | `ledgerWinsOverRegisteredProviderThenFallsBack()` | `DialogCurrentPageSwiftUITests/ledgerWinsOverRegisteredProviderThenFallsBack()` | Passed |

基準 4 件のうち欠けたもの: 0 件。

## Android (tasks 2.4)

### JVM テスト

コマンド: `cd android && ./gradlew test --rerun-tasks` → `BUILD SUCCESSFUL`

- `ksdialogs-core/build/test-results/testDebugUnitTest/TEST-*.xml` の合算: 109 tests / 0 failures / 0 errors / 0 skipped
- 同じ実行で `:api-surface-check:compileDebugKotlin` (公開面の正の検査 `DialogCurrentPageApiSurfaceChecks.kt` を含む) と `:ksdialogs-core:verifyNoDeclarativeUiDependency` が実行され成功した

### instrumented テスト

コマンド: `cd android && ANDROID_SERIAL=<作業専用エミュレータのシリアル> ./gradlew connectedDebugAndroidTest` → `BUILD SUCCESSFUL`

| 結果 XML | tests | failures | errors | skipped |
|---|---|---|---|---|
| `ksdialogs-core/build/outputs/androidTest-results/connected/debug/` | 383 | 0 | 0 | 1 |
| `ksdialogs/build/outputs/androidTest-results/connected/debug/` | 52 | 0 | 0 | 0 |
| 合計 | 435 | 0 | 0 | 1 |

skip の 1 件は `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` (API 30 以上の端末では旧経路を判定できないため自分で skip する。test-execution.md の「API レベルで走る / 走らない Scenario」のとおり。この変更とは無関係)。

`ksdialogs` の結果 XML から、classname が `jp.kamusoft.ksdialogs.compose.ComposeCurrentPageTests` の testcase を取り出した結果:

| # | 基準の一覧のテスト名 | 結果 |
|---|---|---|
| 1 | `Scaffold_の_content_枠に付けると下部バーを避ける` | passed |
| 2 | `遷移で枠が離脱すると台帳から外れ_VISIBLE_AREA_と一致する` | passed |
| 3 | `戻る遷移で再び付けた画面が基準になる` | passed |
| 4 | `入れ子は後から外側が配置されても内側が勝つ` | passed |
| 5 | `窓の外に配置された枠は後から配置されても候補にならない` | passed |
| 6 | `同じ_Activity_で出したモーダルの中の枠は後から配置されると候補になり画面上の位置が基準になる` | passed |
| 7 | `台帳が登録した関数に勝ち_台帳が空になると登録した関数へ進む` | passed |

基準 7 件のうち欠けたもの: 0 件。

## Sample のビルド (tasks 3.2)

local-development-setup.md の「Sample のビルドと実行」のコマンドでビルドした (iOS 系の `-derivedDataPath` はリポジトリ外の作業用ディレクトリ、destination は作業専用 Simulator の id 指定)。

| Sample | コマンド | 結果 |
|---|---|---|
| samples/ios | `xcodebuild -project KsDialogsSample.xcodeproj -scheme KsDialogsSample ... CODE_SIGNING_ALLOWED=NO build` | `** BUILD SUCCEEDED **` (`SampleLayoutPanelScreen.swift` をコンパイル) |
| samples/android | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` (`:app:compileDebugKotlin` 実行) |
| samples/kmp (Android) | `./gradlew :androidApp:assembleDebug` | `BUILD SUCCESSFUL` (`:androidApp:compileDebugKotlin` 実行) |
| samples/kmp (iOS) | `cd iosApp && xcodebuild -project KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp ... CODE_SIGNING_ALLOWED=NO build` | `** BUILD SUCCEEDED **` (`SampleLayoutPanelScreen.swift` をコンパイル) |
