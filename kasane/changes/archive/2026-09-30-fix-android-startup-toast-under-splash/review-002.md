# レビュー結果: fix-android-startup-toast-under-splash (002 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

前回の確定指摘 4 件は、すべて実装とテストで解消されている。deviation.md の 2026-09-30 の各項も、実装と 1 対 1 で照合できた。新しく足された 2 つの仕組みも確かめた。1 つは Loading・Toast の器を残す判定、もう 1 つは描画の観測の張り直し。器が二重に載る・破棄された画面を掴み続ける・Toast の期限の規則が崩れる・観測を張りすぎる、のどの不具合も見つからなかった。

残る指摘は、優先度の低い Minor 2 件で、どちらも 7 章の実測の前に片付けるのが望ましい。

- deviation.md で足すと決めた実測の場面が、tasks.md の 7 章に載っていない
- 器を残す判定のうち「破棄されていない画面から、描画済みの別の画面へ移る」経路を固定するテストが無い

## 照合した規約

- comment-policy.md (always): 変更ファイルに lint をかけ、禁止 0 件。advisory は、前回と同じ型の既存の指摘 (内部型の doc コメントにある ADR 参照) だけ。今回足された `AttachedHostRetention.kt` と、`retainsAttachment` まわりのコメントには出ていない。コメントは現在形で理由を書いており、外部文書の ID だけに頼った説明は無い
- test-execution.md (テストの実行・結果の報告): 依頼の範囲 (android unit 全件、`:ksdialogs-core` の instrumented、ios 全件) を実行した。全件実行表の 7 行との対応は下の表のとおり
- ci-flaky-test-policy.md (実時間の期限を持つ表示 (Toast) を観察するテスト・間欠失敗の切り分け): instrumented の間欠失敗 2 件を、節「CI でだけ落ちるテストの切り分け」の順で切り分けた (下記)。iOS の TS-HW-06 の余裕も照合した
- runtime-behavior-verification.md (実行時挙動の不具合の修正と完了判定): 実測 (tasks の 7 章) は予定どおり未実施。deviation で足した実測の場面が、機構の有無を見分けられるかだけを照合した (lessons code-review L-001)

### テスト実行 (このレビューで実行。端末はこの change 専用の AVD と Simulator)

| ビルドルート | 結果 |
|---|---|
| ios/ | Swift Testing 372 tests / 61 suites 成功、XCTest 0 件 (`Executed 0 tests`)。TS-HW-06 は結果に出ていて、成功 |
| android/ (JVM) | `./gradlew test --rerun-tasks`: 107 tests / 0 failures / 0 skipped。前回から 6 件増えた。内訳は `AttachedHostRetentionTests` 4 件と、張り直しの unit テスト 2 件 |
| android/ (instrumented、API 36) | `:ksdialogs-core` 全件を 3 回実行した。1 回目と 2 回目は 383 tests / 1 failure / 1 skipped、3 回目は 383 tests / 0 failures / 1 skipped。skip は PB-SB-04 で、API レベルによる既存の skip。PB-HA-04・PB-HA-11・TS-HW-01〜06・TS-AN-03・`DecorViewDrawObserverTests` 2 件が結果に出ている。失敗の切り分けは下記。`:ksdialogs` (宣言的 UI) は依頼の範囲外で、今回は実行していない |
| kmp/ | 依頼の範囲外で、今回は実行していない。修正サイクルの差分は Android 本体の内部型と iOS のテストだけで、KMP の公開面・偽の面には触れていない |
| maui/ (`dotnet test`) | 依頼の範囲外で、今回は実行していない (理由は kmp/ と同じ) |
| maui/android/native/ | 依頼の範囲外で、今回は実行していない。Android の提示面の口に足した `retainsAttachment` は既定の実装を持つ。ブリッジ側に提示面の実装が無いことは確認済み (`LoadingPresentationSurface` / `ToastPresentationSurface` を実装する型は `android/ksdialogs-core` の main と androidTest にしか無い) |
| maui/macios/native/ | 依頼の範囲外で、今回は実行していない (理由は kmp/ と同じ) |

lint の結果は次のとおり。

- scenario-id-coverage: 未網羅なし
- ci-skip-lint: 印 0 件
- local-path-lint・identity-lint: 違反なし

#### instrumented の間欠失敗の切り分け

1 回目と 2 回目で、それぞれ別のテストが 1 件ずつ落ちた。

1. `ToastMultiDisplayTests.TS_MX_01`: 手前の色が白と読まれた
2. `ToastContractTests.TS_HW_01`: 受理から消えるまでが 4348 ms。上限は 4000 ms

どちらも、この change の仕組みによるものではないと判断した。根拠は次の 4 つ。

- **負荷**: 実行中の Mac の load average は 17〜25 だった。この change の外の複数のエミュレータが動いていた。ci-flaky-test-policy の手順 1 に当たる汚染下の結果である
- **通る経路**: どちらのテストも偽の提示面 (`ToastTestPresentationSurface`) を使う。そのため、描画の追跡と器を残す判定 (`retainsAttachment` は既定の false) を通らない。TS-MX-01 は今回の差分に含まれない。TS-HW-01 の上限 (duration + 1000 ms) は変更前からある値で、今回の差分は前提を「背面で受理」に直しただけ
- **反復**: 2 件を含む 3 クラス (`ToastContractTests`・`ToastMultiDisplayTests`・`DrawnHostPresentationTests`) を 3 回続けて回し、3 回とも成功した。このときの load average は 12〜16
- **全件**: 3 回目の全件実行は 0 failures だった

### 重点観点について確かめたこと

- **器が残り続けて二重に載る**
  - 器を残すのは、新しい提示先が null のときだけ (`AttachedHostRetention.kt:28-35`)
  - Loading は、残したときは `attach` に進まずに戻る (`LoadingCoordinator.kt:441-446`)
  - Toast は、残した表示の器が付いたままなので、`attachIfPossible` の先頭 (`container != null`) で止まる
  - 同じ画面が再び提示先になったときは `attachedHost === newHost` で何もしない
  - 背面から戻る間に前面の待ちで受け付けた Toast は、残った Toast の後に起動順で載る
  - 以上から、1 つの表示に器が 2 つ付く経路は無い
- **破棄された画面を掴み続ける**
  - 判定は、追跡役が受けた破棄の記録と `Activity.isDestroyed` による (`ResumedActivityTracker.kt:71-72`)
  - 破棄の通知では、記録してから入れ替わりの購読者へ通知する (`:136`・`:144`)。そのため、提示先が null のままでも器は外れる (`AttachedHostRetentionTests` の破棄の 2 件)
  - 破棄の記録は WeakHashMap の集合で持つので、記録が画面の寿命を延ばさない
  - 描画の追跡 (`DrawTracking`) は decorView を強く持つ。ただし破棄の通知で `drawTrackings` から外すので、WeakHashMap の値がキーを掴み続けることは無い (`:139`)
- **Toast の期限の規則**
  - 前面を離れた通知での期限の確定は、器を残す判定より前に行う (`ToastCoordinator.kt:320-323`)
  - `fixDeadline` は一度しか期限を決めない。そのため、残した表示の期限は、載った時点で決めた値のまま動かない
  - 背面の間に期限が来た表示は、残した器の上で撤去される。これは変更前の Android (背面の間も器を付けたまま) と iOS と同じ
  - TS-HW-01〜06 は 3 回目の全件と反復実行で成功した
- **観測の張りすぎ**
  - resume で張り直すのは、未描画で、かつ観測が始まっていない (`isObserving != true`) ときだけ (`ResumedActivityTracker.kt:114`)
  - 始まっている観測は張り直さない (unit テスト「観測が始まっている未描画の画面は、resume で観測を張り直さない」)
  - 張り直す前に古い観測を `cancel` する (`:162`)。取り直し待ちで積まれた処理は `isCancelled` で何もしない (`ActivityDrawObserver.kt:117-118`)
  - 取り直しの処理が画面を強く持つのは 1 周回だけ
  - 描画を受けた後、知らせが届く前に stop した場合は、世代の照合で捨てる
- **描画の合図の取りこぼし (前回 Minor 1 の経路)**
  - 中身を置かずに開いた画面は、resume の通知の時点では decorView がまだ無い
  - resume で張り直した観測は、次の周回で decorView を取り直す。この時点で decorView はフレームワークが作っており、まだウィンドウに付いていなければ、付いた時点で listener を張る
  - あわせて描画を促すので、最初の描画を取りこぼさない

## 前回指摘の解消の確認

| 前回の指摘 (review-001 / 突き合わせ結果) | 対応 | 確認した箇所 | 判定 |
|---|---|---|---|
| 🟠 Major: 背面へ下がって戻るたびに Loading・Toast の器が外れて載り直す (オーナー判断で B を採用) | 提示先が無く、載っている画面が破棄されていなければ器を外さない判定を、Loading と Toast で共有した | `AttachedHostRetention.kt:28-35`、`LoadingCoordinator.kt:441-450`、`ToastCoordinator.kt:319-333`。テストは `AttachedHostRetentionTests` の 4 件 | 解消。背面へ下がって戻る 2 件は、器を残す判定を外す (`retainsAttachment` を常に false にする) と、背面の確定の通知で器が外れて `detachCount == 1` になり落ちる |
| 🟠 Major (相方と一致): decorView が無い画面で描画の観測が始まらず、最初の描画を取り逃す | (a) 未描画のまま resume した時点で観測が始まっていなければ張り直し、描画を促す。(b) 次の周回の取り直しで張ったときも描画を促す | (a) `ResumedActivityTracker.kt:110-118`、(b) `ActivityDrawObserver.kt:86-93`。テストは unit の張り直し 2 件、instrumented の `DecorViewDrawObserverTests` 2 件 | 解消。(a) の unit テストは、張り直しを外すと `startedObservationCount` が 0 のままで落ちる。(b) の instrumented テストは、画面が静止した後に張るので、促す処理を外すと描画が起きず時間切れで落ちる |
| 🟡 Minor: Dialog・Loading の説明が「resumed な Activity」のまま (公開 doc を含む) | 4 か所を「resumed で描画済み」と「前面で表示されている画面」に直した | `KsDialog.kt:23-24`、`ActivityDialogPresentationSurface.kt:4`、`LoadingPresentationSurface.kt:45`、`DialogHostWaitQueue.kt:183` | 解消。本体の main に「resumed な Activity」の記述は残っていない (MAUI ブリッジのテストのコメントに 1 か所あるが、JVM 上に Activity が無いという記述なので今も正しい) |
| 🔵 Suggestion: iOS の TS-HW-06 の上限の余裕が 600 ms | duration 1500 ms、背面の時間 1000 ms にした (Android 版と同じ比) | `ios/Tests/KsDialogsTests/ToastContractTests.swift:464-470` | 解消。「載った時点から数え直す」誤った実装は、下がってから 2500 ms 以上で消えるので、上限 (2500 ms 未満) で見分けられる |

### deviation.md の 2026-09-30 の項と実装の照合 (lessons process L-003)

| deviation の項 | 実装との対応 | 判定 |
|---|---|---|
| [付随修正] iOS `KsToast.swift` の公開 doc | 前面の待ちで受け付けた表示の数え始めを書き足している。シグネチャは変えていない | 一致 |
| [付随修正] Android・KMP・MAUI の Toast の公開 doc | `KsToast.kt:33-34`、KMP の `KsToast.kt`、`IKsToast.cs` に同じ内容を書き足している。シグネチャは変えていない | 一致 |
| design Decision 1: Loading・Toast の `onHostChanged` で器を外さない (B) | 外す条件は `shouldDetachAttachment` の 2 つだけ。1 つは、描画済みの別の提示先が現れたとき (`newHost != null`)。もう 1 つは、提示先が無く、画面を残せない (破棄された) とき | 一致。ただし、この項の「tasks 7 の実測に場面を足す」は tasks.md に反映されていない (Minor 1) |
| design Decision 1 (描画の取りこぼしの保険): (a) resume での張り直し (b) 取り直しで張ったときに描画を促す | (a) `ResumedActivityTracker.kt:114-117`。「start の通知を受けていない画面」(追跡が無い) も同じ分岐で張り直すが、これは前回からある分岐。(b) `ActivityDrawObserver.kt:91` | 一致 |
| 上の B の実装の形 | 破棄の記録と `isDestroyed` による判定 (`ResumedActivityTracker.kt:71-72`)。提示面の口の `retainsAttachment` (既定は false。既定の面は追跡役へ委ねる)。前面を離れた通知での期限の確定を、判定より前に置く (`ToastCoordinator.kt:320-323`) | 一致 |
| [review-001 Minor への対応] `KsDialog.show` の公開 doc | `KsDialog.kt:23-24` | 一致 |

## 指摘事項

### 🟡 Minor deviation で足すと決めた実測の場面が、tasks.md の 7 章に載っていない

**該当箇所**: `tasks.md` の「7. 直した後の実測」(7.1〜7.5)、`deviation.md` の design Decision 1 の項 (2026-09-30)

**問題点**:
deviation の項は、tasks 7 の実測に「Loading の表示中に背面へ下がって戻る」場面を足すと記録している。見るのは「戻った後の最初の描画のコマに Loading が写っているか」。しかし tasks.md の 7 章には、この場面の項目が無い。7.1 は「1.2 と同じ端末・同じ手順で撮る」で、1.2 にもこの場面は無い。

器を残す判定 (B) の効き目を実機で確かめるのは、この実測だけである。unit テストは素の JVM の Activity で行うので、実物のウィンドウでちらつきが消えることは確かめられない。

7 章を tasks.md のチェックリストどおりに進めると、この場面を撮り落とす。また、完了の判定がこの場面を含んでいたかを、後から追えない。

**推奨修正**:
tasks.md の 7 章に、この場面の項目を足す (tasks.md は凍結される足場ではない)。例:「7.x Android ルートで、Loading の表示中にホームへ下がり、履歴から戻る場面を録画し、戻った後の最初の描画のコマに Loading が写っていることを確かめる」。

この観測は、器を残す判定があれば写り、無ければ最初のコマに写らない。つまり、機構の有無で結果が分かれる (L-001)。

### 🟡 Minor 器を残す判定のうち「描画済みの別の画面へ移る」経路を、実物の判定でつないだテストが無い

**該当箇所**: `android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/AttachedHostRetentionTests.kt` (テスト 4 件)、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/AttachedHostRetention.kt:33`

**問題点**:
`AttachedHostRetentionTests` の 4 件が確かめているのは、次の 2 つだけ。

- 背面へ下がって戻っても器が外れない
- 画面が破棄されたら器が外れる

「載っている画面 A が破棄されないまま、上に開いた画面 B が描画されて提示先になったら、器が B へ移る」経路は、器を残す判定を実物でつないだテストでは確かめていない。既存の載せ替えのテスト (TS-AN-03・TS-MX-03 など) は偽の提示面を使い、`retainsAttachment` は既定の false なので、この判定の分岐を区別できない。

そのため、`newHost != null -> true` を `newHost != null -> !retainsAttachment(attachedHost)` に変えるような退行があっても、どのテストも落ちない。この退行が起きると、画面遷移の後も Toast や Loading が背面の画面 A に載ったまま残る。利用者から見ると、Toast が消え、Loading の覆いが効かない。

今の実装は正しい。ただ、この change で足した仕組みが持ち込みうる最大の不具合 (器が古い画面に残り続ける) を、固定するテストが無い。

**推奨修正**:
`AttachedHostRetentionTests` に、Loading と Toast それぞれ 1 件のテストを足す。

1. A を開いて描画し、器を載せる
2. B を開く (A は pause だけ。B はまだ描画しない)
3. この時点では器が A に付いたまま (外す回数が 0) であることを確かめる
4. B を描画する
5. 器が 1 つだけで、A の器とは別のものに入れ替わっていることを確かめる。可能なら、載っている先が B であることも確かめる

判定の分岐を上の退行の形に変えると、5 で器が入れ替わらずに落ちる。

## アクションプラン

1. (Minor 1) tasks.md の 7 章に、deviation で決めた「Loading の表示中に背面へ下がって戻る」実測の項目を足す。7 章に着手する前に行う
2. (Minor 2) `AttachedHostRetentionTests` に、破棄されていない画面から描画済みの別の画面へ器が移るテストを Loading・Toast で 1 件ずつ足す
3. (参考) 8.1 の全件実行は、load average が落ち着いた状態で行う。今回、汚染下の全件実行で TS-MX-01 (色の観測) と TS-HW-01 (実時間の上限) が 1 回ずつ落ちた。落ちたら、この review の切り分けと同じ手順で扱う
