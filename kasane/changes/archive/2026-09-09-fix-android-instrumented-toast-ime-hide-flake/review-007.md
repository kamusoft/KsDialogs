# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (007 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

review-006 の Minor 3 件・Suggestion 4 件はいずれも入っている。特に LD_CO_13 の関門化は、`CoroutineStart.UNDISPATCHED` と `Dispatchers.Main.immediate` の組み合わせで**要求の順序が実行機の速さに依らず確定する**ことをコードを追って確認でき、時限保持の賭けが構造的な保証に置き換わっている。関門を握ったまま assertion が落ちる経路も追ったが、このクラスに `@After` が無く harness / coordinator がテストごとに独立しているため、失敗時にハングする経路は無い (撤去コルーチンが 1 本残るだけ)。Swift の `awaitSettled` / `StateHistory` は Android 側と同型で、handbook の表に載る 3 つの名前 (`InstrumentedStateSettling.awaitSettled` / `StateHistory`、`DialogTestWaiting.awaitSettled` / `DialogTestStateHistory`、`BridgeTestWaiting.awaitSettled` / `BridgeTestStateHistory`) はすべて実物と一致する。Android instrumented・iOS・MAUI iOS 橋渡しの 3 ビルドはすべて成功し、lint 一式と 26 項目の `--selftest` も通る。

一方で 2 件、直っていない食い違いがある。(1) 走査根を 6 つに広げたが、**CI が実際に回す Kotlin JVM テストルートがもう 1 つある** — `maui/android/native/ksdialogs-maui-bridge/src/test/` (`.github/workflows/verify-maui.yml:158` が回す 9 ファイル)。実物へ `@Ignore` を注入して素通りすることを確認した。にもかかわらず script の docstring と規約本文は「CI が回すテストルート**すべて**に掛かる」と全称で書いており、review-006 の Minor 2 が問題にした形 (全称の禁止文と実際の到達範囲の食い違い) がそのまま残っている。(2) lint job の検査集合は accepted な cross/ADR-0017 (cross/ADR-0020 が 6 検査へ改訂) が決めており、ADR-0020 の Context は「無断で足すこともできない」と明記しているが、7 検査目の追加が change のどこにも記録されていない。どちらも小さく閉じられる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / Swift / Python / workflow のコメントを新設・改稿している) |
| `kasane/handbook/cross/ci-flaky-test-policy.md` | 本 change が新設・改訂。レビュー対象であり、かつ観測・切り分け・skip の照合規約 (「レビューでの照合」4 点を含む) |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/**` の変更・lint job の検査の列挙・並列スイートの飢餓の見分け |
| `kasane/handbook/cross/test-execution.md` | instrumented / Swift テストの変更と件数の確認 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | IME・入力の宛先という実行時挙動が絡む調査と完了判定 |
| `kasane/handbook/cross/ci-script-deletion.md` | `scripts/**` の改稿 (`ci-skip-lint.py` に削除操作なし。抵触なし) |
| `kasane/decisions/cross/0017-verification-ci-structure-and-guarantee.md` / `0020-lint-job-includes-spm-sync-script-selftest.md` | lint job に検査 step を追加している (指摘 2) |
| ksn-core `references/handbook.md` / `doc-structure.md` / `evidence.md` / `paths.md` | handbook の改訂・証跡の設置・成果物のパス記述 |
| `kasane/lessons/process.md` L-001 / L-002 | 姉妹面の照合 (Swift 2 面の待ちプリミティブ) / 主張の範囲を実証範囲に限定 (指摘 1・4) |
| kotlin-impl-skill / swift-ui-impl-skill / github-workflow-skill | 変更が触る言語面と workflow |

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)。

## 実行した検査

| 検査 | 結果 |
|---|---|
| `./gradlew :ksdialogs-core:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `xcodebuild build-for-testing -scheme KsDialogsMauiBridge` (maui/macios/native) | ** TEST BUILD SUCCEEDED ** |
| `xcodebuild build-for-testing -scheme KsDialogs` (ios) | ** TEST BUILD SUCCEEDED ** |
| `python3 scripts/ci-skip-lint.py --selftest` | 26 項目すべて OK (exit 0)。`[Explicit]` と `Assert.Ignore(` だけ検体が無い |
| `python3 scripts/ci-skip-lint.py` / `--list` | 印 0 件 / 許可リスト 0 件、違反なし。走査は 291 ファイル (ios 114 / android-instrumented 73 / kmp 35 / maui-nunit 35 / android-unit 23 / maui-bridge 11) |
| **走査根の穴の実測** | `maui/android/native/ksdialogs-maui-bridge/src/test/.../MauiToastPassthroughTests.kt` に `@org.junit.Ignore("CI で面倒なので止める")` を一時注入 → **違反 0 件のまま素通り** (注入後にファイルを復元済み。`git status` clean) |
| `_strip_comments` の境界 | 入れ子ブロックコメント / KDoc / 行コメント / 文字列内エスケープ / Kotlin の char リテラルの 7 検体で誤検出・取りこぼしなし。ブロックコメントの depth が行をまたいで漏れているファイルは 291 件中 0 件 (末尾 15 行が全消しになるファイルの走査) |
| `_swift_trait_lines` の適用範囲 | `ios/Tests` / `maui/macios/native` の `.swift` で `@Test` / `@Suite` が行頭以外に現れる箇所は 0 件 (行頭前提が現状のコードで成立する) |
| `[Explicit]` の巻き込み | 走査根の `.cs` 35 ファイルで既存の該当 0 件 (誤検出なし) |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py` | いずれも exit 0 |
| `doc-structure-lint.py --paths` (`ci-flaky-test-policy.md` / `verification-ci.md` / handbook の index 2 本) | 違反なし |
| **LD_CO_13 の順序の確定性** | `async(start = UNDISPATCHED)` は現スレッド (instrumentation スレッド) で `Loading.show` → `LoadingCoordinator.beginUse` の `withContext(Dispatchers.Main.immediate)` まで同期に進み、そこで main の Handler へ post して戻る。続く `dismissalGate.open()` の再開も別スレッドからの `complete` なので Main へ post される。**両者が同じ Looper に FIFO で並ぶ**ため、`acquireUse` → `waitForPendingDismissal` の中断が関門解放より先に成立する。テスト本体が main スレッドで走る場合 (inline 実行) でも順序は同じ。時限保持に戻る経路は無い |
| **関門を握ったまま落ちる経路** | `LoadingCoalescingTests` に `@After` / `tearDown` は無く harness は毎テスト新規。`dismissing` の assertion が落ちると `coroutineScope` が `hiding` を取り消し (`Job.join()` は取り消し可能)、AssertionError がそのまま上がる — **ハングせず失敗する**。coordinator の `scope` に撤去コルーチンが 1 本残るが、coordinator も Activity もテスト単位で作り直されるため後続へ波及しない |
| LD_CO_13 の検出力 | 第1世代がカスタム View になっても `LoadingCoordinator.kt:197-198` で `latestMessage` は書かれ、第2世代の `acquireUse` が `latestMessage = null` に戻す。`assertNull("旧世代のメッセージを引き継がない", harness.builtinText)` は空振りでない。前段の `coalescedUseCount == 2` がその前提を固定している |
| Swift `awaitSettled` の時間切れ経路 | 述語が崩れたら `heldSince` を捨てて数え直し、`now >= deadline` で `settled: false` を返す。`Outcome.message` が `format()` (head 100 / tail 400、捨てた区間を明示) を assertion の説明文に載せる。`waitUntil` と違い期限直前の 1 回の成立では抜けない (安定を要求する側として正しい) |
| BV-MA-03 の観測の独立性 | `added` (今の顔ぶれ) と `attachedEver` (取り付けの履歴) は別系統で、`added==2 && attachedEver==2` は「途中で出入りが無かった」を実際に縛る。`failingSupply.count` は 3 系統目。ただし `added.count` と `added.first` は同じ読みからの派生 (指摘 4) |
| 撤去の安全性 (review-006 Suggestion) | `MauiBridgeContentSupplyTests.swift:159-162` のコメントが「橋渡しは表示中のまま残るが期限タイマーは橋渡しを保持しない」を明記。`ToastCoordinator.kt:163` の `[weak self]` と一致 |
| 本体の無改変 | `git status` に `src/main/**` の変更なし。差分はテスト・support・workflow・lint・handbook・change 配下のみ |
| review-006 以降に触れたファイル | mtime で確認 — `BridgeTestWaiting.swift` / `DialogTestWaiting.swift` / `MauiBridgeContentSupplyTests.swift` / `LoadingCoalescingTests.kt` / `ci-flaky-test-policy.md` / `ci-skip-lint.py` の 6 本のみ。review-006 で確認済みの範囲 (`ToastSystemInputTests.kt` / `LoadingAttributeTests.kt` / `InstrumentedStateSettling.kt` / `ImeSettleWaiting.kt` / workflow 4 本) は変わっていない |

## review-006 指摘の解消確認

| review-006 の指摘 | 状態 | 確認箇所 |
|---|---|---|
| 🟡 Minor 1: Swift 側の共通プリミティブに settle 待ちも履歴も無い | **解消** | `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestWaiting.swift:72-108` の `awaitSettled` (`Reading` で合意を呼び出し側が組み立て、`stable` 48 ms、崩れたら数え直し) と `:111-172` の `BridgeTestStateHistory` (単調時計・変化時だけ記録・中ほど切り捨て)。`ios/Tests/KsDialogsTests/Support/DialogTestWaiting.swift` に同型 (呼び出し側は無いが deviation の「仕組みの対称性を優先」に一致)。BV-MA-03 の書き起こしは `MauiBridgeContentSupplyTests.swift:135-149` で `awaitSettled` に寄せられ、規約 `ci-flaky-test-policy.md:32-36` の表の 3 面 6 名前がすべて実物と一致 |
| 🟡 Minor 2: 無効化手段の検査が 3 走査根に限られる | **一部解消** | `scripts/ci-skip-lint.py:78-84` の `SCAN_ROOTS` が 6 ルートへ拡張され、`.cs` の `BANNED_SKIPS` (`[Ignore(` / `[Explicit` / `Assert.Ignore(` / `Assert.Inconclusive(`) が定義された。`ci-flaky-test-policy.md:5` の `applies-when.paths` も追随。既存違反 0 件。**ただし CI が回す 7 つ目のルートが漏れている** (指摘 1) |
| 🟡 Minor 3: LD_CO_13 が 500 ms の時限保持 | **解消** | `LoadingCoalescingTests.kt:341` の `dismissal = { dismissalGate.await() }` と `:381-383` の `async(start = CoroutineStart.UNDISPATCHED)` → `dismissalGate.open()` → `showing.await()`。`DISMISSAL_HOLD_MILLIS` は撤去済み。`:352` / `:361` / `:372` の 3 つの待ちに前提を述べる失敗メッセージが付いた |
| 🔵 Suggestion 4: `.disabled(` の一律検出と KDoc の巻き込み | **解消** | `ci-skip-lint.py:333-380` の `_strip_comments` (行 / ブロック / 入れ子 / 文字列) と `:387-408` の `_swift_trait_lines` (`@Test` / `@Suite` の引数並びに限定)。7 検体で実測 |
| 🔵 Suggestion 5: 件数検査の主張が実測より広い | **解消** | `ci-flaky-test-policy.md:98` が「skip した分を実行済みとして数え上げない」に縮み、`:100` が「落ちるのは実行数が 0 のときだけ」「skip が 1 件増えたことは検出しない」を明記。`verify-android-instrumented.yml:319` (`executed <= 0`) / `verify-ios.yml:158` (`total == 0`) と一致 |
| 🔵 Suggestion 6: evidence 内の隣接参照 | **解消** | `evidence/ci-flake-triage.md:3` / `evidence/ci-repeat-signature.md:124` の表示文字列が `evidence/<file>` になった |
| 🔵 Suggestion 7: BV-MA-03 の撤去の安全根拠 | **解消** | `MauiBridgeContentSupplyTests.swift:159-162` に「期限タイマーは橋渡しを保持しないのでテストをまたいで作用しない」の 1 文 |
| 🔵 review-005 の 11 (別モードの署名の持ち出し) | 未対応 (蒸留へ申し送り。この回では不要) | `evidence/ci-repeat-signature.md:135` |

## 指摘事項

### [🟡 Minor] CI が回す Kotlin JVM テストルートが 1 つ走査根から漏れており、規約と docstring の全称が事実に反する

**該当箇所**: `scripts/ci-skip-lint.py:78-84` (`SCAN_ROOTS`)、`scripts/ci-skip-lint.py:31-32` (docstring)、`kasane/handbook/cross/ci-flaky-test-policy.md:106`、同 `:5` (`applies-when.paths`)

**問題点**: 走査根は 6 つになったが、`maui/android/native/ksdialogs-maui-bridge/src/test/` (Kotlin の JVM 単体テスト、追跡済み 9 ファイル) が入っていない。このルートは `.github/workflows/verify-maui.yml:158` の `./gradlew :ksdialogs-maui-bridge:test` で **CI が毎回回している**。`.kt` の走査根は `android/` と `kmp/` の 2 つで、`maui/` の走査根は `.cs` だけなので、このルートには `@Ignore` の検査が一切掛からない。

実測で確かめた — `MauiToastPassthroughTests.kt` のクラス宣言に `@org.junit.Ignore("CI で面倒なので止める")` を注入しても `python3 scripts/ci-skip-lint.py` は「違反なし」(exit 0) のまま通る。

問題は穴そのものより、**穴があるのに全称で書いていること**にある。`ci-skip-lint.py:31-32` は「検査は CI が回すテストルートすべて (SCAN_ROOTS) に掛ける — 正規の印を置ける 3 つと、印の仕組みを持たない 3 つ」と書き、`ci-flaky-test-policy.md:106` も「この検査は **CI が回すテストルートすべて**に掛かる」と太字で断言し、内訳として 6 ルートを数え上げている。読む側は「機械が全部見ている」として読む。review-006 の Minor 2 は「全称の禁止文と実際の到達範囲の食い違い」を問題にしたもので、走査根を 3 → 6 に増やしても、CI の実行から導出し直していないため同じ食い違いが残っている (L-002 の対象)。

`:131` の「上の 6 ルートに当たらない置き場に新設したテスト — は検査されない」は将来の新設についての但し書きで、**今ある CI 実行ルートの漏れ**は打ち消せない。

**推奨修正**: `SCAN_ROOTS` に `("maui-android-unit", "maui", ".kt", None, r"/src/test/")` を足す (既存の該当は 0 件なので既存債務は発生しない)。あわせて `ci-flaky-test-policy.md:5` の `applies-when.paths` に `maui/**/src/test/**` を足し、`:106` と docstring の内訳を「印の仕組みを持たない 4 つ」に直す。全称を残すなら、CI の実行コマンド (`verify-*.yml` の `test` / `connectedDebugAndroidTest` / `xcodebuild test` / `dotnet test`) から導出したことが分かる形で数え上げる。

### [🟡 Minor] lint job の検査集合を固定する accepted ADR に対し、7 検査目の追加が記録されていない

**該当箇所**: `.github/workflows/ci.yml:256-257`、`kasane/decisions/cross/0017-verification-ci-structure-and-guarantee.md:35`、`kasane/decisions/cross/0020-lint-job-includes-spm-sync-script-selftest.md`、`deviation.md`

**問題点**: cross/ADR-0017 の Decision は「lint job は … の **5 検査**を持つ」を決定の項目として持ち、cross/ADR-0020 (accepted) がそれを **6 検査**へ置き換えている。ADR-0020 の Context は「cross/ADR-0017 は lint job の検査を 5 つに固定しているため、**検査を無断で足すこともできない**」と明記し、自己テストを 1 つ足すために ADR を 1 本起こしている。

本変更は `CI skip allowlist lint` を 7 検査目として追加し、`ci.yml` のコメントと `verification-ci.md` の lint job 行は 7 検査に追随しているが、**ADR 側は 6 検査のまま**で、`deviation.md` にも `exploration.md` にも ADR に触れる記述が無い (`grep` で 0 件)。deviation が記録しているオーナー指示は「この change のスコープを workflow 系の問題を片付けることに広げる」であって、ADR-0017 / 0020 の決定を改訂する指示ではない。

ADR の起票・改訂はレビューの権限ではないので、ここでは指摘にとどめる。ただし**この回で何も記録が残らないと蒸留にも届かない**。ADR-0020 が「無断で足せない」と書いた当のものが、無断で足された状態になる。

**推奨修正**: この change では `deviation.md` に「lint job の検査を 7 へ増やした。cross/ADR-0017 (0020 改訂済み) の検査集合の改訂が要る」を記録し、蒸留で cross/ADR-0017 への amends を起票する。オーナー指示が実際にあったなら日付と要旨を deviation に足す (`ci-flaky-test-policy.md:87` が許可リストの追加に求めているのと同じ形)。

### [🔵 Suggestion] `ci-skip-lint.py --selftest` が CI で走らず、検出器の退行が検出されない

**該当箇所**: `.github/workflows/ci.yml:256-257`、`scripts/ci-skip-lint.py:12`

**問題点**: lint job が回すのは `python3 scripts/ci-skip-lint.py` だけで、26 項目の `--selftest` は手元で走らせる想定になっている。この検査は「承認手順を通らない skip」という**規約の歯止め**そのものなので、検出器が壊れて 0 件を返すようになっても CI は緑のまま通り、規約は文だけが残る。

これは cross/ADR-0020 が同期スクリプトの自己テストについて述べた理由 (「実装時に 1 度走らせるだけでは、スクリプトを後から変えたときに事前検証の退行が検出されない」) と同じ形で、しかも当の自己テストは lint job で毎回走っている。片方だけ手元に残るのは非対称。

あわせて自己テストの検体は `[Ignore(...)]` と `Assert.Inconclusive(...)` だけで、同時に足した `[Explicit]` と `Assert.Ignore(...)` は 1 度も確認されていない (`--selftest` の出力に対応する項目が無い)。

**推奨修正**: lint job の step を `python3 scripts/ci-skip-lint.py --selftest && python3 scripts/ci-skip-lint.py` にする (指摘 2 の ADR 改訂と同じ回で扱える)。`[Explicit]` / `Assert.Ignore(...)` の検体も `--selftest` に足す。

### [🔵 Suggestion] BV-MA-03 の落ち着き待ちが、残った 2 枚目が「後続の Toast のもの」だと言える観測を使っていない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:124-149`

**問題点**: 待ちの述語は `added.count == 2` / `attachedEver?.count == 2` / `added.first === firstContainer` / `failingSupply.count == 1` の 4 つだが、そのうち 2 つ (`added.count` と `added.first`) は同じ `added` の 1 回の読みから出ており、コメントの「独立した 4 つの観測」は実際には 3 系統 (L-002 の対象)。

より効くのは、**すぐ隣に用意されているのに一度も観測されていない** `followingSupply` (`:124`) が使えることで、`followingSupply.count == 1` を述語に足せば「後続の Toast の中身が実際に作られた」= 残っている 2 枚目が後続のものだ、が言える。今の述語は「中身なしの 1 枚が器を作って居座り、後続がまだ取り付いていない」並びを 48 ms 続けて読んだ場合と区別できない — それはまさにこの Scenario が否定したい状態で、`attached.count == 2` の assertion はその並びでも通ってしまう。

旧実装 (`waitUntil { added.count > 2 }` の一律 300 ms) は成立しない条件を待ち続ける形だったので、結果として観察窓が 300 ms あった。落ち着き待ちは成立した時点で抜けるため窓は最短 ~48 ms に縮んでおり、識別できる観測を足しておくほうが安全側になる。

**推奨修正**: 述語と `settled` の説明文字列に `followingSupply.count == 1` を足し、コメントの「独立した 4 つの観測」を実際の系統数に合わせる。

### [🔵 Suggestion] NUnit ルートで許される前提条件の書き方が規約に示されていない

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:104`、同 `:110`、`scripts/ci-skip-lint.py:113-118`

**問題点**: 規約は `:110` で「前提条件の表現は skip ではない」として JUnit の `Assume` / `assumeTrue` と Swift の `#require` を名指しし、判定の境目を「CI かどうかで実行を分けているか」だと述べる。一方 `:104` は NUnit の `Assert.Inconclusive(...)` を一律禁止に挙げるが、NUnit で前提条件を表す正規の書き方 (`Assume.That`) はどこにも書かれていない。`Assume.That` は NUnit では `Assert.Inconclusive` と同じ Inconclusive を起こすため、走査根の `.cs` 35 ファイルを書く側からは「前提条件をどう書けば通るのか」が読み取れない。

現状は該当 0 件なので実害は出ていないが、承認運用が始まってから詰まると歯止めを外す圧力になる (review-006 の Suggestion 4 と同じ型)。

**推奨修正**: `:110` の前提条件の列挙に NUnit の `Assume.That` を加える。

## アクションプラン

1. **指摘 1 (走査根の穴)** — `SCAN_ROOTS` に 1 行、`applies-when.paths` に 1 パス、docstring と規約 `:106` の内訳を「4 つ」に直す。既存債務 0 件なので今なら安く閉じられる
2. **指摘 2 (ADR の記録)** — `deviation.md` に 1 行。ADR の amends 起票自体は蒸留の仕事なので、この回は記録だけでよい
3. **指摘 3 (`--selftest` の CI 化)** — 指摘 2 と同じ step を触るので同時に。検体 2 つの追加も
4. 指摘 4 / 5 は文言と述語 1 項の追加。承認運用が始まる前に入れておくと詰まらない
5. review-005 の Suggestion 11 (別モードの署名の持ち出し) は引き続き蒸留へ申し送り
