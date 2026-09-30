# Delta Spec: ios-native (rename-current-page-marker)

対象: SwiftUI の View を表示中のページとして名乗らせる印の公開名。挙動 (台帳規則、登録 provider・既定 provider との優先順位) は変えない。

現行の宣言 (確認先):
- 印: `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift` (`public extension View` の `func ksDialogCurrentPage() -> some View`)
- 印を名指す診断メッセージ: `ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageLedger.swift` (`origin` と `.notFound(reason:)` の文言)
- 印を名指す説明コメント: `ios/Sources/KsDialogs/Contract/DialogCurrentPage.swift`・`Contract/DialogLayoutArea.swift`・`CurrentPage/DialogCurrentPageMarkerView.swift`
- 公開面の正の検査: `ios/Tests/KsDialogsTests/DialogCurrentPageCompileChecks.swift` (非 `@testable`、既定のテストビルドに同梱)
- 挙動のテスト: `ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift` と `Support/CurrentPageSwiftUI*.swift`

## ADDED Requirements

### Requirement: 表示中のページの印の名前 (iOS)

iOS 実装は、SwiftUI の View を表示中のページとして名乗らせる view modifier を `markAsDialogCurrentPage()` の名前で公開すること (SHALL)。旧名 `ksDialogCurrentPage()` は deprecated 別名を含めて残さないこと (SHALL)。台帳規則と、登録 provider・既定 provider との優先順位は改名前と変えないこと (SHALL)。ライブラリの診断メッセージと説明コメントが印を名指すときは新しい名前を使うこと (SHALL)。

#### Scenario: 新しい名前で印を付けられる
- **GIVEN** 利用者と同じ可視性の境界にある公開面の検証 (テストターゲット内の非 `@testable` なファイル)
- **WHEN** SwiftUI の View に `markAsDialogCurrentPage()` を付けるコードを既定のテストビルドでコンパイルする
- **THEN** 型注釈を足さずにコンパイルが通る

#### Scenario: 挙動のテストが名前の追随だけで通る
- **GIVEN** 改名前に列挙した、SwiftUI の印を使う表示中のページのテスト (`DialogCurrentPageSwiftUITests`) のテスト名の一覧
- **WHEN** 改名後に `kasane/handbook/cross/test-execution.md` の ios/ の手順 (Simulator 実行) で全テストを実行する
- **THEN** すべて成功し、一覧のテストは 1 件も欠けずに成功として実行結果に現れる

#### Scenario: 差分は旧名から新名への置き換えだけ
- **GIVEN** 改名前後の ios/ (Sources と Tests)
- **WHEN** 差分を見比べる
- **THEN** 変わった行はどれも `ksDialogCurrentPage` を `markAsDialogCurrentPage` に置き換えただけの行であり、削除だけの行は無い。台帳の診断メッセージ (`origin` と `.notFound(reason:)`) は改名前と同じ英文のまま `markAsDialogCurrentPage()` を名指している

#### Scenario: 旧名が現行のソースに残らない
- **GIVEN** 改名後の ios/ (Sources と Tests)
- **WHEN** `ksDialogCurrentPage` を大文字小文字を区別して検索する
- **THEN** 0 件である (診断メッセージと説明コメントを含む)
