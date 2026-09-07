# 一致検証 (差分): add-presentation-behavior (verify-003)

- **判定: VALID**
- 検証日: 2026-08-22
- 種別: **差分検証 (3 回目)**。[verify-002](verify-002.md) (VALID) の後、オーナーが実機で発見した iOS の入場ちらつきを design Decision 10 どおりへ修正した分の再検証
- verify-001 / verify-002 は書き換えていない。本書には **verify-002 からの変化だけ**を記す
- 集計 (61 Scenario 全体): **✅ 56 / ⚠️ 5 / ❌ 0** — verify-002 (✅ 55 / ⚠️ 6 / ❌ 0) から **⚠️ 1 件が ✅ へ解消**

---

## 1. 差分の範囲

iOS のみ。Android / MAUI / KMP は変更がないため再実行していない (verify-001 の実測が引き続き有効)。

| 種別 | ファイル |
|---|---|
| 変更 (本体) | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift` |
| テスト追加 | `DialogTransitionTests.swift` の回帰テスト「中身の表示と presentation フックの開始の間に描画の機会がない」+ 定数 2 本 |
| テスト変更 | `DialogOutsideTapTests.swift` 2 本 (中身が見えるまで待ってから観察) |
| テスト支援 | `Support/DialogLayoutMeasurement.swift` に `waitUntilContentIsVisible` |
| 文書 | `deviation.md` — Decision 10 の項が**オーナー却下 → 修正**、Decision 3 の項が**オーナー承認済み**へ更新。実装メモに修正内容を追記 |
| 証跡 | `verification/first-frame-ios/` (README + `before/` 5 枚 + `after/` 9 枚) |

### 読み取った実装の要点

- `beginPresentation()` (`:409-424`) は**状態遷移とタスク起動だけ**を行うようになった。同期の alpha 復帰はここから外れている。タスク本体の先頭に `guard !Task.isCancelled, self.containerState == .presenting else { return }` があり、取り消し済みで始まったときは中身を見せないまま退く
- `runPresentationPhase()` (`:432-437`) が

  ```swift
  async let overlay: Void = self.runOverlayFade(toVisible: true)
  contentView.alpha = contentInitialAlpha
  await runHook(resolvedTransition?.presentation, phase: "presentation")
  _ = await overlay
  ```

  の順になり、**alpha 復帰と presentation フックの最初の同期処理が同一の同期区間**に入った。覆いのフェードだけが `async let` の子タスクで並走する (覆いは alpha 0 から始まるため 1 ターン遅れても見えるものが変わらない)
- design Decision 10 の字面「presenting でオーバーレイのフェード開始と同時に表示しフック開始」に**一致した**

### 回帰テストの中身 (ID なし)

`contentBecomesVisibleInTheSameTurnAsThePresentationHook` — 器を直接組み立て、器が進行を始めたのと同じ同期区間で MainActor Task を積み、**その Task がまだ走っていない時点でフックの最初の同期処理が終わっている**ことを観察する (`laterWorkSeenByHook == false`)。あわせて器の組み立て直後は `contentView.alpha == 0`、フック開始時点では `alpha == 1` を固定する。「見せる」と「フックが効く」の間に描画の機会がないことを構造的に押さえており、**同じ退行が再び入れば落ちる**。

### 証跡 (A/B)

`verification/first-frame-ios/README.md` は、`xcrun simctl io recordVideo` の録画を `AVAssetReader` でフレーム単位に切り出す方法で観察している (連写では 1 フレームを捕まえられないため)。

- **before**: `slide-up-f0002-6.462-FLICKER-final-position.png` に「覆いもなく不透明なカードが最終位置に 1 フレーム描かれている」— オーナー報告と一致する再現。次フレーム (`f0003-6.480-offscreen`) で画面外へ消える
- **after**: `slide-up-f0004/f0005-no-card` (カードが出ない) → `f0010-entering-from-bottom` (下辺から入ってくる) → `f0020-sliding-up`。最終位置のフレームは無い。Zoom は `zoom-f0007-scaled-down-start` (0.8 倍から開始)、Custom Hook は `custom-f0005-offset-start` (ずらした開始位置) と、いずれも**フックの開始状態が最初に見える**形へ変わった

「修正前に実環境で症状を再現し、修正後に同一手順で解消を確認する」という実行時挙動の検証規約どおりの A/B になっている。

---

## 2. テスト再実行

| ルート | コマンド | 実測 | 結果 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=3B42B268-…'` (iPhone 17 / **iOS 26.0**) | **134 tests / 25 suites** (verify-002 は 133。**+1 = 回帰テスト 1 本と一致**) | 0 failures (`** TEST SUCCEEDED **`) |

| 検査 | 結果 | verify-002 からの変化 |
|---|---|---|
| `scripts/scenario-id-coverage.py --require-mirror` | 仕様 61 ID / 検出 58 ID / 除外 3、**未網羅なし**、**ミラー OK** | 変化なし (回帰テストは ID を持たないため母数・検出に影響しない) |

Android (unit 50 / instrumented 138) / KMP (51) / MAUI (62 + bridge 15) / 負の検査 10 本は未実行 (変更なし)。

---

## 3. ⚠️ だった 6 件の扱いの更新

### 3.1 PB-TR-01 — **⚠️ → ✅ に解消**

| 項目 | verify-002 | verify-003 |
|---|---|---|
| 状態 | ⚠️ (deviation「オーナー確認待ち (暫定採用)」) | **✅** |
| 理由 | iOS は凍結時点で alpha を同期復帰し、フックは直後の MainActor ホップで開始 (design Decision 10 の「同時」に対する差)。理屈上フック開始前に最終位置が 1 フレーム見え得る | **オーナーが実機で 100% 再現するちらつきとして却下 → design どおりへ修正**。alpha 復帰とフックの最初の同期処理が同一同期区間に入り、暫定採用の根拠が消滅した |

実装 (`DialogContainerViewController.swift:409-437`)・回帰テスト・実機 A/B 証跡の 3 点が揃っており、**design に対する差分が無くなった**ため ⚠️ を外す。Scenario 本文の THEN (フックが 1 回・ホスト View・UI スレッド・ウィンドウ上・レイアウト済み・表示状態) の検査は `PB_TR_01_presentationHookRunsOnceWithHostView` で**変化なし**のまま成立している。

### 3.2 PB-TR-16 / 17 / 18 / 26 / 27 — ⚠️ 据え置き、備考を「承認済み」へ更新

| 項目 | verify-002 | verify-003 |
|---|---|---|
| 状態 | ⚠️ (deviation「オーナー確認待ち (暫定採用)」) | **⚠️ (deviation 記録済み — オーナー承認済みの合意済み差分)** |
| 備考 | iOS プリセット factory 4 本の `@MainActor` は design の完全シグネチャに無い。**却下されたら実装修正へ戻す前提の暫定** | 同じ差分だが **2026-08-22 にオーナー承認済み**。「実装を直すか deviation として合意するか」の宙吊りは解消し、**合意済み差分として確定**した。引数名・順・既定値・戻り型・`none()` 無引数は design どおりで、spec 本文の THEN には影響しない |

ksn-verify の ⚠️ は「deviation 記録済み」を表すため、合意が確定しても記号は ⚠️ のまま (❌ ではない / ✅ でもない)。**判定上の意味は「VALID を妨げない合意済みの差分」で確定**した。

---

## 4. 影響 Scenario の再点検

依頼された 5 件に加え、presenting 経路に触れる周辺も読み直した。**すべて ✅ のまま、期待値が緩んだものはない。**

| Scenario | 実装 | テスト | 期待値の変化 | 状態 |
|---|---|---|---|---|
| PB-TR-05 presentation 中の閉鎖信号は完走後に退出 | `beginPresentation()` のタスクが `runPresentationPhase()` を待ってから `shown` → `beginDismissal()` | `PB_TR_05_closureDuringPresentationWaitsForPresentation` | 変化なし (イベント列 started→finished(presentation)→started→finished(dismissal) の完全一致検査のまま) | ✅ |
| PB-TR-19 提示開始前の報告は演出なし | `beginLifecycle()` が `resultChannel.isResultSettled` を見て `scheduleImmediateRemoval()` へ (`:386-394`)。この経路は `beginPresentation()` を通らないので中身も見せない | `PB_TR_19_reportBeforePresentationSkipsHooks` | 変化なし (`probe.events.isEmpty`) | ✅ |
| PB-TR-20 提示開始前の呼び出し元キャンセル | 同上 + タスク先頭の取り消し済みガード | `PB_TR_20_callerCancellationBeforePresentationSkipsHooks` | 変化なし (`.created` 段階の検査 + `probe.events.isEmpty`) | ✅ |
| PB-TR-23 提示中の OS 発器消失は cancelled | `handleHostLost()` → `finishRemoval(waitsForHostRemoval: false)`。`runHook` は同じ `lifecycleTask` の中なので取り消しが伝わる | `PB_TR_23_hostLossDuringPresentationDeliversCancelled` | 変化なし (`.cancelled` / `cancelled(presentation)` / `removed`) | ✅ |
| PB-TR-28 presentation 中の呼び出し元キャンセル | 同上 | `PB_TR_28_callerCancellationDuringPresentation` | 変化なし (`cancelled(presentation)` あり / `finished` なし / dismissal 1 回)。フックが `async let` の子タスクから直接 `await` へ変わっても取り消しの伝播は同じ | ✅ |
| PB-TR-01 (再掲) | `:409-437` | `PB_TR_01_presentationHookRunsOnceWithHostView` | 変化なし。**⚠️ → ✅** (3.1) | ✅ |

verify-002 で強化された PB-TR-10 / PB-TR-13 の `.dismissing` 検査、PB-TR-09 / 12 / 22 / 29 の期待値はいずれも**今回の変更で触れられていない**(全件 green で裏付け)。

### 既存テスト 2 本の変更 (`DialogOutsideTapTests`)

`outsideTapPredicateSeparatesBackdropFromContent` と内側タップの 1 本に `waitUntilContentIsVisible(stage.container)` の待ちが 1 行入っただけで、**アサーション自体は完全に不変** (`hitTest` の帰属・`isOutsideTap` の判定・結果が確定しないこと)。alpha 復帰が 1 ターン後ろへ動いたことに対する**待ち合わせ点の調整**であり、本体の挙動を変えたものではない (`UIView.alpha` は `layer.opacity` と同じ記憶域のため alpha 0 のままでは `hitTest` が中身を返さない、という制約も deviation に記録されている)。Android で先例のある「テスト側の待ち合わせ点を正す」修正と同型で、保証を緩めていない。

---

## 5. deviation.md の更新と未記録の乖離

| 項目 | 内容 | 判定 |
|---|---|---|
| Decision 10 (attached 中のコンテンツ非表示) | 節が「オーナー確認済みの項目」へ改称され、**オーナー却下 (2026-08-22) → design どおりへ修正**と明記。修正内容 (alpha 復帰を進行タスクへ / フックを直接 `await` / タスク先頭の取り消しガード / 覆いは `async let` 継続) と、不採用案の理由 (`Task.immediate` は iOS 26+ で最低対象 OS iOS 17 に届かない・`layer.opacity` は `alpha` と同記憶域で回避にならない)、既存テスト 2 本の待ち合わせ変更まで実装メモに記録済み | **記録済み。実装と一致** |
| Decision 3 (iOS factory の `@MainActor`) | **オーナー承認済み (2026-08-22、合意済み差分)** と明記 | **記録済み。合意確定** |
| Decision 5-1 / Decision 10 (`removed` の時点) 他 (verify-002 で確認済みの 3 項目) | 変化なし | 記録済み |

**未記録の乖離: なし。** 今回の変更で新たに生じた差分はすべて deviation.md に反映されている。

---

## 6. 足場の逆流検査

`git status` / `git diff` (読み取りのみ):

- `specs/`・`design.md`・`proposal.md`: **差分ゼロ** (HEAD `66ea298` のまま)。**逆流なし**
- 今回の修正は「実装を design に合わせた」方向であり、design を実装に合わせて書き換える逆流ではない

---

## 7. 所見 (判定には影響しない申し送り)

1. **`deviation.md` の前書きが節名と食い違っている** — 3 行目に「『オーナー確認待ち』の項目は暫定採用であり、オーナーが却下した場合は実装修正へ戻す」と残っているが、節は「オーナー確認済みの項目 (旧「オーナー確認待ち (暫定採用)」)」へ改称され、確認待ちの項目はもう無い。文言の整理は蒸留時に
2. **iOS のテスト件数は 130 → 133 → 134 と動いた**。`concepts/cross/conventions/test-execution.md` の実測値 (2026-08-19 の ios 90 / instrumented 94 / kmp 48 / maui 44 / bridge 9 / 負の検査 17 本) は**全ルートで古いまま**。蒸留時に実測へ更新すること (verify-001 の所見 1 の再掲・更新)
3. `verification/kmp-cancellation/README.md` の「ios/ 全件 129 tests」も同様に鮮度差 (verify-001 の所見 2)
4. **UI のオーナー最終承認は引き続き未取得** (`ui/brief.md`)。ただし今回の入場ちらつきはオーナーが実機で見つけて却下 → 修正まで至っており、演出の見え方についてはオーナーの目が入った

---

## 8. 判定

**VALID**

- **⚠️ 1 件 (PB-TR-01) が ✅ へ解消**。残る ⚠️ 5 件はオーナー承認済みの合意済み差分として確定し、宙吊りが無くなった
- 影響 Scenario (PB-TR-05 / 19 / 20 / 23 / 28 と PB-TR-01) はすべて ✅。期待値が緩んだものはなく、既存テストの変更も待ち合わせ点の 1 行追加のみでアサーションは不変
- iOS 全件 **134 tests / 25 suites / 0 failures**。ID 網羅・ミラー検査は verify-001 / 002 と同値
- deviation.md の更新は実装と一致しており、**未記録の乖離 0 件**
- 足場 (specs / design / proposal) の逆流なし。修正は実装を design へ寄せる方向
