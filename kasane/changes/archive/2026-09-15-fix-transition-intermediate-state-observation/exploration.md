# Exploration: fix-transition-intermediate-state-observation

## 課題 / 動機

検証 CI の `android-instrumented / verify` (run 34328835867、commit d165824、2026-09-09) が `DialogTransitionTests.PB_TR_21_none_直後の閉鎖は覆いの出現完了を待ってから退出する` の 1 件で落ちた (「覆いの出現中はまだ退出しない expected PRESENTING but was SHOWN」)。当該コミットは kmp/ と android のビルドファイルだけを変えており本体・instrumented テストは無改変なので、テスト側のタイミング依存 (フレーク) である。

機構: `DialogTransition.none()` は中身の演出を持たず、PRESENTING の期間は覆いのフェード (既定時間) だけになる。テストはその状態をメインスレッド外のポーリング (`awaitState(PRESENTING)`) で捕まえてから `onMainThread { complete(true); 状態を読む }` で「報告時点で PRESENTING」を主張するが、ポーリングがヒットしてからメインスレッドのブロックが走るまでの遅れが CI エミュレータで伸びると、`DialogContainer.beginPresentation` の完了で SHOWN に進んでしまう。handbook `cross/ci-flaky-test-policy.md`「観測は終端状態の合意を待つ」が禁じる「通り過ぎる一瞬 (中間状態) をポーリングで捕まえる」型そのもので、同規約は「その状態をテスト側の仕掛け (完了を押さえる演出フック) で確定させる」ことを求めている。

発見の文脈: add-kmp-maven-distribution の蒸留直後、オーナーの依頼で CI 失敗を切り分けた。オーナーの指示は「同様の禁止系があれば全部直す」。

2 回目の観測 (2026-09-13): リリース PR #4 (0.1.0-beta.2、commit f3644e7) の `android-instrumented / verify` (run 34752439146) が同じ 1 件・同じメッセージで落ちた。当該 PR は release 機構とインストール例の契約だけを変え、Android の本体・instrumented テストは無改変。初観測 (2026-09-09) から 4 日、別 commit・別 run での再現で、タイミング依存という切り分けの裏付けになる。オーナー判断で `Re-run failed jobs` により再実行した。

同型の洗い出し (2026-09-09、4 面のテストで中間状態 PRESENTING / DISMISSING を待つ・主張する箇所を実物で確認):

| # | 面 | 箇所 | 状況 |
|---|---|---|---|
| 1 | Android instrumented | `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogTransitionTests.kt:281-297` (PB_TR_21) | **違反**。`none()` の実時間フェードを頼りに PRESENTING を捕まえて主張。CI で実際に落ちた |
| 2 | iOS | `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:377-392` (PB-TR-21) | **違反 (姉妹面)**。同じ形 — `none()` で `.presenting` をポーリングで待ち、`complete(true)` 後に `.presenting` を主張。手元では未再現だが機構は同じ |
| 3 | iOS | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:319-335` (LD-CO-13) | **違反 (姉妹面)**。`hide()` の実時間の出の演出を `waitUntil { isDismissing }` で捕まえてから `show()` を差し込む。Android 版の LD_CO_13 は fix-android-instrumented-toast-ime-hide-flake で関門 (`LoadingTestGate` / dismissalGate) により中間状態を確定させる形へ直したが、iOS 版は据え置き |

同型に見えるが適合しているもの (直さない): Android PB_AA_03 (`DialogTransitionGate` で退出フックを止めて DISMISSING を確定) / Android ToastMultiDisplayTests・ToastTransitionTests の PRESENTING 待ち (presentationGate で入りを止めている) / Android LD_CO_13 (関門つき) / iOS PB-TR-13 とダイアログの退出系 (`surface.holdsDismissalCompletion` で撤去完了を押さえて `.dismissing` を確定) / PB_TR_17・PB_TR_18 (待つのは終端状態 SHOWN)。MAUI iOS 橋渡しテストに中間状態の観測は無い。

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: PB_TR_21 の観測をどう確定させるか (2026-09-15 探索)

前提の確認 (実物): 概念 `core/api/transition-semantics.md` の PB-TR-21 の主張は「中身の演出と覆いのフェードは並行して走り、両方が終わって初めて表示中になる」。Android の器 (`DialogContainer.beginPresentation`) は出現フェーズ (`DialogTransitionRunner.runPresentationPhase` = 覆いのフェードと中身のフックを 1 つの coroutineScope で束ねた区間) の完了後に `isResultSettled` を見て直列に退出する。覆いのフェードは器の内部 (`DialogTransitionAnimator` 経由) で走り、テストから止める継ぎ目は無い。現行テストは中身側を `none()` で潰し、覆いのフェードの実時間だけで PRESENTING の窓を作っている。

| 案 | 中間状態の確定 | 概念の主張の保持 | 本体への手 | iOS への揃えやすさ | 判定 |
|---|---|---|---|---|---|
| A: 出現フックを関門 (`DialogTransitionGate`) で止めて「出現中」を確定させる | 確定する | 中身側からの検証に変わる。覆いだけが残る組み合わせは固定しない | なし (テストと Scenario の題名のみ) | 同じ関門があるので同形 | **採用** |
| B: 覆いのフェードを止める継ぎ目を器 (アニメータ) に足す | 確定する | 完全に保てる | あり。内部に差し替え点が増え、iOS にも同じ継ぎ目が要る | 要追加 | 却下: 本体を触るコストに見合わない |
| C: `overlayDuration` を長くして窓を広げる | 確定しない (実時間の賭けのまま) | 保てるが信頼できない | なし | 同じく実時間依存 | 却下: 規約 (中間状態をポーリングで捕まえない) の違反型が残る |

A の根拠: 閉鎖を直列化している機構は「出現フェーズ全体の完了を待ってから閉鎖信号を扱う」という 1 本の経路で、中身側を止めても覆い側を止めても通る道は同じ。覆いと中身は同じ束の中で並行に走り分岐が無いので、主張の実質は保てる。

### 論点 2: iOS LD-CO-13 を Android と同じ形に揃えられるか

揃えられる (分岐なし)。iOS の `LoadingTestHarness` も registry にカスタム View を登録して `ksDialogTransition` を添付でき、`DialogTransitionGate` + `DialogTransitionProbe.gatedHook` で出の演出を止める書き方は LD-TR-01 (`ios/Tests/KsDialogsTests/LoadingTransitionTests.swift`) で既に使われている。Android 版 LD_CO_13 (`LoadingCoalescingTests.kt:330-`) と同じく、第 1 世代の出のフックを関門で押さえ、フックの呼び出しを確かめてから show を差し込み、関門を開けて hide を待つ形にする。Android 版にある「入りの演出を終えてから閉じる」の前提待ちも同様に置く。

### 論点 3: iOS 退出系の `Task.sleep` 後の「起きない」確認を含めるか

含めない (採用)。`ios/Tests/KsDialogsTests/DialogTransitionTests.swift` の 5 箇所 (PB-TR-1x 系) は状態を関門・撤去保留で確定させた上で「一定時間待っても起きない」を見る形で、間違って落ちることはなく、弱点は「起きたのを見逃して通る」方向だけ。CI の赤とは無関係で、履歴 (`DialogTestStateHistory`) へ寄せると変更の性格がフレーク修正から観測規律の追随へ広がるため、今回のスコープから外す。別起票もしない (オーナー判断)。

## 決定事項

- 修正対象は中間状態のポーリング捕捉の 3 箇所に限定する: Android PB_TR_21 / iOS PB-TR-21 / iOS LD-CO-13。本体 (器・演出実行部) は無改変
- PB_TR_21 (Android / iOS) は出現フックを関門で止めた `DialogTransition` で「出現中」を確定させ、関門を開ける前に閉鎖を報告して出現中のままであることを主張し、開けてから結果の配送と REMOVED を待つ形にする。Scenario の題名は「none 直後の閉鎖は覆いの出現完了を待ってから退出する」から主語を改め、「出現中の閉鎖は出現完了を待ってから退出する」のように中身の演出を止めた形に合わせる (ID `PB-TR-21` は据え置き。`scripts/scenario-id-coverage.py` は ID だけを突合するので題名の変更は影響しない)。concepts の PB-TR-21 の文 (両方が終わって初めて表示中) はそのまま
- iOS LD-CO-13 は Android 版と同形 (出の関門) に揃える
- ci-flaky-test-policy の切り分け手順に従い、手元反復 (10 回以上) の実測を change の `evidence/` に残す。CI 限定 skip の候補にはしない
- 関連リポジトリ KsSettingsView からの知らせ (2026-09-14 `cmd_wait_published` の空文字アーム) は本件と無関係で、オーナー判断により却下・台帳記録済み

## ADR 候補 (作成済み: なし / 未起票: なし)

覆すコストが低いテストの書き方の決定であり、ADR の選別基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。

## 未決の論点

- 覆いのフェードだけが残っている場合の直列化は今回固定しない (案 A の既知の狭まり)。将来、器の内部に覆いを止める継ぎ目を足す必要が別件で出たら、そのときに覆い側の Scenario を足す
- iOS 退出系の `Task.sleep` 後の「起きない」確認 (5 箇所) は履歴で見る形が規約上は強いが、今回は対象外 (論点 3)
- Android 版 LD_CO_13 の関門 (`LoadingTestGate`、`support/LoadingTestFixtures.kt`) と iOS の `DialogTransitionGate` は別物だが役割は同じ。iOS 側で Loading 用に別の関門を増やさず `DialogTransitionGate` を流用してよい (LD-TR-01 の前例)

- (実装時の発見・蒸留への引き継ぎ) iOS の `PB-TR-05` (presentation 中の閉鎖信号は presentation 完走後に退出する) と書き換え後の `PB-TR-21` は仕掛けがほぼ同じで、違いは前者がフックの出来事の並び・後者が器の状態を主張する点だけ。Android には PB_TR_05 相当が無い。両方を残すかは蒸留時に判断
- (蒸留への引き継ぎ) PB-TR-21 の旧題名はアーカイブ済み change (`2026-08-22-add-presentation-behavior` の spec.md / verify-001.md / review-004.md) にだけ残る。凍結アーカイブなので書き換えない。concepts / handbook / skills に旧題名は無い
- (蒸留への引き継ぎ) iOS LD-CO-13 は差し込みの 1 回だけ公開入口ではなく UI スレッド隔離の受理口 (`LoadingCoordinator.beginUse`) を直接呼ぶ。Swift に `CoroutineStart.UNDISPATCHED` 相当が無いための構造的な固定で、根拠は evidence/ci-flake-triage.md「検出力の根拠」

## 変更級の推奨: S

理由: 触るのはテスト 3 本と Scenario の題名だけで公開 API・本体は無改変、可逆、UI なし。触る能力は transition (Dialog) と Loading の 2 面だがどちらもテスト側の観測方法の修正に閉じる。デルタスペックは不要 (Scenario の意味は変えず、題名の主語を実態に合わせるのみ)。

## UI 素材 (ui/references/ の一覧と注釈)

なし
