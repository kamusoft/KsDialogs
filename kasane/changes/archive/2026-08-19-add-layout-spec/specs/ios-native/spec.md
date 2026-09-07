# ios-native デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 全面改訂 — 供給機構を VM 契約から UIView 添付 + show placement 引数へ (core/ADR-0015)。isCanceledOnTouchOutside 追加。

## ADDED Requirements

### Requirement: メタ属性の供給機構 (iOS)

iOS 実装は `DialogOptions` / `DialogPlacement` を公開し、`UIView` への添付 (extension プロパティ) と show の placement 引数で供給できること (SHALL)。実効値は dialog-contract の優先順位に従うこと。VM 契約に属性を持たせないこと。

#### Scenario: View 添付と show 引数の合成
- **GIVEN** options と placement を添付した UIView を返す factory と、show の placement 引数
- **WHEN** show する
- **THEN** options は添付値、placement は show 引数の値が実効になる

### Requirement: レイアウト規則の実装とケース表全量適合 (iOS)

iOS 実装は軸別レイアウト規則を充足し、共通ケース表の全ケースに適合すること (SHALL)。実装機構 (AutoLayout / frame 計算等) は自由。

#### Scenario: ケース表の全量検証が通る (実 frame)
- **GIVEN** 共通ケース表 (layout-cases.json 改訂版・19ケース)
- **WHEN** シミュレータ上のテストがレイアウト完了後の実 frame を測定する
- **THEN** 全ケースが許容誤差内で期待 rect に一致する (rect 計算関数の単体検証だけでは満たさない — design Decision 7)

#### Scenario: 提示前サイズ確定 (iOS)
- **GIVEN** 初期状態適用で内容が伸びる VM
- **WHEN** show する
- **THEN** 提示時点の高さが初回レイアウト完了後の内容を反映している

### Requirement: 外側タップキャンセル (iOS)

dialog-contract の外側タップキャンセル規則を実装すること (SHALL)。

#### Scenario: 外側タップの結果経路
- **GIVEN** 既定設定のダイアログを表示した状態
- **WHEN** オーバーレイ領域をタップする
- **THEN** show の await が cancelled 結果で返る
