# 一致検証 (差分): add-presentation-behavior (verify-004)

- **判定: VALID**
- 検証日: 2026-08-22
- 種別: **差分検証 (4 回目)**。[verify-003](verify-003.md) (VALID) の後、レビュー指摘 (review-004 Minor / second-opinion-code-004 Major、双方一致) の修正で入った **iOS のみの変更**の再検証
- verify-001 / 002 / 003 は書き換えていない。本書には **verify-003 からの変化だけ**を記す
- 集計 (61 Scenario 全体): **✅ 56 / ⚠️ 5 / ❌ 0** — verify-003 から**増減なし**

---

## 1. 差分の範囲

iOS のみ。Android / MAUI / KMP は変更がないため再実行していない (verify-001 の実測が引き続き有効)。

| 種別 | ファイル |
|---|---|
| 変更 (本体) | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift` (`beginPresentation()` / `runPresentationPhase()` の doc) |
| テスト追加 | `DialogTransitionTests.swift` に回帰 2 本 (`callerCancellationBeforePresentationStartsSkipsHooks` / `reportBeforePresentationStartsSkipsHooks`、Scenario ID なし) |
| テスト支援 | `Support/DialogLayoutMeasurement.swift` の `waitUntilContentIsVisible` → **`waitUntilPresentationCompletes`** (`.shown` まで待つ) |
| テスト追随 | `DialogOutsideTapTests.swift` 2 本が新しい待ち合わせへ差し替え |
| 文書 | `deviation.md` の実装メモ末尾に「attached 中の外側タップを受け付ける」を追記 |

### 直した穴

verify-003 時点の `beginPresentation()` は **`containerState = .presenting` を同期で立ててから** Task を起動していた。Task が動き出すまでの 1 MainActor ターンの間、器は「まだ何も見えていないのに提示中」という状態にあり、その窓に届いた閉鎖信号が遷移表の **presenting 行**の規則 (演出つき・直列) で扱われ得た。すなわち **一度も画面に出ていない中身に対して dismissal フックが走る**経路が残っていた。

修正後 (`:412-434`) は、段階の判定と切り替えが Task 本体の**最初の同期区間**に入る:

```swift
lifecycleTask = Task { @MainActor [weak self] in
    guard let self else { return }
    guard !Task.isCancelled,
          self.containerState == .attached,
          !self.resultChannel.isResultSettled
    else {
        self.finishRemoval()   // 両フックとも実行せず撤去だけ
        return
    }
    self.containerState = .presenting
    await self.runPresentationPhase()
    …
}
```

- 「確定済みかどうかの判定」と「提示中への切り替え」の間に中断点がないため、**載せただけの段階が提示中として扱われる隙間が閉じた**
- 隙間の間に届いた信号 (取り消し / 報告) は `finishRemoval()` の一本道へ落ち、**演出なしで removed** になる
- `runPresentationPhase()` の doc に、無中断の開始が成り立つ根拠 (`DialogTransition.Hook` が `@MainActor` のため同一アクターへの直接 `await` は最初の中断点まで同期実行される / `Task.immediate` は対象 OS 下限で使えない) が明記された。verify-003 で確認した「alpha 復帰とフックの最初の同期処理が同一区間」という性質の**成立条件の明文化**であり、挙動の変更ではない

---

## 2. design Decision 10 の遷移表との対応 (依頼 (1))

遷移表の **created / attached 行**:

| 信号 | 遷移表 | 実装 (修正後) | 一致 |
|---|---|---|---|
| 報告 (factory 内即報告を含む) | ラッチ → **演出なし**で removed (両フックとも実行しない) | `beginLifecycle()` の `isResultSettled` 分岐 (`scheduleImmediateRemoval()`) に加えて、**`beginPresentation()` の Task 先頭でも再判定**して `finishRemoval()` | ✅ |
| 呼び出し元キャンセル | ラッチ (cancelled) → 演出なしで removed | 同上 (`!Task.isCancelled` ガード + `containerState == .attached` 判定) | ✅ |
| OS 発の器消失 | removed へ直行 | `handleHostLost()` → `finishRemoval(waitsForHostRemoval: false)` | ✅ |
| 外側タップ / 戻る | (まだ受け付けない) | `reportOutsideTap()` (`:647`) に `.attached` の除外がなく、**受け付けて cancelled をラッチする** | ⚠️ deviation 記録済み (§4) |

**期待値は強化された。** verify-003 では PB-TR-19 / PB-TR-20 は `beginLifecycle()` の入口判定だけを通る経路で検査されていたが、今回追加の回帰 2 本が **「提示の進行が動き出す直前の 1 ターン」という別経路**を直接突いている:

| 回帰テスト | 何を固定するか |
|---|---|
| `callerCancellationBeforePresentationStartsSkipsHooks` | 器を直接組み立てて `containerState == .attached` を確認したうえで取り消しを届け、`removed` まで進むこと・`probe.events.isEmpty` (両フック非実行)・`cancelled` が 1 回だけ演出なしで配送されることを検査 |
| `reportBeforePresentationStartsSkipsHooks` | 同じ隙間へ報告を届け、`removed` まで進むこと・両フック非実行・**報告された値** (`completed(true)`) が演出なしで配送されることを検査 |

いずれも「一度も見えていない中身に退出の演出が走らない」ことを観察側から押さえており、**同じ退行が再び入れば落ちる**。

| Scenario | テスト | 期待値の変化 | 状態 |
|---|---|---|---|
| PB-TR-19 提示開始前の報告は演出なしで配送される | `PB_TR_19_reportBeforePresentationSkipsHooks` (変化なし) + 上記回帰 2 本目 | **強化** (別経路の追加検査) | ✅ |
| PB-TR-20 提示開始前の呼び出し元キャンセルは演出なし | `PB_TR_20_callerCancellationBeforePresentationSkipsHooks` (変化なし) + 上記回帰 1 本目 | **強化** | ✅ |

---

## 3. 影響 Scenario の再点検 (依頼 (2))

アサーション行を verify-003 時点と突き合わせた。**7 件すべて文字どおり変化なしで ✅。**

| Scenario | テスト | 期待値 | 状態 |
|---|---|---|---|
| PB-TR-01 presentation フックが1回・ホスト View・UI スレッド | `PB_TR_01_presentationHookRunsOnceWithHostView` | 変化なし (1 回 / `hostView` 同一 / `isOnMainThread` / `isOnWindow` / `size != .zero`)。`.presenting` を立てる時点が 1 ターン後ろへ動いても、フック開始時点の条件は同じ | ✅ |
| PB-TR-05 presentation 中の閉鎖信号は完走後に退出 | `PB_TR_05_closureDuringPresentationWaitsForPresentation` | 変化なし (イベント列の完全一致)。今回の修正は「presenting に**入る前**」の扱いだけを変えており、presenting 中の直列規則は不変 | ✅ |
| PB-TR-10 show は dismissal 完了と撤去より先に返らない | `PB_TR_10_deliveryWaitsForDismissalAndRemoval` | 変化なし (verify-002 で強化された `.dismissing` 検査を含む 10 アサーションすべて同一) | ✅ |
| PB-TR-13 添付なしでも配送は撤去後 | `PB_TR_13_defaultTransitionDeliversAfterRemoval` | 変化なし (`.dismissing` 検査を含む) | ✅ |
| PB-TR-23 提示中の OS 発器消失は cancelled | `PB_TR_23_hostLossDuringPresentationDeliversCancelled` | 変化なし | ✅ |
| PB-TR-28 presentation 中の呼び出し元キャンセル | `PB_TR_28_callerCancellationDuringPresentation` | 変化なし | ✅ |
| PB-MD-03 重ね出し中の外側タップは手前のみに届く | `ios/…/DialogMultiDisplayTests.swift` の `PB_MD_03_outsideTapClosesTopOnly` / `android/…/src/test/.../DialogMultiDisplayTests.kt` | 変化なし。iOS 側は `.shown` を待たずに `reportOutsideTap()` を呼ぶが、**手前だけが cancelled になり下段は表示のまま結果未確定**という THEN は段階によらず成立する (§4 の窓に当たっても結果は同じ cancelled)。Android は無変更 | ✅ |

### 既存テスト 2 本の追随 (`DialogOutsideTapTests`)

verify-003 で入った `waitUntilContentIsVisible` (alpha > 0 を待つ) が **`waitUntilPresentationCompletes` (`.shown` を待つ)** へ差し替わった。待ち合わせ点がより後ろの安定点へ動いただけで、**アサーションは不変** (`hitTest` の帰属・`isOutsideTap` の判定・結果が確定しないこと)。保証を緩めていない。

---

## 4. deviation の新規項目 (依頼 (3))

`deviation.md` 末尾 (実装メモ節) に 2026-08-22 付で追記:

> **design Decision 10 (attached 中の外側タップ)**: 遷移表は created / attached 行の「外側タップ / 戻る」を「(まだ受け付けない)」とするが、iOS の `reportOutsideTap` は `.attached` の間も受け付けて cancelled をラッチし、演出なしで removed にする (同じ行の「報告」列と同じ終わり方)。受け付ける窓は覆いも中身も alpha 0 で見えていない 1 MainActor ターンに限られ、結果・フック非実行・配送は遷移表と一致するため実装は変えていない。契約として「受け付けない」を厳密に固定するなら別途判断

実装と突き合わせた結果、**記述は正確**である:

- `reportOutsideTap()` (`:647-651`) のガードは `containerState != .dismissing`・`!isRemovalRequested`・`layout.isCanceledOnTouchOutside` の 3 つで、`.attached` の除外はない → 記述どおり受け付ける
- 受け付けた場合も `resultChannel.settle(.cancelled, origin: .outsideTap)` でラッチするだけで、その後は `beginPresentation()` の Task 先頭ガード (`!resultChannel.isResultSettled`) に落ちて **`finishRemoval()` = 演出なしで removed** へ進む → 遷移表の同じ行「報告」列と同じ終わり方という記述どおり
- 窓は「器を画面へ載せてから提示の進行が動き出すまでの 1 MainActor ターン」に限られ、その間は覆いも中身も alpha 0 (verify-003 で確認した挙動)

**spec (`specs/dialog-contract/spec.md`) の Scenario 本文には状態名も「attached 中のタップ」も現れない**ため、この差分は **design の遷移表に対するものであって Scenario の期待値には影響しない**。deviation.md に「契約として厳密に固定するなら別途判断」と申し送りつきで記録されており、**未記録の乖離ではない**。

**未記録の乖離: なし** (今回の変更で新たに生じた差分はすべて deviation.md に反映されている)。

---

## 5. テスト再実行 (依頼 (4))

| ルート | コマンド | 実測 | 結果 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=3B42B268-…'` (iPhone 17 / **iOS 26.0**) | **136 tests / 25 suites** (verify-003 は 134。**+2 = 回帰テスト 2 本と一致**) | 0 failures (`** TEST SUCCEEDED **`) |

| 検査 | 結果 | verify-003 からの変化 |
|---|---|---|
| `scripts/scenario-id-coverage.py --require-mirror` | 仕様 61 ID / 検出 58 ID / 除外 3、**未網羅なし**、**ミラー OK** | 変化なし (回帰 2 本は ID を持たないため母数・検出に影響しない) |

Android (unit 50 / instrumented 138) / KMP (51) / MAUI (62 + bridge 15) / 負の検査 10 本は未実行 (変更なし)。

---

## 6. 足場の逆流検査 (依頼 (5))

`git status` / `git diff` (読み取りのみ):

- `specs/`・`design.md`・`proposal.md`: **差分ゼロ** (HEAD `66ea298` のまま)。**逆流なし**
- 今回も「実装を design の遷移表へ寄せる」方向の修正であり、遷移表を実装に合わせて書き換える逆流ではない。合わせきれなかった 1 マス (attached 中の外側タップ) は spec ではなく deviation へ記録するという正しい処理になっている

---

## 7. 所見 (判定には影響しない申し送り)

1. **attached 中の外側タップ** — 現状は「窓が 1 ターン・見えていない・終わり方は遷移表と同じ」ため実害はないが、**遷移表と実装が一致していないマスが 1 つ残る**。蒸留で ADR-0017 へ統合する際に「受け付けない」を厳密に固定するか、遷移表側を「受け付けても結果は同じ」に改めるかの判断が要る (deviation にも同旨の申し送りあり)
2. **iOS のテスト件数は 130 → 133 → 134 → 136 と動いた**。`concepts/cross/conventions/test-execution.md` の実測値 (2026-08-19) は全ルートで古いまま。蒸留時に実測へ更新すること (verify-001 所見 1 / verify-003 所見 2 の再掲)
3. **`deviation.md` の前書き 3 行目**が「『オーナー確認待ち』の項目は暫定採用であり…」のまま残り、節名 (「オーナー確認済みの項目」) と食い違う (verify-003 所見 1 の再掲)
4. `verification/kmp-cancellation/README.md` の「ios/ 全件 129 tests」も鮮度差 (verify-001 所見 2 の再掲)
5. **UI のオーナー最終承認は引き続き未取得** (`ui/brief.md`)

---

## 8. 判定

**VALID**

- design Decision 10 の遷移表の created / attached 行 (演出なしで removed・両フックとも実行しない) に対し、**「提示の進行が動き出す直前の 1 ターン」という抜け穴が塞がれた**。PB-TR-19 / PB-TR-20 は ✅ のまま、回帰 2 本の追加で**期待値が強化**された
- 影響 Scenario PB-TR-01 / 05 / 10 / 13 / 23 / 28 と PB-MD-03 は**アサーション行が文字どおり変化なし**で ✅。既存テストの変更は待ち合わせ点の差し替えのみでアサーション不変
- deviation の新規項目 (attached 中の外側タップ) は実装と一致した記録済みの実装メモで、**未記録の乖離 0 件**
- iOS 全件 **136 tests / 25 suites / 0 failures**。ID 網羅・ミラー検査は verify-001〜003 と同値
- 足場 (specs / design / proposal) の逆流なし
