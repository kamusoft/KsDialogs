# samples デルタスペック (expand-api-surface)

改訂履歴: 2026-08-17 相方レビュー採用指摘の反映 — 文言表の設置 (#9)・登録2スタイルの Requirement 化 (#10)。
2026-08-19 add-layout-spec 完了分の反映 — 属性調整パネル操作部の読み上げ対応 Requirement を追加 (phase-5-2 agenda 決定)。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — 同名選択肢の複合名規則の確定・状態と現在値の読み上げ・検査方法の明記。

## ADDED Requirements

### Requirement: API 表面の新デモ項目

samples の 4 ルートすべてに、次のデモ項目をパリティ準拠 (cross/ADR-0007) で追加すること (SHALL): (1) 宣言的 UI コンテンツのダイアログ (Native は Compose / SwiftUI、MAUI は MAUI View での同等デモ) (2) カスタム結果型のダイアログ (テキスト入力 → 文字列結果) (3) インライン show のダイアログ。文言 (メニュー名・ダイアログ内文言・初期入力値・結果表記) は ui/brief.md の文言表を正とし、実装完了後の蒸留で cross の sample-parity 規約へ反映すること。

#### Scenario: 宣言的 UI デモ
- **GIVEN** サンプルアプリのメニュー
- **WHEN** 宣言的 UI デモ項目を起動し、完了操作で閉じる
- **THEN** 結果が既存デモと同じ形式で表示される

#### Scenario: カスタム結果型デモ
- **GIVEN** テキスト入力デモ項目を起動した状態
- **WHEN** 文字列を入力して完了操作で閉じる
- **THEN** 入力した文字列が結果表示に現れる

#### Scenario: インライン show デモ
- **GIVEN** サンプルアプリのメニュー
- **WHEN** インライン show デモ項目を起動し、閉じる
- **THEN** 結果が既存デモと同じ形式で表示される (事前登録なしの経路であることは実装コードで確認する)

### Requirement: 属性調整パネル操作部の読み上げ対応

Layout Dialog の属性調整パネル内の操作部 (配置の選択肢・移動量欄・基準領域トグル・表示操作) に、読み上げ用の名前と役割を4ルート一致で付与すること (SHALL。戻る記号の既存規約 — cross の sample-parity — と同型で、付け方は OS 標準の手段でよい)。読み上げ名の規則: 画面の文言と同一を基本とし、同名の選択肢 (Horizontal / Vertical それぞれの Start / Center / End) だけは「所属行の文言 + 選択肢の文言」の複合名 (例: `Horizontal Start`) とする — OS ごとの読み上げ順に依存せず一意に識別できるようにするため。操作部の状態 (選択肢の選択状態・トグルの ON/OFF・移動量欄の現在値) は OS 標準の役割・状態機構で読み上げ情報に含まれること。検証は accessibility tree の検査 (自動検査、または実機のスクリーンリーダー確認) で行い、4ルートの証跡を残すこと。add-layout-spec 残課題の引き取り (phase-5-2 agenda 決定 2026-08-19)。

#### Scenario: パネル操作部が読み上げで識別できる
- **GIVEN** 属性調整パネルを表示した状態
- **WHEN** 各操作部の読み上げ情報 (名前・役割) を accessibility 検査で確認する
- **THEN** すべての操作部に名前と役割が付与されており、同名の選択肢は複合名で一意に識別できる (4ルート一致)

#### Scenario: 操作部の状態が読み上げに含まれる
- **GIVEN** パネルで配置を選択し、トグルを切り替え、移動量欄に値を入れた状態
- **WHEN** 各操作部の読み上げ情報を accessibility 検査で確認する
- **THEN** 選択肢の選択状態・トグルの ON/OFF・移動量欄の現在値が読み上げ情報に含まれる

### Requirement: KMP iOS Sample の公開 API 化

KMP iOS Sample の登録コードは Swift 向け型付き公開 API のみを使用すること (SHALL)。(既存 Requirement「Sample の consumer 境界」への適合回復 — verify-001 ❌3 の解消)

#### Scenario: 機械面直接利用の解消
- **GIVEN** KMP iOS Sample の登録コード
- **WHEN** 参照している API を確認する
- **THEN** `KsDialogsInteropBridge` への直接参照が存在しない

### Requirement: MAUI Sample の登録2スタイル提示

MAUI Sample の登録コードは、型引数明示スタイルと明示型付きラムダスタイル (型引数なし) の両方を実際の登録に使い、利用例として提示すること (SHALL。phase-5-2 決定「両スタイルを Sample で示す」の反映)。

#### Scenario: 2スタイルが実登録に存在する
- **GIVEN** MAUI Sample の登録コード
- **WHEN** 登録呼び出しのスタイルを確認する
- **THEN** 型引数明示の呼び出しと、明示型付きラムダによる型引数なしの呼び出しの両方が存在する
