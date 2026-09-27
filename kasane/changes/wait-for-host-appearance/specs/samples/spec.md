# samples デルタ (wait-for-host-appearance)

パリティ規約 (handbook/cross/sample-parity) に従う。デモ項目・安定デモ ID・文言は追加も変更もしない。Sample 専用の Scenario は scenario-id-coverage の除外 ID に登録する (機械検証はせず、手で通して証跡を残す)。

対象: iOS Sample (`samples/ios/KsDialogsSample/SampleMenuScreen.swift:93-99`) と KMP の iOS Sample (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:94`)。どちらも前回の change (fix-ios-sample-demo-autoplay) で、ライブラリの不具合を避けるために「シーンがアクティブになってから自動再生する」待ちを入れた。この change でライブラリが提示先の出現を待つようになるので、この待ちを外す (design Decision 10)。

## ADDED Requirements

### Requirement: iOS 系 Sample の自動再生は、最初の画面の表示時に始める

iOS Sample と KMP の iOS Sample は、起動引数で指定されたデモの自動再生を、最初の画面の表示時の処理から始める SHALL。シーンがアクティブになるのを待つ処理や、ライブラリの提示先の探し方をなぞる処理は持たない。提示先が現れるまで待つのはライブラリの役目とする。

自動再生の入口 (メニュー項目のタップと同じ入口)・1 回限りの取り出し・引数なしの起動の既定動作は、既存の「指定デモの自動再生」の要件のまま変えない。

#### Scenario: [CA-SA-08] 起動直後の自動再生で、指定デモの起動直後の状態が表示される
- **GIVEN** iOS Sample と KMP の iOS Sample のそれぞれを、安定デモ ID 14 件のうち Dialog・Loading・Toast の代表 (`basic-dialog`・`default-loading`・`custom-loading`・`default-toast`) を指定して起動する (起動と撮影の手順は `kasane/config.yaml` の `ui.screenshot`)
- **WHEN** 起動直後から連続で撮影する
- **THEN** どのデモでも、指定デモの起動直後の状態 (sample-parity の安定デモ ID 表) が画面に出て、プロセスは落ちない (手で通して証跡を残す Scenario)

#### Scenario: [CA-SA-09] 自動再生の処理が、シーンの状態を待たない
- **GIVEN** iOS Sample と KMP の iOS Sample の最初の画面のソース
- **WHEN** 自動再生を始める処理を読む
- **THEN** シーンの状態 (`scenePhase` など) を条件にする処理と、提示先の key window を探す処理が無い (コードレビューで受け入れる Scenario)
