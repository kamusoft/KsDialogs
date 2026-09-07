# 一致検証 (差分): add-presentation-behavior (verify-002)

- **判定: VALID**
- 検証日: 2026-08-22
- 種別: **差分検証**。[verify-001](verify-001.md) (VALID) の後、レビュー指摘の修正サイクル 2 周目で入った **iOS のみの変更**の影響範囲を再検証したもの
- verify-001 は書き換えていない。本書には **verify-001 からの変化だけ**を記す (変化のない Requirement / Scenario の対応表は verify-001 が引き続き正)
- 集計 (61 Scenario 全体): **✅ 55 / ⚠️ 6 / ❌ 0** — verify-001 から**増減なし**

---

## 1. 差分の範囲

再検証の対象は iOS のみ。Android / MAUI / KMP は変更がないため再実行していない (verify-001 の結果が引き続き有効)。

| 種別 | ファイル |
|---|---|
| 新設 (internal) | `ios/Sources/KsDialogs/Presentation/DialogOutcomeDelivery.swift` |
| 変更 | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift` |
| 変更 | `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift` |
| テスト追加 | `DialogTransitionTests.swift` の `deliveryOutlivesContainerRelease` / `PB_TR_12_containerIsReleasedAfterHostLossDelivery`、`UIKitDialogPresentationSurfaceTests.swift` の `dismissCompletionArrivesFromPresentingViewController` |
| テスト支援追加 | `Support/DialogWeakContainerReference.swift`・`Support/RecordingPresentingViewController.swift`・`Support/DialogTestPresentationSurface.swift` の `simulateHostLossOfTopmostContainer()` |
| 文書 | `deviation.md` の実装メモ節に 3 項目追記 |

読み取った実装の要点:

- `DialogOutcomeDelivery` は **器とは別の寿命**を持ち、届け先 (`destination`) と配送待ちの outcome (`pendingOutcome`) を所有する。`isDelivered` で **配送は先着 1 回だけ**に絞られる。届け先が未設定でも `submitLatchedOutcome()` を受け取れ、`setDestination` の時点で配送される (取りこぼしなし)
- 器の `deinit` が同じ口へ `submitLatchedOutcome()` を引き継ぐ (`DialogContainerViewController.swift:177`)。撤去まで進めないまま器が解放されても呼び出し元は取り残されない
- `viewDidDisappear` の後始末は **器を強保持**した Task で進め、`presentingViewController == nil` を確認してから `isRemovalRequested` の有無で `completeRemoval()` / `handleHostLost()` に分岐する (`:360-379`)
- `containerState = .removed` は `completeRemoval()` の中だけで立つ (`:522-529`)。撤去要求済みかどうかは `isRemovalRequested` フラグが持ち、退出中の入力遮断もこのフラグで判定する (`:574`・`:615`)
- `DialogPresenter.swift:68` が `container.outcomeDelivery.setDestination { … }` で届け先を配線する

公開面への影響なし: `DialogOutcomeDelivery.swift` に `public` は 0 件、`outcomeDelivery` プロパティも修飾子なし (internal)。公開 API 形状の正の検査 (`DialogApiSurfaceCompileChecks.swift` 等) はテストビルドに同梱されており、下記の全件 green がその通過を含む。

---

## 2. テスト再実行

| ルート | コマンド | 実測 | 結果 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=3B42B268-…'` (iPhone 17 / **iOS 26.0**) | **133 tests / 25 suites** (verify-001 は 130 / 25。**+3 = 追加テスト 3 本と一致**) | 0 failures (`** TEST SUCCEEDED **`) |

| 検査 | 結果 | verify-001 からの変化 |
|---|---|---|
| `scripts/scenario-id-coverage.py --require-mirror` | 仕様 61 ID / 検出 58 ID / 除外 3、**未網羅なし**、**ミラー OK** | 変化なし |

Android (unit 50 / instrumented 138) / KMP (51) / MAUI (62 + bridge 15) / 負の検査 10 本は変更がないため未実行 (verify-001 の実測が有効)。

---

## 3. 影響 Scenario の再点検

依頼された 12 Scenario をテスト本文まで読み直した。**すべて ✅ のまま**で、期待値が緩んだものはない。

### 3.1 配送・撤去に直接触れる 5 件

| Scenario | 実装 | テスト | 期待値の変化 | 状態 |
|---|---|---|---|---|
| PB-TR-09 OS 発の器消失ではフック非実行で即 cancelled | `DialogContainerViewController.swift:498-517` (`waitsForHostRemoval: false` 経路) → `completeRemoval()` | `PB_TR_09_hostLossSkipsHooks` | **変化なし** (`.cancelled` / dismissal 0 回 / `containerState == .removed` / `contentHost == nil`)。器消失経路は完了を待たないため `removed` が即立つ点も従来どおり | ✅ |
| PB-TR-10 show は dismissal 完了と撤去より先に返らない | `finishRemoval()` → 完了通知 → `completeRemoval()` → `outcomeDelivery.submitLatchedOutcome()` | `PB_TR_10_deliveryWaitsForDismissalAndRemoval` | **強化**。従来の検査 (フック完了前は閉鎖要求なし / 完了通知前は器が残り未配送) に加えて `containerState == .dismissing` (「撤去の完了通知が来るまでは撤去済みにならない」) が追加された。spec の THEN は従来どおり満たす | ✅ |
| PB-TR-12 退出中の OS 発器消失はフックをキャンセルして即配送 | 同上 + `outcomeDelivery` | `PB_TR_12_hostLossDuringDismissalDeliversLatchedOutcome` (**変化なし**) に加えて **新規 `PB_TR_12_containerIsReleasedAfterHostLossDelivery`** — 配送後に器を握り続ける者がいないこと (`DialogWeakContainerReference.isReleased`) を固定。強保持の後始末が配送後に残らないことの回帰止め | 追加のみ (既存の期待値は不変) | ✅ |
| PB-TR-13 添付なしでも配送は撤去後 | 同上 | `PB_TR_13_defaultTransitionDeliversAfterRemoval` | **強化**。`containerState == .dismissing` の検査が追加 | ✅ |
| PB-TR-23 提示中の OS 発器消失は cancelled | `handleHostLost()` → `finishRemoval(waitsForHostRemoval: false)` | `PB_TR_23_hostLossDuringPresentationDeliversCancelled` | **変化なし** (`.cancelled` / `cancelled(presentation)` / `removed`) | ✅ |

### 3.2 状態機械に触れる 7 件

| Scenario | テスト | 期待値の変化 | 状態 |
|---|---|---|---|
| PB-TR-05 presentation 中の閉鎖信号は完走後に退出 | `PB_TR_05_closureDuringPresentationWaitsForPresentation` | 変化なし (イベント列の完全一致検査のまま) | ✅ |
| PB-TR-06 複数の閉鎖信号でも dismissal は1回 | `PB_TR_06_dismissalHookRunsAtMostOnce` | 変化なし | ✅ |
| PB-TR-19 提示開始前の報告は演出なし | `PB_TR_19_reportBeforePresentationSkipsHooks` | 変化なし | ✅ |
| PB-TR-20 提示開始前の呼び出し元キャンセル | `PB_TR_20_callerCancellationBeforePresentationSkipsHooks` | 変化なし (`.created` 段階の検査を含む) | ✅ |
| PB-TR-22 退出中の入力は無視される | `PB_TR_22_inputDuringDismissalIsIgnored` | 変化なし。入力遮断の判定が `isRemovalRequested` へ移った後も `isUserInteractionEnabled == false` / `hitTest == nil` / 最初の報告値の配送はそのまま成立 | ✅ |
| PB-TR-28 presentation 中のキャンセル | `PB_TR_28_callerCancellationDuringPresentation` | 変化なし | ✅ |
| PB-TR-29 退出中のキャンセルで脱出 | `PB_TR_29_callerCancellationDuringDismissal` | 変化なし (`containerState == .removed` を含む) | ✅ |

### 3.3 Scenario に紐づかない追加テスト

- `deliveryOutlivesContainerRelease` — 器を解放した後もラッチ済みの結果が 1 回だけ届くこと。Requirement「結果のラッチと配送」の**内部不変条件**を固定するもので、既存 Scenario の期待値を置き換えてはいない
- `dismissCompletionArrivesFromPresentingViewController` — 提示元へアニメーションなしの閉鎖が 1 回依頼され、その完了通知が撤去の完了として流れること。deviation.md「撤去完了の合図が 2 つある」の一方 (提示層からの完了通知) を固定する

いずれも ID を持たないため `scenario-id-coverage.py` の母数・検出には影響しない (再実行して確認済み: 58/61 + 除外 3 で verify-001 と同値)。

### 3.4 ⚠️ 6 件の扱い

verify-001 の ⚠️ 6 件 (deviation.md「オーナー確認待ち (暫定採用)」2 項目由来 — iOS プリセット factory の `@MainActor` が 5 件、attached 中の alpha 復帰時点が 1 件) は、今回の変更で**解消も悪化もしていない**。⚠️ のまま据え置く。

---

## 4. deviation.md の新規 3 項目

いずれも「実装メモ (契約の範囲内、公開面に影響なし)」節に 2026-08-22 付で記録されており、**未記録の乖離ではない**。実装と記述の対応も確認した。

| 追記 | 実装との対応 | 判定 |
|---|---|---|
| design Decision 5-1「iOS の『器の撤去の後』の観察点が 2 つある」への追記 — `viewDidDisappear` 側は `presentingViewController == nil` まで確認してから進み、通常の撤去では完了通知が先着する | `DialogContainerViewController.swift:370` の `guard self.presentingViewController == nil else { return }` と一致。`dismissCompletionArrivesFromPresentingViewController` が完了通知側を固定 | 記録済み ⚠️ 相当ではなく**内部実装メモとして整合** |
| design Decision 5-1「配送口を器から切り離した」 — `DialogOutcomeDelivery` (internal、公開 API 増減なし)。器が撤去まで進めないまま解放される場合は `deinit` が同じ口へ引き継ぐ | 新設ファイルと `deinit` (`:177-183`)、`DialogPresenter.swift:68` の配線と一致。`public` 0 件を確認 | 同上 |
| design Decision 10「`removed` になる時点」 — 撤去要求から完了通知までは退出中のままとし、要求済みは内部フラグで持つ。撤去要求後の入力遮断は状態ではなくフラグで判定 | `isRemovalRequested` (`:107`・`:498`・`:574`・`:615`) と `completeRemoval()` 内の `containerState = .removed` (`:525`) に一致。PB-TR-10 / PB-TR-13 の `.dismissing` 検査が観察側から固定 | 同上 |

**未記録の乖離: なし。**

design Decision 10 の遷移表 (`created → attached → presenting → shown → dismissing → removed`) は変わっていない。今回の変更は「`removed` を立てる時点を『撤去要求時』から『撤去完了 + 後始末時』へ寄せた」もので、`removed` の定義 (「器を撤去し、確定済みの結果を配送した後」) に**より忠実**になっている。spec 本文には状態名が現れないため、Scenario の期待値への影響もない。

---

## 5. 足場の逆流検査

`git status` / `git diff` (読み取りのみ) で確認:

- `specs/`・`design.md`・`proposal.md`: **差分ゼロ** (HEAD `66ea298` のまま)。**逆流なし**
- `tasks.md`・`ui/brief.md` の変更は verify-001 で確認済みのもの (チェック付与 / 純粋な追記) から変化なし
- 新規の未追跡ファイルとして `review-002.md`・`second-opinion-code-002.md`・`verify-001.md` が増えている (レビュー 2 周目の成果物。足場ではない)

---

## 6. 判定

**VALID** (verify-001 から変化なし)

- 影響 Scenario 12 件はすべて ✅ のまま。うち PB-TR-10 / PB-TR-13 は期待値が**強化**され (`dismissing` の明示検査)、PB-TR-12 は同 ID のテストが 1 本増えた。緩んだ期待値はない
- iOS 全件 **133 tests / 25 suites / 0 failures** (追加 3 本ぶん増加)。ID 網羅とミラー検査も verify-001 と同値
- deviation.md の新規 3 項目は実装と一致した記録済みの実装メモであり、**未記録の乖離は 0 件**
- 足場 (specs / design / proposal) の逆流なし
- verify-001 の所見 (test-execution.md の件数が古い / kmp-cancellation 証跡の「129 tests」/ UI のオーナー最終承認が未取得) は未解消のまま引き継ぐ。特に **iOS の件数は 130 → 133 へさらに動いた**ので、蒸留時の実測更新で拾うこと
