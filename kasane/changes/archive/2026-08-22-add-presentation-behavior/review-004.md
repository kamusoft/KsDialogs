# レビュー結果: add-presentation-behavior (004 回目)

**日付**: 2026-08-22
**判定**: APPROVED

対象: オーナー差し戻し (iOS のスライド系プリセットの入場で最終位置のカードが 1 フレーム見える) の修正確認。
成果物・deviation.md・証跡のみで判断した。

## サマリー

修正は狙いどおり効いている。`beginPresentation()` から同期の alpha 復帰を外し、進行タスクの中で「alpha 復帰 → presentation フックを直接 `await`」を同一同期区間に置く形は、design Decision 10 の「表示とフック開始が同時」を iOS 17 の制約下で実現できる唯一に近い選択で、代替案 (`Task.immediate` / `layer.opacity`) の却下理由もいずれも成立している。実機 A/B 証跡は 1 フレーム単位で症状の再現と解消を示しており、実行時挙動の検証規約の 3 条件を満たす。関連経路 (PB-TR-05 / 19 / 20 / 23 / 28) はいずれも壊れておらず、全ビルド・全テストが green。

指摘は **Minor 1 件 / Suggestion 3 件**。いずれも本修正の是非を覆すものではない。Minor は「状態 `presenting` に入る時点」と「ホスト View が表示される時点」が本修正で 1 メインアクターターンだけずれたことによる、狭い競合窓の契約ずれ。

## 自分で実行した検査

| 検査 | コマンド | 結果 |
|---|---|---|
| iOS 全テスト | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=F899B356-…'` (iOS 26.5) | **134 tests / 25 suites / 0 failures**、warning 0 件 |
| 新回帰テストの安定性 | 上記を `-only-testing:KsDialogsTests/DialogTransitionTests` で 5 連続 | 5/5 green (32 tests、11.0 秒前後で揺れなし) |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` | 合計 0 ファイル / 禁止 0 件 (検査対象 543 ファイル) |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` | 58/61 (Sample 3 件は除外)、未網羅なし |
| MAUI iOS ブリッジ | `xcodebuild -project maui/macios/native/KsDialogsMauiBridge.xcodeproj …` | **BUILD SUCCEEDED** |
| 証跡の実物確認 | `verification/first-frame-ios/` の before/after PNG を目視 | README の記述と一致 (下記) |

証跡の目視結果 — `before/slide-up-f0002-6.462-FLICKER-final-position.png` は覆いのないまま**カード (「トランジションのデモです」/ キャンセル・OK) が最終位置に不透明で 1 フレーム描かれて**おり、報告された症状そのもの。`after/slide-up-f0004-7.740-no-card.png` / `f0005` にはカードが一切無く、最初にカードが現れる `after/slide-up-f0010-7.838-entering-from-bottom.png` では画面下端から上辺だけが覗いている。最終位置のフレームは after 側に 1 枚も無い。**A/B として成立している**。

## 依頼された 5 点への評価

### (1) 同一アクター上の直接 `await` への依存と、`async let` の覆いフェードとの順序

**妥当と判断する。** 依存している性質は「呼び出し元と呼び出し先の isolation が同じ `async` 呼び出しでは、最初の中断点までが同じ同期区間で実行される」こと。`DialogTransition.Hook` は型自体が `@MainActor` (`Contract/DialogTransition.swift:19`) なのでコンパイラが isolation を静的に知っており、実行時のエグゼキュータ切り替えも同一エグゼキュータでは即時継続になる。`runPresentationPhase()` の中で `contentView.alpha = contentInitialAlpha` (行 435) と `await runHook(...)` (行 436) の間に他の中断点が無いことも読んで確認した。さらに `beginPresentation()` の取り消し済みガード (行 415) から alpha 復帰までも同じ同期区間に入っているため、**「ガードは通ったが復帰の直前に取り消された」という隙間も無い**。

覆いのフェードの 1 ターン遅れは実害なしと判断する。`overlayView.alpha` は生成時に 0 (行 63) で、`async let` の子タスクが動き出すまで見えるものが変わらない。`runPresentationPhase()` は `_ = await overlay` (行 437) で両方の完了を待つので、PB-TR-21 (none で覆いの出現完了まで presenting に留まる) の契約も維持されている — テストも green。

ただしこの性質は言語仕様として明文化された保証ではない (Suggestion 1 参照)。

### (2) 各経路が壊れていないか

読解と実行の両方で確認した。壊れていない。

- **PB-TR-05** (presenting 中の報告): `handleSettled()` は `.presenting` で何もせず (行 577)、進行タスクが `runPresentationPhase()` 完走後に `.shown` へ進めてから `beginDismissal()` を呼ぶ (行 417-422)。直列化は維持。
- **PB-TR-28** (presenting 中の呼び出し元キャンセル): `handleCallerCancellation()` の `.presenting` → `beginDismissal()` (行 555-557)。`beginDismissal()` が `lifecycleTask?.cancel()` する経路は変わらず、フックが親タスク側で走る形になっても `DialogTransitionAnimator.run` の `withTaskCancellationHandler` / `DialogTransitionGate.wait()` の取り消し観測はそのまま効く。テストは `.started(.presentation)` を待ってから取り消すため、新設ガードの影響を受けない。
- **PB-TR-19** (提示前の報告): `beginLifecycle()` が `resultChannel.isResultSettled` を見て `scheduleImmediateRemoval()` へ分岐する経路 (行 388-392) は無改変。中身は alpha 0 のまま撤去される。
- **PB-TR-20** (提示前の呼び出し元キャンセル): `.created` / `.attached` の扱いは無改変。
- **PB-TR-23** (提示中の OS 発器消失): `handleHostLost()` が `lifecycleTask?.cancel()` → `settle(.cancelled, origin: .hostLost)` → `finishRemoval(waitsForHostRemoval: false)` (行 589-595)。進行タスクの取り消しでフックが取り消されることは変わらない。
- 状態機械の取りこぼし確認: 「取り消し済みなのに `.presenting` のまま残る」経路が無いことを全 cancel 元 (`beginDismissal` / `finishRemoval` / `handleHostLost`) から辿って確認した。いずれも cancel の前後で状態が `.dismissing` か撤去へ進むため、ガードの早期 return で状態機械が止まることはない。
- 中身を見せる箇所が 1 か所に集約されていること (`grep contentView.alpha` の結果が 118/212/214/435 のみ) も確認した。

### (3) 回帰テストの MainActor FIFO 依存

**実用上の脆さは確認できなかった。** 5 連続実行で 5/5 green、実行時間の揺れも無し。依存しているのは「メインアクター (= main dispatch queue) のジョブが投入順に実行されること」で、`layoutInWindow()` が同期的に `beginPresentation()` まで到達すること (直前の `#expect(contentView.alpha == 0)` がその前提を見張っている) と併せて成立している。前提が崩れた場合は `alpha == 0` の期待が落ちるので、**空振りで通り抜ける形にはなっていない**。

一方でこのテストが直接固定しているのは「メインアクターのターンの切れ目が無いこと」であって「フレームが描かれないこと」ではない。フレーム級の性質は実機 A/B 証跡側が担保しており、2 つ合わせて必要十分と判断する (テスト単体では過剰に厳しい方向の近似なので、偽陰性ではなく偽陽性側に倒れる)。

### (4) 既存テスト 2 本の待ち合わせ追加

**検査は弱まっていない。** `DialogOutsideTapTests` の 2 本は、追加されたのが `try #require(await DialogLayoutMeasurement.waitUntilContentIsVisible(...))` という**前提の明示**だけで、hitTest の経路と assertion は一切変わっていない (`stage.container.view.hitTest(...)` の結果と `isOutsideTap(touching:)` の判定)。待ちが空振りすれば `#require` でテストが落ちる。ただし待ち条件の選び方に将来の脆さがある (Suggestion 3 参照)。

### (5) `Task.immediate` を採らなかった判断

**妥当。** `ios/Package.swift:9-11` の `platforms: [.iOS(.v17)]` を確認した。`Task.immediate` は iOS 26.0+ であり、`if #available` で分岐すれば OS 版で「1 フレーム見える / 見えない」が割れる — 挙動が OS で割れる形は契約として採れないという判断に同意する。`layer.opacity` 案の却下理由 (`UIView.alpha` と同一記憶域なので hitTest が中身を返さなくなる) も事実として正しい。

## 指摘事項

### [🟡 Minor] `presenting` に入る時点とホスト View が表示される時点が 1 ターンずれ、spec の「提示開始前」の定義から外れる

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:409-424`

**問題点**:

`beginPresentation()` は `containerState = .presenting` を**同期的に**立ててから進行タスクを積む。中身を見せるのはそのタスクの中 (行 435) なので、**タスクが走り出すまでの 1 メインアクターターンだけ「状態は `presenting` だがホスト View はまだ表示されていない」**区間ができる。

デルタスペック `specs/dialog-contract/spec.md` の Requirement「退出の開始条件と直列化」は、この境界を状態名ではなく表示の有無で定義している — 「提示開始前 (**ホスト View が表示される前**) に閉鎖信号が来た場合は、両フックとも実行せず演出なしで撤去・配送する」。design Decision 10 も presenting を「オーバーレイのフェード開始と同時にホスト View を表示し presentation フックを開始」と定義しており、状態と表示は同時であることが前提になっている。本修正前は alpha 復帰が `presenting` 遷移と同じ同期区間にあったためこの前提が成立していたが、修正後は成立しなくなっている。

観察できる条件 (いずれもこの 1 ターンの窓に閉鎖信号が入った場合):

- **呼び出し元キャンセル** → `handleCallerCancellation()` の `.presenting` 分岐 (行 555-557) が `beginDismissal()` を呼ぶため、**一度も表示されていない中身に対して利用者の dismissal フックが実行される**。spec PB-TR-20 の THEN 「両フックとも呼ばれない」に対する差分であり、配送も退出の演出時間 (既定 250ms) だけ遅れる
- **報告 (completed / cancelled)** → `handleSettled()` の `.presenting` 分岐 (行 577) は何もしないため、**表示前に確定していたにもかかわらず presentation が最後まで走ってから退出**する (PB-TR-19 の「演出なしで撤去」ではなく PB-TR-05 の直列化として扱われる)

`DialogResultChannel` は「任意のスレッドからの報告を受け付け」る設計 (`Contract/DialogResultChannel.swift:3-4`) なので、この窓は理屈上の話ではなく、別スレッドからの `notifier.complete(...)` / `Task.cancel()` で到達し得る。ただし窓は 1 ターンで、結果 (outcome) の正しさは全経路で保たれるため、実害は「見えないダイアログに対して利用者フックが 1 回走る」「配送が 250ms 遅れる」に留まる。現行テストはこの窓を突く構成になっていない (PB-TR-28 / PB-TR-23 はいずれもフック開始を待ってから信号を送る)。

**推奨修正**:

`containerState = .presenting` を進行タスクの中へ移し、中身を見せる直前に立てる。これで「`presenting` ⇔ ホスト View 表示済み」が再び同値になり、窓に入った信号は `.attached` として扱われて spec どおり「演出なしで撤去・配送」になる。

```swift
private func beginPresentation() {
    lifecycleTask = Task { @MainActor [weak self] in
        guard let self else { return }
        guard !Task.isCancelled, self.containerState == .attached else { return }
        self.containerState = .presenting
        await self.runPresentationPhase()
        …
    }
}
```

`handleSettled()` / `handleCallerCancellation()` の `.attached` 分岐はどちらも `finishRemoval()` (フックなし・演出なし) なので、この窓の扱いはそのまま spec に一致する。`scheduleImmediateRemoval()` は既に確定済みの場合しか走らないため干渉しない。既存 Scenario テストで `.presenting` を同期的に読んでいるものは無い (PB-TR-21 は報告後に読むためタスク実行後)。**変更後に全テストの再実行で確認すること**。

判断が割れるようなら NEEDS_DISCUSSION 相当の論点として持ち越しても構わない (「状態名と表示の一致をどこまで契約とみなすか」の解釈問題であり、outcome の契約は現状でも守られている)。

### [🔵 Suggestion] 依存している「同一 isolation の直接 await は中断しない」性質を蒸留時に明文化する

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:426-438` / `kasane/decisions/core/0017-animation-hooks-transition-attachment.md`

**問題点**: この修正の正しさは、言語仕様として明文化されていない実装依存の性質 (同一エグゼキュータへの切り替えが即時継続になること) に乗っている。コードコメントは「同じ同期区間に置く」と意図を書いているが、**なぜそれが成り立つのか (フックの型が `@MainActor` であること)** までは書かれていないため、将来フック型の isolation を変える改修が入ったときに前提が静かに崩れる。ADR-0017 は本文に状態機械・表示タイミングを持たないので、蒸留で design Decision 10 を統合する際にこの前提が落ちる恐れがある。

**推奨修正**: (a) `runPresentationPhase()` のコメントに「フック型が `@MainActor` で宣言されているため直接 `await` で中断が入らない」という成立条件を 1 文足す、(b) 蒸留時に ADR-0017 (または Decision 10 の統合先) へ「表示とフック開始の同時性は iOS 17 制約下では isolation の一致に依存する / `Task.immediate` は最低対象 OS 引き上げ後の代替」として残す。回帰テストが番人として既にあるので、優先度は低い。

### [🔵 Suggestion] 回帰テストに Scenario ID が無いことを網羅検査の除外として明示しなくてよいか

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:85`

**問題点**: 新しい回帰テストは PB-xx-NN を持たない (spec が UI lint でアニメーション/フレームの記述を禁じているため Scenario 化できず、これ自体は正しい)。`scripts/scenario-id-coverage.py` は「ID → テスト」方向の検査なので落ちないが、**この 1 本が何を守っているのかは spec からは辿れない**状態になる。同じ性質の非 ID テスト (「器が解放されてもラッチ済みの結果は配送される」等) が既にあるので新しい問題ではない。

**推奨修正**: 蒸留時、design Decision 10 を ADR へ統合する本文に「ちらつき防止の固定はデルタスペックではなく回帰テストが担う」旨を 1 行残す。コード側の対応は不要。

### [🔵 Suggestion] `waitUntilContentIsVisible` の待ち条件が「既定トランジションが中身を動かさない」ことに暗黙依存する

**該当箇所**: `ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift:71-73` / `ios/Tests/KsDialogsTests/DialogOutsideTapTests.swift:57,84`

**問題点**: 待ち条件が `contentView.alpha > 0` なので、待ちが解けた時点では**出現の演出がまだ走っている最中**になる。現在の既定が fade プリセット (transform を触らない) なので `contentView.frame` は最終位置と一致し、`tapOnContentDoesNotSettleCancelled` が `frame.midX / midY` から作る内側の点も正しく当たる。しかし既定が slide 系に変わる、あるいは既定 fade の実装が transform を併用する形になると、**演出の途中の transform が `frame` に反映されて内側の点が中身から外れ、2 本が突然落ちる**。落ち方は明示的なので黙って弱くなるわけではないが、原因が分かりにくい。

**推奨修正**: 2 本の待ちを「演出の完了 (`containerState == .shown`)」まで進めると、既定トランジションの中身に依存しなくなる。既定 250ms × 2 本の実行時間増と引き換えなので、採否は実行時間の許容次第。

## アクションプラン

1. **Minor 1 件の扱いを決める** — 推奨修正 (`containerState = .presenting` を進行タスク内へ) を入れるか、「状態名と表示時点の一致は契約外」としてオーナー判断で見送るかを決める。入れる場合は iOS 全テストの再実行まで
2. **Suggestion 1** — `runPresentationPhase()` のコメントに成立条件を 1 文追加 (コスト極小)。ADR への反映は蒸留フェーズへ
3. **Suggestion 2 / 3** — いずれも蒸留フェーズ・後続の判断で足りる。本修正のブロッカーではない
