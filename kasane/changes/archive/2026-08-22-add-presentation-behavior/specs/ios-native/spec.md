# ios-native デルタ (add-presentation-behavior)

## ADDED Requirements

### Requirement: トランジション添付面 (iOS)

iOS Native ライブラリは、従来 View 系 (`UIView.ksDialogTransition`) と SwiftUI 系 (`View.dialogTransition(_:)` modifier) の両方で DialogTransition を添付できる (SHALL)。フックは `@MainActor (UIView) async throws -> Void` で、SwiftUI コンテンツではホスト View (UIHostingController の view) が渡る。プリセット factory (fade / slide / zoom / none) は `TimeInterval` と `UITimingCurveProvider` で引数を受け取る。公開 API の形は design Decision 3 の表に従う。

#### Scenario: [PB-IA-01] UIView への添付が器で採用される
- **GIVEN** UIView コンテンツに `ksDialogTransition` で DialogTransition を設定する
- **WHEN** show で表示する
- **THEN** presentation フックがそのコンテンツのホスト View を引数に MainActor で呼ばれる

#### Scenario: [PB-IA-02] SwiftUI modifier での添付が器で採用される
- **GIVEN** SwiftUI コンテンツの body ルートに `dialogTransition` modifier を宣言する
- **WHEN** show で表示する
- **THEN** presentation フックが SwiftUI ホスト View を引数に呼ばれる

### Requirement: ステータスバー表示状態の非干渉 (iOS)

ダイアログの表示は、提示元の画面のステータスバー表示状態を変えない (SHALL)。器の提示はステータスバーの制御を奪わず、提示元が保つ。

#### Scenario: [PB-IA-03] ステータスバー非表示の画面でダイアログを出しても再出現しない
- **GIVEN** 提示元の画面がステータスバーを非表示にしている
- **WHEN** ダイアログを表示する
- **THEN** ステータスバーは非表示のまま維持される
