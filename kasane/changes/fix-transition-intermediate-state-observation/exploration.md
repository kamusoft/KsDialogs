# Exploration: fix-transition-intermediate-state-observation

## 課題 / 動機

検証 CI の `android-instrumented / verify` (run 34328835867、commit d165824、2026-09-09) が `DialogTransitionTests.PB_TR_21_none_直後の閉鎖は覆いの出現完了を待ってから退出する` の 1 件で落ちた (「覆いの出現中はまだ退出しない expected PRESENTING but was SHOWN」)。当該コミットは kmp/ と android のビルドファイルだけを変えており本体・instrumented テストは無改変なので、テスト側のタイミング依存 (フレーク) である。

機構: `DialogTransition.none()` は中身の演出を持たず、PRESENTING の期間は覆いのフェード (既定時間) だけになる。テストはその状態をメインスレッド外のポーリング (`awaitState(PRESENTING)`) で捕まえてから `onMainThread { complete(true); 状態を読む }` で「報告時点で PRESENTING」を主張するが、ポーリングがヒットしてからメインスレッドのブロックが走るまでの遅れが CI エミュレータで伸びると、`DialogContainer.beginPresentation` の完了で SHOWN に進んでしまう。handbook `cross/ci-flaky-test-policy.md`「観測は終端状態の合意を待つ」が禁じる「通り過ぎる一瞬 (中間状態) をポーリングで捕まえる」型そのもので、同規約は「その状態をテスト側の仕掛け (完了を押さえる演出フック) で確定させる」ことを求めている。

発見の文脈: add-kmp-maven-distribution の蒸留直後、オーナーの依頼で CI 失敗を切り分けた。オーナーの指示は「同様の禁止系があれば全部直す」。

同型の洗い出し (2026-09-09、4 面のテストで中間状態 PRESENTING / DISMISSING を待つ・主張する箇所を実物で確認):

| # | 面 | 箇所 | 状況 |
|---|---|---|---|
| 1 | Android instrumented | `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogTransitionTests.kt:281-297` (PB_TR_21) | **違反**。`none()` の実時間フェードを頼りに PRESENTING を捕まえて主張。CI で実際に落ちた |
| 2 | iOS | `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:377-392` (PB-TR-21) | **違反 (姉妹面)**。同じ形 — `none()` で `.presenting` をポーリングで待ち、`complete(true)` 後に `.presenting` を主張。手元では未再現だが機構は同じ |
| 3 | iOS | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:319-335` (LD-CO-13) | **違反 (姉妹面)**。`hide()` の実時間の出の演出を `waitUntil { isDismissing }` で捕まえてから `show()` を差し込む。Android 版の LD_CO_13 は fix-android-instrumented-toast-ime-hide-flake で関門 (`LoadingTestGate` / dismissalGate) により中間状態を確定させる形へ直したが、iOS 版は据え置き |

同型に見えるが適合しているもの (直さない): Android PB_AA_03 (`DialogTransitionGate` で退出フックを止めて DISMISSING を確定) / Android ToastMultiDisplayTests・ToastTransitionTests の PRESENTING 待ち (presentationGate で入りを止めている) / Android LD_CO_13 (関門つき) / iOS PB-TR-13 とダイアログの退出系 (`surface.holdsDismissalCompletion` で撤去完了を押さえて `.dismissing` を確定) / PB_TR_17・PB_TR_18 (待つのは終端状態 SHOWN)。MAUI iOS 橋渡しテストに中間状態の観測は無い。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- PB_TR_21 (Android / iOS) の意図は「覆いの出現中に来た閉鎖信号は、出現を完走させてから直列に退出する」(`DialogContainer.beginPresentation` の `isResultSettled` 分岐)。`none()` のまま観測するのではなく、PB_TR_28 と同じく presentation フックを関門 (`DialogTransitionGate` / iOS の同等物) で止めた `DialogTransition` で「出現中」を確定させ、関門を開ける前に閉鎖を報告 → 開けてから REMOVED を待つ形にするのが素直。ただし Scenario 名が `none` を掲げているので、「none プリセットでも覆いの出現を待つ」という主張を残すなら、覆いのフェード側を止める仕掛け (器の既定時間を長くする・覆いのアニメーションを差し替える) が要るか、Scenario の主語を「中身の演出が無い組」から「出現フック付き」へ改める必要がある (core の layout-spec / transition の Scenario ID 網羅検査 `scripts/scenario-id-coverage.py` との対応も確認)
- iOS LD-CO-13 は Android 版と同じ形 (出の演出を関門で止め、show の要求を受理列へ載せてから関門を開ける) に揃えるだけでよいか。iOS の Loading テストハーネスに関門相当 (`LoadingTestGate` の Swift 版) が既にあるかは未確認
- iOS の退出系テスト (PB-TR-1x 系) にある `Task.sleep(150ms)` 後の「起きない」確認は、同規約の「『起きない』ことの確認には履歴を使う」に照らすと弱いが、状態は `holdsDismissalCompletion` で確定しているため今回の型 (中間状態のポーリング捕捉) とは別。今回のスコープに含めるかは探索時に決める
- 切り分けの実測 (ci-flaky-test-policy の手順): 手元 10 回反復で通ることと、CI での落ち方 (1 件・初観測。過去の instrumented 失敗 LD_CO_13 とは別テスト) を evidence に残す。CI 限定 skip の候補にはしない (観測の修正が先)

## 変更級の推奨

未判定 (暫定 S: テスト 3 本の観測方法の修正で本体は無改変。Scenario の主語を改める場合は core の Scenario ID の扱いが絡むため M に上がりうる)

## UI 素材 (ui/references/ の一覧と注釈)

なし
