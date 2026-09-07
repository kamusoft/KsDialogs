# UI Brief: add-layout-spec

改訂履歴: 2026-08-17 オーナー指示によりレイアウトデモを「属性調整パネル方式」(原典 AiForms サンプル踏襲) に変更。

## 画面と状態

1. **Sample メニュー画面**: タイトル + デモ項目一覧 (Basic Dialog / Layout Dialog の2項目) + 結果表示エリア。状態は「初期 (結果エリアなし)」と「結果確定後 (結果エリアあり)」の2状態
2. **Basic Dialog**: 既存デザイン (mock-b 承認済み) を踏襲。ボタンのタップ領域のみ改訂
3. **Layout Dialog (新規デモ・属性調整パネル)**: 配置 (水平/垂直の Start・Center・End 択一)・OffsetX/Y (数値入力)・LayoutArea (visibleArea トグル) を調整し、「Show」でダイアログを表示する。パネル初期値は契約の既定値 (Center / 0 / visibleArea)
4. **Layout Dialog の表示例**: パネル設定に応じた配置 (モックでは End 配置 + Offset の例)

## リファレンス注釈

- `references/vertical-slice-approved.png` (add-vertical-slice の承認モック): カード形状・塗りボタン・scrim を踏襲。ボタン高さのみ本改訂の対象
- **原典 AiForms サンプルの属性調整画面** (オーナー提示のスクリーンショット、2026-08-17。画像実体は原典リポジトリ — 在り処は [参考リポジトリの在り処](../../../concepts/cross/conventions/reference-repositories.md) — の Sample アプリ MainPage が恒久リファレンス): セクション見出し (HorizontalAlignment / VerticalAlignment / Offset / Options) + 択一チェック + 数値行 + トグルの settings リスト形式。**採用する要素**: セクション分け・択一チェック・数値行・トグルの構成。**今回対象外**: 原典の Loading / Toast 項目 (phase-7/8)、Proportional サイズ調整 (パネルは最小構成で開始し、必要なら実装フェーズで追加提案)

## デザイントークン参照

- 既存トークン (mock-b 由来) + surface-variant (本変更で昇格)。パネルのセクション見出し背景に surface-variant を使用
- タップ領域の最小値 44pt (iOS HIG) / 48dp (Material)。パネルの行も同基準
- 生値はモック内 CSS 変数が正。デルタスペックには書かない

## 文言表 (パリティの正 — 4ルート一字一句一致)

| 場所 | 文言 |
|---|---|
| メニュー項目2 | `Layout Dialog` |
| パネル画面タイトル | `Layout Dialog` |
| セグメント行ラベル | `Horizontal` / `Vertical` (各行に `Start` / `Center` / `End` のセグメント) |
| 数値行 | `OffsetX` / `OffsetY` (初期値 `0`) |
| トグル行 | `Use visible area` (初期 ON = visibleArea) |
| 表示操作 | `Show` |
| ダイアログ本文 | `レイアウト確認` |
| ボタン | `キャンセル` / `OK` |
| 結果表示 | `結果: completed(true)` / `結果: cancelled` (Basic と同形式) |

※ オーナー指示 (2026-08-17): 原典のセクション+択一リスト形式に拘らず、1行セグメント形式のコンパクト表現を採用。

## 承認モック

mock/mock-revised.html を採用 (approved.png、2026-08-17 オーナー承認 — メニュー・Basic・結果エリア2状態・タップ領域44・surface-variant)。

mock/mock-layout-panel.html (コンパクト版) を採用 (approved-layout-panel.png、2026-08-17 オーナー承認 — Layout Dialog の属性調整パネル。セグメント1行形式 + Offset 数値フィールド + visible area トグル、Show 後の End/End 表示例)。初版のセクション+択一リスト形式 (原典踏襲) はオーナー指示でコンパクト版に改訂のうえ承認。なお mock-revised.html 内の旧 Layout Dialog 表示例 (固定 End 配置) はパネル方式への変更に伴い参考扱いとする。

## 実装時の合意事項 (モックとの差分)

承認済みモックとの差分のうち、オーナー指示によるものは deviation.md が正 (戻る導線・パネル内結果表示の2点)。以下はプラットフォーム制約と意匠トークン内の裁量による差分:

- **トグルは各 OS 標準のスイッチを使う**: モックは自作トグル (46×28・primary トラック + 白ノブ)。iOS 系 (iOS Native / KMP / MAUI iOS) は OS 標準スイッチ、Android 系 (Android Native / KMP / MAUI Android) は標準 Switch に同寸・同色の塗りを与えた。ノブの可動域とドロップシャドウは OS 実装のまま
- **Layout Dialog のカード幅は 240** (Basic Dialog は 272): モックどおり。既定余白 24 の下で End 配置が見て取れる幅として意味を持つ
- **MAUI の移動量入力のキーボードは platform 側で指定する**: MAUI の `Keyboard` に符号付き数値の選択肢がないため、ハンドラー接続後に platform のキーボード種別を上書きする (iOS は記号を含む数値キーボード、Android は符号付き数値)。これで4ルートとも負値を直接入力でき、見た目は変わらない
- **パネル内の結果表示エリアは surface-variant を地に敷く**: 設定行の連なりと切り分けるため (オーナー裁量指示による)。メニューの結果表示エリアは承認済みモックのまま地色なし
- **Show と戻るのタップ領域は iOS 44pt / Android 48dp**: ダイアログボタンと同じ基準に揃えた。セグメント (34) ・数値欄 (36) ・トグル (28) は行 (48) の内側の操作部としてモックの寸法どおり
- **戻る導線には読み上げ用の名前「戻る」と、ボタンとしての役割を与える**: 画面に出る記号は `‹` のままで、読み上げにだけ効く属性を4ルートに足した (見た目は不変)。名前の値は4ルート一致。iOS 系 (iOS Native / KMP) は SwiftUI の Button に名前を与えるだけで役割は既定で付く。Android 系 (Android Native / KMP) は押せる TextView に名前を与え、読み上げ情報の種別をボタンに差し替える。MAUI は役割を指定する共通の API がなく、加えて文字を持つ platform の TextView には共通の名前指定が乗らないため、表示が組み上がったあとに platform 側の属性 (iOS は名前と種別、Android は名前・種別・起動の道) を直接与えて他ルートに揃えた

トークン候補: なし。パネルの意匠は既存トークン (primary / on-primary / surface / surface-variant / on-surface / on-surface-muted / divider) の組み合わせで賄えた。

## 照合結果

verification/ に4ルート×6状態のスクリーンショットを保存し、approved.png (メニュー初期・Basic Dialog・結果確定後) と approved-layout-panel.png (パネル初期・Show 後の End/End) に照合した (2026-08-19)。構造・トークン・状態・意図の4観点で不一致なし。deviation.md の2点と上記の合意事項は差分として扱わない。撮影は iOS Simulator (iPhone 17 Pro) と Android 実機 (Pixel 6a)。KMP / MAUI は iOS 側で撮影しており、両ルートの Android 側はビルド green までの確認にとどまる。オーナーによる最終承認を取得済み (2026-08-19、蒸留時)。

KMP ルートの Android 側も照合の穴だったため、Pixel 6a で追撮して補完照合した (2026-08-19)。メニュー初期・Basic Dialog・メニュー結果確定後・パネル初期・Show 後 (End/End)・パネル結果確定後の6状態を verification/kmp-android-*.png に保存し、approved.png / approved-layout-panel.png と Android Native ルートの同状態に照合した。構造・トークン・状態・意図の4観点で不一致なし。乖離は0件で、修正は不要だった (MAUI Android で出たトグルの塗り・移動量欄の下線に相当する不備は、KMP androidApp が Android Native と同じ自前の塗りを持つため最初から出ていない)。Android Native ルートとの突き合わせは画素差分で行い、6状態すべて時計の表示以外に差がないことを確認している。

MAUI ルートの Android 側は照合の穴だったため、Pixel 6a で追撮して補完照合した (2026-08-19)。パネル初期と Show 後 (End/End) の2状態を verification/maui-android-layout-panel-initial.png / verification/maui-android-layout-panel-result.png に保存し、approved-layout-panel.png と Android Native ルートの同状態に照合した。この過程で見つかった2点 — 基準領域のトグルが既定の色合いのまま細く出る・移動量欄に既定の下線が出る — は実装を直して解消し、Android Native ルートと同じ見た目に揃えた。iOS 側は同じ状態を撮り直して既存の verification/maui-layout-panel-initial.png と突き合わせ、時計の表示以外に差分がないことを確認済み。

戻る導線への読み上げ属性の追加 (上の合意事項) は見た目を変えないことを、5つの実行形態すべてで撮り直して確かめた (2026-08-19)。iOS Native / KMP iOS / MAUI iOS は iPhone 17 Pro のシミュレータで、Android Native / KMP Android / MAUI Android は Pixel 6a でパネル初期を撮り、既存の同名の証跡と画素差分を取った結果、差は時計の表示だけだった (Android の3ルートは完全一致)。差が無いため証跡の差し替えはしていない。読み上げ側は、Android の3ルートで読み上げ情報を直接読み出し、いずれも名前「戻る」・種別ボタン・起動可能であることを確認した。MAUI iOS も実行時に名前・種別が付いていることを確認した。iOS Native / KMP iOS は SwiftUI の標準の指定のみで、実行時の読み上げ情報までは見ていない。
