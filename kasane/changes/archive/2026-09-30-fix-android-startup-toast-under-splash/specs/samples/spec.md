# samples デルタ (fix-android-startup-toast-under-splash)

パリティ規約 (handbook/cross/sample-parity) に従う。デモ項目・安定デモ ID・文言は追加も変更もしない。Sample 専用の Scenario は scenario-id-coverage の除外 ID に登録する (機械検証はせず、手で通して証跡を残す)。実現経路は design Decision 6。

対象:
- `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:102` (`menuView.post`)
- `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:105` (`menuView.post`)
- `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:95-103` (Android のときだけの `Dispatcher.DispatchAsync`)

どれも前回の change (align-sample-autoplay-start) で、起動直後の Toast が起動画面の下で表示時間を使い切るのを避けるために残した待ちである。この change で、ライブラリが描画された画面にだけ載せ、前面の待ちの間に受け付けた Toast は載った時点から数えるようになるので、この待ちを外す。

## ADDED Requirements

### Requirement: Android 系 Sample の自動再生も、最初の画面の表示時に始める

Android Sample・KMP の Android Sample・MAUI Sample の Android は、起動引数で指定されたデモの自動再生を、最初の画面の表示時の処理 (Activity の作成時の処理、MAUI はページの表示時の処理) から始める SHALL。再生を最初の描画の後や UI スレッドの次の周回へ回す処理は持たない。提示先が現れるまで待つのはライブラリの役目とする。

自動再生の入口 (メニュー項目のタップと同じ入口)・1 回限りの取り出し・画面の作り直しで再発火しない守り・引数なしの起動の既定動作は、既存の要件のまま変えない。

#### Scenario: [CA-SA-10] 起動直後の自動再生で、指定デモの起動直後の状態が表示される
- **GIVEN** 3 ルートのそれぞれを、`basic-dialog`・`default-loading`・`default-toast`・`custom-toast` を指定して起動する (起動と撮影の手順は `kasane/config.yaml` の `ui.screenshot`)
- **WHEN** 起動直後から連続で撮影する
- **THEN** どのデモでも、指定デモの起動直後の状態 (sample-parity の安定デモ ID 表) が画面に出る。Toast の 2 デモは、コールド起動とプロセスが残ったままの再起動のどちらでも、5 回とも起動画面が退いた後の画面に写り、見えている時間 (画面の録画で測る) が表示時間の 2/3 以上になる。プロセスは落ちない (手で通して証跡を残す Scenario)

#### Scenario: [CA-SA-11] 自動再生の処理が、最初の描画の後や次の周回へ回さない
- **GIVEN** 3 ルートの最初の画面のソース
- **WHEN** 自動再生を始める処理を読む
- **THEN** 再生を `post`・`Dispatcher.DispatchAsync` などで後ろへ回す処理と、提示先の条件をなぞる処理が無い (コードレビューで受け入れる Scenario)
