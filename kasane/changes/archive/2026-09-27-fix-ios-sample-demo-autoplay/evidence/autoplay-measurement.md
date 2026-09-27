# 自動再生の実測 (fix-ios-sample-demo-autoplay)

- 環境: iPhone Air / iOS 26.5 の Simulator (この実測のために新しく作って boot したデバイス)、Xcode 26.5。3 Sample とも Debug ビルド
- 起動: `xcrun simctl terminate` → 1.5 秒待ち → `xcrun simctl launch <bundle-id> --demo <デモID> --loading-step-interval-ms 5000`
- 判定: 起動直後から 0.3〜1 秒間隔で連続撮影し、指定デモの「起動直後の状態」(sample-parity「安定デモ ID」表) が画面に出たか、プロセスが落ちていないかを見る
- 表の「成功 / 試行」は、その状態が出て落ちなかった回数 / 起動回数

## 修正前

| Sample | basic-dialog | default-loading | custom-loading | default-toast |
|---|---|---|---|---|
| ios | 0 / 3 (3 回とも起動直後に落ちる) | 1 / 3 (2 回は結果表示が「処理中」になるが Loading が出ない) | 0 / 3 (Loading が出ない) | 0 / 5 (Toast が出ない。0.3 秒刻みの撮影でも一度も写らない) |
| kmp (iOS) | 2 / 2 | 2 / 2 | 2 / 2 | 2 / 2 |
| maui (iOS) | 2 / 2 | 2 / 2 | 2 / 2 | 2 / 2 |

- 崩れていたのは ios だけ。kmp (iOS) は自動再生の前に提示先ができるまで短い間隔で確かめる待ちを持っており、maui (iOS) は画面表示 (`OnAppearing`) から Dispatcher 経由で再生している
- maui (iOS) は崩れていないため変更していない。kmp (iOS) は崩れていないが、その待ちが探索で却下した方式 (Sample がライブラリの提示先の探し方を複製する) に当たるため、ios と同じ待ち方へ揃えた (deviation.md)

## 修正後 (ios)

| デモ ID | 成功 / 試行 |
|---|---|
| basic-dialog | 3 / 3 |
| declarative-dialog | 2 / 2 |
| model-dialog | 2 / 2 |
| text-input-dialog | 2 / 2 |
| inline-dialog | 2 / 2 |
| transition-dialog | 2 / 2 |
| layout-dialog | 2 / 2 |
| default-loading | 3 / 3 |
| custom-loading | 3 / 3 |
| default-toast | 3 / 3 |
| custom-toast | 2 / 2 |
| toast-stack | 2 / 2 |
| toast-placement | 2 / 2 |
| toast-overlap | 2 / 2 |

追加の確認:

- シーンの状態の到着順: 起動直後の画面は `inactive` で始まり (この時点では前面アクティブのシーンの key window が無い)、続いて `active` が届く。`active` を受け取った時点で key window があることを 3 回の起動で確かめた (一時的な診断出力で観測し、観測後に取り除いた)
- 1 回限り: `default-toast` の自動再生のあと、設定アプリを前面にしてから Sample を前面に戻しても、Toast は再び出なかった
- 引数なしの起動: 自動再生せず通常のメニューを表示した

## 修正後 (kmp (iOS))

ios と同じ「シーンが前面でアクティブになってから再生」へ置き換えたあとの実測。

| デモ ID | 成功 / 試行 |
|---|---|
| basic-dialog | 3 / 3 |
| declarative-dialog | 2 / 2 |
| model-dialog | 2 / 2 |
| text-input-dialog | 2 / 2 |
| inline-dialog | 2 / 2 |
| transition-dialog | 2 / 2 |
| layout-dialog | 2 / 2 |
| default-loading | 3 / 3 |
| custom-loading | 3 / 3 |
| default-toast | 3 / 3 |
| custom-toast | 2 / 2 |
| toast-stack | 2 / 2 |
| toast-placement | 2 / 2 |
| toast-overlap | 2 / 2 |

追加の確認:

- 1 回限り: `default-toast` の自動再生のあと、設定アプリを前面にしてから Sample を前面に戻しても、Toast は再び出なかった
- 引数なしの起動: 自動再生せず通常のメニューを表示した

## 画像

| ファイル | 内容 |
|---|---|
| `ios-default-loading-before.png` | 修正前の ios、`default-loading`。結果表示は「処理中」だが Loading が出ていない |
| `ios-default-loading-after.png` | 修正後の ios、`default-loading`。Loading が出ている |
| `ios-basic-dialog-after.png` | 修正後の ios、`basic-dialog`。ダイアログが出ている (修正前は起動直後に落ちるため画像なし) |
| `kmp-basic-dialog-after.png` | 修正後の kmp (iOS)、`basic-dialog`。ダイアログが出ている |
| `kmp-default-loading-after.png` | 修正後の kmp (iOS)、`default-loading`。Loading が出ている |
