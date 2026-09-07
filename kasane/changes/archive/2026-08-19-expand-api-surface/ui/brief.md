# UI Brief: expand-api-surface

## 画面と状態

1. **Sample メニュー画面**: add-layout-spec 承認モックの構成に、デモ項目3つを追加して5項目にする (Basic / Layout / Declarative / Text Input / Inline)。結果表示エリアの表示条件 (初期非表示) は add-layout-spec の決定を踏襲
2. **Declarative Dialog / Inline Dialog**: 見た目は Basic Dialog と同一デザイン (差は実装経路のみ)。専用モックは作らない
3. **Text Input Dialog (新規)**: メッセージ + テキスト入力欄 + キャンセル/OK。文字列結果が結果エリアに表示される

## リファレンス注釈

- `references/layout-spec-approved.png` (add-layout-spec の承認モック): カード形状・ボタン形状 (最小44)・トークン・メニュー行48 をすべて踏襲する。本変更で上書きする要素はない。2026-08-19 鮮度照合済み — add-layout-spec 最終承認モック (アーカイブの approved.png) と同一
- `references/layout-spec-approved-panel.png` (add-layout-spec の属性調整パネル承認モック、2026-08-19 引き継ぎ): パネル操作部の読み上げ対応 (specs/samples) の対象画面。**見た目の変更はない** (読み上げ名・役割はスクリーンリーダーにだけ効く) ため、本変更でモックは改訂しない

## デザイントークン参照

- add-layout-spec 承認モックのトークンセット (primary / on-primary / surface / surface-variant / on-surface / on-surface-muted / scrim / divider) をそのまま使用。新規トークンなし
- 入力欄はボタンと同じ最小44・角丸系を踏襲 (生値はモック CSS が正)

## 文言表 (パリティの正 — 4ルート一字一句一致)

| 場所 | 文言 |
|---|---|
| メニュー項目3〜5 | `Declarative Dialog` / `Text Input Dialog` / `Inline Dialog` |
| Declarative Dialog 本文 | `こんにちは、KsDialogs!` (Basic と同文言。差は実装経路のみ) |
| Text Input Dialog 本文 | `メッセージを入力してください` / 入力欄プレースホルダ `ここに入力` / 初期入力値 なし (空。承認モックの `こんにちは` は入力後の状態例) |
| Inline Dialog 本文 | `インライン表示です` |
| ボタン (全デモ共通) | `キャンセル` / `OK` |
| 結果表示 | 真偽値: `結果: completed(true)` / 文字列: `結果: completed("<入力値>")` / キャンセル: `結果: cancelled` |

実装完了後の蒸留で cross の sample-parity 規約 (文言表) へ反映する (tasks 7.4)。

## 承認モック

mock/mock-api.html を採用 (approved.png、2026-08-17 オーナー承認)。add-layout-spec 承認モックの完全踏襲 + デモ項目3追加 (Declarative / Text Input / Inline)。新規の見た目は Text Input Dialog のみ (入力欄は最小44・角丸系をボタンと統一)。1案提示 (既承認デザインの増分のため)。

## 実装時の追記 (2026-08-19)

### 照合結果

`verification/` に4ルート分の実装スクリーンショット (メニュー5項目 + 文字列結果 / Declarative / Text Input / Inline、
kmp は iosApp・androidApp の両方、maui は Android・iOS の両方) を保存し、
approved.png と構造・トークン・状態・意図の4観点で照合した。2周で収束。**2026-08-19 オーナー最終承認**。

- 1周目: maui の iOS 側で入力欄の枠が二重に見える乖離を1件検出 (外側の `Border` に加えて `UITextField` 既定の角丸枠が出る)。
  同じ症状が属性調整パネルの移動量欄にも出ていたため、両方で platform 既定の枠を消して揃えた
- 2周目: 乖離なし

- **構造**: タイトル帯 → 区切り線 → デモ項目5行 (文言 + `›`) → 結果表示エリア (見出し + 値) の並びが4ルートで一致
- **トークン**: SampleTheme の同一 RGBA を使用。カード 272 幅・角丸20・ボタン角丸10・入力欄角丸10 も一致
- **状態**: 結果表示エリアの初期非表示、入力欄の下書き文言表示と入力後表示の両方を確認
- **意図**: 完了操作 (OK) の強調 (primary の塗り + 太字) が4ルートで保たれている

### 合意済み妥協 (platform 制約)

- **Text Input Dialog の入力欄の高さ**: mock では最小44。実装は iOS 44 / Android 48 とした。
  理由: 各 OS の推奨タップ領域が異なり、既存デモのボタン高さが同じ規則 (iOS 44 / Android 48) で実装済みのため、
  入力欄だけ規則を変えると Sample 内で不揃いになる

### トークン候補

なし (新規トークンは発生しなかった)

### 読み上げ対応の視覚影響

パネル操作部の読み上げ対応は名前・役割・状態の付与だけで、見た目には影響しない。
`verification/panel-accessibility/android-panel-changed.png` が対応後のパネル外観で、
リファレンスの `references/layout-spec-approved-panel.png` と構造・トークンが一致していることを確認した。
