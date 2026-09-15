# レビュー結果: fix-transition-intermediate-state-observation (002 回目)

**日付**: 2026-09-15
**判定**: APPROVED

## サマリー

001 回目の 🟠 Major (iOS `LD-CO-13` の差し込み位置が固定できていない) は解消した。差し込みを UI スレッド隔離の受理口 (`LoadingCoordinator.beginUse`) の直接呼び出しへ付け替えたことで、「印を観測できた = 撤去待ちの列に載り終えた」が実行機の都合ではなく構造で決まる形になっている。本体のコードで中断点を追って確かめた (下記)。🟡 Minor (evidence の検出力の根拠) と 🔵 Suggestion (PB-TR-21 の同形化) も対応済み。Android 側と PB_TR_21 両面に退行はない。

残るのは、evidence が記録している手元の全件実行の条件が handbook の「手元は並列のまま」と食い違っている点だけで、これは 001 回目のアクションプランで当方が直列を指示したことに起因する (低優先度の Minor 1 件)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | always |
| cross/ci-flaky-test-policy.md | 状態遷移を観測するテストの書き換え・CI の間欠失敗の切り分け |
| cross/test-execution.md | 全件実行と件数の確認 (完了判定) |
| cross/verification-ci.md | ios job のスイート直列化と手元の実行条件・並列スイート飢餓の見分け |
| cross/runtime-behavior-verification.md | 実行時挙動が絡む修正の完了判定 |

`kasane/lessons/code-review.md` L-001・`kasane/lessons/process.md` L-001 / L-002 も適用した。

## Major の解消の確認 (本体コードでの裏取り)

主張は「印を押した仕事が撤去待ちの中断点まで UI スレッドを手放さず進む」こと。`ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift` を読んで、印から最初の中断点までに別の中断点が無いことを確かめた。

| 位置 | 内容 | 中断するか |
|---|---|---|
| `LoadingCoalescingTests.swift` の `Task { @MainActor in ... }` 本体 | `queueing.markStarted()` | しない (同期) |
| `LoadingCoordinator.swift:33` | `LoadingCoordinator` は `@MainActor final class` で `beginUse` も隔離されている。MainActor の仕事から呼ぶので executor の乗り換えが起きない | しない |
| `LoadingCoordinator.swift:125` | `beginUse` の**先頭の文**が `await waitForPendingDismissal()`。この前に文は無い | しない (同一 executor) |
| `LoadingCoordinator.swift:150-154` | `waitForPendingDismissal` は `while let dismissalTask { await dismissalTask.value }` の 1 文のみ | `await dismissalTask.value` で中断する |

`dismissalTask` がこの時点で非 nil であることも確かめた。`finishDisplay` (`LoadingCoordinator.swift:210-224`) は `Task { @MainActor in await container.dismiss(); self.completeDismissal() }` を作った**直後の同期文**で `dismissalTask = task` を代入し、その間に await が無い。撤去仕事の本体 (=出のフックを走らせる側) は UI スレッドが空くまで動けないので、テストが `probe.callCount(.dismissal) == 1` を観測できた時点では必ず `dismissalTask` が入っている。`dismissalTask = nil` に戻すのは `completeDismissal()` で、これは門が開いて `container.dismiss()` が戻った後 (`LoadingCoordinator.swift:227-231`)。

したがって `queueing.hasStarted` が観測できた時点で、新しい開始は `await dismissalTask.value` で待ちに入り終えている。`dismissalGate.open()` はその後なので、「出の途中に差し込まれた開始」であることが実行機の速さに依らず定まる。001 回目に指摘した「印を観測できた時点でまだ列に載っていない回」は、印と受理口の間の executor の乗り換えが消えたことで存在しなくなった (旧経路 `Loading.show(message:placement:)` が隔離されていない async メソッドで本体が UI スレッドを離れることは、001 回目に最小再現で確認済み)。

## 指摘事項

### [🟡 Minor] evidence が記録する手元の全件実行の条件が handbook と食い違っている

**該当箇所**: `evidence/ci-flake-triage.md` の「手元の実行条件」表の iOS 行と「全件の実測 (版 B)」表の ios 行

**問題点**: どちらも `-parallel-testing-enabled NO` を付けた実行として記録されている。`kasane/handbook/cross/verification-ci.md`「CI の Swift テストはスイートを直列で回す」は、直列化は CI 側の実行条件であって**手元の実行条件は変えない (並列のまま)** と定めている。理由を書かずに直列の数字だけを残すと、この記録が「手元も直列で回す」の先例として読まれる。

なお、この直列化は 001 回目のアクションプラン 4 で当方が指示したものであり、実装側の判断ではない。指示のほうが handbook と食い違っていた。当方の再レビューでは、同じ検証機で**並列のまま**全件が 277 tests / 50 suites・失敗 0 (11.6 秒) で通ることを実測した。001 回目に並列で 6 件落ちたのは、当時 iOS と Android instrumented を同時に走らせて検証機を飽和させていたため (load average 22〜128) で、verification-ci.md の見分け表どおり並列スイートの飢餓に当たる。

**推奨修正**: 「手元の実行条件」表と「全件の実測」表の ios 行を並列 (既定) の実行に戻して数字を取り直すか、直列で測った理由 (検証機の負荷) を 1 行添えて、手元の標準条件を変えたわけではないと分かる形にする。反復のほうは絞り込み実行なので、直列で回したことを条件として書いてあれば問題ない。

### [🔵 Suggestion] 差し込みの固定は本体側の構造に依存していて、崩れても検査は落ちない

**該当箇所**: `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:353-370`

**問題点**: 今の固定は「`beginUse` が UI スレッド隔離であること」と「その最初の中断点が撤去待ちであること」に依存している。将来この 2 つのどちらかが崩れると (受理口の隔離が外れる・`beginUse` の先頭に別の await が入る)、テストは落ちずに「撤去完了後に始まった開始」を検査する形へ静かに戻る。前提はテストのコメントと `LoadingTestCallStartRecorder` の doc コメントに書かれており、現時点で追加の手当ては不要。

**推奨修正**: 対応不要。蒸留時に「この Scenario は受理口が UI スレッド隔離であることに依存する」ことが concepts / 実装乖離メモのどこかに残ると、将来の読み手が気づける。

## 前回指摘への対応状況

| 前回 | 判定 |
|---|---|
| 🟠 Major: LD-CO-13 の差し込み位置 | **解消**。差し込みを隔離された受理口の直接呼び出しへ付け替え。非弁別な `#require(harness.coordinator.isDismissing)` も削除されている |
| 🟡 Minor: evidence の検出力の根拠 | **解消**。「固定しているのは反復回数ではなく差し込みの経路」と書き直され、旧経路では「失敗せずに通り抜ける」ことまで明記されている (lessons code-review L-001 の求める「機構が無いと何が変わるか」を満たす)。公開入口を通らなくなる守備範囲の狭まりも節を分けて書かれており、lessons process L-002 (実証した範囲と対象外を分ける) にも沿う |
| 🔵 Suggestion: PB-TR-21 の同形化 | **対応**。`complete(true)` の直後に `#expect(container.containerState == .presenting, "出現中はまだ退出しない")` が入り、Android 版の `stateAtReport` と同じく報告時点と待ちの後の 2 回主張する形になった |

## 確認した観点 (指摘なし)

- **守備範囲から外れる面**: 公開入口 `Loading.show()` → 受理口の委譲は `LD-CO-01` (`ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:21`) が `await harness.loading.show()` で押さえており、ほかにも `LoadingAttributeTests` / `LoadingStyleTests` / `LoadingTransitionTests` を含め 11 箇所で踏まれている。`LD-CO-13` 自身も第 1 世代の表示・メッセージ合流・`hide()` は公開入口のままで、付け替えたのは差し込みの 1 回だけ。evidence にもその旨が独立した段落で書かれている
- **本体無改変**: 差分は `android/**/src/androidTest/**`・`ios/Tests/**`・`kasane/**` のみ。`ios/Sources/**`・`android/**/src/main/**` に変更なし
- **Android 側と PB_TR_21 の退行**: `android/**` の差分は 001 回目と同一 (`git diff -- android` の内容が一致)。再実行でも `PB_TR_21` / `LD_CO_13` とも失敗なし。iOS `PB-TR-21` は主張が 1 つ増えただけで、門で出現を押さえる構造と `dismissal` フックの回数検査は変わっていない
- **`LD-CO-13` の弁別力**: 第 1 世代をカスタム View + メッセージ合流にしたことで `builtinText == nil` と `contentView !== firstContentView` が実際に見分けられる形は維持されている。`coalescedUseCount == 2` の前提確認もそのまま
- **`beginUse` 直接呼び出しの副作用**: `.builtin` / `message: nil` / `placement: nil` は `Loading.show()` が渡す組と同じ。`activeCount` は 1 のまま残るが、末尾の `await harness.loading.hide()` が世代ごと閉じるので後始末は成立している。`try?` で握り潰す誤りも `.builtin` では起きない (コンテンツ解決の失敗が無い)
- **コメント規約**: 追加・改稿したコメントに作業文書のパス・変更識別子・ローカル通番・進捗ログは無い。`LoadingTestCallStartRecorder` の doc コメントは「印が何を示さないか」まで書かれており、単独で意味が通る
- **lint**: `scenario-id-coverage.py` 未網羅なし / `ci-skip-lint.py` 印 0 件・許可リスト 0 件 / `comment-policy-lint.py` 禁止 0 件 / `local-path-lint.py` / `identity-lint.py` 違反なし
- **不要な抽象化・スコープ逸脱**: なし。`exploration.md` と `kasane/relations/KsSettingsView.md` の差分は 001 回目から変わっていない

## 手元の実行結果

| ルート | 結果 |
|---|---|
| ios (既定の並列 — handbook の手元条件) | 277 tests / 50 suites・失敗 0 (11.6 秒) |
| ios (スイート直列) | 277 tests / 50 suites・失敗 0 (88.3 秒)。`PB-TR-21` / `LD-CO-13` とも通過 |
| android instrumented (`connectedDebugAndroidTest`、API 36 AVD・アニメーション 3 種 0) | `:ksdialogs-core` 294 件 (skipped 1 = `PB_SB_04` の `assumeTrue`・失敗 0) / `:ksdialogs` 39 件 (失敗 0)。`PB_TR_21` / `LD_CO_13` とも通過 |

検証に使った Simulator と AVD インスタンスはこの再レビューのために新規に起こし、実測後に削除・停止した。接続中の実機は使っていない。

## アクションプラン

1. (Minor・蒸留前で足りる) `evidence/ci-flake-triage.md` の iOS 行を並列 (既定) で取り直すか、直列で測った理由を添える
2. (Suggestion・任意) 蒸留時に「`LD-CO-13` の差し込みは受理口が UI スレッド隔離であることに依存する」ことを残す
