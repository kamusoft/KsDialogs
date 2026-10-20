# テスト実行の記録 (2026-10-20、修正サイクル 2 の後)

完了判定に使った、絞り込みなしの全件実行の件数と結果。

| ルート | コマンド | 件数と結果 | 起点との差 |
|---|---|---|---|
| ios/ (Simulator: iPhone 17 Pro / iOS 26.0、`<udid>`) | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<udid>' -collect-test-diagnostics never` | Swift Testing: `Test run with 395 tests in 66 suites passed` / XCTest: `Executed 0 tests, with 0 failures` / `** TEST SUCCEEDED **` | 372 → 395 (+23) |
| kmp/ | `./gradlew allTests --rerun-tasks` | `ksdialogs-kmp/build/test-results/` の XML (`iosSimulatorArm64Test` と `testAndroidHostTest`) の合計 tests 169 / failures 0 / errors 0 / skipped 0 | 169 → 169 (変化なし) |
| maui/macios/native/ (同じ Simulator) | `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,id=<udid>' -collect-test-diagnostics never` | Swift Testing: `Test run with 17 tests in 7 suites passed` / XCTest: `Executed 0 tests` / `** TEST SUCCEEDED **` | — (`ios/` をローカルパッケージとして取り込むため、器のレイアウト部品の変更の影響を見た) |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` | `結果: 未網羅なし`。`PB-IS 19/19` | — |

回していないルート: android/・android/ (instrumented)・maui/・maui/android/native/。この change は ios/ だけを触り、Android 側には届かない。maui/ の中身は UIView で、変えた SwiftUI のホストを通らない。

## 足したテスト (ios/ の全件実行の結果に現れていることを確認した)

| スイート | テスト |
|---|---|
| `DialogSwiftUISafeAreaTests` | PB-IS-01・02・03・04・07・08・09・10 (引数 2)・11 (引数 2)・12 (引数 4)・13・14・15・16・17 (引数 2)・18 |
| `LoadingSwiftUISafeAreaTests` | PB-IS-05 |
| `ToastSwiftUISafeAreaTests` | PB-IS-06・19 |
| `DialogSwiftUIContentSizeFollowTests` | 「表示中に理想の幅を広げても狭めても、器の幅が追従する」(既定の可視領域 / 窓全体・上端)・「表示中に文言を長くすると、器は最初から長い文言で出したときと同じ大きさになる」(同じ 2 条件) |
| `DialogSwiftUIContentWidthSettleTests` | 「縦長の窓で窓全体の端に付けた長い本文の器は、可視領域の上端の器と同じ大きさになる」(上端 / 下端)・「可視領域で水平に広げた長い本文の器は、窓を横長に変えると最初から横長で出した器と同じ大きさになる」 |

## 追従の回帰テストが機構の有無で分かれることの確認

`DialogSwiftUIContentSizeFollowTests` を、同じテストのまま 3 つのソースで絞り込み実行した (`-only-testing:KsDialogsTests/DialogSwiftUIContentSizeFollowTests`)。変更前のソースは、作業ツリーのソースを一時的に 62b4981 の内容へ戻して回し、回した後に作業ツリーへ戻した。

| ソース | 結果 |
|---|---|
| 変更前 (62b4981) | `Test run with 2 tests in 1 suite passed` |
| 修正前の作業ツリー (幅を今の幅で提案する形) | `Test run with 2 tests in 1 suite failed`。理想の幅を 280 / 120 にした後も器の幅は 200 のまま。文言を長くすると 23.67 × 306.67 (期待 234.67 × 20.33) |
| 修正後の作業ツリー | 上の全件実行で成功 |

## 修正サイクル 2 の回帰テストの確認

`DialogSwiftUIContentWidthSettleTests` を、同じテストのまま 3 つのソースで絞り込み実行した。変更前のソースは、作業ツリーのソースを一時的に 62b4981 の内容へ戻して回し、回した後に作業ツリーへ戻した。

| ソース | 結果 |
|---|---|
| 変更前 (62b4981) | 上の 2 本 (引数つきで 3 ケース) は成功。依頼にあった 3 本目「可視領域で理想の幅 600 の中身を縦長で出し、横長へ変えると器の幅が 600」は **変更前でも失敗** (幅 393 のまま) |
| 修正サイクル 1 の作業ツリー | 2 本とも失敗。窓全体の上端・下端の長い本文が 393 × 20.33 (期待 389 × 130.33)。水平に広げた長い本文を横長に変えると 734 × 130.33 (期待 734 × 86.33) |
| 修正後の作業ツリー | 上の全件実行で成功 |

3 本目は、変更前でも通らないためスイートに入れていない (「変更前で通る」という条件と食い違う)。修正後も変更前と同じく幅 393 のまま。
