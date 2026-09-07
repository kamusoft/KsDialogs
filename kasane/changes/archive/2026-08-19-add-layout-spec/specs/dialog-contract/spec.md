# dialog-contract デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 全面改訂 — core/ADR-0014 (属性の取捨)・0015 (供給機構) の反映。属性は VM 契約から `DialogOptions` / `DialogPlacement` + コンテンツ添付供給へ、明示サイズ・描画系属性の廃止、isCanceledOnTouchOutside の編入。2026-08-17 初版 (相方スペックレビュー反映)。

## ADDED Requirements

### Requirement: メタ属性セットと既定値

ダイアログ契約は core/ADR-0015 の写像表 (design.md Decision 6 に転記) に定めるメタ属性を、静的メタ `DialogOptions` (layoutArea / dialogMargin / proportionalWidth / proportionalHeight / overlayColor / isCanceledOnTouchOutside) と動的メタ `DialogPlacement` (horizontalAlignment / verticalAlignment / offsetX / offsetY) の2つの値オブジェクトとして公開し、公開する形態間で同一の意味を持たせること (SHALL)。KMP 公開面は供給経路を持つ `DialogPlacement` のみを公開する (kmp-facade spec 参照)。全フィールドは既定値を持ち、無効値 (0 以下・1 超の比率、非有限値、負の Margin 辺) は写像表の正規化規則で丸めること。VM 契約に属性を持たせないこと。

#### Scenario: 既存コードの互換性
- **GIVEN** メタ属性を一切指定していない既存の VM・登録・show 呼び出し
- **WHEN** 本変更適用後にビルドし show する
- **THEN** 変更なしでコンパイルが通り、表示は現行実装の挙動 (ケース表 C19) と一致する

#### Scenario: 無効値の正規化
- **GIVEN** 非有限値 (NaN / ±Infinity) の数値フィールドや負の dialogMargin 辺を含む添付
- **WHEN** show する
- **THEN** 実効値は正規化規則 (非有限は当該フィールドの既定値、負の Margin 辺は 0、比率は 0 以下を未指定・1 超を 1 へ丸め) どおりになり、表示はその実効値での規則適合位置になる

### Requirement: 属性の供給と優先順位

`DialogOptions` と `DialogPlacement` はダイアログのコンテンツ (View) 定義への添付で供給できること (SHALL)。`DialogPlacement` のみ show の引数でも供給でき、実効値は「show 引数 > コンテンツ添付 > 契約既定値」の優先順位で決まること (SHALL)。show 引数の placement は添付 placement を**オブジェクト単位で置換**し、フィールド単位で合成しないこと (SHALL)。show の引数で `DialogOptions` を供給する経路を設けないこと。器が採用する実効値は**初回レイアウトパス完了時点で添付されている値のスナップショット**とし、以降の添付変更は表示に反映しないこと (SHALL)。

#### Scenario: 添付だけで供給される
- **GIVEN** コンテンツ定義に options (透明 overlay) と placement (End 配置) を添付し、show は引数なし
- **WHEN** show する
- **THEN** 表示は添付した options と placement を反映する

#### Scenario: show 引数の placement が添付に勝つ
- **GIVEN** コンテンツ定義に placement (End 配置) を添付し、show の引数で placement (Start 配置) を渡す
- **WHEN** show する
- **THEN** 配置は Start (show 引数) になり、添付の options は引き続き有効である

#### Scenario: show placement はオブジェクト単位で置換する
- **GIVEN** End/End + Offset を添付したコンテンツと、水平 Start だけを指定した show の placement 引数
- **WHEN** show する
- **THEN** 実効 placement は show 引数のオブジェクト全体 (水平 Start・垂直 Center・Offset 0) になり、添付の垂直配置と Offset は使われない

#### Scenario: 初回レイアウト完了後の添付変更は反映されない
- **GIVEN** 表示済みダイアログのコンテンツ View
- **WHEN** 添付 placement / options (isCanceledOnTouchOutside を含む) を変更する
- **THEN** 表示位置・見えは変わらず、操作挙動 (外側タップの結果) もスナップショット時点の値に従う

#### Scenario: 何も供給しなければ既定値
- **GIVEN** 添付も show 引数もないコンテンツ
- **WHEN** show する
- **THEN** 実効値はすべて契約既定値になる (ケース表 C19 と同じ表示)

### Requirement: 軸別レイアウト規則

最終 rect は軸ごとに design.md Decision 5 の手順 (基準 rect → Margin 控除 → サイズ選択 [比率 > Fill > 内容] → クランプ → anchor [Start/Center/End、Fill が負けた軸は Center] → Offset 加算 [クランプなし]) で一意に導くこと (SHALL)。Start / End は物理方向とする。

#### Scenario: 比率と Fill の競合
- **GIVEN** ProportionalWidth = 0.5 かつ HorizontalAlignment = Fill (ケース表 C05)
- **WHEN** show する
- **THEN** 幅は基準領域の 5 割になり、位置は Center 扱いになる

#### Scenario: 非対称 Margin の中心
- **GIVEN** 左のみ DialogMargin > 0 の Center 配置 (ケース表 C09)
- **WHEN** show する
- **THEN** Margin 控除後の有効領域の中心に配置される

#### Scenario: Offset の座標系は配置によらず一定で、領域外を許容する
- **GIVEN** HorizontalAlignment = End かつ OffsetX = 正の値 (ケース表 C07)
- **WHEN** show する
- **THEN** End 基準位置からさらに右へ移動する (符号反転しない)。基準領域を超えてもクランプされない

#### Scenario: visibleArea 基準は両軸に効く
- **GIVEN** LayoutArea = visibleArea と Start 配置 (ケース表 C11)
- **WHEN** show する
- **THEN** システムバーを除いた領域 (テストでは insets 入力で定義) を基準に配置される

### Requirement: 共通ケース表への適合

[layout-cases.json](layout-cases.json) (2026-08-18 改訂版・19ケース、提案フェーズで再凍結。ケース ID ↔ Requirement の対応表を内包) を単一の正とし、Native 実装は全ケースに許容誤差内で適合すること (SHALL)。ケース表の `attributes` は供給合成後の実効値として解釈する。OS 差は理由とオーナー承認を記録した `approvedDiff` エントリだけが有効であり、承認のない期待値分岐を持ち込まないこと。期待値の変更は deviation として扱うこと。

#### Scenario: 同一ケースが両 Native 実装で同じ期待 rect になる
- **GIVEN** ケース表の任意の1ケース
- **WHEN** iOS / Android 実装それぞれで同じ入力 (screen / insets / contentSize / attributes) を検証する
- **THEN** 双方がレイアウト完了後の実測 rect として期待値に一致する (approvedDiff があればその OS の承認済み期待値)

### Requirement: 提示前サイズ確定

ダイアログは、factory が返した内容に VM の初期状態を適用し、初回のネイティブレイアウトパスを完了した後のサイズで提示されること (SHALL)。

#### Scenario: 初期状態で伸びた内容が初期表示に反映される
- **GIVEN** VM の初期状態を適用すると本文が複数行へ伸びる内容
- **WHEN** show する
- **THEN** 提示時点のダイアログ高さは伸びた本文を収めたサイズになっている

### Requirement: 透明オーバーレイはシステムバーの見えを変えない

OverlayColor に透明を指定したとき、ダイアログの表示前後でシステムバー領域の表示状態は変化しないこと (SHALL)。

#### Scenario: 透明オーバーレイ表示中のステータスバー
- **GIVEN** OverlayColor = 透明を指定したコンテンツ
- **WHEN** show する
- **THEN** ステータスバー領域の見えは非表示時と同一である (Android で暗転しない)

### Requirement: 外側タップキャンセル

isCanceledOnTouchOutside = true (既定) のとき、ダイアログ外形 rect の外側へのタップでキャンセル操作と同一の経路 (cancelled 結果) によりダイアログが閉じること (SHALL)。false のとき外側タップは何も起こさず、イベントは背後の画面へ透過しないこと (SHALL)。この機構は overlayColor の値と独立に機能すること。

#### Scenario: 既定では外側タップでキャンセルされる
- **GIVEN** isCanceledOnTouchOutside を指定しない (既定 true) ダイアログを表示した状態
- **WHEN** ダイアログの外側をタップする
- **THEN** ダイアログが閉じ、show の結果は cancelled になる

#### Scenario: false なら外側タップは無反応でモーダル性を保つ
- **GIVEN** isCanceledOnTouchOutside = false を添付したダイアログを表示した状態
- **WHEN** ダイアログの外側をタップする
- **THEN** ダイアログは表示されたままで、背後の画面の要素も反応しない
