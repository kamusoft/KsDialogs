# レビュー結果: add-presentation-behavior (003 回目)

**日付**: 2026-08-22
**判定**: APPROVED

## サマリー

2 周目の突き合わせ表 #1 (Major) / #2 (Minor) / #3 (ホスト M-2) の対応を、成果物・deviation.md・自分で回した実測だけで確認した。**3 件とも解消**しており、見せかけの対応 (テストだけ通して構造は直っていない類) は無い。特に #1 は「器の寿命に依存しない配送口」という**構造の是正**で応えており、待ち合わせ窓に限らず「器が撤去まで進めないまま死ぬ」経路すべてを 1 か所で塞いでいる — 窓を狭める対症療法ではない。

iOS の退行も自分で全件回して green (133 tests / 25 suites / 0 failures)。負の検査・スクリプト 3 種・MAUI ブリッジのビルドも確認した。

指摘は **Minor 1 / Suggestion 2**。Minor は新しい寿命保証の要になっている 1 行 (`[weak self]`) が無注釈で、次に触る人が「対になる強保持に揃える」方向へ直すと宙吊りが復活する、という**保守上の壊れやすさ**の話。Scenario も契約も破らないため APPROVED とするが、**アーカイブ前に 1 行入れてほしい**。

---

## 2 周目の指摘への対応状況

| # | 指摘 | 判定 | 根拠 (自分で確認したこと) |
|---|---|---|---|
| 1 | 待ち合わせ窓で器が解放されると配送口が失われ show が返らない (Major) | **解消** | 下記 |
| 2 | `containerState = .removed` が実際の撤去完了より前に立つ (Minor) + S-1 | **解消** | 下記 |
| 3 | UIKit 提示面の本命経路が自動検査を 1 本も通っていない (ホスト M-2) | **解消** | 下記 |
| 4 | MAUI iOS の無効チップの読み上げ | 降格済み (突き合わせで確定)。`handoff-distill.md` に記録あり — 再確認のみ |
| 5 | handoff の負の検査 8 本 / iOS 件数 | **解消** | `handoff-distill.md:5` に `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION` と期待診断、`:30` に「ios 133 tests / 25 suites」。自分の実測 (133 / 25) と一致 |

### #1 — 解消 (構造の是正として)

`ios/Sources/KsDialogs/Presentation/DialogOutcomeDelivery.swift` (新設、internal) が continuation の届け先 (`destination`) とラッチ済み outcome (`pendingOutcome`) を所有し、器は `nonisolated let outcomeDelivery` で参照するだけになった。結線は 3 点:

1. **口の切り離し**: `DialogPresenter.swift:64-71` が `container.outcomeDelivery.setDestination { continuation.resume(...) }`。届け先は器ではなく口が握るので、器が死んでも届け先は残る
2. **正常経路**: `DialogContainerViewController.swift:522-529` の `completeRemoval()` が `outcomeDelivery.submitLatchedOutcome()`
3. **引き継ぎ**: `DialogContainerViewController.swift:177-184` の `deinit` が同じ口へ `submitLatchedOutcome()` を投げる。撤去まで進めないまま器が解放される**すべての**経路がここで合流する

`submitLatchedOutcome()` は `guard !isDelivered, pendingOutcome == nil` で二重配送を塞ぎ、未確定なら `settle(.cancelled, origin: .hostLost)` してから配送する — ADR-0006 規則6 (「結果報告を経ずに器が画面から外れたら cancelled」) と、同 ADR が負の帰結として挙げていた「検知漏れは show の宙吊りとして現れる」への正面からの回答になっている。

`DialogResultChannel.settle` はラッチ済みなら no-op、`settledOutcome` はラッチ値を返すので、**ラッチ済みの結果が `.cancelled` に化ける経路は無い** (PB-TR-12 の「cancelled には変わらない」が守られる)。2 回のロック取得に分かれているが、`latchedOutcome` は初回確定以降不変なので競合しない。

`viewDidDisappear` (`:360-379`) の `Task` は `[weak self]` を落として器を強く捕まえるようになった。2 周目に指摘した「実態と食い違うコメント」(`guard let self else { … settle して return }` とその説明) は**分岐ごと消滅**しており、訂正ではなく不要化で解決している。

**配送後に器が残らないこと**も固定された (`DialogTransitionTests.swift:418-436` の `PB_TR_12_containerIsReleasedAfterHostLossDelivery`)。強保持を導入したことの裏返しをテストで押さえてあるのは良い。参照グラフを追っても循環は無い (器 → 口 → destination クロージャ → continuation。continuation は器を握らない)。

### #2 — 解消

`isRemovalRequested` (`:107`) が新設され、`finishRemoval()` (`:497-518`) は要求済みの印を立てるだけ、`containerState = .removed` は `completeRemoval()` (`:522-529`) へ移った。design Decision 10 の `removed = 器を撤去し outcome を配送` と字面が一致する。deviation.md にも「`removed` になる時点」として記録済み。

状態機械の整合も自分で追って確認した。要求〜完了の窓 (state は `.dismissing` または `.attached`、`isRemovalRequested == true`) で:

- `handleSettled()` の `.dismissing` → break / `.attached` → `finishRemoval()` は要求済みガードで no-op。従来 (`.removed` → break) と同じ結果
- `handleCallerCancellation()` の `.dismissing` → `finishRemoval()` も同じく no-op
- `handleHostLost()` は `guard !isRemovalRequested` で早期 return
- `reportOutsideTap()` (`:614-618`) は `containerState != .dismissing, !isRemovalRequested` の 2 条件。`.removed` は必ず `isRemovalRequested == true` を伴う (`completeRemoval()` の呼び元が `finishRemoval` 系だけ) ので、旧 `.removed` 判定の穴埋めができている

S-1 も入っている。`viewDidDisappear` の `guard self.presentingViewController == nil else { return }` が**両分岐の手前**に上がり、host-lost 側 (base から在った条件) と撤去完了側で同じ性質を見るようになった。base との diff でも host-lost 側の条件が失われていないことを確認済み。

テストも窓を観察している: `DialogTransitionTests.swift:331` / `:452` の `#expect(container.containerState == .dismissing, "撤去の完了通知が来るまでは撤去済みにならない")` が PB-TR-10 / PB-TR-13 の両方に入り、`heldDismissalCount == 1` かつ `presentedContainers.count == 1` かつ未配送、と同時に観察している。

### #3 — 解消

`ios/Tests/KsDialogsTests/UIKitDialogPresentationSurfaceTests.swift:110-135` の `dismissCompletionArrivesFromPresentingViewController` が、`Support/RecordingPresentingViewController.swift` を root に据えて本命分岐を通す。**ライブラリ側の行は全部実行される** — `container.presentingViewController` の解決、`presenting.dismiss(animated: false)` の依頼、`MainActor.assumeIsolated { completion() }` の通過。`presenting.dismissalRequests == [false]` で「提示元へ・アニメーションなしで・ちょうど1回」まで押さえており、もし実装が `container.dismiss(...)` を呼んでいれば落ちる。有意な検査になっている。

スタブなので UIKit 自身の撤去タイミングは依然として手動確認の領域だが、それはこの実行環境では原理的に取れない (テストファイルの doc コメントにその旨が書かれている)。2 周目の M-2 で求めたのは「結線が自動検査の外にあることの解消」なので、達成したと判断する。

---

## 評価してほしい設計判断への回答

### (a) 配送口の切り離し + deinit 引き継ぎ + 強保持 Task の併用は、寿命保証として十分か / 過剰か

**結論: 十分であり、過剰ではない。3 つは役割が重ならない。**

- **配送口の切り離し**は「届け先を器の寿命から外す」だけで、配送を**起こす**役はいない
- **deinit 引き継ぎ**は「器が撤去まで進めずに死んだ」ことを検知して配送を**起こす**役。口が器の中にあった頃は、この検知点があっても届け先が同時に消えるので成立しなかった
- **強保持 Task** (`viewDidDisappear`) は「判定そのものが消える」のを防ぐ役。弱参照のままだと、器が死んだ場合に `handleHostLost()` / `completeRemoval()` へ到達せず、後始末 (`releaseContentHost()`) の**正規経路**が通らない。deinit の受け皿は結果を届けるが、child containment の解除など正規の後始末はしない (器ごと解放されるので実害はないが、通れる経路は通すのが正しい)

どれか 1 つを外すと穴が開くので、重複ではない。**残る穴は「提示機構が器を握ったまま、完了通知も `viewDidDisappear` も来ない」場合のみ**で、これは UIKit 側の契約違反にあたり検知手段が無い。ADR-0006 が負の帰結として書いている範囲を超えないので、指摘にはしない。

**代償の評価**: `deinit` ごとの `Task { @MainActor in … }` 1 本は、ダイアログ 1 回につき 1 本。同じ 1 回のダイアログで `lifecycleTask` / フックの見張り (DEBUG) / `viewDidDisappear` の判定 / `onSettle` の中継と既に複数の Task が回っていることを思えば、比率として無視できる。`isDelivered` を nonisolated な原子フラグにして早期 return する最適化は考えられるが、並行面 (器の外から読める可変状態) を増やす割に得るものが無く、**現状のままでよい**と評価する。

### (b) `finishRemoval` の完了通知だけ `[weak self]` を残す非対称の是非

**結論: 正しい。ただし無注釈なのは直してほしい (M-1)。**

判断基準として一貫している — **無限に伸びうる保持は weak、有限で必ず終わる保持は strong**。

- `finishRemoval` (`:515-517`) が提示面へ渡す完了通知は、面がいつまで握るかライブラリ側から制御できない。強保持にすると、面が握り潰した瞬間に器が永久に生き、**`deinit` の受け皿に到達できず宙吊りが復活する**。つまりこの `[weak self]` は (a) の deinit 引き継ぎを機能させるための前提条件になっている
- `viewDidDisappear` の Task は MainActor を 1 ホップ回れば必ず終わるので、強保持しても寿命は延びない

非対称は正当だが、**片方 (強保持) にだけ理由コメントがあり、もう片方 (weak) には無い**。これは M-1 として下に書く。

### (c) 「show 越しに外部の強参照を落とす」テストを、器を直接組み立てる形へ組み替えた判断

**結論: 妥当。むしろこちらが正しい。**

`DialogPresenter.present` の async フレームは、結果を待つ間ずっとローカルの `container` を保持する (`withCheckedContinuation` のクロージャが捕まえ、Debug ではスコープ末尾まで生存)。したがって「show 越しに外部の強参照を落とす」テストは**何も落としていない**ことになり、常に green になる — 偽の安全証明にしかならない。実装者の実測と一致する。

組み替え先が 2 本に分かれているのも良い設計:

- `DialogTransitionTests.swift:390-416` `deliveryOutlivesContainerRelease` — 機構そのもの (器を落としても口が配送する)
- `DialogTransitionTests.swift:418-436` `PB_TR_12_containerIsReleasedAfterHostLossDelivery` — 機構が余計な保持を残さない (強保持 Task が配送後も居座らない)。show が返ったあとに弱参照で確認する形なので、presenter フレームの保持に邪魔されずに観察できている

**残る差**は、直接組み立て版が「撤去要求〜完了通知の窓」を通っていないこと (要求を出す前の器を落としている) — S-2 として下に書く。ただし配送の機構は窓の内外で同一 (`deinit` → `submitLatchedOutcome()` → ラッチ済み配送) なので、実害の差は小さい。

---

## 指摘事項

### [🟡 Minor] M-1: 寿命保証の要になっている `[weak self]` が無注釈で、対になる強保持だけに理由が書かれている

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:515-517`

**問題点**:

```swift
dismissRequest { [weak self] in
    self?.completeRemoval()
}
```

この `weak` は好みではなく、(b) で書いたとおり **`deinit` の受け皿を機能させるための前提条件**になっている。ところが同じファイルの `viewDidDisappear` (`:365-367`) には

```
// 器を強く捕まえるのは、この1回分の判定を必ず後始末と配送まで進めるため。
// 判定の前に提示機構が器を手放すと、撤去まで進めないまま呼び出し元が取り残される。
```

という理由コメントがあり、**強保持の側にだけ根拠が書かれている**状態になっている。この非対称を知らずにファイルを読んだ人が「取り残されないよう強く捕まえる」という説明を一般則と受け取り、`:515` も strong に揃えて "直す" のは十分ありうる。そうすると、提示面が完了通知を保持し続けた場合に器が永久に生き、`deinit` へ到達できず #1 の症状 (show が返らない) が静かに戻る — しかも既存テストは面が完了通知を確実に呼ぶ実装なので、緑のまま通ってしまう。

規約 (comment-policy) の「そのファイルだけを読んでいる人にとって意味が通ること」と、レビュー観点の「コメントが単独で理解できるか」に照らして、根拠が半分しか書かれていないのは不足と考える。

**推奨修正**: `:515` の直前に 1 行足す。例:

```swift
// 完了通知をいつまで握るかは提示面しだいなので、ここで強く捕まえると
// 器が永久に生き、解放時の配送 (deinit) へも到達できなくなる。
dismissRequest { [weak self] in
```

**アーカイブ前に入れてほしい** — この非対称は蒸留で ADR / concepts へ写る種類の知識ではなく、コードのそばにしか残せない。

### [🔵 Suggestion] S-2: 「撤去要求〜完了通知の窓での解放」そのものを固定するテストが無い

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:390-416`

**問題点**: `deliveryOutlivesContainerRelease` は、撤去の要求を出す**前**の器を落として配送を観察している。#1 が実際に問題にしたのは「要求を出したあと、完了通知を待っている間に器が解放される」窓であり、その窓での解放は 1 本も通っていない。

配送の機構は窓の内外で同一 (`deinit` → `submitLatchedOutcome()` → ラッチ済み配送) なので、実害の差はほとんど無い。ただし窓の中では `isRemovalRequested == true` / `didCompleteRemoval == false` / outcome ラッチ済みという別の状態の組み合わせになっており、将来 `submitLatchedOutcome()` のガードや `completeRemoval()` の順序をいじったときに、この組み合わせだけ壊れても気づけない。

**推奨修正**: 器を直接組み立て、完了通知を保留する提示面 (`DialogTestPresentationSurface` の `holdsDismissalCompletion`) を繋いだ状態で `handleHostLost()` ではなく通常の撤去要求まで進め、そこで器への参照を落として配送を観察する形が取れると直接的。`finishRemoval` が private のため素直には呼べないので、器をウィンドウに載せて `reportOutsideTap()` から状態機械を回す等の段取りが要る。手間に見合わないと判断するなら見送りで構わない (機構は同一なので)。

### [🔵 Suggestion] S-3: `DialogOutcomeDelivery.setDestination` が、配送済みのときに届け先を黙って捨てる

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogOutcomeDelivery.swift:30-34`

**問題点**: `guard !isDelivered else { return }` を通ると、渡された届け先は保存も実行もされずに消える。この届け先は `continuation.resume` を握っているので、万一この分岐に入ると **show が永久に返らない**。

現状は到達不能で正しい — presenter は器の生成から `setDestination` まで MainActor 上で同期に進み、その間に配送を起こしうる経路 (`onSettle` / `onCallerCancellation` / `beginLifecycle`) はすべて `Task { @MainActor }` を 1 ホップ挟むので追い越せない。だが「配送を起こす側」と「届け先を登録する側」の順序保証がコード上どこにも書かれておらず、presenter 側の 1 行の並べ替えで破れる。

**推奨修正**: どちらかで十分。

1. `guard` に「到達したら呼び出し元が取り残される」ことを述べたコメントを添え、`assertionFailure` を置く (DEBUG でだけ気づける)
2. 順序の前提を `DialogPresenter.swift:64-71` 側に 1 行書く (「配送を起こす経路はすべて MainActor を 1 ホップ挟むので、ここより先に配送は起きない」)

---

## 確認した観点 (指摘に至らなかったもの)

- **`submitLatchedOutcome()` の副作用**: 正常経路 (`completeRemoval()` 経由) でも `settle(.cancelled, .hostLost)` を通るが、ラッチ済みなら no-op。仮に確定を起こしても `onSettle` → `handleSettled()` は `origin == .hostLost` で早期 return するため、二重の退出開始は起きない。`resultOrigin` を読む箇所は `handleSettled()` だけ (grep 済み)
- **二重配送 / 二重解放**: `isDelivered` (口)・`didCompleteRemoval` (器)・`isRemovalRequested` (器)・`contentHost = nil` (解放) の 4 つで、配送・後始末・撤去要求がそれぞれ 1 回に切り詰められている。`pendingOutcome == nil` のガードにより、届け先未登録のまま 2 回 submit しても状態は壊れない
- **多段表示**: 下の器を閉じると上の器も `viewDidDisappear` → (`presentingViewController == nil`) → `isRemovalRequested == false` → `handleHostLost()` で cancelled。`presentingViewController` の解除が遅れて早期 return した場合も、器が解放されれば `deinit` の受け皿が cancelled を届けるので宙吊りにならない — 2 周目より堅くなっている。PB-MD-01〜05 は green
- **`deinit` の隔離**: `deinit` は nonisolated、`outcomeDelivery` は `nonisolated let`、`DialogOutcomeDelivery` は `@MainActor` クラス (= 暗黙 Sendable) なので、Task へ渡す形は Swift 6 の隔離規則に沿っている。`Task` が `delivery` を保持するため、器の解放と配送の間に口が消える隙間は無い
- **`MainActor.assumeIsolated`**: `UIKitDialogPresentationSurface.swift:42` の前提 (提示機構の完了通知はメインスレッド) は、新テストで実際に通過するようになった
- **deviation.md**: 今回追加された 2 項目 (配送口の切り離し / `removed` の時点) はいずれも公開面に影響せず、契約の字義を破らないので「実装メモ」への分類は妥当。既存の「撤去完了の合図が 2 つ」の項も、テストランナーの実測が 2 経路目の**対象外**であること (提示関係が残るため) を明記する形に整理されており、実装と読み合わせて矛盾がない。この整理により 2 経路目は「View が消えて提示関係も解けたのに completion だけ届かない」場合の保険という位置づけになったが、通常の撤去では完了通知が先着するので契約テストは骨抜きにならない (PB-TR-10 / 13 は保留中に View を外さない面で観察している)
- **足場**: `git status` で `kasane/changes/add-presentation-behavior/` 配下の追跡ファイルの変更は `tasks.md` / `ui/brief.md` のみ。proposal / design / specs に書き換えなし
- **1 ファイル 1 型**: 新設 3 ファイル (`DialogOutcomeDelivery.swift` / `DialogWeakContainerReference.swift` / `RecordingPresentingViewController.swift`) はいずれも 1 型
- **コメント規約**: `comment-policy-lint.py` 禁止 0 件 (543 ファイル)。新規コメントの外部参照は `core/ADR-0017` のみで、ADR-0017 の本文 (「呼び出し元への配送は退出フック完了と器の撤去の後に行う」「ラッチと配送の分離」) と内容が対応している
- **M-2 の残余 (Sample 通し)**: 「結果を受け取った直後に次のダイアログを出す」観察点は `verification/sample-walkthrough/` にまだ無い (grep で確認)。#3 が結線を自動検査へ載せたことで優先度は下がったが、実機での順序を見た証跡が無いことは変わらない。`handoff-distill.md` へ 1 行残す候補として挙げておく (指摘にはしない)
- **降格済み #4 (MAUI iOS の読み上げ)**: `handoff-distill.md` にオーナー判断待ちとして記録が残っていることを再確認。今回のサイクルで状況は変わっていない
- **`ui/brief.md` の「オーナーの最終承認: 未取得」**: 1 周目・2 周目と同じくレビュー対象外のゲートとして残っている

## アクションプラン

1. **[Minor]** M-1 — `DialogContainerViewController.swift:515` に `[weak self]` の理由を 1 行。**アーカイブ前に入れてほしい**
2. **[Suggestion]** S-3 — `setDestination` の捨て分岐に注記か `assertionFailure`、または presenter 側に順序前提を 1 行 (任意)
3. **[Suggestion]** S-2 — 撤去要求〜完了通知の窓での解放を直接固定するテスト (任意。見送り可)
4. **(指摘外)** Sample 通しへの「結果直後に次を出す」観察点を、実施しないなら `handoff-distill.md` へ 1 行

## 実行結果 (本レビューでの実測)

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<uuid>'` (iOS 26.5) | **133 tests / 25 suites / 0 failures** (TEST SUCCEEDED) |
| iOS 負の検査 | `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION` | 期待どおり `'defaultDuration' is inaccessible due to 'internal' protection level` でビルド失敗 |
| maui/macios/native | `xcodebuild -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge build` | BUILD SUCCEEDED |
| スクリプト | `comment-policy-lint.py` | 禁止 0 件 / 検査対象 543 ファイル |
| スクリプト | `scenario-id-coverage.py --require-mirror` | 58/61 (除外 3)・未網羅なし・両 Native ミラー OK |
| スクリプト | `scenario-id-coverage.py --selftest` | 全件 OK |

Android / MAUI / KMP は本サイクルで変更が無いため回していない (2 周目で android 50 / kmp 51 / maui 62 / bridge 15 を確認済み)。

---

## 追記 (M-1 / S-3 の対応確認)

**日付**: 2026-08-22 / コメントのみの追加であることを確認したうえで、独立に該当箇所を読んで判定した。

### M-1 — 解消

`ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:515-518`。指摘の意図 (揃える方向への誤修正を防ぐ理由の明示) を満たしている。

- **weak にする理由**が書かれている — 提示面が完了通知を握りつぶしたときに閉包が器を永久に生かさないため
- **取り残されない根拠**が書かれている — 器が先に解放されても `outcomeDelivery` (deinit で引き継ぐ) が配送を担う。ここを読めば「weak にして大丈夫なのか」の疑問がその場で解ける
- **揃えてはいけない理由**が明示されている (最終行) — 「強保持へ揃えると、握りつぶされた completion が器の寿命を無限に伸ばす」。M-1 が想定した誤修正 (`viewDidDisappear` の強保持に合わせる) を名指しで塞いでいる

同ファイルの `viewDidDisappear:365-367` の強保持側コメントと読み合わせると、「無限に伸びうる保持は weak / 有限で終わる保持は strong」という判断基準が両方から復元できる。コメント規約の許容参照 (コード識別子のみ) の範囲内で、`comment-policy-lint.py` も禁止 0 件 (543 ファイル) を自分で再確認した。

### S-3 — 解消

`ios/Sources/KsDialogs/Presentation/DialogOutcomeDelivery.swift:31-33`。指摘の意図 (届け先を捨てる前提条件の明文化) を満たしている。

- **前提条件**が言語化されている — 「届け先は配送より前に 1 回だけ決まる (提示層が show の開始時に設定する)」。S-3 が問題にしたのは「順序保証がコード上どこにも書かれていない」ことなので、これが要点
- **捨てる判断の根拠**が書かれている — 配送済みの後に届け先が来る経路が無いこと、来ても結果は既に届いていること、再配送はしないこと

推奨修正の 2 案のうち後者 (順序前提の明文化) を、presenter 側ではなくガードの直上へ置いた形。ガードを読んだ人がその場で前提に行き当たるので、置き場所としてはむしろこちらが良い。

残余 (指摘ではない): 前提が破れた場合の症状 (捨てられた届け先が握る continuation が再開されず show が返らない) までは書かれていない。前提が成り立つ限り実害は無く、`assertionFailure` を置かない判断も妥当なので、追加対応は不要と考える。

### 判定

**APPROVED を維持**。M-1 / S-3 とも解消、未解消の指摘は無い。S-2 (任意) は `handoff-distill.md` への申し送りで処理済みとの報告を受けており、レビュー側から追加で求めることはない。コメントのみの変更であり、テストの再実行は不要と判断した (本レビュー本体で 133 tests / 0 failures を実測済み)。
