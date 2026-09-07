# Exploration: fix-android-instrumented-toast-back-loading-coalescing

## 課題 / 動機

Android の instrumented テスト (`android/` の `./gradlew connectedDebugAndroidTest`、305 tests) に、端末 / API レベルに依存して失敗する既存の 2 件がある。rename-dialog-contract-singular (Dialog 契約型の改名) の追加検証 (同 change の `verify-002.md`) で見つかったもので、改名の差分は androidTest に 1 件もなく、両テストは Dialog 契約に触れない — 改名起因ではない既存の失敗。同 change では deviation として記録し、修正はこちらへ切り出した。

1. **F1: Toast 表示中の戻る操作でホームへ抜ける確認が API 36 で落ちる** — `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt` の `Toast_表示中でも戻るとホームが通る`。Pixel 6a (API 36) で全件 2 回 + 単体 1 回の計 3 回すべて失敗。`GLOBAL_ACTION_BACK` の後に `ActivityScenario.onActivity` で `currentActivity()` を読む箇所が `NullPointerException: Cannot run onActivity since Activity has been destroyed already` で落ちる。戻る操作で提示先 Activity が破棄される API 36 の挙動差 (API 29 / 33 では成功)。テストの観測方法の問題か、Toast が戻る操作を素通しする実装の挙動差かは未切り分け
2. **F2: Loading の「出の途中の新しい開始は出の完了後に新世代として表示される」(LD_CO_13) が時間依存で落ちる** — `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:338` の `waitUntil { coordinator.isDismissing }` が、出の演出を観測する前に完了してしまう競合。`ksn_api29` エミュレータで 2/2 失敗、Pixel 6a (API 36) で 1 回目成功・2 回目失敗の間欠、Pixel 4a (API 33) で成功。端末を問わない不安定さを持つ

付随: `kasane/handbook/cross/test-execution.md` の android/ (instrumented) 行の実測値「305 tests / 0 failures」(2026-08-28 実測) が、現在の端末構成 (`ksn_api29` + API 30 以上の実機) では再現しない。件数 305 と API レベル別 skip の設計は一致しており、乖離は failures の実測値だけ。修正後に取り直す (handbook の更新は本 change の実装で行う)。

## 原因 (探索で特定。2026-09-06)

**F1**: テスト APK の targetSdk は compileSdk と同じ 36 (`build/intermediates/.../debugAndroidTest/AndroidManifest.xml` で確認)。Android 16 (API 36) では targetSdk 36 のアプリで予測型バック (predictive back) が既定で有効になり、`Activity.onBackPressed()` が呼ばれなくなる。テスト用画面 `support/ToastInputTestActivity.kt` は `onBackPressed()` の上書きで「戻るを数えて閉じない」設計のため、API 36 ではシステム既定の戻るで Activity が finish → 破棄 → 次の `currentActivity()` が NullPointerException。Toast は戻るを仕様どおり素通ししており (Activity まで届いたから閉じた)、壊れたのは「届いたことを数える受け皿」の側。ライブラリ本体は `onBackPressed` / `KEYCODE_BACK` に依存せず (Dialog の器は `setCancelable(true)` + `setOnCancelListener` で framework の `Dialog` が API 33+ の `OnBackInvokedDispatcher` 経由でキャンセルを届ける)、Pixel 6a でも Dialog 系テストは全件通過。L-001 の姉妹面照合でも本体側に穴はなし。

**F2**: `loading.show()` は器をウィンドウに載せた時点で戻り、レイアウト確定 (`layoutSnapshot.isFrozen`) や入りの演出の完了を待たない。テストはその直後に `hide()` を呼ぶため、凍結前なら `LoadingContainer.runDismissal` の分岐で出の演出を走らせず即撤去し、`dismissalJob` が立って消えるまでが main スレッドの 1 スライス内に収まる → 8ms ポーリングの `isDismissing` 観測が間に合わない。遅い API 29 エミュレータでは毎回この経路、Pixel 6a ではレイアウトが先に間に合うかどうかの競合 (間欠)。実装は仕様どおりで、テストの前提「出の途中」が成立していないだけ。

## 検討した選択肢 (却下案と理由を含む)

F1:
- **A (採用)**: テスト画面が API 33+ では `OnBackInvokedDispatcher` に既定優先度のコールバックを登録して戻るを数える (旧 API は `onBackPressed` のまま)。「システムが受け取る戻ると同じ入口」という検証の意図に忠実で、将来 API でも壊れない
- B (却下): androidTest の manifest で `enableOnBackInvokedCallback=false` に opt-out。1 行で済むが非推奨経路で将来無視される予定、テストが旧い戻る経路だけを見ることになる

F2:
- **A (採用)**: `hide()` の前に `firstContainer.containerState == SHOWN` を待つ (`DialogTransitionAttachmentTests` と同じ待ち方) 。観測も器の `DISMISSING` 状態 (@Volatile) で行う。Scenario の前提「出の途中」を実際に成立させ、既定の演出 250ms が走るので観測は安定する
- B (却下): A に加えて出の演出を門で止めるフックを Loading にも渡して完全決定的にする。演出スケール 0 の端末でも落ちなくなるが、Loading の演出フック経路が要り規模が上がる。今つながっている 3 台 (API 33 / 35 / 36) は animator_duration_scale が 1.0 で、A で十分

## 決定事項

- F1 = A、F2 = A を 1 change に同梱する (どちらも「テストの観測方法が新しい端末構成に追いついていない」同種の数行修正で、分割より全件取り直しを 1 回で済ませるほうが素直)
- 修正後に instrumented 全件を取り直し、`kasane/handbook/cross/test-execution.md` の android/ (instrumented) 行の実測値を更新する
- ライブラリ本体は触らない

## ADR 候補 (作成済み: なし / 未起票: なし)

ADR 級の決定なし (テストと handbook の局所修正)。

## 未決の論点

- 演出は `ValueAnimator` なので animator_duration_scale が 0 の端末では F2 の A でも即撤去になる。現行の端末構成では問題にならないため、必要になったら B (演出の門) を検討する

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (確定・オーナー承認 2026-09-06)

androidTest の 2 ファイルと handbook の実測値 1 行のみ。公開 API 変更なし・可逆・UI なし。提案は作らず直接実装 (Plan モード + instrumented 全件再実行) に進む。
