# Tasks: rename-current-page-marker

## 0. 改名前の基準
- [ ] 0.1 改名前のテスト名の一覧を記録する: iOS の `DialogCurrentPageSwiftUITests` と Android の `ComposeCurrentPageTests` のテスト宣言を列挙し、change の evidence に残す (→ Scenario: 挙動のテストが名前の追随だけで通る (iOS / Android))

## 1. iOS
- [ ] 1.1 `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift` の modifier を `markAsDialogCurrentPage()` に改名する。旧名・deprecated 別名は残さない (→ Requirement: 表示中のページの印の名前 (iOS))
- [ ] 1.2 印を名指す診断メッセージ (`CurrentPage/DialogCurrentPageLedger.swift` の `origin` と `.notFound(reason:)`) と説明コメント (`Contract/DialogCurrentPage.swift`・`Contract/DialogLayoutArea.swift`・`CurrentPage/DialogCurrentPageMarkerView.swift`) を新しい名前にする。メッセージの言語と文型は変えない (→ Requirement: 表示中のページの印の名前 (iOS))
- [ ] 1.3 公開面の正の検査 `ios/Tests/KsDialogsTests/DialogCurrentPageCompileChecks.swift` を新しい名前にする (→ Scenario: 新しい名前で印を付けられる (iOS))
- [ ] 1.4 テスト (`DialogCurrentPageSwiftUITests.swift`・`Support/CurrentPageSwiftUI*.swift`) を新しい名前に追随させ、`kasane/handbook/cross/test-execution.md` の ios/ の手順で全テストを Simulator 実行する。実行結果 (xcodebuild のログまたは xcresult) からテストごとの結果を取り出し、0.1 の一覧のテストが 1 件も欠けずに成功していることを突き合わせて evidence に残す。全スイートの合計件数だけでは判定しない (→ Scenario: 挙動のテストが名前の追随だけで通る (iOS))
- [ ] 1.5 ios/ の差分を見て、変わった行がどれも旧名から新名への置き換えだけで、台帳の診断メッセージが同じ英文のまま新しい名前を名指していることを確かめる。あわせて ios/ の Sources と Tests で `ksDialogCurrentPage` を検索し、0 件であることを確かめる (→ Scenario: 差分は旧名から新名への置き換えだけ (iOS) / 旧名が現行のソースに残らない (iOS))

## 2. Android
- [ ] 2.1 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogCurrentPage.kt` の拡張関数を `markAsDialogCurrentPage()` に改名し、デバッグ用の表示名 (`InspectorInfo` の `name`) も新しい名前にする。旧名・deprecated 別名は残さない。ファイル名は、旧名を含まず中身を表す名前に改める (例: `DialogCurrentPageModifier.kt`) (→ Requirement: 表示中のページの印の名前 (Android))
- [ ] 2.2 印を名指す診断メッセージ (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPageLedger.kt`) と説明コメント (同じディレクトリの `DialogCurrentPage.kt`・`DialogLayoutArea.kt`) を新しい名前にする。メッセージの言語と文型は変えない (→ Requirement: 表示中のページの印の名前 (Android))
- [ ] 2.3 公開面の正の検査 `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/DialogCurrentPageApiSurfaceChecks.kt` を新しい名前にする (→ Scenario: 新しい名前で印を付けられる (Android))
- [ ] 2.4 instrumented テスト (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt`・`support/CurrentPageComposeTestActivity.kt`) を新しい名前に追随させ、`kasane/handbook/cross/test-execution.md` の android/ の手順で JVM テストと instrumented テストを実行する。instrumented テストの結果 (`build/outputs/androidTest-results/` の XML) から、0.1 の一覧のテストが 1 件も欠けずに成功していることを突き合わせて evidence に残す (→ Scenario: 挙動のテストが名前の追随だけで通る (Android))
- [ ] 2.5 android/ の差分を見て、変わったのが旧名から新名への置き換えだけの行と印を定義するファイルの改名だけで、台帳の診断メッセージが同じ英文のまま新しい名前を名指し、デバッグ用の表示名が `markAsDialogCurrentPage` であることを確かめる。あわせて android/ で `ksDialogCurrentPage` を検索し、0 件であることを確かめる (→ Scenario: 差分は旧名から新名への置き換えだけ (Android) / 旧名が現行のソースに残らない (Android))

## 3. Sample
- [ ] 3.1 4 ルートの Sample (samples/ios・samples/kmp/iosApp・samples/android・samples/kmp/androidApp の `SampleLayoutPanelScreen`) の印の呼び出しと Kotlin の import を新しい名前にする。付ける場所は変えない (→ Requirement: Sample は新しい名前の印を使う)
- [ ] 3.2 `kasane/handbook/cross/local-development-setup.md` の「Sample のビルドと実行」の手順で、samples/ios・samples/android・samples/kmp (Android と iOS) をビルドする (→ Scenario: 印を使う 4 ルートの Sample がビルドできる)
- [ ] 3.3 4 ルートの差分が印の呼び出しと import だけであること、samples/ で `ksDialogCurrentPage` が 0 件であることを確かめる (→ Scenario: Sample の差分は印の名前だけ / 旧名が Sample に残らない)

## 4. 蒸留への申し送り
- [ ] 4.1 リポジトリ全体で `ksDialogCurrentPage` を検索し、残るのが経緯の記録 (`kasane/changes/`・`kasane/decisions/`・`kasane/concepts/log.md`) と、蒸留で直す concepts (`kasane/concepts/ios/api/layout-surface.md`・`kasane/concepts/android/api/layout-surface.md`) と handbook (`kasane/handbook/cross/sample-parity.md`) だけであることを確かめ、残った箇所の一覧を change の evidence に残す (→ proposal の「蒸留時に反映」)
