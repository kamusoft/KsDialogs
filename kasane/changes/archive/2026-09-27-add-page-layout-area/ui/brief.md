# UI Brief: add-page-layout-area

## 画面と状態

1. **Layout Dialog の属性調整パネル (既存画面の改訂)**: 画面の下部にタブバー (`Panel` / `Info`、アイコン + 文字) を持つ。パネルのタブのタイトル帯は自前の帯ではなく**各 OS 標準のナビゲーションバー** (ページの外側にある本物のバー) に変え、戻る `‹` と `Show` はそのバーに置く。`Info` タブは**タイトルバーを持たないページ**で、説明文と `Show` を持つ。こうして「Current page ではタブバーだけでなく上のバーも基準から除かれる」ことを、Start/Start をパネルのタブ (バーの下に出る) と Info タブ (画面の上端に出る) で見比べられる (オーナー指示 2026-09-26)。パネルの基準領域の行は、トグル (`Use visible area`) から 3 択セグメント (Window / Visible area / Current page) に変わり、ラベルの下に全幅で折り返す。初期値は契約の既定値 (Center / 0 / Visible area)。状態はモックの 6 枚: パネル初期 / Current page + End/End (タブバーの上) / Visible area + End/End (タブバーに重なる) / Current page + Start/Start (ナビゲーションバーの下) / Info タブ / Info タブで Current page + Start/Start (画面の上端)
2. **その他の画面**: 変更なし (メニュー・結果表示・戻る導線は add-layout-spec の承認モックと deviation のまま)

## リファレンス注釈

- `../../archive/2026-08-19-add-layout-spec/ui/mock/mock-layout-panel.html` (approved-layout-panel.png): パネルの行構成・セグメント・数値欄・Show・カード形状を踏襲。**採用する要素**: 全部。**今回変える要素**: 基準領域の行 (トグル → セグメント)、下部のタブバーの追加
- 画像の新規リファレンスはなし

## デザイントークン参照

- 既存トークン (primary / on-primary / surface / surface-variant / on-surface / on-surface-muted / scrim / divider) のみ。タブバーは surface 地 + divider の上罫線、選択タブは primary
- タブバーとナビゲーションバーは各 OS 標準の部品を使う (SwiftUI: TabView + NavigationStack の toolbar — 各タブの content 枠に `.ksDialogCurrentPage()` / Compose: Scaffold の bottomBar (NavigationBar) と topBar (TopAppBar) — content 枠に modifier / MAUI: TabbedPage の子に NavigationPage、Info タブは素の ContentPage / KMP: Android は Compose、iOS は SwiftUI と同じ)。高さ・アイコン・選択表現は OS 実装のまま (モックの寸法は目安)。タブのアイコンは OS 標準のシンボルから「一覧」「情報」に相当するものを選ぶ (4 ルートで意味が揃えばよく、字形の一致は求めない)
- タップ領域は既存規約どおり (iOS 44pt / Android 48dp)
- 生値はモック内 CSS 変数が正。デルタスペックには書かない

## 文言表 (パリティの正 — 4 ルート一字一句一致。承認モックの案で確定)

| 場所 | 文言 |
|---|---|
| 基準領域の行ラベル | `Layout area` |
| 基準領域の選択肢 | `Window` / `Visible area` / `Current page` (初期 `Visible area`) |
| タブ名 | `Panel` / `Info` (アイコン + 文字) |
| Info タブの本文 | `このタブにはタイトルバーがありません。Current page を選ぶと、ダイアログはタブバーの内側 (このページの領域) を基準に置かれます。` |
| Info タブの表示操作 | `Show` (パネルの設定をそのまま使う) |
| 既存の文言 (Horizontal / Vertical / OffsetX / OffsetY / Show / `レイアウト確認` / `キャンセル` / `OK` / 結果表示 / 戻る `‹`) | 変更なし |

案 A (`Window` / `Visible` / `Page`、`Panel` / `About`) は 2026-09-26 に不採用 (契約語のほうが API の値と一対一で結び付く)。

## 承認モック

mock/mock-b2.html を採用 (approved.png、2026-09-26 オーナー承認 — 案 B のセグメント + アイコン付きタブ、パネルのタブは OS 標準ナビゲーションバー、Info タブはタイトルバーなし + Show。6 状態)。mock-a.html / mock-b.html は不採用の比較案として残す。

## 実装時の裁量 (モックとの差分として許容するもの)

- 戻る `‹` と `Show` はナビゲーションバーの中に置く (SwiftUI は toolbar の leading / trailing、Compose は TopAppBar の navigationIcon / actions、MAUI は NavigationPage の戻るとページの ToolbarItem)。バーの高さ・タイトルの寄せ・戻るの字形は OS 実装のまま
- タブのアイコンは OS 標準シンボルの「一覧」「情報」相当。字形の一致は求めない (4 ルートで意味が揃えばよい)
- 基準領域のセグメントは既存の Horizontal / Vertical と同じ部品を全幅で使う。3 つの選択肢の幅は等分
