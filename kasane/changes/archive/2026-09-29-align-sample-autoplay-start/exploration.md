# Exploration: align-sample-autoplay-start

## 課題 / 動機

wait-for-host-appearance (`kasane/changes/archive/2026-09-29-wait-for-host-appearance/`) で、ライブラリは全形態で提示先の出現を待つようになった (core/ADR-0041)。あわせて iOS 系の 2 つの Sample (`samples/ios`・`samples/kmp/iosApp`) の自動再生から、「シーンがアクティブになってから再生する」Sample 側の待ちを外し、最初の画面の表示時の処理から呼ぶ形に戻した (同 change の specs/samples CA-SA-09)。

残りの 3 つの Sample は、今も Sample 側で「提示先が決まるまで再生を待つ」処理と説明を持つ:

- Android Sample `samples/android/app/.../MainActivity.kt` (`post` で再生を後ろへ回す)
- KMP の Android Sample `samples/kmp/androidApp/.../MainActivity.kt` (同上)
- MAUI Sample `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs` (`Dispatch` で再生を後ろへ回す)

説明は「ダイアログの提示先はメニューが画面に載ってから決まるため、再生もその時点まで待つ」。動きとしては今も正しいが、提示先の出現を待つのはライブラリの役目になったので、4 ルートで説明の立場と書き方が揃っていない (wait-for-host-appearance の review-004 Suggestion、verify の所見への対応の報告で発見)。

## 決定事項

- **3 つの Sample の自動再生から、提示先が決まるまで待つ Sample 側の処理と説明を外し、iOS 系と同じく最初の画面の表示時の処理から呼ぶ形に揃える。この場で行う** (2026-09-29、オーナー判断)
- デモ項目・1 回限りの取り出し・再生中のデモを画面の状態の変化で打ち切らない形は変えない
- 長命層 (handbook cross/sample-parity.md の自動再生の記述など) の追随は、実装の作業から外して蒸留時に反映する

## 級: S

Sample 3 ルートの自動再生の起動位置とコメントだけの変更で、ライブラリと公開 API は変えない。デルタスペックは持たない。

domain: cross
