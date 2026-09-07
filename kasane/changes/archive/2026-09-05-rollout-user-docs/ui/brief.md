# UI Brief: rollout-user-docs

本変更は**新規 UI の設計を含まない**。ルート README に載せるスクリーンショットの選定だけが UI アーティファクトの対象で、撮影対象は既存の Sample アプリの画面である。HTML モックは生成せず、実機相当 (シミュレータ / エミュレータ) で撮った候補の中から選ぶ形を承認ゲートとする (mock 承認ゲートの変形、phase-2 agenda 決定)。

## 画面と状態

撮影対象は Sample のデモ駆動モード (`kasane/config.yaml` `ui.screenshot`、安定デモ ID は sample-parity.md へ移送予定) で起動直後に作れる状態:

| 機能 | 候補デモ ID | 撮れるもの |
|---|---|---|
| Dialog | `basic-dialog` / `declarative-dialog` / `text-input-dialog` | 覆いの上にカードが出た状態 |
| Loading | `default-loading` (刻み間隔を延ばして途中段階を撮る) | 操作ブロックとインジケータ |
| Toast | `toast-stack` / `default-toast` | 角丸ピルの通知 (重なりを見せるなら 3 枚) |

必要な組み合わせは iOS / Android × Dialog / Loading / Toast の 6 枚。

## レイアウト

README 上では横 2 列 (左 = iOS、右 = Android) × 縦 3 行 (Dialog / Loading / Toast)。.NET MAUI と KMP は Native をラップして同じ画面になるため画像を置かず、文で 1 行補足する。

## リファレンス注釈

`references/` は実装時に撮影する候補の置き場。撮影前は空。

## 撮影の統制

- シミュレータ / エミュレータで撮影する (実機を使わない)。起動中の Simulator は流用せず別デバイスを boot する
- ステータスバーに端末を特定できる表示 (キャリア名・実機の時刻・バッテリー残量) を写さない
- 6 枚は同一のリポジトリ revision から各 platform で同一構成でビルドした Sample を、同一の画面向きで撮り、platform と機能以外の差を作らない
- 撮影は scratchpad へ撮ってから `ui/references/` へ cp する (プロジェクト配下へ直接書けないため)

## 承認

承認日: 2026-09-05

撮影 revision: `53914ccb9571998245d044f5f927aa6acb71d477`

| Platform | 機能 | 採用元 | 配置先 | 比較条件 |
|---|---|---|---|---|
| iOS | Dialog | `ui/references/ios-dialog-basic-dialog.png` | `assets/ios-dialog.png` | `basic-dialog` |
| Android | Dialog | `ui/references/android-dialog-basic-dialog.png` | `assets/android-dialog.png` | `basic-dialog` |
| iOS | Loading | `ui/references/ios-loading-default-loading.png` | `assets/ios-loading.png` | `default-loading`、50%、`Soon...` |
| Android | Loading | `ui/references/android-loading-default-loading.png` | `assets/android-loading.png` | `default-loading`、50%、`Soon...` |
| iOS | Toast | `ui/references/ios-toast-toast-stack.png` | `assets/ios-toast.png` | `toast-stack`、3 枚を完全表示 |
| Android | Toast | `ui/references/android-toast-toast-stack.png` | `assets/android-toast.png` | `toast-stack`、3 枚を完全表示 |

iOS / Android の比較条件は、Dialog を `basic-dialog`、Loading を同じ 50% / `Soon...`、Toast を `toast-stack` に揃えた。6 枚はいずれも実画像を原寸で再確認し、個人情報および status / navigation 領域を含まないことを確認した。

iOS Toast はデモ自動再生が表示 host の準備前に取りこぼされるため、通常 UI の表示完了後に実メニューの `Toast Stack` を 1 回操作し、デモと同じ表示経路を起動して撮影した。この合意済み差分は `deviation.md` にも記録済みである。
