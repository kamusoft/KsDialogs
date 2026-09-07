# レビュー結果: add-presentation-behavior (005 回目)

**日付**: 2026-08-22
**判定**: APPROVED

対象: review-004 / second-opinion-code-004 の突き合わせで確定した指摘 **#1 (Major)** / **#4** / **#6** の修正確認 (iOS のみ)。
成果物・deviation.md・前回レビューだけで判断した (実装者の経緯報告は受け取っていない)。

## サマリー

**#1 / #4 / #6 はいずれも解消**。`.presenting` への遷移を進行タスクの最初の同期区間へ移し、その手前で「取り消し済み・`.attached` でない・結果確定済み」の3条件を最初の中断点より前に見る形は、Decision 10 の遷移表の created / attached 行 (両フック非実行・演出なしで removed) を状態機械の上で正しく閉じている。窓に入った閉鎖信号がどちらの順で処理されても二重撤去・二重配送が起きないことを、`isRemovalRequested` / `didCompleteRemoval` の二重の冪等ガードから辿って確認した。新規回帰テスト2本は修正前のコードで**2つの独立した assertion が落ちる**形で、検出力は十分。全ビルド・全テスト green。

指摘は **Minor 2 件 / Suggestion 1 件**。いずれも本修正の是非を覆すものではなく、実行時の挙動を変える必要のあるものは無い。Minor はどちらも「守られている契約に対して、証跡 (テスト / deviation 記録) の側が実装より狭い」型。

## 自分で実行した検査

| 検査 | コマンド | 結果 |
|---|---|---|
| iOS 全テスト | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=F899B356-…'` (iOS 26.5) | **136 tests / 25 suites / 0 failures** (`** TEST SUCCEEDED **`)、warning 0 件 |
| 新規2本の安定性 | 上記を `-only-testing:KsDialogsTests/DialogTransitionTests` で 3 連続 | 3/3 green (34 tests、10.9〜11.0 秒で揺れなし)。新規2本は 0.002〜0.011 秒で安定 |
| 退行対象の個別確認 | 同ログから該当テストを抽出 | PB-TR-01 / 05 / 10 / 13 / 19 / 20 / 21 / 23 / 28 と「中身の表示と presentation フックの開始の間に描画の機会がない」すべて passed |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` | 合計 0 ファイル / 禁止 0 件 (検査対象 543 ファイル) |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` | 58/61 (Sample 3 件は除外)、未網羅なし |
| MAUI iOS ブリッジ | `xcodebuild -project maui/macios/native/KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge …` | **BUILD SUCCEEDED** |

※ 実機の A/B 証跡 (`verification/first-frame-ios/`) は review-004 で目視確認済み。今回の修正はその修正の第2段階 (状態遷移の位置だけの移動) で、見た目に効く区間 (alpha 復帰とフックの同期区間) は変わっていないため再取得は不要と判断した。

## 前回指摘の解消確認

### #1 (🟠 Major、確定) — `.presenting` が進行タスクの開始より先に立つ → **解消**

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:412-437`

`beginPresentation()` は `lifecycleTask = Task { … }` の起動だけを行い、状態は `.attached` のまま。タスク本体の**最初の中断点より前**に

```swift
guard !Task.isCancelled,
      self.containerState == .attached,
      !self.resultChannel.isResultSettled
else {
    self.finishRemoval()
    return
}
self.containerState = .presenting
await self.runPresentationPhase()
```

が置かれ、判定と `.presenting` への切り替えが同一同期区間に入っている。相方の推奨形と一致し、「状態 `presenting` ⇔ ホスト View 表示済み」の同値が回復している。

### #4 (🔵、採用) — 直接 `await` の成立条件をコメントへ → **解消**

`runPresentationPhase()` の doc (同ファイル `:440-449`) に、(a) `DialogTransition.Hook` が `@MainActor` で宣言されているため同じアクター上への直接 `await` は最初の中断点まで同期実行されること、(b) 中断のない開始を明示できる `Task.immediate` は対象 OS の下限では使えないこと、の2点が入った。将来フック型の isolation を変える改修が入ったときに前提が静かに崩れる懸念に対する番人として機能する。コメント規約 lint も 0 件。

### #6 (🔵、採用) — 外側タップ2本の待ち条件 → **解消**

`waitUntilContentIsVisible` → `waitUntilPresentationCompletes` (`containerState == .shown` まで待つ) へ改名 + 条件変更 (`ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift:63-74`)。旧名の残存なし (`grep` で 0 件)。呼び出しは `DialogOutsideTapTests.swift:57,84` の2箇所のみで、hitTest の座標・経路・assertion は不変。既定トランジションが中身の transform を動かす形に変わっても `frame` が最終位置であることが保証される。実行時間の増加は suite 全体で計測誤差内 (34 tests で 10.9〜11.0 秒、前回 11.0 秒前後と同水準)。

## 依頼された 5 点への評価

### (1) 窓の間 `.attached` に留めたときの各分岐 — 遷移表どおりか

**遷移表どおりになっている。** タスク到達時の状態が `.attached` か `.removed` のみに絞られることを、`.attached` から出る全経路を列挙して確認した。

- `containerState` が `.attached` から動くのは (a) このタスク自身の `.presenting`、(b) `completeRemoval()` の `.removed` の2つだけ。`beginDismissal()` は `.presenting` / `.shown` でしか通らないので `.attached` からは呼ばれない
- (b) へ至る全入口 (`handleSettled` / `handleCallerCancellation` の `.attached` 分岐、`scheduleImmediateRemoval`、`handleHostLost`) はいずれも `finishRemoval()` を経由し、`finishRemoval()` は必ず `lifecycleTask?.cancel()` する。したがって「状態が `.attached` でないのにタスクが取り消されていない」到達は起こらない (第2条件は防御的な冗長ガード)

各分岐の結果:

| 窓に届いた信号 | 実装の経路 | 遷移表 (created / attached 行) |
|---|---|---|
| 報告 (別スレッド発) | 進行タスクが `isResultSettled` を見て `finishRemoval()` / または先に走った `handleSettled` の `.attached` 分岐が `finishRemoval()` | ラッチ → 演出なしで removed (両フック非実行) ✔ |
| 呼び出し元キャンセル | `cancelFromCaller()` が**先に同期で** `settle(.cancelled, origin: .callerCancellation)` するため `isResultSettled` が true。加えて `handleCallerCancellation` の `.attached` 分岐が `finishRemoval()` | ラッチ (cancelled) → 演出なしで removed ✔ |
| OS 発の器消失 | `handleHostLost()` が `lifecycleTask?.cancel()` → `settle(.cancelled, .hostLost)` → `finishRemoval(waitsForHostRemoval: false)`。タスクは `Task.isCancelled` で弾かれる | removed へ直行 ✔ |
| 外側タップ | 受け付けて cancelled をラッチする (遷移表は「まだ受け付けない」)。指摘 [Minor 2] 参照 | (差分あり、deviation 記録済み) |

**`cancelFromCaller()` が settle を先に済ませる点が重要**で (`Contract/DialogResultChannel.swift:79-85`)、これがなければ「呼び出し元キャンセルの観察 Task より進行タスクが先に走る」順序で第3条件をすり抜け、両フックが走る形が残っていた。第3条件が意味を持つのはこの並びなので、条件の並べ方は正しい。

### (2) 「報告は届いたが `handleSettled` の Task が未実行」— どちらが先でも同じ結果か

**同じ結果になる。** 二重撤去・二重配送は起きない。

- **進行タスクが先**: 第3条件で `finishRemoval()` → `isRemovalRequested = true` → (撤去要求 →) `completeRemoval()` → `didCompleteRemoval = true` / `.removed` / 配送1回。あとから走る `handleSettled` は `.attached` (完了通知待ちの間は状態が `.attached` のまま) か `.removed` に当たるが、前者は `finishRemoval()` の `guard !isRemovalRequested` で、後者は `handleSettled` の `.removed` 分岐で止まる
- **`handleSettled` が先**: `.attached` 分岐で `finishRemoval()` → 同時に `lifecycleTask?.cancel()`。進行タスクは `Task.isCancelled` で弾かれ、else 節の `finishRemoval()` は `guard !isRemovalRequested` で no-op
- **判定の途中で報告が入る**: `isResultSettled` は `NSLock` 下の読みなので、true を読めば撤去、false を読めば `.presenting` へ進んで PB-TR-05 の直列扱いになる。どちらも契約で定義済みの側に落ちる。「第3条件を false で読んだのに `handleSettled` の Task が既に投入済み」という穴は無い (Task の投入は `isSettled = true` より後)

冪等性は `isRemovalRequested` (撤去要求の重複防止) と `didCompleteRemoval` (後始末・配送の重複防止) の2段で担保されており、`DialogOutcomeDelivery` 側にも配送済みフラグがある。三重で塞がっている。

### (3) 新規2本の検出力 — 修正前のコードで fail するか

**両方とも fail する形になっている。**しかも1本あたり独立した2つの assertion が落ちる。

- `callerCancellationBeforePresentationStartsSkipsHooks` (`DialogTransitionTests.swift:279-307`): 修正前は `beginPresentation()` が同期で `.presenting` を立てるため `:299` の `#expect(containerState == .attached)` が落ち、さらに `handleCallerCancellation()` が `.presenting` 分岐で `beginDismissal()` に入るため `:304` の `#expect(probe.events.isEmpty)` も落ちる
- `reportBeforePresentationStartsSkipsHooks` (`:313-340`): 同様に `:333` が落ち、`handleSettled` が `.presenting` で no-op → presentation を完走して両フックが走るため `:337` も落ちる

`layoutInWindow` 直後・一度も `await` していない地点で信号を届ける構成なので、タイミング待ちが要らず安定している (3 連続 green、0.002〜0.011 秒)。

ただし**第3条件 `!resultChannel.isResultSettled` だけを殺す変異は、この2本では捕まらない** → [Minor 1] 参照。

### (4) deviation「attached 中の外側タップ」の妥当性

**妥当。記録として残す判断に同意する。**lessons の `review-must-not-accept-weakened-structural-guarantee` に従って「観察できる条件」を書き出したが、**製品経路では観察できない**と結論した。

- `reportOutsideTap()` を呼ぶ製品経路は `handleBackdropTap`、すなわち `viewDidLoad` で器の root View に付けた `UITapGestureRecognizer` の1本だけ (`DialogContainerViewController.swift:220-222, 647-655`)
- タップの成立には `touchesBegan` と `touchesEnded` が要り、最低2回の run loop ターンをまたぐ。かつジェスチャは**自分の View がヒットテスト連鎖に入る前に始まったタッチを受け取らない**ので、器が載る前に始まったタッチが窓の中で成立することもない
- 窓は `beginLifecycle()` から進行タスクが走り出すまでの1メインアクターターン (主キューは同一 run loop ターン内で捌かれる) で、2ターンを要するタップが収まる余地は無い
- 仮に到達しても、結果 (cancelled)・両フック非実行・配送はいずれも遷移表の同じ行と一致する

deviation の書きぶりのうち「結果は同じ行の報告列と同じ」は**終わり方の形**が一致することの説明であって、遷移表のタップ列 (信号を無視して提示を続ける) と等価という意味ではない。そこは読み違えられ得るので、蒸留で ADR へ載せる際は「終わり方は一致するが、無視ではなく受け付ける点が差分」と書き分けたほうがよい。加えて記録範囲が実装より狭い → [Minor 2] 参照。

### (5) 退行

**退行なし。**上表のとおり PB-TR-05 / 19 / 20 / 23 / 28、回帰テスト「中身の表示と presentation フックの開始の間に描画の機会がない」、PB-TR-10 / 13 はすべて green。読解でも各経路が壊れていないことを確認した。

- **PB-TR-05**: 窓を抜けて `.presenting` に入った後の報告は `handleSettled` の `.presenting` 分岐が no-op → タスクが `runPresentationPhase()` 完走後に `.shown` → `beginDismissal()`。直列化は不変
- **PB-TR-19**: `beginLifecycle()` の `isResultSettled` → `scheduleImmediateRemoval()` は無改変。今回の第3条件はその**後**に開いた窓を塞ぐ追加であって置き換えではない
- **PB-TR-20 / 28**: `.created` の break と `.presenting` の `beginDismissal()` は無改変。PB-TR-28 はフック開始を待ってから取り消すため新設ガードの影響外
- **PB-TR-21**: `_ = await overlay` による覆いの完了待ちは `runPresentationPhase()` に残っており、報告後に同期で `.presenting` を読む assertion (`DialogTransitionTests.swift:351`) も green (この時点では既に進行タスクが走り終えている)
- **PB-TR-23**: `handleHostLost()` は無改変。窓の中で来た場合も `Task.isCancelled` で弾かれ、`isRemovalRequested` で二重に進まない
- **PB-TR-10 / 13**: 撤去完了後の配送経路 (`finishRemoval` → 完了通知 → `completeRemoval`) に触れていない

## 指摘事項

### [🟡 Minor] 第3条件 `!resultChannel.isResultSettled` を単独で殺す変異が、どのテストでも落ちない

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:421-427` / `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:279-340`

**問題点**:

新設ガードの3条件のうち、**契約上いちばん重い仕事をしているのは第3条件**である。第1条件 (`Task.isCancelled`) が効くのは「器のほうが先に撤去へ進んだ」場合で、これは元々どこかが `finishRemoval()` を呼んでいる。これに対し第3条件は「別スレッドから結果だけが確定し、`handleSettled` の Task がまだ走っていない」という、この修正が本来塞ぎに来た並びを唯一カバーする。

ところが新規2本はどちらもこの条件を単独では検査していない。

- `callerCancellationBeforePresentationStartsSkipsHooks` は `handleCallerCancellation()` を**同期で直接呼ぶ**ため、進行タスクが走る前に `finishRemoval()` → `cancel()` が済んでいる。第1条件だけで通る
- `reportBeforePresentationStartsSkipsHooks` は `observeResultChannel()` を登録しているので `handleSettled` の Task が投入される。主キューの FIFO で進行タスクが先に走る前提では第3条件が効くが、順序が入れ替われば第1条件が拾うため、**第3条件を削っても落ちない**

結果として「第3条件を削除する」変異はテストで検出されない。実害が今あるわけではないが、この行は今回の修正サイクル 2 周分の核であり、番人が無いまま残ると将来の整理で静かに落ちる。

**推奨修正**:

`observeResultChannel()` を**登録しないまま**確定させる 1 本を足すと、取り消しも `handleSettled` も一切走らないので第3条件だけが分岐を決める形になり、変異が確実に落ちる。

```swift
@Test("観察者が付く前の確定でも提示は動き出さない")
func settlementWithoutObserverStillSkipsPresentation() async throws {
    // …layoutInWindow で器を組み立て、outcomeDelivery.setDestination だけ設定する
    // (observeResultChannel は呼ばない = handleSettled の Task が投入されない)
    #expect(stage.container.containerState == .attached)
    resultChannel.settle(.completed(true))

    try #require(await waitUntil { stage.container.containerState == .removed })
    #expect(probe.events.isEmpty, "進行タスク側の確定判定だけで両フックが抑止される")
    #expect(recorder.firstCompletedValue(as: Bool.self) == true)
}
```

既存2本の書き換えは不要で、追加のみ。実行時間の増加も無視できる。

### [🟡 Minor] 外側タップの deviation が `.created` を含んでおらず、記録範囲が実装より狭い

**該当箇所**: `kasane/changes/add-presentation-behavior/deviation.md` 末尾の「design Decision 10 (attached 中の外側タップ)」 / `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:647-648`

**問題点**:

`reportOutsideTap()` のガードは `containerState != .dismissing, !isRemovalRequested` だけなので、**`.attached` に限らず `.created` でも受け付ける**。design Decision 10 の遷移表は created と attached を**同じ行**にまとめて「(まだ受け付けない)」としているため、差分は2状態にまたがっている。deviation はこのうち `.attached` しか書いていない。

`.created` での受け付けは理屈上の話ではなく、**テストが意図的に固定している**:

- `ios/Tests/KsDialogsTests/DialogOutsideTapTests.swift:18-33` (`outsideTapSettlesCancelledByDefault`) は、window に載せていない = `.created` の器に対して `reportOutsideTap()` を呼び、cancelled が1回確定することを期待している

さらに `.attached` での受け付けも、`DialogSwiftUIAttributeDslTests.swift:69` / `DialogAttributeSupplyTests.swift:142,236` など、`layoutInWindow` の直後 (= `.attached`) に `reportOutsideTap()` を呼ぶテストが複数依存している。つまり deviation の末尾にある「契約として『受け付けない』を厳密に固定するなら別途判断」は、**ガードに1行足すだけでは済まず、既存テスト4本以上の書き換えを伴う**。この見積もりが記録に無いと、後段 (verify / 蒸留 / オーナー判断) が「1行で直せる」と読み違える。

なお `.created` / `.attached` のどちらも、製品経路 (タップジェスチャ) からは到達できないという (4) の結論は変わらない。これは記録の正確さの問題であり、挙動を変える必要は無い。

**推奨修正**: deviation の当該項目を次の3点で補う (コード変更なし)。

1. 対象状態を「`.attached`」→「`.created` / `.attached` (遷移表の同一行)」に広げる
2. 「終わり方は同じ行の報告列と一致するが、遷移表のタップ列が定める『無視して提示を続ける』とは異なる」と書き分ける
3. 厳格化を選ぶ場合のコストとして「`DialogOutsideTapTests` / `DialogSwiftUIAttributeDslTests` / `DialogAttributeSupplyTests` の外側タップ呼び出しが `.created` / `.attached` に依存しているため、ガード変更と同時にこれらの待ち合わせ点を `.shown` まで進める必要がある」を1文添える

### [🔵 Suggestion] 「提示前の確定」を塞ぐ機構が `scheduleImmediateRemoval()` と新設ガードの2本立てになっている

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:383-402`

**問題点**: 今回の修正で、`beginLifecycle()` の `scheduleImmediateRemoval()` と `beginPresentation()` の新設ガードが、**同じ契約セル (created / attached 行の報告列) を2つの機構で担う**形になった。実際の中身も

- `scheduleImmediateRemoval`: 1ターン譲ってから `containerState == .attached` を見て `finishRemoval()`
- 新設ガード: 進行タスクの先頭で `containerState == .attached` (と他2条件) を見て `finishRemoval()`

とほぼ同一で、後者は前者を包含する (`beginPresentation` の起動もタスクなので「1度譲ってから撤去する」という `scheduleImmediateRemoval` の狙いを満たす)。片方だけ直して他方が取り残される型の保守ハザードになる。

**推奨修正**: `beginLifecycle()` から `isResultSettled` 分岐を外して常に `beginPresentation()` を呼び、撤去はタスク先頭のガードに一本化する案がある。ただし PB-TR-19 (factory 内即報告) が通る経路が変わるため、採るなら iOS 全テストの再実行が要る。**今回のサイクルで入れる必要は無く、蒸留または後続での判断で足りる。**

## アクションプラン

1. **[Minor 1] 第3条件の番人テストを1本追加** — 追加のみ・実行時間増ほぼ無し。入れる場合は iOS 全テスト再実行まで
2. **[Minor 2] deviation の外側タップ項目を3点補正** — ドキュメントのみ、コード変更なし。verify / 蒸留の前に済ませておくと後段が読み違えない
3. **[Suggestion] 撤去機構の一本化** — 蒸留または後続フェーズの判断で足りる。本修正のブロッカーではない

いずれも実行時の挙動を変えるものではないため、**判定は APPROVED**。1 と 2 を今サイクルで入れるか後段へ送るかはオーケストレーターの裁量で構わない。

---

## 追記 (Minor 1 / Minor 2 の対応確認)

**日付**: 2026-08-22 / **判定**: 両件とも **解消**。判定は APPROVED のまま変わらない。

### 自分で回し直した検査

| 検査 | 結果 |
|---|---|
| iOS 全テスト (iOS 26.5) | **137 tests / 25 suites / 0 failures** (`** TEST SUCCEEDED **`)、error / warning 0 件。前回 136 件 + 新規 1 件で件数が合う |
| 新規テストの実行 | 「観察が付かないまま確定した場合も提示は動き出さない」passed (0.010 秒) |
| コメント規約 lint | 合計 0 ファイル / 禁止 0 件 (検査対象 543 ファイル) |
| 本体の復元確認 | `DialogContainerViewController.swift:421-427` のガードが 3 条件 (`!Task.isCancelled` / `containerState == .attached` / `!resultChannel.isResultSettled`) のまま原形であることを確認。変異は残っていない |

### Minor 1 (ガード第 3 条件の変異検出) — 解消

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:341-374`

**根拠**: `settlementWithoutObserverStillSkipsPresentation()` は `observeResultChannel()` を登録せず、取り消しも行わないまま `.attached` で `settle(.completed(true))` する。この構成では `handleSettled` の仕事が一度も投入されず `Task.isCancelled` も立たないため、**進行タスク先頭の第 3 条件だけが分岐を決める**。指摘で求めた隔離条件をそのまま満たしている。

変異解析も独立に確認した。第 3 条件を外すと、guard は `!Task.isCancelled` (false) と `.attached` (true) を通過して `.presenting` へ進み、presentation フック完走 → `.shown` → `isResultSettled` → `beginDismissal()` で dismissal フックまで走る。したがって `probe.events` は 4 イベントになり `:371` の `#expect(probe.events.isEmpty)` が落ちる — ホスト側が報告した変異実行結果と一致する。**変異が確実に殺される形**であり、番人として機能する。

`recorder.count == 1` / 確定値 true も併せて固定しており、「演出なしで撤去・配送」の THEN 側 (Decision 10 の created / attached 行) を過不足なく押さえている。doc コメントも成立条件 (観察を登録しないことで判定を進行側に絞る意図) を単独で読める形で書かれており、コメント規約に抵触しない。

### Minor 2 (外側タップの deviation) — 解消

**該当箇所**: `kasane/changes/add-presentation-behavior/deviation.md` 末尾「design Decision 10 (created / attached 中の外側タップ)」

**根拠**: 推奨した 3 点がすべて入っている。

1. 見出しと本文が `.created` / `.attached` の両方を対象に改まり、遷移表の同一行と対応が取れている
2. `DialogOutsideTapTests.swift:18-33` が `.created` での挙動を明示的に固定していることが記録され、暗黙の依存が可視化された
3. 製品経路 (器 root View の `UITapGestureRecognizer`、タップ成立に 2 run loop ターン) では 1 ターンの窓に収まり得ないという到達性の根拠と、厳格化する場合に「1 行では済まず既存テスト 4 本以上の書き換えを伴う」というコスト見積もりが入り、後段が「1 行で直せる」と読み違える余地が消えた

これで実装と記録の範囲が一致し、未記録の仕様逸脱はなくなった。

### Suggestion (2 機構の一本化)

`handoff-distill.md:38` に後続候補として申し送られていることを確認した。本サイクルでの対応は不要という当初の判断のとおり。

### 追記時点の残指摘

なし。Critical / Major / Minor いずれもゼロ、Suggestion 1 件は後続へ申し送り済み。
