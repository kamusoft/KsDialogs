# レビュー結果: fix-transition-intermediate-state-observation (001 回目)

**日付**: 2026-09-15
**判定**: CHANGES_REQUESTED

## サマリー

3 本とも「通り過ぎる一瞬をポーリングで捕まえる」形が消え、演出フックを門で押さえて中間状態を確定させる形になっている。Android `PB_TR_21` / iOS `PB-TR-21` は「門を開けるまで出現中のまま」「開ける前に退出フックが 0 回・開けた後に 1 回」という、直したい欠陥 (出現完了を待たずに退出する) が真なら必ず落ちる検査になっており、即完了のダブルで素通りする余地もない (出現フックは門が開くまで完了しないため)。本体は無改変、Scenario ID は据え置きで網羅検査も通り、lint 5 種と全件テスト (iOS 277 / Android instrumented 333) も手元で通る。

ただし iOS `LD-CO-13` の「出の途中に差し込まれた開始である」ことの固定だけは、Android 版の `CoroutineStart.UNDISPATCHED` に相当する押さえを持っていない。コメントと evidence が主張している「実行機の速さに依らず定まる」は成立しておらず、順序がずれた回は黙って通り抜ける (Major 1 件)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | always (コメント構文を持つファイルを書く) |
| cross/ci-flaky-test-policy.md | 状態遷移を観測するテストの書き換え・CI の間欠失敗の切り分け・skip の要否判断 |
| cross/test-execution.md | 全件実行と件数の確認 (完了判定) |
| cross/verification-ci.md | CI の失敗の切り分け・ios job の並列スイート飢餓の見分け |
| cross/runtime-behavior-verification.md | 実行時挙動 (出入りの演出のタイミング) が絡む修正の完了判定 |

`kasane/lessons/code-review.md` L-001 (受け入れ条件・証跡手順の弁別力)、`kasane/lessons/process.md` L-001 (姉妹面の照合) も適用した。`lint.ci-skip.allow` の追加は diff に無く、正規の印以外の無効化手段 (`@Ignore` / 素の `.disabled(...)`) も入っていない (`scripts/ci-skip-lint.py` 印 0 件 / 許可リスト 0 件)。

## 手元の実行結果

| ルート | 結果 |
|---|---|
| ios (`xcodebuild test -scheme KsDialogs`、スイート直列) | 277 tests / 50 suites・失敗 0 |
| ios (既定の並列) | 6 件失敗。すべて `waitForPresentedContainers` / `waitUntilPresenting` の提示待ち時間切れ (DialogPresentationSizing・DialogInlineShow・DialogSwiftUIContent・ToastTransition・ToastContract・ToastSwiftUIContent)。verification-ci.md が定義する並列スイート飢餓の落ち方そのもので、本 diff とは無関係 (load average 22〜56 の検証機。本 diff が触る 3 スイートはこの回も全件通過) |
| android instrumented (`connectedDebugAndroidTest`、API 36 AVD・アニメーション 3 種 0) | `:ksdialogs-core` 294 件 (skipped 1 = `PB_SB_04` の `assumeTrue`・失敗 0) / `:ksdialogs` 39 件 (失敗 0) |
| lint | `scenario-id-coverage.py` 未網羅なし / `ci-skip-lint.py` 違反なし / `comment-policy-lint.py` 禁止 0 件 / `local-path-lint.py` / `identity-lint.py` / `install-example-lint.py` すべて違反なし |

## 指摘事項

### [🟠 Major] iOS LD-CO-13 の「出の途中に差し込まれた開始」は実行機の速さに依らずには固定できていない

**該当箇所**: `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:359-369`

**問題点**:

`LoadingTestCallStartRecorder` が押す印は「別仕事の本体が走り出した」ことだけで、「その開始が進行中の撤去の後ろに並んだ」ことではない。両者の間には executor の乗り換えが挟まる。

- `harness.loading.show()` (`Loading.show(message:placement:)`) は `Loading` が MainActor 隔離でない nonisolated async メソッドで、その本体は呼び出し元の MainActor を離れて協調プールで走る (手元で確認: `@MainActor` から nonisolated async を呼ぶと callee 側は main スレッドではない)。撤去の後ろに並ぶのは、そこからさらに MainActor へ戻って `LoadingCoordinator.beginUse` の先頭 `waitForPendingDismissal()` に入った時点。
- 一方 `DialogTestWaiting.waitUntil` も MainActor 上の 5 ms ポーリングで、`markStarted()` の直後に MainActor が解放された時点から観測できる。つまり `hasStarted` を観測できた時点で `beginUse` がまだ MainActor の順番待ちにいる回があり得る。その回は `dismissalGate.open()` が先に走り、新しい開始は「撤去が完了した後」から始まる。
- 直後の `try #require(harness.coordinator.isDismissing, "前提: 新しい開始が走り出した時点でまだ出の途中にいる")` はこのずれを検出できない。門を閉じている間 `dismissalTask` は必ず残っているので、開始が並んでいてもいなくても真になる。
- 後続の検査 (`secondContainerView !== firstContainerView` / `contentView !== firstContentView` / `superview == nil` / `attachedContainerViews.count == 1` / `builtinText == nil`) は、撤去完了後に始まった回でもすべて成立する。したがってずれた回は失敗ではなく**黙って通る** (検出力の喪失)。

Android 版 (`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:377-381`) は同じ問題を `CoroutineStart.UNDISPATCHED` で解き、「要求が受理列に載って中断するまでこの行で進むため、次の行の解放より先に並ぶ」とコメントに明示している。iOS 版は同じ保証をコメントで主張しながら機構を持っておらず、姉妹面として同形になっていない (`kasane/lessons/process.md` L-001 / `kasane/lessons/code-review.md` L-001)。

補足 (実測): 同じ形を最小再現したプログラムでは、負荷をかけた回を含め 18/18 で `beginUse` 到達のほうが先だった。実害が出る確率は低いが、押さえは何も無く、ずれた回は観測できないまま通る。

**推奨修正**: 印から受理列に載るまでの間に executor の乗り換えが起きない経路で差し込む。たとえば `Loading.show()` が 1 行で委譲している先の MainActor 隔離の入口を直接呼べば、`Task { @MainActor in ... }` の本体は `waitForPendingDismissal()` の中断点まで同期に進むため、`hasStarted` の観測が「受理列に載った後」であることを保証できる (公開入口を通らなくなる点はコメントに書く)。公開入口のまま直すなら、順序がずれた回に偽になる観測 (撤去完了後に始まった回では成り立たない検査) を代わりに置くこと — 現在の `isDismissing` の `#require` はその役を果たしていないので、置き換えるか削るのが望ましい。

### [🟡 Minor] evidence の「検出力の根拠」が、確かめられていないことを確かめたと書いている

**該当箇所**: `evidence/ci-flake-triage.md:52-54`

**問題点**: 「版 B の iOS `LD-CO-13` には、新しい開始が走り出した時点でまだ出の途中にいること (`coordinator.isDismissing`) を前提として固定する検査を置いた。10 回の反復すべてでこの前提が成立しており、差し込みが撤去完了後にずれていないことを実測で確かめている」とあるが、上の Major のとおり `isDismissing` は門を閉じている限り常に真で、ずれた回でも成立する。10 回成立したことは「ずれていない」の根拠にならない。`kasane/lessons/code-review.md` L-001 が求める「その機構を丸ごと外しても同じ観測が得られないか」を満たしていない記述。

**推奨修正**: Major の対処に合わせて書き直す。押さえを入れたなら「何が順序を固定しているか」を書き、入れないなら「差し込み位置は固定できておらず、ずれた回は通る」と限界として書く (`kasane/lessons/process.md` L-002 の要領で、実証した範囲と対象外の面を分けて書く)。

### [🔵 Suggestion] PB-TR-21 の姉妹 2 面が完全には同形でない

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:392-396`

**問題点**: Android 版は報告と同じメインスレッドブロックで状態を読み取り (`stateAtReport`)、「出現中はまだ退出しない」を報告時点で 1 回、待ちの後にもう 1 回主張する。iOS 版は待ちの後の 1 回だけ。判定の要になるのは待ちの後の読みなので実害はないが、姉妹面を同形に保つなら `complete(true)` の直後にも `#expect(container.containerState == .presenting)` を置くと読み比べやすい。

**推奨修正**: 任意。合わせないなら現状のままでよい。

## 確認した観点 (指摘なし)

- **本体無改変**: 作業ツリーの差分は `android/**/src/androidTest/**`・`ios/Tests/**` と `kasane/**` のみ。`android/**/src/main/**`・`ios/Sources/**` に変更なし
- **欠陥の検出力 (PB_TR_21 両面)**: 出現フックを門で押さえたまま閉鎖を報告し、(1) 器が出現中のままであること (2) 退出フックが 0 回であること を主張したうえで、門を開けてから結果の配送・REMOVED・退出フック 1 回を確かめている。「出現完了を待たずに退出する」実装ならいずれかが必ず落ちる。出現フックは門が開くまで完了しないので、即完了のダブルで素通りすることもない
- **時限に頼った「起きない」確認**: Android の `delay(NO_REACTION_WAIT_MILLIS)` (400 ms) と iOS の `Task.sleep(150 ms)` はどちらも門で状態を止めた後の追加確認で、待ち時間が短いほど検出が甘くなる方向にしか効かない (偽の赤にはならない)。iOS `LD-CO-13` の待ちも `probe.callCount(.dismissal) == 1` という、門を開けるまで覆らない述語に変わっており、時限依存は残っていない
- **Scenario ID と題名**: ID は `PB-TR-21` のまま。`scripts/scenario-id-coverage.py` は「未網羅なし」。`kasane/concepts/core/api/transition-semantics.md:78` の PB-TR-21 の文 (両方が終わって初めて表示中) は変えておらず、覆いのフェードが実時間で終わってもなお出現中であり続けることを新しいテストが押さえるため、概念との乖離は生じていない
- **LD-CO-13 の最終検査の弁別力**: 第 1 世代をカスタム View + メッセージ合流にしたことで `builtinText == nil` が「引き継がない」を実際に見分けられる形になっている (合流が成立していないと引き継ぐ元が無く素通りする、という注記どおり)。`coalescedUseCount == 2` の前提確認も付いている
- **evidence の切り分け手順**: 検証機の負荷の実測 → 落ちた assertion が中間状態を読んでいることの確認 → 手元反復 (Android 対応前 5/10 失敗・対応後 30/30 成功、iOS 各 10 回) → 「手元で落ちるなら直す」への当てはめ、の順で ci-flaky-test-policy の 4 段を満たしている。Android 版は版 A / 版 B の A/B まで取れており、runtime-behavior-verification.md の 3 条件も満たす。CI 限定 skip は候補にせず、`lint.ci-skip.allow` の変更も無い
- **コメント規約**: 新規コメントに作業文書のパス・変更識別子・ローカル通番・進捗ログは無く、`comment-policy-lint.py` も 0 件
- **scope**: `kasane/relations/KsSettingsView.md` の 1 行追記は exploration.md の決定事項 (KsSettingsView からの知らせを却下・台帳記録) に対応するもので、合意済みスコープ内
- **不要な抽象化**: 追加した `LoadingTestCallStartRecorder` は同ファイルの既存の観測子と同じ粒度で、汎用化の先取りは無い (ただし Major の対処によっては不要になりうる)

## アクションプラン

1. (Major) `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift` の差し込み位置の固定を、executor の乗り換えを挟まない形にするか、ずれた回に偽になる観測へ置き換える。`isDismissing` の `#require` は現状ずれを検出できないので、置き換えるか削る
2. (Minor) 1 に合わせて `evidence/ci-flake-triage.md` の「検出力の根拠」を書き直す (押さえの機構、または固定できていない限界)
3. (Suggestion) 任意で iOS `PB-TR-21` に報告直後の状態主張を足し、Android 版と同形にする
4. 修正後は iOS を**スイート直列** (`-parallel-testing-enabled NO`) で全件、Android instrumented を全件回して件数を添えて報告する (既定の並列で出る提示待ちの時間切れは並列スイート飢餓であり、本変更とは無関係)
