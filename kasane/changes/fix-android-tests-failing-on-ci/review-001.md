# レビュー結果: fix-android-tests-failing-on-ci (001 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー
exploration.md「決定事項」の 2 件 (Toast の状態を UI スレッドで読む / モーダルの寸法を画面から決める) はどちらも満たされている。テストのスレッドから `ToastCoordinator` の表示の列を走査する箇所は grep で網羅を確かめて 0 件になっており、製品コード (`src/main`) には触れていない。指摘は重複の整理と証跡の扱いに関する Suggestion だけ。

## 照合した規約
- comment-policy.md (always)
- ci-flaky-test-policy.md (適用のきっかけ: `android/**/src/androidTest/**`・`android/**/src/test/**` のテスト / CI の間欠失敗の切り分け)
- test-execution.md (適用のきっかけ: テストを実行するとき・テスト結果を報告するとき)

ロードしたスキル: ksn-review, kotlin-impl-skill

lessons: `kasane/lessons/code-review.md` の L-001 (証跡が命題の真偽で分かれるか) と L-002 (全件実行表との行ごとの照合) を当てた。

## 確認した観点

### 決定事項の充足
- **instrumented の集約点**: `ToastTestHarness` の `displayCount` / `containers` / `contentViews` / `defaultContentViews` / `isPresenting` がすべて `readOnMain` を通る。`waitUntilPresenting` は `containers` を経由するので、ポーリングの各回が UI スレッドで読む
- **coordinator を直接読むもの**: `ToastMultiDisplayTests` (2 箇所)・`DrawnHostPresentationTests` (全箇所) が `ToastTestHarness.readOnMain` を通る
- **unit**: `AttachedHostRetentionTests` の Toast 3 本で、待ち (`waitUntilOnMain`) と直後の読みがすべて `Dispatchers.Main` の上にある。`observeResumedChange` のコールバック内の読み (`AttachedHostRetentionTests.kt:83`) は元から UI スレッド上
- **網羅の確認** (自分で grep した範囲): `android/` 配下の `src/test`・`src/androidTest` と `android/ksdialogs`・`kmp`・`maui/android/native` のテストで `presentedContainers` / `presentedContentViews` / `presentedDefaultContentViews` / `displayCount` / `isPresenting` / `ToastCoordinator` を洗った。Toast の coordinator を harness を通さずに読む箇所は上記のみで、残りはない。`harness.coordinator` の利用は `discardAll()` (UI スレッド上) と `Toast(...)` の組み立てだけ。Dialog 側の `presentedContainers` はテスト用 surface が `synchronized` でコピーしており対象外。Loading は `@Volatile` の単一参照で対象外。`ToastTestAnnouncer` / `ToastTestPresentationSurface` も `synchronized`
- **製品コード**: diff は `src/androidTest` と `src/test` の 6 ファイルだけ
- **モーダルの寸法**: 前提の assert (`DialogCurrentPageTests.kt:207-208`) は残っている

### UI スレッドからの呼び出し・デッドロック
- `ToastTestHarness.readOnMain` は `Looper.getMainLooper().isCurrentThread` のときその場で読むので、`runOnMainSync` を UI スレッドから呼んで例外になる経路はない。既存の `ToastSystemBarsTests.kt:255` (`withContext(Dispatchers.Main) { harness.displayCount }`) もこの分岐で動く
- `DrawnHostPresentationTests` の「描画を止めた」状態は UI スレッドを塞ぐ形ではない (`releaseDraw` を `withContext(Dispatchers.Main)` で呼んでいる) ので、止めている間の `runOnMainSync` も詰まらない
- unit の `waitUntilOnMain` はテスト本体 (`runBlocking`) から `Dispatchers.Main` (`DialogUiThreadTest` の専用スレッド) へ切り替えて読むだけで、UI スレッド側からテストのスレッドを待つ経路はない

### ポーリングの負荷
- instrumented は 8ms ごと、unit は 5ms ごとに UI スレッドへ 1 回の読みを積む。読みは表示の列の写しを作るだけの軽い処理で、演出のフレーム (16ms) を押し出すほどの負荷にはならない

### モーダルの寸法
- CI 端末 (縦 320x640dp): 幅 `min(320, 320 - 64) = 256`、高さ `min(400, screenHeightDp - 240)`。`screenHeightDp` がシステムバーを含む場合 (API 35 以降で targetSdk 35 以上) は 400、含まない場合も 300 以上。左 32dp + 256dp = 288dp で画面に収まるので左端のずらしが保たれる。上端はステータスバーの下からの 120dp で、下端は下部のナビゲーションバーより上に収まる
- 広い端末 (例 411dp 幅): `min(320, 347) = 320`、高さ 400 で変更前と同じ寸法
- 中身 200x120dp (`DialogCurrentPageStage.CONTENT_WIDTH_DP` / `CONTENT_HEIGHT_DP`) はどちらの端末でもページに収まる。収まらない端末では `check` がモーダルを出す前に理由付きで落とすので、無言で前提が崩れることはない

### コメント規約
- 追加したコメントはすべて現在形の自己完結した説明で、作業文書の参照・変更識別子・履歴記述はない。`scripts/comment-policy-lint.py --advisory` の対象 6 ファイルへの要確認は、diff の外にある既存行 (`DialogCurrentPageTests.kt:394`・`ToastMultiDisplayTests.kt:32`。どちらも非公開のテストコードでヒューリスティックの誤検出) だけ

### テスト実行 (L-002: handbook 全件実行表との行ごとの照合)

| ビルドルート | 実行 | 結果 |
|---|---|---|
| ios/ | 対象外 (diff なし) | - |
| android/ | `./gradlew test --rerun-tasks` | 109 tests / 0 failures / 0 errors / 0 skipped (`testDebugUnitTest` の結果 XML 20 本)。`AttachedHostRetentionTests` の 6 件を含む |
| android/ (instrumented) | 未実行 (呼び出し元の指示どおり)。`:ksdialogs-core:compileDebugAndroidTestKotlin` のコンパイルだけ確認し成功 | 未実行。完了判定には検証 CI (`android-instrumented / verify`) か手元の全件実行の件数が要る |
| kmp/ | 対象外 (テストコードだけの変更で、kmp が巻き込むのは android/ の main のみ) | - |
| maui/ | 対象外 (同上) | - |
| maui/android/native/ | 対象外 (同上) | - |
| maui/macios/native/ | 対象外 (diff なし) | - |

追加で `AttachedHostRetentionTests` を `--rerun-tasks` で 10 回回し、10 回とも 6 件成功した (下の Suggestion 3 のとおり、これは修正の効果の証明にはならない)。

## 指摘事項

### 🔵 Suggestion UI スレッドで読む補助が 4 つ目になり、Toast の補助が Loading の補助に依存している
**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/ToastTestHarness.kt:106-108`
**問題点**: `ToastTestHarness.readOnMain` は `LoadingLayoutObservation.readOnMain` に委ねている。Toast の harness が Loading のレイアウト観察という名前の補助に依存しており、読み手には依存の理由が見えにくい。同じ役目の補助は `DialogCurrentPageTests.onMainSync`・`DialogSystemBarsTests.readOnMainThread`・`LoadingLayoutObservation.readOnMain` に続いて 4 つ目になり、UI スレッドから呼ばれたときの分岐を持つのは今回の 1 つだけ。
**推奨修正**: 今回は直さなくてよい。蒸留で handbook に「UI スレッドが書き換える状態は UI スレッドで読む」を足すときに、`support/` に中立な名前の補助 (UI スレッドから呼ばれたらその場で読む分岐付き) を 1 つ置き、既存の 3 つをそちらへ寄せる作業を別 change として起票するのがよい。

### 🔵 Suggestion `waitUntilOnMain` が `waitUntil` のループを複製している
**該当箇所**: `android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/support/DialogTestWaiting.kt:32-42`
**問題点**: 期限とポーリングのループが `waitUntil` と同じ形で 2 つある。片方だけ直される形のずれが起こり得る。
**推奨修正**: `waitUntil` の条件を `suspend () -> Boolean` にすれば、`waitUntilOnMain` は `waitUntil(timeoutMillis) { withContext(Dispatchers.Main) { condition() } }` の 1 行で書ける。既存の呼び出し側は変えずに済む。優先度は低い。

### 🔵 Suggestion 繰り返し実行の成功回数を CME 解消の証跡にしない
**該当箇所**: `exploration.md` の決定事項「検証」(手元での繰り返し実行)
**問題点**: 修正前も CME は間欠的で、失敗した job だけを再実行すると通っていた (instrumented も 12 件中 12 件成功)。このため、手元で N 回通ったことは修正があってもなくても同じ結果になり得て、修正が効いたことの証明にならない (L-001)。修正が効くのは、表示の列を書き換えるのと同じスレッドで読むので、走査と追加・削除が重なり得ないという構造による。
**推奨修正**: 完了の根拠は (1) 上の構造の説明と網羅の grep 結果、(2) 検証 CI の緑、の 2 つにする。繰り返し実行は「新しい失敗を持ち込んでいない」ことの確認として扱う。区別できる証跡が要るなら、修正前のコードで UI スレッドが列を書き換える間にテストのスレッドから `presentedContainers` を走査し続けるストレスで CME が再現し、修正後の読み方では再現しないことを見せる。ただし S 級で 1 回限りの修正なので、そこまでは求めない。

## アクションプラン
1. (完了条件) 検証 CI の `android / verify` と `android-instrumented / verify` が緑であることを確かめ、instrumented の件数を変更の証跡に残す (L-002 の instrumented の行が未実行のため)
2. (蒸留時) handbook `ci-flaky-test-policy.md` への規約の追加とあわせて、UI スレッドで読む補助の統一を別 change として起票するか判断する (Suggestion 1)
3. (任意) `DialogTestWaiting.waitUntil` の条件を suspend にして重複を消す (Suggestion 2)
