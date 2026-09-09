# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (006 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

review-005 の Major (`@Ignore` / 素の `.disabled(` という無承認の抜け道) は塞がれている — 実物のテストファイルへ一時的に注入した検証で、Kotlin の `@Ignore` と Swift の `.disabled(...)` / `XCTSkip` が違反として拾われ、前提条件の表現 (`assumeTrue`) とヘルパ自身は拾われないことを確認した。Minor 5 件も、コード・evidence・handbook・config の全面で入っている。`git stash` による欠落は見当たらない (`finally` の IME 後始末・`SkipOnCi.kt`・3 workflow の CI 引数・handbook 3 本と `concepts/log.md` の整合はすべて揃っている)。ビルド・lint・selftest 20 項目はすべて通る。

一方、修正で新しく見えた食い違いが 3 件ある。(1) 新設した規約が名指しする Swift 側の共通プリミティブ `BridgeTestWaiting` には settle 待ちも観測履歴も存在せず、review-005 の Minor 3 が Android 側だけで解かれている。(2) 無効化手段の一律検査が 3 走査根に限られ、CI で実際に回る他の 3 テストルート (MAUI の NUnit・kmp・Android の JVM 単体) は素通りするのに、規約は「上の 2 つ以外の書き方で外さない」と全称で書いている。(3) LD_CO_13 の「出の途中」の窓が 500 ms の時限保持で、この change 自身が定めた「中間状態はポーリングで捕まえず仕掛けで確定させる」に届いていない。いずれも小さな修正で閉じられる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / Swift / Python / workflow のコメントを新設・改稿している) |
| `kasane/handbook/cross/ci-flaky-test-policy.md` | 本 change が新設・改訂。レビュー対象であり、かつ観測・切り分け・skip の照合規約 |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/**` の変更・job ごとの実行条件・並列スイートの飢餓の見分け |
| `kasane/handbook/cross/test-execution.md` | instrumented / Swift テストの変更と件数の確認 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | IME・入力の宛先という実行時挙動が絡む調査と完了判定 |
| `kasane/handbook/cross/ci-script-deletion.md` | `scripts/**` の改稿 (`ci-skip-lint.py` に削除操作なし。抵触なし) |
| ksn-core `references/handbook.md` / `doc-structure.md` / `evidence.md` / `paths.md` | handbook の改訂・証跡の設置・成果物のパス記述 |
| `kasane/lessons/process.md` L-001 / L-002 | 姉妹面の照合 (Swift ヘルパ 2 本と待ちの共通プリミティブ) / 主張の範囲を実証範囲に限定 |
| kotlin-impl-skill / swift-ui-impl-skill / github-workflow-skill | 変更が触る言語面と workflow |

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)。

## 実行した検査

| 検査 | 結果 |
|---|---|
| `./gradlew :ksdialogs-core:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `python3 scripts/ci-skip-lint.py --selftest` | 20 項目すべて OK (exit 0)。`.enabled(if:)` だけは自己テストの検体に無い |
| `python3 scripts/ci-skip-lint.py` / `--list` | 印 0 件 / 許可リスト 0 件、違反なし (deviation の「skip は 1 件も適用していない」と一致) |
| **無効化手段の実物確認** | 走査根の実ファイルの写しに `@Ignore` (`ToastAttributeTests.kt`) と `.disabled("面倒なので CI で止める")` (`DialogNotifierTests.swift`) を注入したところ、両方が `承認手順を通らない skip` として違反に載った。ヘルパ 2 本 (`.disabled(if:)` を含む) は拾われない |
| 走査根の既存債務 | `@Ignore` / `.disabled(` / `.enabled(if:)` / `XCTSkip` は 3 走査根で 0 件 (追跡・未追跡とも)。検出を足したことによる既存違反は発生しない |
| 走査対象の起点 | `git ls-files --cached --others --exclude-standard` に切り替わり、`ios/DerivedData/` 等の生成物は歩かない (review-005 指摘 8 の解消)。実測で 198 ファイルを走査 |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py` | いずれも exit 0 |
| `doc-structure-lint.py --paths` (`ci-flaky-test-policy.md` / `verification-ci.md`) | 違反なし |
| `actionlint .github/workflows/*.yml` | 指摘 4 件 (`ci.yml:61` / `verify-ios.yml:34` / `verify-kmp.yml:33` / `verify-maui.yml:37`) はいずれも本変更が触っていない領域の既存分。`verify-android-instrumented.yml` は 0 件 |
| YAML パース (workflow 6 本 + `kasane/config.yaml`) | 全件 OK。`workflow_call:` の入力を空にした後も構文は成立 |
| 作業ツリーの欠落 (stash の申告に対する確認) | `ToastSystemInputTests.kt:76-80` の `finally` (`restoreHidden` → `detachToasts` → `detach`)、`support/SkipOnCi.kt` / `ImeSettleWaiting.kt` / `InstrumentedStateSettling.kt`、Swift ヘルパ 2 本、3 workflow の CI 引数 (`ksdialogsCi` / `TEST_RUNNER_KSDIALOGS_CI` × 2)、handbook 3 本と `kasane/concepts/log.md` の 4 行 — すべて在り、相互参照も揃っている |
| 本体の無改変 | `git status` に `src/main/**` の変更なし。差分はテスト・support・workflow・lint・handbook・change 配下のみ |
| `assertWindowFocused` の観測の独立性 | 6 フィールドは 3 系統 (成立 / 旗 / 相手ウィンドウ)。`hasWindowFocus` と `rootHasWindowFocus` が独立でないことは doc が明記済み。`LoadingContainer` は `android.app.Dialog` を継承し独自ウィンドウを持つため、新しい 4 か所すべてで `otherWindowFocused` が非 null になり、3 系統が実際に成立する |
| 時間切れ経路 | `awaitSettled` は述語の真偽を先に見てから期限を見るため、期限直前に成立した回も拾う。時間切れ時は `StateHistory.format()` (head 100 / tail 400、捨てた区間を明示) が assertion メッセージに載る |
| `ImeSettleWaiting` の観測追加が現象を変えないか | `windowFocus` は `readState()` の読みが 1 つ増えるだけで、要求も配送も変えない。`onProgress` は insets を素通しし、コールバック登録が framework から見えることは doc が明記済み (所要時間の A/B に使えない旨も) |
| LD_CO_13 の検出力 | `LoadingCoordinator.kt:197-198` により、カスタム View 世代への合流でも `latestMessage` は書かれる。第 2 世代 (既定ローディング) が引き継げば `builtinText` に "A" が出るため、`assertNull` は空振りでない。追加された `coalescedUseCount == 2` の待ちが前提を固定している |
| BV-MA-03 の撤去経路 | `BridgeTestOverlayObserver.added` は window の subview から都度算出するため、`removeFromSuperview()` で確実に空になる。`ToastCoordinator.kt:163` の期限タイマーは `[weak self]` で、テスト内で作った `MauiToastBridge` が解放されれば 60 秒後の発火は無害。`BridgeTestSharedHost.run` の `cleanup: () async -> Bool` に対し `return await ...` は型として正しい |

## review-005 指摘の解消確認

| review-005 の指摘 | 状態 | 確認箇所 |
|---|---|---|
| 🟠 Major: `@Ignore` / 素の `.disabled(` が検査ゼロ | **解消** | `scripts/ci-skip-lint.py:78-89` の `BANNED_SKIPS` (`@Ignore` / `.disabled(` / `.enabled(if:)` / `XCTSkip` 系) と `:274-291` の `collect_bans`。許可リストとの突き合わせを持たず一律違反。ヘルパ 3 本のみ `SKIP_HELPER_FILES` で除外。`Assume` / `#require` は対象外で、判定の境目 (「CI かどうかで実行を分けているか」であって結果が skipped かではない) を `kasane/handbook/cross/ci-flaky-test-policy.md:92-96` が明記。selftest 20 項目・実物注入の両方で確認 |
| 🟡 BV-MA-03 の対処が期限の 2 倍化で根拠が 1 回 | **解消** | 期限延長は撤回され `MauiBridgeContentSupplyTests.swift:20` は 60 秒 + `:139-148` の明示的撤去。`evidence/ci-flake-triage.md:28` に 11 回反復 0 回と観察の所要 (0.313〜0.323 秒)、`:38` に「3.26 秒はテスト全体の所要」の書き分け |
| 🟡 規約の「合意」「履歴」が共通プリミティブで満たされない | **一部解消** | `InstrumentedStateSettling.assertWindowFocused` が 3 系統の合意 + 安定 + `StateHistory` を持ち、`ImeSettleWaiting.State.isSettled` に `windowFocus` が入った。Swift 側は未対応 (指摘 1) |
| 🟡 LD_CO_13 の合流が検査されていない | **解消** | `LoadingCoalescingTests.kt:349-353` の `coalescedUseCount == 2` の待ち。`:375` に「旧世代の中身を引き継がない」も追加 |
| 🟡 evidence の帰属と版 | **解消** | `evidence/ci-flake-triage.md:30-38` の版の定義表 (A / B / C) と、`evidence/ci-repeat-signature.md:110` の但し書き (待ち条件と観測の取り付けが同時に変わっている)・`:124` の 2 本の食い違いの説明 |
| 🟡 許可リストの provenance | **解消** | `ci-flaky-test-policy.md:79` (diff に `lint.ci-skip.allow` の追加があれば `deviation.md` にオーナー指示の記録。無ければ CHANGES_REQUESTED) と `:113` のレビュー照合項目。lint 側は `ci-skip-lint.py:370-390` が `mechanism` 空 / `approved` 不在・不正・未来日を違反にする (selftest 済み) |
| 🔵 7: 許可リストに書く識別子 | 解消 | `ci-flaky-test-policy.md:75` に `--list` の出力をそのまま使う旨と、入れ子の型で食い違う理由 |
| 🔵 8: 走査が生成物を歩く | 解消 | `ci-skip-lint.py:205-238` が `git ls-files` 起点になり、fallback の実体走査も `DerivedData` を除外 |
| 🔵 9: `ci.yml` の lint 列挙 | 解消 | `.github/workflows/ci.yml:9-10` に `ci-skip-lint` と「等」 |
| 🔵 10: パスの書き方 | ほぼ解消 | handbook 参照はリポジトリ相対になった。`evidence/` 内の隣接参照はリンクとしては辿れるが change 相対ではない (指摘 6) |
| 🔵 11: 別モードの署名の持ち出し | 未対応 (蒸留へ申し送り。review-005 の判断どおりこの回では不要) | `evidence/ci-repeat-signature.md:135` |

## 指摘事項

### [🟡 Minor] 新設規約が名指しする Swift 側の共通プリミティブに settle 待ちも履歴も無い

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:30`、`maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestWaiting.swift:4-17`、`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:125-129`

**問題点**: 規約は「この待ちは共通プリミティブとして置き、テストごとに書き起こさない。Android instrumented は `InstrumentedStateSettling`、その IME 版が `ImeSettleWaiting`、MAUI iOS 橋渡しは `BridgeTestWaiting` の settle 待ちを使う。時間切れの説明文には、単調時計付きの観測履歴 (`StateHistory`) を添える」と述べる。しかし `BridgeTestWaiting` が持つのは 5 ms 間隔の素朴な `waitUntil` だけで、合意・非進行・安定のいずれも判定せず、履歴も持たない。`StateHistory` に相当するものも Swift 側には無い。

実際、BV-MA-03 は「枚数が 2 に達した瞬間は途中の並びとも一致する」という、規約が名指しする当の問題に自力で対処しており、その落ち着き待ち (`waitUntil(timeout: settleTimeout) { added.count > 2 }`) をテスト本体に書き起こしている — 規約が「テストごとに書き起こさない」と定めた形そのもの。規約の `applies-when.paths` には `ios/Tests/**` も入っているが、そちらには名指しされたプリミティブすら無い。

review-005 の Minor 3 は「規約を実態へ寄せるか、プリミティブへ履歴を足すか」の選択だった。実装は後者を Android 側だけで採ったため、Swift 側で食い違いが残っている。規範層はコードが従う側なので、この状態は「規約は満たしていないが動いている」を既定にする (L-001 の姉妹面照合の対象でもある)。

**推奨修正**: どちらか。(a) `BridgeTestWaiting` に settle 待ち (述語が一定時間続けて成り立つこと) と時間切れ時の履歴を足し、BV-MA-03 の書き起こしをそれへ寄せる。(b) 規約の当該 2 文を実態に合わせ、共通プリミティブの要求を Android instrumented に限定し、Swift 側は「落ち着き待ちを挟む」までを求める形にして `BridgeTestWaiting` の名指しと `StateHistory` の要求を外す。

### [🟡 Minor] 無効化手段の一律検査が 3 走査根に限られ、CI で回る他の 3 テストルートは素通りする

**該当箇所**: `scripts/ci-skip-lint.py:64-69` (`SCAN_ROOTS`)、`kasane/handbook/cross/ci-flaky-test-policy.md:92-96`

**問題点**: 規約は「**上の 2 つ以外の書き方で、テストを CI の実行から外さない。**」と全称で書き、`ci-skip-lint.py` が一律で違反にすると述べる。しかし走査根は `android/**/src/androidTest/`・`ios/Tests/`・`maui/macios/native/**/KsDialogsMauiBridgeTests/` の 3 つで、CI が実際に回す残り 3 ルートは検査の外にある。

- `maui/KsDialogs.Maui.Tests/` (NUnit、24 ファイル) — `[Ignore("...")]` / `[Explicit]` / `[Test(Description=...)]` 相当の無効化が可能で、`.cs` は `BANNED_SKIPS` に定義自体が無い
- `kmp/ksdialogs-kmp/src/{commonTest,iosTest,androidHostTest}/` (35 ファイル) — `kotlin.test.Ignore` が使える
- `android/**/src/test/` (23 ファイル) — `@Ignore` が使える。走査根のコメントは「実行機の揺らぎを持たないローカル単体テスト」を*印*の対象外にする理由を述べているが、それは「そこに `@SkipOnCi` を置く場所が無い」理由であって、「そこで `@Ignore` を使ってよい」理由ではない

review-005 の Major が根拠にした「承認された道より安く、痕跡も残らない出口」は、これら 3 ルートにもそのまま当てはまる。実測したところ 3 ルートとも既存の該当箇所は 0 件なので、今なら既存債務なしで閉じられる (Major を閉じたときと同じ条件)。境界が意図的なら、規約にも script の docstring にも「どこまでが機械の歯止めの届く範囲か」が書かれていないのが問題で、読む側は全称の禁止文だけを見ることになる。

**推奨修正**: `SCAN_ROOTS` に 3 ルートを足し、`.cs` の `BANNED_SKIPS` (NUnit の `[Ignore` / `[Explicit`) を定義する。または境界を意図的なものとして残すなら、規約の当該節と script の docstring に「機械の検査が届くのは 3 ルートで、他ルートはレビューが見る」と明記し、`ci-flaky-test-policy.md:117` の「走査根の外は検査されない」の但し書きに具体名を挙げる。

### [🟡 Minor] LD_CO_13 の「出の途中」が 500 ms の時限保持で、規約自身が求める「仕掛けで確定させる」に届いていない

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:335-341`、`:364-368`、`:444`

**問題点**: 新設した規約は「通り過ぎる一瞬 (「出の途中」のような中間状態) をポーリングで捕まえない。……その状態をテスト側の仕掛け (完了を押さえる演出フック) で**確定させる**」と定める。今回の対処は `dismissal = { delay(DISMISSAL_HOLD_MILLIS) }` で 500 ms 押さえ、その間に `waitUntil { containerState == DISMISSING }` (8 ms 間隔) が当たることに賭ける形で、状態は確定していない。押さえが時限である以上、実行機が 500 ms 止まれば旧来と同じ「窓を取り逃がす」経路が戻る — しかもその時の失敗は `assertTrue(InstrumentedDialogWaiting.waitUntil { ... })` (`:364-368`) にメッセージが無く、素の `AssertionError` になる。これは今回 CI で観測された LD_CO_13 の落ち方そのものの再現形で、切り分けがまた最初からになる。

同じファイルは `LoadingTestGate` (`support/LoadingTestFixtures.kt:104-116`、`CompletableDeferred` ベース) を既に持っており、`dismissal = { gate.await() }` にすれば「新世代を開始するまで出の演出は完了しない」が構造として保証される (手順: `hide()` を async → `DISMISSING` を確認 → `show()` → `gate.open()` → `hiding.await()`)。窓の広さが実行機に依存しなくなる。

**推奨修正**: `DISMISSAL_HOLD_MILLIS` (`:444`) の時限保持を `LoadingTestGate` による決定的な保持へ置き換える。時限のまま進めるなら、少なくとも `:364-368` の待ちに失敗時のメッセージ (何を待っていたか・観測できた状態) を添え、時間切れの回に窓を取り逃がしたのか状態が来なかったのかを区別できるようにする。

### [🔵 Suggestion] `.disabled(` の一律検出が、将来の正当な SwiftUI 修飾子と KDoc 中の言及を巻き込む

**該当箇所**: `scripts/ci-skip-lint.py:79-89`、`:294-307`

**問題点**: `ios/Tests/` は `path_marker` が空で配下の `.swift` を全件走査し、`.disabled\s*\(` を一律違反にする。同ディレクトリには `import SwiftUI` するファイルが 10 本以上あり、無効状態のコントロールを確かめるテストが将来 `.disabled(true)` を書けば、承認の余地なく落ちる。逃げ道は `SKIP_HELPER_FILES` にハードコードされたファイル単位の除外だけで、行単位の抑止手段が無い。あわせて `_strip_line_comment` は行コメントしか落とさないため、KDoc / ブロックコメントに `@Ignore` と書いた説明文 (規約の引用など) も違反になる。

現状は誤検出 0 件なので実害は出ていないが、承認運用が始まってから詰まると、歯止め自体を外す圧力になる。

**推奨修正**: Swift の検出を trait の位置 (`@Test(` / `@Suite(` の引数並び) に限るか、`// ci-skip-lint: allow <理由>` のような行単位の抑止をひとつ用意する。ブロックコメントも `_strip_line_comment` と同じ扱いで落とす。

### [🔵 Suggestion] 「件数検査が skip があっても実行数の不足を検出できる」が実測より広い主張になっている

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:90`

**問題点**: 規約は「実行数を「tests から skipped を引いた数」で見る件数検査は、skip があっても実行数の不足を検出できる」と述べ、これが skip を許容する側の安全網として読める。実物の検査は `verify-android-instrumented.yml:319` が `executed <= 0` のときだけ、`verify-ios.yml` / `verify-maui.yml` の Swift 件数検査は合算 0 件のときだけ落ちる。つまり検出できるのは「全件が消えた」ときだけで、1 件の skip が増えても検査は緑のまま通る。L-002 (主張の範囲を実証した範囲に限定する) の対象。

**推奨修正**: 「skipped を実行数から除くので、skip した分を実行済みとして数え上げない」までに主張を狭めるか、期待件数の下限をどこかで持つ形にする。

### [🔵 Suggestion] evidence 内の隣接ファイル参照が change 相対になっていない

**該当箇所**: `evidence/ci-flake-triage.md:3`、`evidence/ci-repeat-signature.md:124`

**問題点**: `[ci-repeat-signature.md](ci-repeat-signature.md)` / `[ci-flake-triage.md](ci-flake-triage.md)` はリンクとしては解決するが、ksn-core `references/paths.md` は同じ change 内の成果物を change ディレクトリからの相対パス (`evidence/ci-repeat-signature.md`) で書くことを求めている。handbook 参照は今回リポジトリ相対へ直っており、ここだけ形が揃っていない。

**推奨修正**: 表示文字列を `evidence/<file>` にする (リンク先の相対パスはそのままでよい)。

### [🔵 Suggestion] BV-MA-03 の撤去が coordinator を通らないことの安全性が、コメントから読み取れない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:139-148`

**問題点**: 新しい片付けは器の View を直接 `removeFromSuperview()` する。Android 側の同型 (`ToastSystemInputTests.detachToasts` が `harness.changeHost(null)` で coordinator に提示先の離脱を伝える) と違い、coordinator は「表示中」のまま 60 秒の期限タイマーを抱えて残る。読んだだけでは「60 秒後に別のテストの最中で発火するのでは」と疑える形になっている。

実際には安全であることを確認した — `ToastCoordinator.kt:163` の期限タイマーは `[weak self]` で self を保持せず、テスト内で生成した `MauiToastBridge` が解放されれば発火時に何もしない。ただしこの根拠はテスト側からは見えない。

**推奨修正**: 撤去のコメントに「coordinator は表示中のまま残るが、期限タイマーは coordinator を保持しないのでテストをまたいで作用しない」を 1 文足す。

## アクションプラン

1. **Minor 2 (走査根の範囲)** を先に決める — 3 ルートを足すか、境界を規約と docstring に明記するか。既存債務 0 件なので足す側が安い
2. **Minor 1 (Swift 側の共通プリミティブ)** — 規約 2 文を実態へ寄せる (b) が最小。`BridgeTestWaiting` に settle 待ちを入れる (a) を選ぶなら BV-MA-03 の書き起こしもそこへ寄せる
3. **Minor 3 (LD_CO_13)** — `LoadingTestGate` への置き換え。置き換えないなら待ちに失敗メッセージを足す
4. Suggestion 4 / 5 は承認運用が始まる前に入れておくと詰まらない。6 / 7 は文言だけで、蒸留に回してもよい
5. review-005 の Suggestion 11 (別モードの署名の持ち出し) は引き続き蒸留へ申し送り
