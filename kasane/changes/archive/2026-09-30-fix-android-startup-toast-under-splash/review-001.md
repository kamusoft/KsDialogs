# レビュー結果: fix-android-startup-toast-under-splash (001 回目)

**日付**: 2026-09-30
**判定**: NEEDS_DISCUSSION

## サマリー

Android の「resumed で、かつ描画された Activity」の追跡 (描画の印・世代・次の周回での背面の確定) と、両 OS の「前面の待ちの Toast は期限を決めずに待ち、載った時点か背面へ下がった時点から数える」は、デルタスペックどおりに実装されている。全 7 ルートのテストは成功し、足したテストは修正前の実装で落ちる作りになっている。ただし、背面へ下がって戻るたびに、Android では既に載っている Loading・Toast の器が外され、戻った画面の最初の描画の後に載せ直されるようになった。これは spec と design が扱っていない副作用で、見た目を変える。直し方に設計の判断が要るので NEEDS_DISCUSSION とする。ほかに Minor が 2 件と Suggestion が 1 件ある。

## 照合した規約

- comment-policy.md (always): 変更ファイルに lint をかけ、禁止 0 件。advisory は既存と同じ型 (内部型の doc コメントの ADR 参照) だけ
- test-execution.md (テストの実行・結果の報告): 全件実行表の 7 ルートを行ごとに実行した (下表)。scenario-id-coverage も実行した
- ci-flaky-test-policy.md (実時間の期限を持つ表示 (Toast) を観察するテスト): TS-HW-04〜06・PB-HA-11 の待ち方と許容幅を照合した
- sample-parity.md (`samples/` を触るとき): 自動再生の入口・1 回限りの取り出し・作り直しで再発火しない守りが変わっていないことを確かめた。自動再生の節の Android 系の例外の記述は、proposal どおり蒸留で直す
- runtime-behavior-verification.md (実行時挙動の不具合の修正と完了判定): 実測 (tasks グループ 7) は依頼によりこのレビューの範囲外。下の Major の確かめ方の提案だけに使った
- ci-script-deletion.md (`scripts/**` のレビュー): `scripts/scenario-id-coverage.py` の変更は除外 ID の追加だけで、削除の操作は無い

### テスト実行 (このレビューで実行。端末はこの change 専用の AVD と Simulator)

| ビルドルート | 結果 |
|---|---|
| ios/ | Swift Testing 372 tests / 61 suites 成功、XCTest 0 件。PB-HI-05〜09・TS-HW-01〜06・TS-CO-08 が結果に出ている |
| android/ (JVM) | 101 tests / 0 failures / 0 skipped。PB-HA-01・02・05〜11 が結果に出ている |
| android/ (instrumented、API 36) | ksdialogs-core 381 tests / 0 failures / 1 skipped (PB-SB-04。API レベルによる既存の skip)、ksdialogs 52 tests / 0 failures。PB-HA-04・PB-HA-11・TS-HW-01〜06・TS-AN-03 が結果に出ている |
| kmp/ | 169 tests / 0 failures / 0 skipped |
| maui/ (`dotnet test`) | 206 件合格 / 失敗 0 / スキップ 0 |
| maui/android/native/ | 41 tests / 0 failures / 0 skipped |
| maui/macios/native/ | Swift Testing 17 tests / 7 suites 成功 |

lint: scenario-id-coverage は未網羅なし (PB-HA 11/11・PB-HI 9/9・TS-HW 6/6・CA-SA-10・11 は除外に登録済み)。ci-skip-lint は違反なし。local-path-lint・identity-lint は違反なし。

### 重点観点について確かめたこと

- **描画の合図の取りこぼし**
  - pause だけからの復帰: 印を pause で下ろさないので、resume の時点で提示先に戻り通知が届く (`ResumedActivityTracker.kt:114-118`、PB-HA-05 の unit テスト)
  - 背面からの復帰: start で観測を張り直し、resume で描画を促す。stop 前に受けた描画は世代で捨てる (PB-HA-06・PB-HA-10)
  - 作り直し: ウィンドウが引き継がれて decorView がウィンドウに付いたままでも、start で張った観測が setContentView の後の描画を受ける (PB-HA-11 の instrumented テスト)
  - decorView が無い画面: 次の周回で 1 回だけ取り直す。その時点でも無い経路は下の Minor 1
- **世代による古い知らせの捨て方**: stop・破棄と start で世代を進め、`onDrawn` は世代が一致するときだけ印を立てる (`ResumedActivityTracker.kt:150-178`)。PB-HA-10 の unit テストは、世代の照合を外すと落ちる
- **前面の判定と背面の確定の周回**: 集合が空になったら次の周回で確かめ直す (`ResumedActivityTracker.kt:202-210`)。PB-HA-09 は即時に確定させる実装で落ちる。PB-HA-11 (instrumented) は、作り直しの間に背面と確定させる実装で Toast が破棄されて落ちる
- **未確定の期限が決まらないまま残る経路**: 両 OS とも、期限が決まるのは載った時点か背面の確定の時点だけ。前面にいるのに提示先が現れない間 (Android の描画しない画面、iOS の foregroundInactive が続く間) は残るが、design の Risks と core/ADR-0043 の負の帰結で受け入れ済み。iOS の購読は `stopWaitingIfSatisfied` で「期限未確定の表示が無くなったら」解除され、PB-HI-08 で担保されている
- **姉妹面の入力経路と状態分岐**: 開始処理での判定 (`hostView/hostContext == null && isAppInForeground`)・期限切れでの捨て方・取り付け成功での確定・背面の合図での一括確定は、Android と iOS で同じ形。購読の張り方の違い (Android は通知 1 本、iOS は出現と前面を離れた合図の 2 本) は spec の実現経路どおり
- **テストの検出力**: PB-HA-04 は描画を待たない実装で、TS-HW-04 は受理時点から数える実装で、TS-HW-05 は背面の合図で期限を決めない実装で、TS-HW-06 は載った時点から数え直す実装で、それぞれ落ちる。iOS の PB-HI-07・09 は、通知の時点で前面を読み直さない実装で落ちる

## 指摘事項

### 🟠 Major 背面へ下がって戻るたびに、Android では載っている Loading・Toast の器が外れ、戻った画面の最初の描画の後に載せ直される

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityTracker.kt:100-124,202-210`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:434-448`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:317-333`

**問題点**:
変更前は、pause と stop では通知しなかったので、Loading・Toast の器は背面の間も画面 A に付いたままだった。戻ったときの resume の通知でも、提示先が A のままなので何も起きなかった。変更後の流れは次のとおり。

1. A の stop で描画の印が下りる (spec PB-HA-06 のとおり)
2. 次の周回で背面が確定し、通知が出る (PB-HA-09)。このとき提示先は null で、載っている Activity (A) と違う。そのため `LoadingCoordinator.onHostChanged` と `ToastCoordinator.onHostChanged` が器を外す
3. 戻ったとき、A の resume の時点ではまだ描画の印が無いので、提示先は null のまま
4. A が描画され、次の周回で印が立ってから、器が演出なしで載り直す

仮に背面の通知が無くても、3 の resume の通知 (提示先は null、載っている Activity は A) で同じく外れる。つまり「stop で印を下ろす」という定義から必ず起きる。

利用者から見える変化:
- 戻った直後の最初の 1〜数フレームは、Loading・Toast の無い画面が描かれ、そのあと演出なしで Loading・Toast が現れる (ちらつき)
- Loading は操作を遮る覆いなので、その数フレームの間は下の画面が見え、触れる可能性もある (小さい)
- iOS は背面の間も器を付けたままなので、姉妹面の振る舞いがこの点で分かれた。変更前の Android は iOS と同じだった

design Decision 1 は「Loading と Toast の載せ直しには手を入れない。印を立てたときの通知で、これまでどおり動く」としている。しかし背面をまたぐ経路は「これまでどおり」にならない。spec・design・ADR-0044 の帰結のどれも、この経路を扱っていない。実測の予定 (tasks 7.x) にもこの場面は無い。

**推奨修正** (どちらにするかはオーナー判断):
- **A: 受け入れて記録する** — ADR-0044 の負の帰結と design の Risks に「背面から戻ると、載っている Loading・Toast は最初の描画の後に演出なしで載り直す」を足す。tasks 7 の実測に、この場面 (Loading の表示中にホームへ下がり、履歴から戻る場面の録画) を足す
- **B: 載っている器は外さない** — Loading・Toast の `onHostChanged` で、新しい提示先が null で、載っている Activity がまだ破棄されていないときは、器を外さずに残す。外すのは、破棄されたときか、別の描画済みの提示先が現れたときに限る。こうすると、作り直し (TS-AN-03、旧 Activity は破棄される) の経路は変わらない。unit テストを足す: 「描画済みの A に載った Loading は、A の stop・背面の確定・start・resume・描画をまたいでも外れない (外す回数が 0)」。この仕組みを外すと、外す回数が 1 になって落ちる
- A か B か決まるまで、どちらの場合でも、tasks 7 の実測でこの場面を撮ることを勧める。録画で「戻った後の最初の描画のコマに Loading が写っているか」を見る。B なら写り、B の仕組みが無ければ写らない (つまり、この観測で A と B が見分けられる)

### 🟡 Minor decorView が次の周回でも無かった画面では、描画の観測が始まらないまま残り、resume で張り直されない

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ActivityDrawObserver.kt:67-79`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityTracker.kt:100-109`

**問題点**:
start の時点で decorView が無いと、次の周回で 1 回だけ取り直す。その時点でも無ければ、観測は何も張らないまま残る。この観測の登録は null ではないので、あとで resume が来ても `tracking?.registration == null` が偽になり、観測を張り直さない。`requestDraw` は decorView が無いので何もしない。

この経路の例: setContentView を呼ばず、onCreate の中で半透明の画面を開く Activity。この Activity は、最初の resume ではウィンドウが作られない。半透明の画面が閉じると stop を経ずに resume し、そこで初めて decorView が作られて描画される。しかし観測が張られていないので、描画済みの印は立たない。その結果、Dialog は返らず、Toast は前面の待ちのまま残る。

まれな構成だが、design Risks の「描画の合図の取りこぼし」に当たる。

**推奨修正**:
- 未描画のまま resume したときは、観測がまだ始まっていなければ始め直す。例: `DecorViewDrawObserver.requestDraw` で、decorView を取れれば、待っている観測にそれを渡して開始する。または、追跡役が resume で未描画なら観測を張り直す
- 追跡役の側で張り直す形なら、`ManualActivityDrawObserver` で「resume の時点で未描画なら観測が 1 本張られている (張り直しの回数)」を確かめる unit テストを足す。今の実装ではこのテストは落ちる

### 🟡 Minor Dialog・Loading の説明が「resumed な Activity」のまま残っている (公開 doc を含む)

**該当箇所**:
- `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialog.kt:23` (公開 doc)
- `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ActivityDialogPresentationSurface.kt:4`
- `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingPresentationSurface.kt:36`
- `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogHostWaitQueue.kt:183`

**問題点**:
提示先の定義が「resumed で、かつ描画された Activity」に変わった。しかしこれらのコメントは、今も提示先を「resumed な Activity」と説明している。特に `KsDialog.show` の公開 doc の「提示先 (resumed な Activity) が無ければ…待ってから表示する」は、今は誤りである。resumed でも、まだ描画されていない画面では待つからだ。

Toast の提示面 (`ToastPresentationSurface.kt:45`) と `ResumedActivityProvider` は直してあるので、同じ変更の中で説明が食い違っている。comment-policy の「コメントは現在の仕様を書く」に反する。

deviation.md の付随修正は、Toast の公開 doc だけを挙げている。

**推奨修正**: 4 か所を「resumed で、かつ描画された Activity」(公開 doc なら「表示されている画面」などの利用者向けの言い方) に直す。公開 doc は形を変えない修正なので、Toast の公開 doc と同じく付随修正として deviation.md に足す。

### 🔵 Suggestion iOS の TS-HW-06 の上限の余裕が 600 ms しかない

**該当箇所**: `ios/Tests/KsDialogsTests/ToastContractTests.swift` の `TS_HW_06_departedDisplayShowsUntilDeadlineFromDeparture` と定数 `backgroundStay`

**問題点**:
上限は「下がってから duration + backgroundStay (1000 + 600 ms) より前に消える」。正しい実装の消える時刻は、下がってから約 1000 ms である。したがって、スケジューリングの遅れに使える余裕は 600 ms しかない。

Android 版は 1500 + 1000 ms で、余裕が 1000 ms ある。MainActor が混み合う CI では、iOS 側だけが先に間欠的に落ちる余地がある。

「載った時点から数え直す」誤った実装を見分けるには、上限が backgroundStay + duration 以下であれば足りる。

**推奨修正**: duration と backgroundStay を Android 版と同じ比 (例: 1500 ms と 1000 ms) に広げる。こうしても、誤った実装は上限 (2500 ms) 以上になるので、見分けられることは変わらない。

## アクションプラン

1. (オーナー判断) Major の A / B を決める。B なら実装と unit テストを足す。A でも B でも、tasks 7 の実測に「Loading の表示中に背面へ下がって戻る」場面を足す
2. Minor 1: 未描画のまま resume したときに、描画の観測を張り直す (取り直す) 処理と、その unit テストを足す
3. Minor 2: Dialog・Loading のコメントと `KsDialog.show` の公開 doc を新しい提示先の定義に揃え、deviation.md の付随修正に足す
4. Suggestion: iOS の TS-HW-06 の時間の余裕を Android 版に揃える
