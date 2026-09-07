# 視覚照合の記録 (tasks 6.5 / 6.6)

承認モック `../mock/approved.png` (mock-b) と、各 Sample の実行スクリーンショットを突き合わせた結果。
最終承認 (オーナーによる 6 セル手動確認) は tasks 7.2 が担うため、本ファイルは実装者側の照合結果の記録。

- 初回照合: 2026-08-15 (Native iOS / Native Android / KMP iOS / KMP Android の4セル)
- 再照合: 2026-08-15 (MAUI iOS / MAUI Android。提示経路の修正後に実施し、**6セル全部が照合済みになった**)
- 照合環境: iPhone 17 Simulator (iOS 26.5) / Android Emulator API 35 (1080x2340)

## 保存した証跡

| ファイル | セル | 内容 |
|---|---|---|
| `ios-native-dialog.png` | Native iOS | ダイアログ表示中 |
| `ios-native-menu.png` | Native iOS | キャンセル後の結果表示 |
| `android-native-dialog.png` | Native Android | ダイアログ表示中 |
| `android-native-menu.png` | Native Android | 完了後の結果表示 |
| `kmp-ios-dialog.png` | KMP iOS | ダイアログ表示中 |
| `kmp-ios-menu.png` | KMP iOS | 完了後の結果表示 |
| `kmp-android-dialog.png` | KMP Android | ダイアログ表示中 |
| `kmp-android-menu.png` | KMP Android | 完了後の結果表示 |
| `maui-ios-menu.png` | MAUI iOS | メニュー画面 |
| `maui-ios-dialog.png` | MAUI iOS | ダイアログ表示中 |
| `maui-ios-completed.png` | MAUI iOS | 完了後の結果表示 |
| `maui-ios-cancelled.png` | MAUI iOS | キャンセル後の結果表示 |
| `maui-android-menu.png` | MAUI Android | メニュー画面 |
| `maui-android-dialog.png` | MAUI Android | ダイアログ表示中 |
| `maui-android-completed.png` | MAUI Android | 完了後の結果表示 |
| `maui-android-cancelled.png` | MAUI Android | キャンセル後の結果表示 |
| `maui-ios-dialog-before-fix.png` | MAUI iOS | **修正前の記録**。覆いだけが出て中身が寸法 0 で描かれなかった状態 (提示経路の修正で解消) |

## 照合結果

### 構造

**6セルすべてで一致**。上から タイトル帯 → 区切り線 → デモ項目行 (文言 + 右端シェブロン) → 区切り線 →
結果表示エリア (見出し + 値) の順。ダイアログは 覆い → 中央のカード → メッセージ → ボタン列 (キャンセル / OK) の順。

- ボタンの並びはモックどおり左がキャンセル・右が OK、幅は等分、間隔 10
- カードは幅 272・角丸 20・上 24 / 左右 20 / 下 20 の余白

### トークン

モックの SampleTheme と同一の RGBA を各ルートで宣言している (`samples/README.md` に一覧)。
覆いはライブラリの器が描く黒 40% で、モックの `rgba(0,0,0,0.4)` と一致。

**トークン候補 (brief.md のトークン一覧に無い値)**: キャンセルボタンの塗り `#F3F4F6`。
モックでは生値で書かれており、実装では `surface-variant` 相当として各ルートに置いた。蒸留時に concepts 化を検討する。

### 状態

brief.md の状態は「通常のみ」。実装した状態は メニュー (結果なし) / メニュー (結果あり) / ダイアログ表示中 の3つ。

**モック・brief に定義が無く実装判断した点**: 一度もダイアログを閉じていない初期状態の結果表示エリア。
モックは結果がある状態しか描いていないため、**結果が出るまではエリアごと非表示**とした (新しい文言を作らない選択)。
オーナー確認が要る (tasks 7.2)。

### 意図

強調 (OK = primary 塗り + 太字 / キャンセル = 無彩色塗り)、情報の優先順位 (見出しは小さく muted、結果値は等幅) を維持。

### 結果経路 (6セル)

| セル | 完了 (`結果: completed(true)`) | キャンセル (`結果: cancelled`) |
|---|---|---|
| Native iOS | 確認済み | 確認済み |
| Native Android | 確認済み | 未確認 (7.2) |
| MAUI iOS | 確認済み | 確認済み |
| MAUI Android | 確認済み | 確認済み |
| KMP iOS | 確認済み | 未確認 (7.2) |
| KMP Android | 確認済み | 未確認 (7.2) |

## 未照合の項目 (7.2 送り)

- 外側タップによるキャンセル (全セル)
- 戻るボタンによるキャンセル (Android 3セル)
- 多段表示の見え方 (全セル)
- 一部セルのキャンセル経路 (上表)
- 初期状態の結果表示エリアを非表示にした判断のオーナー確認

## モックとの差 (プラットフォーム制約・要オーナー判断)

- **ボタンの高さ**: モックの余白 (上下 10 + 14px 相当の文字) をそのまま実装したため、タップ領域の高さは約 38pt/dp。
  HIG の 44pt・Material の 48dp の推奨を下回る。モック忠実を優先して**そのまま**にしてあり、
  推奨に合わせるならモック側の改訂が要る
- **メニュー行の押下フィードバック**: モックに表現が無いため、6セルとも押下時の色変化を付けていない
  (iOS は `.plain` ボタン、Android は ripple なし)
