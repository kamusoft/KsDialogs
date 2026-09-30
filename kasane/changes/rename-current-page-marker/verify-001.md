# 一致検証結果: rename-current-page-marker (001 回目)

**日付**: 2026-09-30
**判定**: VALID

検証対象: コミット 077da71 に対する作業ツリーの未コミット差分 (`git diff HEAD` と未追跡の `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogCurrentPageModifier.kt`)。デルタスペック 3 本 (specs/ios-native・specs/android-native・specs/samples)、tasks.md、evidence/ を突き合わせた。deviation.md はない。UI 変更 (見た目) はないため ui/brief.md は対象外。

## 対応表

### ios-native — Requirement: 表示中のページの印の名前 (iOS)

| Requirement / Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| Requirement 本体 (新名で公開・旧名と deprecated 別名を残さない・挙動不変・診断文言とコメントは新名) | `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift:19` (`func markAsDialogCurrentPage() -> some View`、本体 `background(DialogCurrentPageMarker())` は不変)。診断 `ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageLedger.swift:22`・`:67`。コメント `Contract/DialogCurrentPage.swift:10`・`Contract/DialogLayoutArea.swift:16`・`CurrentPage/DialogCurrentPageMarkerView.swift:4` | 以下の各 Scenario | ✅ 一致 |
| Scenario: 新しい名前で印を付けられる | 同上 | `ios/Tests/KsDialogsTests/DialogCurrentPageCompileChecks.swift:42` (非 `@testable`)。evidence/test-results.md の iOS 全件実行でテストビルドに同梱。検証側でも `xcodebuild build-for-testing` (generic iOS Simulator) が終了コード 0 | ✅ 一致 |
| Scenario: 挙動のテストが名前の追随だけで通る | テスト支援 `Support/CurrentPageSwiftUI{FrameView,InfoPage,PanelPage,TabHostView,TabSwitchHost}.swift` の呼び出しを新名へ | evidence/baseline-test-names.md (改名前 4 件) と evidence/test-results.md (xcresult から取り出した 4 件すべて Passed、全体 372 tests / 0 failures) | ✅ 一致 |
| Scenario: 差分は旧名から新名への置き換えだけ | ios/ の 12 ファイル・17 行 | `git diff HEAD -U0 -- ios` を検証側で目視: 全ハンクが 1 行対 1 行で差は旧名 → 新名のみ。削除だけの行なし。診断 2 文は同じ英文で `markAsDialogCurrentPage()` を名指す | ✅ 一致 |
| Scenario: 旧名が現行のソースに残らない | — | 検証側で `git grep --untracked -n ksDialogCurrentPage -- ios` → 0 件 | ✅ 一致 |

### android-native — Requirement: 表示中のページの印の名前 (Android)

| Requirement / Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| Requirement 本体 (新名で公開・旧名と deprecated 別名を残さない・挙動不変・診断文言・コメント・デバッグ表示名は新名) | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogCurrentPageModifier.kt:35` (`public fun Modifier.markAsDialogCurrentPage(): Modifier`)、`:44` (`name = "markAsDialogCurrentPage"`)。旧 `compose/KsDialogCurrentPage.kt` は削除。診断 `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPageLedger.kt:91`。コメント `DialogCurrentPage.kt:10`・`DialogLayoutArea.kt:21` | 以下の各 Scenario | ✅ 一致 |
| Scenario: 新しい名前で印を付けられる | 同上 | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/DialogCurrentPageApiSurfaceChecks.kt:8`・`:44` (`frame.markAsDialogCurrentPage().then(Modifier.markAsDialogCurrentPage())` で戻り値を連ねる)。evidence/test-results.md の `./gradlew test --rerun-tasks` で `:api-surface-check:compileDebugKotlin` 成功。検証側でも同タスクが終了コード 0 | ✅ 一致 |
| Scenario: 挙動のテストが名前の追随だけで通る | `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/support/CurrentPageComposeTestActivity.kt` の呼び出し 7 箇所と import を新名へ | evidence/baseline-test-names.md (改名前 7 件) と evidence/test-results.md (ksdialogs の結果 XML から `ComposeCurrentPageTests` の 7 件すべて passed。JVM 109 / 0 failures、instrumented 435 / 0 failures / 1 skipped — skip は本変更と無関係の既知の自己 skip) | ✅ 一致 |
| Scenario: 差分は旧名から新名への置き換えだけ | android/ の 6 ファイル変更 + 印の定義ファイルの改名 1 | 検証側で `git show HEAD:…/KsDialogCurrentPage.kt` と `DialogCurrentPageModifier.kt` を `diff`: 差は 35・37・44 行目の 3 行 (旧名 → 新名) のみ。他ファイルは `git diff HEAD -U0` で全ハンクが旧名 → 新名の置き換えのみ。削除だけの行なし。診断文言は同じ英文で `markAsDialogCurrentPage()` を名指し、デバッグ表示名は `markAsDialogCurrentPage` | ✅ 一致 |
| Scenario: 旧名が現行のソースに残らない | — | 検証側で `git grep --untracked -n ksDialogCurrentPage -- android` → 0 件 (main・test・androidTest・api-surface-check を含む) | ✅ 一致 |

### samples — Requirement: Sample は新しい名前の印を使う

| Requirement / Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| Requirement 本体 (4 ルートが同じ枠に新名の印・MAUI 無変更・画面と文言不変) | `samples/ios/KsDialogsSample/SampleLayoutPanelScreen.swift:21`・`:36`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleLayoutPanelScreen.swift:23`・`:38`、`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleLayoutPanelScreen.kt:13`・`:40`・`:72`、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleLayoutPanelScreen.kt:13`・`:40`・`:72`。samples/maui は差分なし | 以下の各 Scenario | ✅ 一致 |
| Scenario: 印を使う 4 ルートの Sample がビルドできる | 同上 | evidence/test-results.md の「Sample のビルド」: samples/ios・samples/kmp (iOS) が `** BUILD SUCCEEDED **`、samples/android `:app:assembleDebug`・samples/kmp `:androidApp:assembleDebug` が `BUILD SUCCESSFUL` | ✅ 一致 |
| Scenario: Sample の差分は印の名前だけ | 同上 | 検証側で `git diff HEAD -U0 -- samples`: 変わった行は印の呼び出し (Swift 4・Kotlin 4) と Kotlin の import 2 行のみ。付ける位置の行番号も改名前と同じ | ✅ 一致 |
| Scenario: 旧名が Sample に残らない | — | 検証側で `git grep --untracked -n ksDialogCurrentPage -- samples` → 0 件 | ✅ 一致 |

## 追加検査

- [x] tasks.md: 0.1〜4.1 の全タスクがチェック済みで、対応表・evidence と一致する。虚偽チェックなし (4.1 の残存一覧は evidence/diff-and-residue.md にあり、検証側のリポジトリ全体検索の結果とも一致)
- [x] 逆流検査: `git diff HEAD --stat -- kasane` で変わっているのは tasks.md のチェック欄 15 行だけ。proposal.md・exploration.md・specs/ は 077da71 から無変更
- [x] 未記録乖離: なし (❌ なし。diff の全行が上表のいずれかの Scenario に対応し、Scenario に対応しない変更もない)
- [x] 付随修正: deviation.md なし・該当なし
- [x] UI 変更: 見た目の変更はなし (Sample の画面・文言は差分なし)
- [x] テスト: evidence/test-results.md に ios/ (372 / 0 failures) と android/ JVM (109 / 0)・instrumented (435 / 0 failures / 1 無関係 skip) の全件実行と、改名前基準一覧との 1 件ずつの突き合わせがある。オーケストレーターの指示によりテスト本体の再実行は省き、検証側では公開面の正の検査とテストターゲットのコンパイル (Android `:api-surface-check:compileDebugKotlin :ksdialogs:compileDebugAndroidTestKotlin`、iOS `xcodebuild build-for-testing`) のみ再実行して成功を確かめた

## リポジトリ全体の残存 (参考 — proposal の「蒸留時に反映」)

`ksDialogCurrentPage` の残存は `kasane/changes/` (本 change とアーカイブ 2026-09-27-add-page-layout-area)、`kasane/decisions/core/0045-ks-prefix-limited-to-view-attachments.md`、`kasane/concepts/ios/api/layout-surface.md`、`kasane/concepts/android/api/layout-surface.md`、`kasane/handbook/cross/sample-parity.md` だけ。いずれも経緯の記録か蒸留で直す長命層で、検証対象の違反ではない。

## 判定

全 Requirement / Scenario (3 Requirement・11 Scenario) が ✅ 一致。虚偽チェック・逆流・テスト失敗なし。**VALID**。
