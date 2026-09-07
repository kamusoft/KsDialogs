# UI Brief: add-toast

## 画面と状態

- **デフォルト View** (ライブラリ同梱の内蔵コンテンツ — core/ADR-0028・0032):
  - 状態: 1行メッセージ / 複数行メッセージ (折り返し) / 多重時の重なり (起動順)
  - Toast は覆いを持たないため、デフォルト View が自分で背景 (ピル) を描く
  - スタイル (背景色・文字色・フォントサイズ・角丸・既定 duration・アプリ既定配置) は ToastStyle で一括設定可。モックは既定値の見え
  - 既定配置: 可視領域の下部中央 + 上方向オフセット 80 (標準的なボトムバー1本ぶんを回避 — core/ADR-0032)
- **Custom Toast デモ View** (Sample 側の参考実装):
  - 登録経路 (primary 地 + バッジ) とインライン経路 (surface-variant 地) の2枚が重なって表示される
  - SampleTheme トークン準拠

## リファレンス注釈

- Android の OS 標準 Toast: 半透明ダークのピルにテキストのみ。デフォルト View の見えの参照元 (画像の持ち込みはなし)
- 原典 (AiForms.Maui.Dialogs) の Toast にはデフォルト View が存在しないため、見えの参照元にならない

## デザイントークン参照

- Sample デモ View: [Sample パリティ規約の SampleTheme](../../../concepts/cross/conventions/sample-parity.md) (primary / on-primary / surface-variant / on-surface / divider)
- デフォルト View: ライブラリ既定値 (ToastStyle の既定。モックの値が実装の既定値の指標になる)

## 承認モック

- デフォルト View: **mock/default-pill.html (案A: OS Toast 準拠ピル) を採用** (approved.png、2026-08-27 オーナー承認)。案B (default-rounded.html) は不採用 — OS 慣習寄せの中立デザイン (core/ADR-0032) への忠実さを優先
- 承認時の反映2点: (1) オーナー指示で長文メッセージの複数行折り返し (高さがコンテンツに追随する確認) をモックと Sample 仕様 (`Toast Stack` の3枚目) に追加。(2) 完全角丸 (radius = 高さ/2) は複数行で卵形になるため、**1行時にピルに見える固定角丸 22** に修正 (複数行では角丸長方形として高さだけ伸びる — Android OS Toast と同じ振る舞い)
- Sample の Custom Toast デモ View: **mock/sample-custom.html を採用** (approved-sample-custom.png、同日承認・修正指示なし)

## 実装の既定値の指標 (ToastStyle / デフォルト View)

モックの値が実装の既定値の指標になる:

| 項目 | 既定値 | 根拠 |
|---|---|---|
| 背景色 | ダークグレー 92% (#323232 相当・半透明) | モック (OS Toast 慣習) |
| 文字色 | 白 | モック |
| フォントサイズ | 14 | モック (LoadingStyle 既定と同じ) |
| 角丸半径 | 22 (固定。複数行でも変えない) | 承認時のオーナー確認 (卵形回避) |
| 既定 duration | 1500ms | core/ADR-0031 (原典踏襲) |
| アプリ既定配置 | なし (nil = 契約既定値) | core/ADR-0032 |
| 契約既定配置 | 可視領域 下部中央 + 上方向オフセット 80 | core/ADR-0032 / design Decision 6 |

**内蔵コンテンツ側の固定値** (ToastStyle の設定項目ではない): 余白 11×22 / 最大幅 80% / 中央寄せ / 行間 1.5。

## 視覚照合結果

- デフォルト View (ピル): 実装スクリーンショットと approved.png の照合で**一致 (2周で収束)** — 初回照合後に review-001 が Android の落ち影欠落を検出し、実装追加 + 再撮影の再照合で影を含め一致 (iOS / Android とも実装定数は上表と一致)。詳細: verification/default-view-mock-match.md (2026-08-27)
- Sample Custom Toast デモ View: approved-sample-custom.png と一致 (同上)


