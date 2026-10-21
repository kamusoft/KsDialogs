# 実装の確認: テスト件数と下端の帯の撮影 (2026-10-21)

## テストの全件実行 (タスク 3.1)

端末は作業専用に作った iPhone 17 Pro / iOS 26.0 の Simulator (作業後に削除)。表はレビュー指摘の修正後 (修正サイクル 2) に回し直した値で、修正サイクル 2 の端末は `ksn-impl-edge-fix2` (UDID `<uuid>`、作業後に `simctl delete` で削除)。修正サイクル 1 の端末は `ksn-impl-edge-fix1` (UDID `<uuid>`、削除済み)。

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'id=<uuid>' -collect-test-diagnostics never` | `Test run with 421 tests in 69 suites passed` / XCTest は `Executed 0 tests` (すべて Swift Testing 製) |
| android/ | 対象外 | `ios/` を取り込まないため回していない |
| android/ (instrumented) | 対象外 | 同上 |
| kmp/ | `./gradlew allTests --rerun-tasks` | `iosSimulatorArm64Test` 86 tests / 0 failures、`testAndroidHostTest` 83 tests / 0 failures (結果 XML の合算) |
| maui/ | `dotnet test` | `失敗: 0、合格: 206、スキップ: 0、合計: 206` |
| maui/android/native/ | 対象外 | `ios/` を取り込まないため回していない |
| maui/macios/native/ | `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'id=<uuid>' -collect-test-diagnostics never` | `Test run with 17 tests in 7 suites passed` / XCTest は `Executed 0 tests` |
| 網羅検査 | `python3 scripts/scenario-id-coverage.py` | `結果: 未網羅なし`。`PB-IS 35/35` |

足したスイート: `DialogSwiftUIInnerEdgeAlignmentTests` (PB-IS-20〜25・27・32・33・35 と、Scenario ID を持たない回帰テスト `settlesAfterOnlyShrinkableRangeChanges`・`settlesBackAfterShrinkableRangeReturns`)、`ContentWidthLimitFollowTests` (PB-IS-28〜31・34)。既存スイートへの追加: `LoadingSwiftUISafeAreaTests`・`ToastSwiftUISafeAreaTests` に PB-IS-26。直した既存テスト: `DialogSwiftUISafeAreaTests` の PB-IS-10・12・15。

### テストの検出力

本体の 2 つの仕組み (はみ出しの判定と上限の通知) を同時に一時無効にして、該当の 5 スイートを回すと、次が失敗した (戻して全件成功を確認済み)。2 つを別々に止めた実行はしていないので、下の振り分けは Scenario の内容からの見立てである。

- はみ出しの判定に当たるもの: PB-IS-12・20〜26・32・33・35
- 上限の通知に当たるもの: PB-IS-15・28・29・31
- 失敗しなかったもの: PB-IS-10・27 (並び方を変えない側)、PB-IS-30 (UIView の中身。本体の変更に関係なく広がる — タスク 2.2 の確認)、PB-IS-34 (求め直しすぎないことの回帰の見張り)

はみ出しの判定で包みの変化をホストへ同じパスで反映させない形 (包みを外してすぐ測る) では、PB-IS-25 が失敗した (固定 200 × 160 の中身が y 675 = 安全領域の内側の中央に残った)。今の形では、PB-IS-25・33 を 5 ms ごとのレイアウトパスで 1 秒間測り、どのパスでも中身の内側の辺が器の内側の辺にそろったままだった (位置が跳ねない)。

### 縮められる範囲だけが変わったときの判定し直し (修正サイクル 1)

回帰テスト `settlesAfterOnlyShrinkableRangeChanges` (縦長の窓・窓全体基準、理想 200 × 100 の縮められる中身を表示し、理想の大きさ・器・安全領域を変えずに最小の高さ 100 を足す) は、描かれた位置 (UIView の目印) が下端で y 752・高さ 100、上端で y 0・高さ 100 に落ち着き、その後 1 秒間 (5 ms ごとのレイアウトパス 200 回) どのパスでも動かないことを確かめる。

中身が置き直された知らせを受け取らない形 (`hostedContentDidResize` を何もしない形) に一時的に戻してこのテストだけを回すと、下端で `実測 (96.67, 735.0, 200.0, 100.0)`、上端で `実測 (96.67, 29.67, 200.0, 100.0)` となって失敗した (レビューで報告された位置と同じ)。戻して全件成功を確認済み。

### 縮められる範囲だけが変わって収まる状態へ戻ったときの判定し直し (修正サイクル 2)

中身が置き直された知らせを受けたら、そろえが効いている軸も含めて包みを外して測り直す形にした (deviation.md 3 行目)。包み自身の変化による知らせを受け取らないことと、同じレイアウトパスの中で反映することは変えていない。

回帰テスト `settlesBackAfterShrinkableRangeReturns` (縦長の窓・窓全体基準、理想の高さ 1000 の縮められる中身を表示し、最小の高さ 1000 を足してから外す。器は 96.5, 0, 200 × 852 で変わらない) は、下端 / 上端それぞれで次を確かめる。

- 最初の縮められる中身: 描かれた位置 (UIView の目印) が y 59・高さ 759
- 最小の高さを足した後: 下端で y 0・高さ 1000、上端で y -148・高さ 1000 (内側の辺にそろう)
- 最小の高さを外した後: 描かれた位置が y 59・高さ 759 に戻り、その後 1 秒間 (5 ms ごとのレイアウトパス 200 回) 器も描かれた位置も動かない。中身の一番外側と内側の子から読む位置も y 59・高さ 759、一番外側が受け取る安全領域は上 59・下 34 (最初から縮められる中身で表示した場合と同じ)

そろえが効いている軸を測り直さない前の形 (知らせを受けるのは包みが効いていない軸があるときだけ) に一時的に戻してこのテストだけを回すと、足す側までは通り、外した後で失敗した。描かれた位置は下端で `実測 (96.67, 0.0, 200.0, 818.0)`・受け取る安全領域の最後が `t0 b34`、上端で `実測 (96.67, 59.0, 200.0, 793.0)`・`t59 b0` (レビューで報告された位置と同じ)。戻して全件成功を確認済み。`settlesAfterOnlyShrinkableRangeChanges`・[PB-IS-25]・[PB-IS-33] は今の形でも通る。

[PB-IS-22] は、表示した後に右から左へ切り替える形に加えて、器を載せる前から窓を右から左にした形も回す (左右の端それぞれ)。

## 下端の帯の撮影 (タスク 3.2)

![窓全体基準・下端・水平全面の帯](bottom-confirm-window-end-after.png)

- 撮影元: Sample にこの形 (SwiftUI の中身・窓全体基準・下端) のデモが無いため、`samples/ios` を scratchpad へ複製したスクラッチのアプリで撮った (`samples/` は改変していない)。中身は本文 1 行とボタン 2 つを並べた縮まない `VStack` に、余白 (上 20・下 32) と地色を付けたもの。添付は `layoutArea: .window`・`DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .end)`
- 端末: 作業専用の iPhone 17 Pro / iOS 26.0 の Simulator (窓 402 × 874、画像は 1206 × 2622 の @3x)
- 画素で測った位置: 帯の上端は 2225 px (741.7 pt)、帯の地色は画像の最下行まで続く (帯の高さ 132.3 pt)。窓の下端にぴったり付いている
- 照合: `ui/references/original-maui-bottom-confirm.png` (原本) と同じく、帯の下端が窓の下端に付き、ホームインジケータの領域に帯が重なる。0.1.0-beta.5 (`ui/references/beta5-bottom-confirm.png`) のように重なりの半分だけ上がってはいない。`simctl io screenshot` の画像にはホームインジケータ自体は写らない
