# UI Brief: revisit-dialog-margin-default

## 画面と状態

1. **Layout Dialog の属性調整パネル (既存画面の改訂)**: `Layout area` の行の下に `Margin` の行を足す。全辺そろえの余白を選び、`Show` (Panel タブのナビゲーションバーと Info タブの両方) で出すダイアログの中身にその値を添付する。初期値は契約の既定値 (0)。タブを切り替えても保たれる (配置・移動量・基準領域と同じ)。状態はモックの 3 枚: パネル初期 (Margin 0) / Current page + End/End + Margin 0 (タブバーの上端と右端に接する) / 同じ設定で Margin 24 (下端・右端から 24 内側)
2. **その他の画面**: 変更なし。メニュー、結果表示、Info タブの本文と構成、Toast の各デモの見た目はそのまま (Custom Toast の 2 枚は契約既定の余白が 0 になるぶん下へ寄るが、画面の構成は変えない)

## リファレンス注釈

- `../../archive/2026-09-27-add-page-layout-area/ui/mock/mock-b2.html` (前回の承認モック): 行構成・セグメント・数値欄・ナビゲーションバー・タブバー・カード形状を踏襲。**採用する要素**: 全部。**今回変える要素**: `Margin` の行の追加だけ
- 画像の新規リファレンスはなし

## デザイントークン参照

- 既存トークン (primary / on-primary / surface / surface-variant / on-surface / on-surface-muted / scrim / divider) のみ。モックの追加行の薄青は差分を示す注記で、実装では色を付けない
- 余白のセグメントは配置の行 (Horizontal / Vertical) と同じ部品を使う
- 生値はモック内 CSS 変数が正。デルタスペックには書かない

## 余白の行 (A 案で確定)

- 形: プリセットのセグメント `0` / `24` / `48` (初期 `0`)。0 は契約の既定値、24 は旧既定値と既定 Toast の余白。選んだ値を全辺そろえで添付する。選択肢以外の値は出ない
- 置き方: `Layout area` の行の下に 1 行。ラベルを左、セグメントを右に置く (配置の行と同じ並び)
- 読み上げ: 選択肢は複合名 `Margin 0` / `Margin 24` / `Margin 48` (数字だけでは何の値か読めないため。`Horizontal Start` と同型)。選択状態は OS 標準の状態機構に載せる

辺ごとの指定は持たない (パネルの目的は配置と基準領域の効き方を見ることで、非対称な余白の確認はケース表のテストが受け持つ)。

## 文言表 (パリティの正 — 4 ルート一字一句一致)

| 場所 | 文言 |
|---|---|
| 余白の行ラベル | `Margin` |
| 余白の選択肢 | `0` / `24` / `48` (初期 `0`) |
| 既存の文言 (Horizontal / Vertical / OffsetX / OffsetY / Layout area とその選択肢 / Show / タブ名 / Info タブの本文 / `レイアウト確認` / `キャンセル` / `OK` / 結果表示 / 戻る `‹`) | 変更なし |

## 承認モック

mock/mock-a.html を採用 (approved.png、2026-09-27 オーナー承認 — 余白をプリセットのセグメント `0` / `24` / `48` で選ぶ。3 状態)。mock-b.html (数値欄) は不採用の比較案として残す。

## 実装時の裁量 (モックとの差分として許容するもの)

- ナビゲーションバー・タブバー・戻る記号・タブのアイコンは前回の承認どおり OS 実装のまま
- 行の高さ・行間はプラットフォームの既存パネルの行に合わせる (モックの寸法は目安)

## 照合結果 (ksn-ui 視覚照合 — 2026-09-27、オーナー最終承認 2026-09-27)

`verification/` の 18 枚 (4 ルート × 3 状態。MAUI・KMP は iOS / Android の両方) を `mock/approved.png` の A-1〜A-3 と照合した。1 周で収束し、要修正の乖離はなし。合意済み妥協は 0 件 (下の「実装時の裁量の範囲で出た差」は brief の裁量に収まるもの)。

| 状態 (モック) | iOS Native | Android Native | KMP (iOS / Android) | MAUI (iOS / Android) |
|---|---|---|---|---|
| A-1 パネル初期 (Margin 0) | `ios-panel-initial.png` | `android-panel-initial.png` | `kmp-ios-panel-initial.png` / `kmp-android-panel-initial.png` | `maui-ios-panel-initial.png` / `maui-android-panel-initial.png` |
| A-2 Current page + End/End + Margin 0 | `ios-current-page-end-end-margin-0.png` | `android-current-page-end-end-margin-0.png` | `kmp-ios-current-page-end-end-margin-0.png` / `kmp-android-current-page-end-end-margin-0.png` | `maui-ios-current-page-end-end-margin-0.png` / `maui-android-current-page-end-end-margin-0.png` |
| A-3 同じ設定で Margin 24 | `ios-current-page-end-end-margin-24.png` | `android-current-page-end-end-margin-24.png` | `kmp-ios-current-page-end-end-margin-24.png` / `kmp-android-current-page-end-end-margin-24.png` | `maui-ios-current-page-end-end-margin-24.png` / `maui-android-current-page-end-end-margin-24.png` |

- 構造: `Layout area` の行の下に `Margin` の行を 1 行、ラベル左・セグメント右 (配置の行と同じ部品)。選択肢 `0` / `24` / `48`、初期 `0`。4 ルート一致
- トークン: 追加行に色は付けない (モックの薄青は注記)。セグメントは既存の primary / on-primary / surface-variant / on-surface-muted をそのまま使う
- 意図 (配置の実測。カード = 覆いの上の白の外接矩形を画素から測定): Margin 0 は右端が画面の右端に、下端がタブバーの上端に接する (iOS 3 ルート: 下端 791pt = TabBar の frame 上端 791pt。Android Native / KMP Android: 下端 809dp = タブバー上端の罫線 809dp)。Margin 24 は右端・下端とも 24 内側 (iOS: 右 24.3pt・下 24pt / Android 3 ルート: 右 24dp・下 23.6〜24dp、2px 刻みの測定誤差内)。同じ OS の 3 ルートは同じ座標
- タブを切り替えても余白が保たれる (Scenario): Panel で Margin 24・Current page・Start/Start にして Info タブの `Show` で出すと、左端 24・上端はステータスバーの下端から 24 (iOS 3 ルート: 左 24pt・上 86pt = safe area 62 + 24 / Android Native・KMP Android: 左 24.4dp・上 48dp = ステータスバー 24 + 24 / MAUI Android: 左 24.4dp・上 73.1dp = 上端の帯 48.8 + 24)。Panel タブへ戻っても Margin 24 が選択されたまま (読み上げ階層の back-to-panel で確認)。画像は証跡に残していない (モックの 3 状態の外のため)
- 読み上げ: `accessibility-<ルート>.txt` (iOS 3 ルートは XCUITest の debugDescription、Android 3 ルートは uiautomator dump)。4 ルートとも選択肢の読み上げ名は `Margin 0` / `Margin 24` / `Margin 48`、役割は Button、選択状態は OS 標準の機構 (iOS: Selected trait / Compose: checkable・checked / MAUI Android: View.Selected — いずれも既存の配置・基準領域のセグメントと同じ載せ方)
- 撮影環境: iOS は新規作成した iPhone 17 Simulator (iOS 26.5)、Android は API 35 の Pixel 6 相当 AVD (1080x2400) をこの照合用に read-only で起動。Android の画像は上端のステータスバーの文字が欠けて写るが、AVD の表示設定によるもので Sample の描画ではない。MAUI Android の上端の紫の帯は既存 (この change の差分ではない)。写り込みの個人要素 (通知・アカウント・端末名) がないことを 18 枚とも開いて確認し、md5 の重複がないことを確認した

### 実装時の裁量の範囲で出た差

- モックの数値セグメントは等幅書体・最小幅付きで描かれているが、brief の「余白のセグメントは配置の行と同じ部品を使う」に従い、配置のセグメントと同じ書体・寸法で実装した (見た目は選択肢の幅がわずかに狭い)
- モック A-1 の `結果: —` の欄は、既存の規約どおり一度も結果が出ていない間は表示しない (前回承認時と同じ)
