# android-native デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 全面改訂 — 供給機構を VM 契約から View 添付 + show placement 引数へ (core/ADR-0015)。isCanceledOnTouchOutside 追加。透明オーバーレイ要件は初版から維持。

## ADDED Requirements

### Requirement: メタ属性の供給機構 (Android)

Android 実装は `DialogOptions` / `DialogPlacement` を公開し、`View` への添付 (extension プロパティ) と show の placement 引数で供給できること (SHALL)。実効値は dialog-contract の優先順位に従うこと。VM 契約に属性を持たせないこと。

#### Scenario: View 添付と show 引数の合成
- **GIVEN** options と placement を添付した View を返す factory と、show の placement 引数
- **WHEN** show する
- **THEN** options は添付値、placement は show 引数の値が実効になる

### Requirement: レイアウト規則の実装とケース表全量適合 (Android)

Android 実装は軸別レイアウト規則を充足し、共通ケース表の全ケースに適合すること (SHALL)。実装機構 (LayoutParams / Gravity 等) は自由。

#### Scenario: ケース表の全量検証が通る (実 View)
- **GIVEN** 共通ケース表 (layout-cases.json 改訂版・19ケース)
- **WHEN** instrumented test (または忠実度を実証記録済みの Robolectric) が実 View のレイアウト後 rect を測定する
- **THEN** 全ケースが許容誤差内で期待 rect (approvedDiff があれば Android の承認済み期待値) に一致する

#### Scenario: 提示前サイズ確定 (Android)
- **GIVEN** 初期状態適用で内容が伸びる VM
- **WHEN** show する
- **THEN** 提示時点の高さが初回レイアウト完了後の内容を反映している

### Requirement: 透明オーバーレイの正式対応 (Android)

OverlayColor 透明時にステータスバーのみ暗転する事象を、提示構成の切り替えハック (原典方式) によらず解消すること (SHALL)。

#### Scenario: 透明時も配置規則が保たれる
- **GIVEN** OverlayColor = 透明かつ任意の配置指定
- **WHEN** show する
- **THEN** ステータスバーは暗転せず、かつ配置・サイズはケース表の期待どおりである (透明時だけ配置規則が変わる特殊分岐を持たない)

### Requirement: 外側タップキャンセル (Android)

dialog-contract の外側タップキャンセル規則を実装すること (SHALL)。

#### Scenario: 外側タップの結果経路
- **GIVEN** 既定設定のダイアログを表示した状態
- **WHEN** オーバーレイ領域をタップする
- **THEN** show の suspend 呼び出しが cancelled 結果で返る
