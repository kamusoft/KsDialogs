# レビュー結果: adopt-docs-refresh (001 回目)

**日付**: 2026-09-04
**判定**: CHANGES_REQUESTED

## サマリー

翻案元 (KsSettingsView commit `33ce94e`) からの一式コピーは正確で、`scripts/` 8 本は byte 一致、2 つの入口は同一 inode を指し、SKILL.md からは翻案元の契約 (manifest 検証 4 ケース・承認前無変更・器 `ksn-implementer` 固定・最大 3 並列・`--all` / `--readme-only` の意味・manifest を最後に書く規律・未処理 concept の旧ハッシュ保持・自動発動禁止) が一つも欠けずに残っている。KsDialogs 固有の差し替え (5 Skill・移行 Skill の源泉・excluded 既定・3d の 4 行・6-⑤ / 6-⑧) も phase-1 agenda の決定事項 5 件と一致し、3d の取得元 4 行は実読で全項目が値を返した。

ただし 6-⑧ の grep パターンが、現存する公開 Compose API `KsDialogAttributes` を誤検出する。phase-2 で Compose レシピを持つ Skill を生成すると必ず整合性チェックが落ち、「行が出たら再修正対象に追加する」という規律のもとで直しようのない再修正ループになるため、Major として差し戻す。あわせて 6-⑤ ① の `kasane/` 検出が相対パス形を取りこぼす点を Minor で挙げる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 変更に含まれるコメント構文を持つファイルは翻案元から無改変でコピーした `.py` 8 本のみ。`scripts/comment-policy-lint.py` の既定拡張子に `.py` は含まれず、`config.yaml` の `lint.comment-policy.exclude` 変更後も lint は違反 0 件 (検査対象 919 ファイル) で通る
- `kasane/handbook/cross/test-execution.md` (きっかけ: 変更の完了判定) — 全ビルドルート全件実行は proposal の Impact どおり合意済み例外。diff が製品コード・テストに一切触れていないことを確認したうえで適用外と判断した
- 適用外と判定: `sample-parity.md` (`samples/` 無変更)、`runtime-behavior-verification.md` (実行時挙動に触れない)、`local-development-setup.md` (ビルド・環境構築を行わない)、`aiforms-origin-reference.md` (未移植機能の実装・調査でない)
- `kasane/lessons/code-review.md` は存在しない。`impl.md` L-001 / `process.md` L-001 を読み、本 change には該当なし (証跡画像なし・姉妹面実装なし) と判断した

## 指摘事項

### [🟠 Major] 6-⑧ の表記ゆれ grep が現存する公開 API `KsDialogAttributes` を誤検出する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:489` (パターン本体)、同 `:496` (パターンの読み方の注記)、同 `:472-478` (正しい識別子の表)

**問題点**:
パターン中の `KsDialog([^s]|$)` は「複数形の落ちた `KsDialog`」を拾う意図だが、`KsDialog` の直後が `s` 以外なら何にでも当たる。KsDialogs には `KsDialogAttributes` という**現存する公開 Compose API** があり (`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:30` の `public fun KsDialogAttributes(`)、これが誤検出される。

実走で確認した:

```
$ grep -rnE "…|KsDialog([^s]|$)|…" skills/en/ksdialogs-android/recipe.md
skills/en/ksdialogs-android/recipe.md:3:import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
skills/en/ksdialogs-android/recipe.md:4:KsDialogAttributes(placement = DialogPlacement())
```

`KsDialogAttributes` は `kasane/concepts/core/api/layout-semantics.md` / `registration-show-semantics.md` / `transition-semantics.md` の 3 本に記載があり、これらは phase-1 agenda の決定で `ksdialogs-android` / `ksdialogs-kmp` の `targets` に載る源泉である。したがって phase-2 で Compose の登録・配置・遷移レシピを書けば、この API 名はほぼ確実に生成物へ現れる。SKILL.md:491 は「行が出たら該当ファイルを再修正対象に追加する」と定めているため、正しい生成物が毎回失敗し、直しようのない再修正ループになる — SKILL.md 自身が 3c / 6-① について警戒している失敗形 (「生成物の不備ではないループ」) と同じ型である。

デルタスペックの Requirement「配信識別子の表記ゆれ検査」の Scenario「正しい識別子は素通り」(GIVEN 正しい識別子のみを含む Skill ファイル → THEN 6-⑧ は何も報告しない) にも抵触する。

**推奨修正**:
`KsDialog([^s]|$)` を `KsDialog([^sA-Za-z0-9_]|$)` に置き換える (実走で確認済み: `KsDialogAttributes` は素通り、`KsDialog package` / `KsDialog.` のような真の誤表記は引き続き検出する)。あわせて `:496` のパターンの読み方に「`KsDialogAttributes` のような `KsDialog` で始まる正しい API 名には当たらない」旨を、`:472-478` の識別子表かその直後に「Compose の公開 API に `KsDialogAttributes` がある」旨を添える (次に触る人が単数形パターンを緩める根拠を持てるように)。`grep -P` の否定先読みは BSD grep で使えないため文字クラスで解く。

### [🟡 Minor] 6-⑤ ① の `kasane/` 検出が相対パス形 (`./kasane/` `../kasane/`) を取りこぼす

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:395` (grep)、同 `:439` (① の注記)

**問題点**:
パターン `(^|[^A-Za-z0-9_./-])kasane/` は直前 1 文字から `.` `/` `-` `_` を除いているため、`./kasane/…` と `../kasane/…` が検出されない。実走で確認した:

```
# skills/README.md: Provenance: see [notes](../kasane/concepts/index.md) and ./kasane/index.md
$ grep -rn -E "(^|[^A-Za-z0-9_./-])kasane/" "${TARGETS[@]}"
（skills/README.md は 1 行も出ない）
```

Skill ファイル (`skills/<lang>/<name>/…`) については ③ の相対リンク検査が同じ漏れを拾うため実害は薄いが、③ は `skills/` 配下の 4 階層以上のパスにしか適用されず、README 群 (`skills/README.md` / `skills/README_ja.md`) には掛からない。結果として `skills/README.md` から `](../kasane/…)` を張ると ① と ③ の両方をすり抜ける。Requirement「閉世界性と機械面の漏れ検査」の ① は「対象一覧 (Skill + README)」に掛かる規定なので、README 側に穴が残る。

なお `:439` の注記が挙げる誤検出の回避対象「`skills/en/…/kasane/…` のような別文脈」はリポジトリに実在しないディレクトリであり、この除外が守っている実益は現状ない。

**推奨修正**:
`(^|[^A-Za-z0-9_-])(\.{1,2}/)*kasane/` のように相対プレフィックスを明示的に許す形へ広げる (この形なら `mykasane/` のような単語内一致は引き続き避けられる)。あわせて `:439` の注記を実際に守っている誤検出例に書き直す。

### [🔵 Suggestion] 相対リンク検査の python 断片に空リンクでの例外と、隣のブロックと揃わない空チェックがある

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:422` / `:409`

**問題点**:
`:422` の `href = raw.split()[0]` は、`[]( )` のように括弧内が空白のみのリンク記法があると `IndexError` で断片全体が落ちる (検査が「通った」のか「落ちた」のか区別しづらい形で止まる)。また `:409` の `raise SystemExit("検査対象が空です…")` は stderr へ書いて exit 1 になり、同じ 6-⑤ の bash ブロック (`:393` の `echo`) や 6-⑦ の空ガードが標準出力へ案内を出すのと挙動が揃わない。

**推奨修正**:
`parts = raw.split()` を取ってから `href = parts[0] if parts else ""` とするか、既に直後にある `if not href:` のガードへ寄せる。空チェックは `print(...)` + `raise SystemExit(0)` にして他ブロックと揃える。

### [🔵 Suggestion] tasks 2.2 の文言と実装が食い違う (実装側が正しい)

**該当箇所**: `tasks.md:14` と `.agents/skills/docs-refresh/SKILL.md:41`

**問題点**:
tasks 2.2 は「『platform / Sample ディレクトリに README を置かない』の根拠参照 (cross/ADR-0023) を KsDialogs の phase-2 踏襲決定への参照に直す」と書いているが、実装は参照を置かず「`samples/` 配下の開発者向け README は利用者向けでないため `readmes` に載せない」という事実記述に置き換えている。KsDialogs には実際に `samples/README.md` が存在し (`kasane/config.yaml` の `ui.screenshot` が「撮影のための起動引数」節を正典として参照している)、翻案元 ADR-0023 の「README を置かない」をそのまま踏襲すると事実に反するため、**実装側の判断が正しい**。デルタスペックの Requirement「追従対象の規範」も README 4 枚と `readmes` が正であることしか求めていないので仕様違反ではない。

**推奨修正**:
修正不要。蒸留時に tasks の文言ではなく実装の記述を正として扱えるよう、この食い違いを認識しておく (必要なら deviation の `[付随修正]` ではなく、蒸留メモとして残す)。

### [🔵 Suggestion] 検証で使った `/tmp/docs-refresh-targets.txt` が残っている

**該当箇所**: リポジトリ外 (`/tmp/docs-refresh-targets.txt`)

**問題点**:
リポジトリ内には何も残っていない (`git status` で確認済み) が、`/tmp/docs-refresh-targets.txt` に検証時の fixture 一覧 (14 行) が残っており、`link-resolution-check.py` を素で実行すると存在しない `skills/…` を MISSING として並べる。次に手で 6-⑥ を叩く人が古い一覧を読む事故につながる。

**推奨修正**:
`trash /tmp/docs-refresh-targets.txt` で片付ける (規約どおり `rm` は使わない)。

## アクションプラン

1. **Major**: `SKILL.md:489` の `KsDialog([^s]|$)` を `KsDialog([^sA-Za-z0-9_]|$)` へ修正し、`:496` の注記と `:472-478` の識別子表に `KsDialogAttributes` の扱いを明記する。修正後、正例 (`KsDialogAttributes` を含む Skill ファイル) が素通りし、`Ksdialogs` / `ks-dialogs` / `com.kamusoft` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui` / 単数形 `KsDialog ` が引き続き検出されることを fixture で再実走する
2. **Minor**: `SKILL.md:395` の `kasane/` パターンを相対プレフィックス対応へ広げ、`:439` の注記を実在する誤検出例に書き直す。`skills/README.md` から `](../kasane/…)` を張った fixture で検出されることを確認する
3. **Suggestion**: `:422` / `:409` の断片の堅牢化と挙動の統一、`/tmp/docs-refresh-targets.txt` の片付け
4. tasks 2.2 の文言と実装の食い違いは蒸留時への申し送りとして扱う (修正不要)
