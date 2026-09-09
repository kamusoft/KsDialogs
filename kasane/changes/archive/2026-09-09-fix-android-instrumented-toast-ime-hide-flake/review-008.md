# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (008 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

review-007 の Minor 2 件・Suggestion 2 件 (対応対象) はいずれも実体として入っている。走査根の穴は実測で塞がったことを確認した — `maui/android/native/**/src/test/` に `@Ignore` を注入すると違反 1 件で落ち、同時に `src/main/` へ注入した分は拾わない (`path_marker` が本体を巻き込んでいない)。`--selftest` は lint job の同じ step で本検査より先に走り、GitHub の既定シェル (`bash -e`) で最初の失敗が step を落とす。deviation の ADR 記録も入り、handbook の数字 (`:106` の「4 つ」・`:131` の「7 ルート」・`:5` の `applies-when.paths`) は SCAN_ROOTS の実体と一致する。

残るのは `scripts/ci-skip-lint.py:31-32` の docstring だけで、内訳が 3+3=6 のまま更新されておらず、直前の行の「CI が回すテストルートすべて (SCAN_ROOTS)」および handbook `:106` の「4 つ」と食い違う。ただし今回は**実体より狭く書いている**方向の食い違い (「掛かっていない」と読ませる) なので、歯止めを過信させる危険はない。1 行の追随なので蒸留前に閉じることを勧めるが、変更を止める理由にはしない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Python docstring・workflow コメントの改稿) |
| `kasane/handbook/cross/ci-flaky-test-policy.md` | 本 change が改訂した規約そのもの (走査根の数え上げ・前提条件の表現・レビューでの照合 4 点) |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/ci.yml` の lint job に検査 step を追加 |
| `kasane/handbook/cross/ci-script-deletion.md` | `scripts/**` の改稿 (削除操作なし。抵触なし) |
| `kasane/handbook/cross/test-execution.md` | CI が回すテストルートの網羅性の確認 |
| `kasane/decisions/cross/0017-...md` / `0020-...md` | lint job の検査集合 (deviation の記録内容の照合) |
| `kasane/lessons/process.md` L-002 | 主張の範囲を実証範囲に限定 (指摘 1) |
| ksn-core `references/handbook.md` / `paths.md` | handbook の改訂・成果物のパス記述 |
| github-workflow-skill / kotlin-impl-skill | 触った workflow と Kotlin 面 |

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)。

## 実行した検査

| 検査 | 結果 |
|---|---|
| `python3 scripts/ci-skip-lint.py --selftest` | 26 項目すべて OK (exit 0) |
| `python3 scripts/ci-skip-lint.py` / `--list` | 印 0 件 / 許可リスト 0 件、違反なし (exit 0) |
| **新走査根の到達 (実測)** | `maui/android/native/ksdialogs-maui-bridge/src/test/.../ZzInjected.kt` に `@org.junit.Ignore(...)` を新規作成 → **違反 1 件で exit 1**。同時に `src/main/kotlin/ZzInjectedMain.kt` にも同じ注釈を置いたが**検出されない** (`path_marker` `/src/test/` が本体を巻き込まない)。両ファイル削除後に再実行して exit 0、`git status` に残留なし |
| CI 実行ルートからの網羅性の導出 | `verify-*.yml` の実行 7 系統 (`connectedDebugAndroidTest` / `gradlew test` (android) / `allTests` (kmp) / `dotnet test` (`maui/KsDialogs.Maui.Tests`) / `:ksdialogs-maui-bridge:test` / `xcodebuild test` (ios) / `xcodebuild test` (maui bridge)) が SCAN_ROOTS の 7 根に 1 対 1 で対応。kmp のテストソースセットは `commonTest` / `iosTest` / `androidHostTest` の 3 つで、`/src/[A-Za-z]*[Tt]est/` にすべて当たる。**取りこぼしのある実行系統は無い** |
| `.github/workflows/ci.yml` の YAML パース | OK |
| `actionlint .github/workflows/ci.yml` | 指摘 1 件 (`:61` の SC2001。本 change の diff 外の既存箇所) |
| lint step の失敗伝播 | `defaults.shell` / step の `shell:` 指定なし → GitHub 既定の `bash -e {0}`。`--selftest` が非 0 なら 2 行目に進まず step が落ちる |
| lint job の step 順序 | gitleaks → local-path → identity → comment-policy → scenario-id → **CI skip allowlist lint** → SPM sync selftest の 7 検査。`verification-ci.md:24` の列挙と一致 |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py` | いずれも exit 0 |
| `doc-structure-lint.py --paths` (`ci-flaky-test-policy.md` / handbook の index 2 本) | 違反なし |
| review-007 以降に触れたファイル | `ci-skip-lint.py` / `ci.yml` / `ci-flaky-test-policy.md` / `deviation.md` の 4 本のみ (`MauiDialogClosureReportTests.kt` は mtime のみ更新され内容は無改変 = 注入実測の復元跡)。テスト・support・他 workflow は review-007 で確認済みの状態から変わっていない |
| 本体の無改変 | `git status` に `src/main/**` の変更なし |

## review-007 指摘の解消確認

| review-007 の指摘 | 状態 | 確認箇所 |
|---|---|---|
| 🟡 Minor 1: CI が回す Kotlin JVM テストルートが走査根から漏れ、全称の記述が事実に反する | **一部解消** | `scripts/ci-skip-lint.py:88-89` に `("maui-android-bridge", maui/android/native, ".kt", None, r"/src/test/")` を追加 (実測で到達を確認)。`ci-flaky-test-policy.md:5` の `applies-when.paths` に `maui/android/native/**/src/test/**`、`:106` の内訳が「印の仕組みを持たない 4 つ」、`:131` が「上の 7 ルート」。**docstring `:31-32` だけ「3 つ (…・kmp・MAUI の NUnit)」のまま** (指摘 1) |
| 🟡 Minor 2: 7 検査目の追加が accepted ADR に対して無記録 | **解消** | `deviation.md` 末尾に cross/ADR-0017 / ADR-0020 を名指しで挙げ、追加した検査・オーナー指示 (「何でもスキップに逃げるのを防ぐ工夫」2026-09-09)・amends は蒸留で起票することを記録。ADR-0020 の Context (「無断で足すこともできない」) に対する記録として成立している |
| 🔵 Suggestion 3: `--selftest` が CI で走らない / 検体 2 つが未確認 | **一部解消** | `ci.yml:258-261` が `--selftest` → 本検査の 2 行に。`:256-257` のコメントが cross/ADR-0020 と同じ理屈であることを説明している。**`[Explicit]` / `Assert.Ignore(...)` の検体は未追加** (自己テストは 26 項目のまま。指摘 2) |
| 🔵 Suggestion 5: NUnit の前提条件の書き方が規約に無い | **解消** | `ci-flaky-test-policy.md:110` の前提条件の列挙に「NUnit の `Assume.That(...)`」が入り、`:104` の一律禁止 (`Assert.Inconclusive`) との境目が読める |
| 🔵 Suggestion 4: BV-MA-03 の `followingSupply` 観測 | 未対応 (蒸留への申し送り。この回では対象外) | `MauiBridgeContentSupplyTests.swift:124-149` |
| 🔵 review-005 の 11 (別モードの署名の持ち出し) | 未対応 (引き続き蒸留へ申し送り) | `evidence/ci-repeat-signature.md:135` |

## 指摘事項

### [🟡 Minor] `ci-skip-lint.py` の docstring だけ走査根の内訳が 6 のままで、直上の全称・handbook・SCAN_ROOTS と食い違う

**該当箇所**: `scripts/ci-skip-lint.py:31-32`

**問題点**: docstring は「検査は CI が回すテストルートすべて (SCAN_ROOTS) に掛ける — 正規の印を置ける 3 つと、印の仕組みを持たない 3 つ (Android のローカル単体テスト・kmp・MAUI の NUnit) の両方」と書くが、`SCAN_ROOTS` は 7 根あり、`marker=None` は 4 つ (MAUI Android 橋渡しの Kotlin JVM テストが増えている)。同じ文中で「すべて (SCAN_ROOTS)」と言いながら内訳が 1 つ足りず、`ci-flaky-test-policy.md:106` の「印の仕組みを持たない 4 つ」とも食い違う。

review-007 の推奨修正はコードと handbook と docstring の 3 点を挙げていたが、docstring だけ追随していない。ここは `SCAN_ROOTS` を触る人が最初に読む説明で、「`maui/android/native` は検査対象外」と読める。

今回の食い違いは実体より**狭く**書く方向 (歯止めが無いと読ませる) なので、review-006 / 007 が問題にした「無いのに有ると読ませる」形の危険はない。優先度は低い。

**推奨修正**: `:32` を「印の仕組みを持たない 4 つ (Android のローカル単体テスト・kmp・MAUI の NUnit・MAUI Android 橋渡しの Kotlin JVM テスト)」に直す。

### [🔵 Suggestion] 自己テストに、後から足した検出項目と走査根の検体が無い

**該当箇所**: `scripts/ci-skip-lint.py:698-710` (NUnit 検体)、同 `:691-697` (印の仕組みを持たないルートの検体)、同 `:114-121` (`BANNED_SKIPS[".cs"]`)

**問題点**: `--selftest` が CI で毎回走るようになった (Suggestion 3 の主眼) ことで、この 26 項目が検出器の退行を止める唯一の網になった。ところがその網に、本 change で足した 2 つがまだ入っていない。

- `.cs` の検体は `[Ignore(...)]` と `Assert.Inconclusive(...)` だけで、同時に足した `[Explicit]` と `Assert.Ignore(...)` は 1 度も実行されない
- 印の仕組みを持たないルートの検体は `android/core/src/test/` と `kmp/mod/src/commonTest/` と `maui/KsDialogs.Maui.Tests/` の 3 つで、**今回足した `maui/android/native/**/src/test/` の検体が無い**。将来この根が消えても自己テストは緑のまま通る (今回は手作業の注入で確認したが、それは残らない)

**推奨修正**: `maui/android/native/mod/src/test/kotlin/` に `@Ignore` を置く検体と、`.cs` 検体への `[Explicit]` / `Assert.Ignore(...)` を足す。期待件数を持つ既存項目 (「@Ignore を承認手順を通らない skip として検出する」) の一覧に増える形で足せるので追加は 2 箇所で済む。

### [🔵 Suggestion] 「7 検査目」がどの検査を指すかが deviation と `ci.yml` で食い違う

**該当箇所**: `deviation.md` 末尾、`.github/workflows/ci.yml:263`

**問題点**: deviation は「lint job に 7 検査目 `scripts/ci-skip-lint.py` (自己テスト付き) を追加した」と書くが、step の並びでは `CI skip allowlist lint` は 6 番目で、`ci.yml:263` のコメントは SPM 同期スクリプトの自己テストを「lint job の 7 検査目」と呼んでいる。検査集合が 6 → 7 になったという事実は正しく、蒸留で ADR-0017 の amends を起こすときに困る種類の誤りではないが、序数を頼りに step を探すと別のものに当たる。

**推奨修正**: deviation を「lint job の検査を 6 → 7 に増やし、`scripts/ci-skip-lint.py` (自己テスト付き) を 6 番目に置いた」の形にする。あるいは `ci.yml` 側の序数を落として「lint job の検査の 1 つ」とする (序数は step を足すたびにずれるため、後者のほうが腐りにくい)。

## アクションプラン

1. **指摘 1 (docstring の内訳)** — 1 行。蒸留前に閉じておく
2. 指摘 2 (自己テストの検体 2 種) — CI で毎回走る網になった分、入れておくと退行に強い
3. 指摘 3 (序数の食い違い) — deviation か `ci.yml` のどちらかの文言
4. review-007 の Suggestion 4 (BV-MA-03 の `followingSupply`) と review-005 の Suggestion 11 は引き続き蒸留へ申し送り
