# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (004 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

決定事項の (A) 残り — Toast の表示時間の独立化と後始末での撤去、待ち条件の硬化、時計付き履歴を添えた時間切れメッセージ — はいずれも実装されており、`evidence/ci-repeat-signature.md` は署名の同定と A/B (旧条件 3/20 → 硬化後 0/50) まで含めて明快で、sanitize も通っている。本体無改変・Scenario 名不変・lint 全通過も確認した。一方で、この change の evidence 自身が実測した「IME を出したまま落ちた回が次のテスト (戻るとホーム) を巻き添えにする」経路が、新設された `finally` の後始末から抜けている。加えて、硬化が効いている機構の帰属 (`bottom > 0` ではなく show 側の「アニメーション停止」待ち) がコメント・evidence の説明とずれており、将来この条件が落とされると署名が黙って戻る。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin テスト・workflow のコメントを書き換えている) |
| `kasane/handbook/cross/test-execution.md` | instrumented テストの変更・完了判定 |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/` の変更・CI の失敗の切り分け |
| `kasane/handbook/cross/runtime-behavior-verification.md` | IME という実行時挙動が絡む不具合の調査と完了判定 |
| `kasane/handbook/cross/ci-script-deletion.md` | `.github/workflows/**` のレビュー (今回の diff に削除操作なし。抵触なし) |
| ksn-core `references/evidence.md` | `evidence/` への抜粋ログの設置 |
| `kasane/lessons/process.md` L-001 / L-002 | 姉妹面の照合 (今回は姉妹面なし) / 主張の範囲を実証範囲に限定 |

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)。

## 実行した検査

| 検査 | 結果 |
|---|---|
| `./gradlew :ksdialogs-core:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `python3 scripts/comment-policy-lint.py --advisory` | 禁止 0 件。要確認 450 件はすべて本変更の外。対象 2 ファイルの該当行なし |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | exit 0 / exit 0 |
| `python3 scripts/scenario-id-coverage.py` | exit 0 (「結果: 未網羅なし」)。Scenario 名と `DialogScreenshotEvidence.capture` の引数は不変 |
| `actionlint .github/workflows/verify-android-instrumented.yml` | exit 0 |
| `python3 scripts/log-sanitize.py evidence/ci-repeat-signature.md` | 置換 0 件 (再実行しても本文と差分なし)。ローカル絶対パス・端末個体・個人情報の混入なし |
| 本体・support の無改変 | `git status` は `.github/workflows/verify-android-instrumented.yml` と `ToastSystemInputTests.kt` の 2 本と未追跡の `evidence/` のみ。`src/main/**`・`support/**`・`exploration.md`・`repeat-android-instrumented.yml` は無改変 |
| `sdkmanager --list_installed` の grep | 出力は `  emulator | 35.x.x | Android Emulator | emulator` 形式で `^[[:space:]]*emulator[[:space:]]` に一致する。直下の system-images 行と同じ実行ファイルパス・同じ `|| echo` の防御で揃っており、`set -euo pipefail` 配下でも `||` が受ける |
| テストの意図 | 「Toast 前と後で IME の出し入れが変わらない」は保たれている (前後とも同じ `awaitImeSettled` で判定し、`isPresenting` の確認位置も不変) |

## 指摘事項

### [🟠 Major] 失敗した回に IME を出したまま抜けるため、次のテストを巻き添えにする経路が残っている

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:76-80`

**問題点**: 新設した `finally` は「途中で落ちた回もウィンドウと観測の口を残さない」と宣言しているが、後始末するのは Toast の器と観測の口だけで、IME の見えは落ちた時点のまま残る。この残り方が次のテストを壊すことは、この change 自身の実測で確認済み — `evidence/ci-repeat-signature.md`「結果 (8 run)」の obs-4 で「戻るとホームが通る」が同時に落ちており、原因は「前のテストが残した可視状態の IME が最初の BACK を食べた」(`HIDE_SOFT_INPUT_REQUEST_HIDE_WITH_CONTROL fromUser true`) と同文書が結論している。`ActivityScenarioRule` による Activity の終了では IME が消えないことが、この観測でそのまま示されている。

硬化によって「引っ込まない」型の失敗は減るが、IME を可視のまま抜ける経路は残る — 62〜67 行 (`awaitImeSettled(target=true)` の後の `isPresenting` / `hasFocus` の 2 つの assertion) と 70 行の hide 側の時間切れは、いずれも IME が出ている状態で例外を投げる。1 件の失敗が 2 件の赤に化ける形が残っている限り、CI の赤の読み取りは今回入れた署名の判別を毎回やり直す必要がある。

**推奨修正**: どちらか (または両方) を入れる。

- `finally` に IME の後始末を足す。`hideSoftInputFromWindow` は今回の署名のとおり捨てられうるので、それだけに頼らず `activity.inputField.clearFocus()` と `activity.window.insetsController?.hide(WindowInsets.Type.ime())` を併せて呼び、短めの上限 (判定はしない) で `isSettled(false)` を待つ
- `Toast_表示中でも戻るとホームが通る` の側で、BACK を起こす前に IME が出ていないことを確かめる (出ていたら引っ込めてから進む)。テスト間の順序依存を受け側でも切れる

### [🟡 Minor] 硬化が効いている機構の帰属がずれており、`animating` 条件が落とされると署名が黙って戻る

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:357-363` と同 `:155-166`、`evidence/ci-repeat-signature.md`「手元での検出力の確認」

**問題点**: `isSettled` の 3 条件のうち `(bottom > 0) == target` は、実質 `visible == target` と同値に近い。`WindowInsets.getInsets(type)` は不可視の type に対して `Insets.NONE` を返す (`InsetsSource.calculateInsets` が不可視なら `NONE`) ため、**`bottom > 0` は `isVisible(ime())` が true であることを含意する**。したがって署名の核心である「要求可視性が約 32 ms だけ hidden に振れる窓」(`evidence/ci-repeat-signature.md`「署名」2) では `isVisible=false` と `bottom=0` が同時に立ち、`bottom` の項はこの窓を弾けない。この change の evidence 自身も、強制再現の節で「`isVisible(ime())` が false の間に `getInsets(ime()).bottom > 0` になる標本」を 30 回探して 0 件だったと記録しており、両者が食い違う観測は 1 件も得られていない。

実際に署名を塞いでいるのは、hide を出す前の `awaitImeSettled(target=true)` が `!animating` まで待つことで、hide#1 が show#1 のアニメーション中に落ちる分岐 (署名の 1) をそもそも作らせない点である。ところが `awaitImeSettled` の doc は「3 条件が揃った時点」とだけ述べ、`isSettled` の doc も 3 条件を並列に扱っている。この帰属のままだと、将来「アニメーションの追跡は重い / `onEnd` が来ない」といった理由で `animating` を外す判断が、`bottom` が守っているという誤解の下で通り、署名が黙って戻る。

**推奨修正**: `isSettled` または `awaitImeSettled` の doc に、「hide を出す前にアニメーションの終了まで待つことが、表示途中の hide が保留になる分岐を作らせない要」であることと、「枠の高さは要求可視性とほぼ同値で、単独では偽の合格を弾けない」ことを書き分ける。evidence の A/B の説明も、硬化後 0 件を 3 条件全体に帰属させるのではなく、どの条件が何を塞いだかで書き直す (L-002)。

### [🟡 Minor] 観測の取り付け自体が IME アニメーションの挙動を変えるため、A/B が 2 変数を同時に動かしている

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:243-248`、`evidence/ci-repeat-signature.md`「旧条件と硬化後の A/B」

**問題点**: attach の doc は「観測を足したこと自体で画面の insets の扱いを変えないため」と述べるが、これは配送方式 (`DISPATCH_MODE_CONTINUE_ON_SUBTREE`) と `onProgress` の素通しについては正しくても、**取り付けたこと自体**には当てはまらない。`InsetsController` は IME のアニメーションを組み立てるときにホストへ `hasAnimationCallbacks()` を問い合わせ、コールバックが 1 つでも登録されているかで所要時間の系列を切り替える。つまりコールバックの登録は framework から観測可能な状態変化であり、IME の出入りの時間軸は観測の有無で変わる。

そのため A/B の「旧条件 20 回 / 硬化後 50 回」は、待ち条件だけでなく観測の有無も同時に変わった 2 版の比較になっている。0/50 という結果自体は強い (旧条件の 15% が保たれるなら 50 回で 0 件は起こりにくい) が、「待ち条件の硬化が効いた」と一意には切り分けられていない。

**推奨修正**: attach の doc の主張を「配送方式と `onProgress` の扱いは変えていない」に限定し、「コールバックの登録自体は framework から見えており、IME アニメーションの時間軸に影響する」ことを併記する。evidence 側は A/B の但し書きとして、2 版が待ち条件と観測の有無の両方で異なることを明記する (L-002)。切り分けまで取り直す必要はない。

### [🟡 Minor] `animating` に stale ガードが無く、`onEnd` の取りこぼしが恒久的な時間切れになる

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:251-279`, `:313`

**問題点**: `runningAnimations` は `onPrepare` / `onStart` で足し、`onEnd` でだけ引く。`onEnd` が届かない回 (取り付けの前後をまたぐアニメーション、`onPrepare` の後に走行が組み立たなかった回、コールバックが差し替わった回) があると、集合が空にならず `isSettled` が恒久的に false になり、待ちは必ず 8 秒の時間切れになる。今回の硬化は「間欠失敗を減らす」ことが目的なので、判定の唯一の要 (前項) に無期限のラッチを置くのは向きが逆になりうる。

**推奨修正**: 走行中の記録に開始時刻を持たせ、IME アニメーションとして妥当な上限 (例: 2 秒) を超えたものは走行中から外す。外したことは履歴に残す (時間切れのメッセージで「取りこぼしで滞留した」と分かる)。

### [🟡 Minor] 履歴の打ち切りが先頭を残して末尾を捨てるため、時間切れの直前が消える

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:292-303`

**問題点**: `record` は `entries.size >= MAX_ENTRIES` になった時点で以降を捨てる。決定事項 (3) の目的は「時間切れの回に何が起きていたかを読めるようにする」ことなので、捨てるべきは古い側で、残すべきは時間切れの直前側になる。状態が変わったときだけ積むので通常は 500 件に届かないが、届くのは状態が細かく振れている回 — つまり最も読みたい回に限って末尾が落ちる。

**推奨修正**: 先頭の数十件を残して以降をリングバッファにするか、少なくとも「先頭 N + 末尾 M」を残す形にする。打ち切りの目印は現状どおり本文に残す。

### [🟡 Minor] 検証 CI の workflow ヘッダが、この change の実測と食い違ったまま残っている

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:11-14`

**問題点**: ヘッダは「手元 (arm64) では自然再現も強制再現も得られておらず、失敗した回の ImeTracker / InsetsController の時系列が唯一の判別材料になる」と述べている。`evidence/ci-repeat-signature.md`「手元での検出力の確認」は、同じ arm64 の AVD で旧条件 20 回中 3 件が CI と同じ署名で再現したと記録しており、この文はもう事実ではない。コメントはそのファイルだけを読む人にとって正しい必要があるので、実測に追い越された記述を残さない。review-003 のアクションプラン 3 も、署名が取れた時点でこのヘッダを恒久の文面へ書き直すことを申し送っている。

**推奨修正**: 「なぜ instrumented job だけ logcat を常設するのか」を現在形で述べる恒久の文面へ書き直す (再現性の当時の見立ては落とす)。

### [🔵 Suggestion] 一時 workflow の削除条件が満たされている

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:10-11`

**問題点**: この workflow は自ら「署名が取れた時点、または 16 run 回しても失敗が出なかった時点で削除する」と宣言しており、`evidence/ci-repeat-signature.md` で 8 run から署名が取れているため条件は満たされている。今回の diff には削除が含まれていない。前項のヘッダ書き直しと同じタイミングで扱う想定なら、この change の完了判定に載せておく。

**推奨修正**: ヘッダの恒久化と併せて削除する (削除自体は本レビューの diff の外なので、判定には含めない)。

### [🔵 Suggestion] `waitUntilEmpty` の置き換えで「期限で表示が終わる」担保が両テストから落ちている

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:74-75`, `:123-124`

**問題点**: 旧コードの `harness.waitUntilEmpty()` は `displayCount == 0`、つまり期限の経過で表示自体が終わることまで見ていた。新しい `awaitContainersDetached` は `containers.isEmpty()` で、直前に呼んだ `changeHost(null)` が機械的に起こす結果 (`onHostChanged` → `detachForReattach`) を確認するにとどまる。表示は coordinator に残り、60 秒のタイマーは後続テストの実行中に発火する。

ただし実害は小さいと判断した — 「duration の経過で消える」は `ToastContractTests` / `ToastTypedShowTests` / `ToastMultiDisplayTests` / `ToastNonModalTests` が同じ `waitUntilEmpty` で担保しており、このテスト固有の保証ではない。Toast は fire-and-forget で dismiss の口を持たないため、期限を待たずに表示を終わらせる手段も無い。決定事項 (1) の「表示時間を IME 待ちの上限から独立させ後始末で撤去」とも整合する。惑わされないよう、`awaitContainersDetached` の doc に「表示自体の終了は別テストが見る」ことを一言添えるとよい。

**推奨修正**: doc の一言追加のみ (構造は現状で可)。

### [🔵 Suggestion] 硬化後も残る別モードの失敗が、change の外へ残らない

**該当箇所**: `evidence/ci-repeat-signature.md`「旧条件と硬化後の A/B」

**問題点**: 硬化後 50 回でも「Toast を出す前から IME が出ない」が 2 件残っており、evidence は別機構 (Activity 起動に伴う server 起源の `HIDE_UNSPECIFIED_WINDOW` が最初の show を潰す) と正しく書き分けている。しかし evidence は蒸留でアーカイブへ移るため、次に CI がこの形で赤くなったとき、切り分けが最初からやり直しになる。

**推奨修正**: 蒸留の際に、この残存モード (署名・見分け方・今回は対象外とした理由) を `kasane/lessons/inbox/` か `handbook/cross/verification-ci.md` の見分け表のいずれか、アーカイブされない側へ 1 行残す。

## アクションプラン

1. **Major 1 件を先に塞ぐ**: `finally` に IME の後始末を足す (`clearFocus` + `insetsController.hide(ime())` + 判定しない短い待ち)。受け側 (戻るとホーム) の防御も入れるとより堅い
2. Minor 2 件 (機構の帰属 / 観測の影響) は doc と evidence の書き直しだけで、コードは動かさない。L-002 の適用範囲なので同じ回で入れる
3. Minor 2 件 (`animating` の stale ガード / 履歴の打ち切り方向) は挙動の改善。1 と同じ回で入れられる規模
4. Minor 1 件 (workflow ヘッダの恒久化) と Suggestion の一時 workflow 削除は対で扱う。この change を閉じる前に
5. Suggestion 2 件 (doc の一言 / 残存モードの持ち出し) は蒸留でよい
