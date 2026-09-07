# レビュー結果: adopt-docs-refresh (003 回目)

**日付**: 2026-09-04
**判定**: APPROVED

## サマリー

review-002 の Major (6-⑤ ① の許可リスト化による全角区切り・強調記号の取りこぼし) は解消した。`.agents/skills/docs-refresh/SKILL.md:395` のパターンは推奨どおり否定クラス `(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/` に戻っており、fixture 実走で回帰 6 形 (`**` `「」` `（）` `*` `“”` `【】`) がすべて検出され、前サイクルで解消した検出 (相対パス形・行頭形・参照形式リンク) と素通り (URL 途中・`mykasane/`・`my_kasane/`・`skills/en/x/kasane/y.md`・`KsDialogAttributes`) にも後退がない。`:448` の注記も許可リストの列挙から除外の説明へ書き直されており、パターンと整合している。

翻案元の契約 11 項目・Guardrails 17 項目は今回の編集でも欠落しておらず、`scripts/` 8 本は依然 byte 一致、標準 lint 3 本は exit 0。Critical / Major / Minor はなし。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — コメント構文を持つファイルは翻案元から無改変コピーした `.py` 8 本のみで、今回の編集 (`SKILL.md` のみ) は対象外。`config.yaml` の `lint.comment-policy.exclude: [skills]` 変更後も `comment-policy-lint.py` は exit 0 (検査対象 919 ファイル / 違反 0)
- `kasane/handbook/cross/test-execution.md` (きっかけ: 変更の完了判定) — 全ビルドルート全件実行は proposal の Impact どおり合意済み例外。diff が製品コード・テストに触れないことを再確認して適用外とした
- 適用外と判定: `sample-parity.md` (`samples/` 無変更)・`runtime-behavior-verification.md` (実行時挙動に触れない)・`local-development-setup.md` (ビルド・環境構築を行わない)・`aiforms-origin-reference.md` (未移植機能の実装・調査でない)
- `kasane/lessons/code-review.md` は存在しない。`impl.md` L-001 (証跡実体の突き合わせ) / `process.md` L-001 (姉妹面の照合) を読み、本 change には該当なし (証跡画像なし・姉妹面実装なし) と判断した
- `deviation.md` の tasks 2.2 の差分は合意済み差分として扱い、指摘に含めない
- 降格・申し送り済み (外部 URL の意味レビュー工程・ルート README の identity-lint 範囲・`/tmp/docs-refresh-*` の固定パス衝突) は本レビューでも再掲しない

## 前回指摘の解消状況

| 前回の指摘 | 出典 | 今回の確認 (fixture 実走) | 状態 |
|---|---|---|---|
| 6-⑤ ① の文字クラス許可リスト化で全角区切り・強調記号の直後を取りこぼす (回帰) | review-002 Major | `SKILL.md:395` が `(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/` へ。review-002 が列挙した 6 行 (a `**kasane/…**` / b `「kasane/…」` / c `（kasane/…）` / d `*kasane/…*` / e `“kasane/…”` / g `【kasane/…】`) をすべて検出。加えて `・kasane/…`・日本語直後 (`詳細はkasane/…`) も検出 | ✅ 解消 |
| ① の注記が許可リストの列挙になっている | review-002 Major (同項) | `SKILL.md:448` は「行頭または英数字・`_`・`-`・`/` 以外の 1 文字 … の直後」という**除外の説明**に書き直され、「除外を文字クラスで書く (許可文字の列挙にしない) のは、`skills/ja/` の生成物で多用される全角記号を取りこぼさないため」と意図も明記。SKILL.md 全文に許可リスト型の記述 (`[[:space:]…]` 等) の残留はなし | ✅ 解消 |
| 6-⑧ `KsDialog([^s]\|$)` が `KsDialogAttributes` を誤検出 | review-001 Major | 正例 12 行 (spec 列挙の正しい識別子 + `KsDialogAttributes` + `jp.kamusoft.ksdialogs.compose.KsDialogAttributes` + 配布座標 URL) で 0 件報告 (exit 1)、誤例 11 行はすべて検出。後退なし | ✅ 維持 |
| 6-⑤ ① が `./kasane/` `../../kasane/` を取りこぼす | review-001 Minor / second-opinion-code Major | `](../../../kasane/…)` / `./kasane/index.md` / 行頭 `../kasane/…` / `[source]: ../../../../kasane/…` / 行頭 `kasane/…` をすべて検出。`skills/README_ja.md` (③ 対象外) でも検出 | ✅ 維持 |
| 6-⑤ ③ が参照形式リンク `[label]: path` を解析しない | second-opinion-code Major | python 断片実走: インライン `[other](../../ksdialogs-android/SKILL.md)`・参照定義 `[o]: ../../ksdialogs-android/SKILL.md`・`[source]: ../../../../kasane/…` の 3 件をルート外として検出。同一 Skill 内の `[SKILL](../SKILL.md)` と `[ref]: ../SKILL.md` は報告なし。`[]( )` で例外なし。`skills/README_ja.md` は `len(parts) < 4` で対象外 | ✅ 維持 |

素通りの後退がないことも同じ fixture で確認した — `https://example.com/kasane/spec.md`・`mykasane/foo`・`skills/en/x/kasane/y.md`・`my_kasane/foo` を含むファイルは 6-⑤ ① で 0 件報告 (exit 1)。パターンはロケール非依存で、`LC_ALL=C` / `LC_ALL=ja_JP.UTF-8` のいずれでも検出件数は同じ 16 件だった (BSD grep)。

## 指摘事項

なし (Critical / Major / Minor いずれも 0 件)。

## 確認したがこの版では問題なしと判断した観点

- **否定クラスから `.` を落とした差分**: 修正前の初版は `[^A-Za-z0-9_./-]` で `.` も除いていたが、今回は `[^A-Za-z0-9_/-]`。相対パス形は `(\.{1,2}/)*` が受けるため取りこぼしは生じず、`.` を許した分は検出が**広がる**方向 (`~/.kasane/` のような形も拾う) で、閉世界性検査としては安全側。`:448` の注記も除外集合を「英数字・`_`・`-`・`/`」と正確に列挙しており、本文とパターンに食い違いはない
- **`_kasane/…_` (下線強調) が引き続き未検出**: `_` は識別子構成文字として意図的に除外されており (`foo_kasane/` の誤検出回避)、review-002 の推奨修正がこのトレードオフを含めて提示したもの。注記の「単語内一致を誤検出しないため」が根拠として本文にある。CommonMark では単語内の `_` が強調にならないこともあり、実害は限定的と判断した
- **翻案元契約の欠落**: 今回の編集領域は 6-⑤ ① のパターンと注記の 2 箇所のみ。Requirement が列挙する契約項目 (manifest 検証 4 ケース `:94-100` / 停止案内 `:102-115` / 承認前無変更 / 器 `ksn-implementer` 固定 `:55` `:269` `:549` / 最大 3 並列 `:271` / `--all` と `--readme-only` の同時指定エラー `:63` / `--readme-only` の concepts スナップショット非更新 / manifest を最後に書く / 旧ハッシュ保持 / 自動発動禁止 `:3` + Guardrails) を本文で 1 件ずつ再照読し、全件残存を確認した
- **`scripts/` の無改変**: 8 本すべて `cmp` で `../KsSettingsView/.agents/skills/docs-refresh/scripts/` と byte 一致
- **2 つの入口**: `.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` の symlink。両入口の `SKILL.md` は同一 inode (100700537)
- **3d の取得元 4 行**: 実読で全項目が非空 (`agp=9.3.0` / `kotlin=2.4.10` / `android-minSdk=24` / `android-compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / OS 下限 `17.0`・`24.0` / `Microsoft.Maui.Controls 10.0.1`)。`kmp/settings.gradle.kts:32` の catalog 共有前提も成立
- **規約記述**: `AGENTS.md:11-12` の 2 行に実行手順・フラグは含まれず、`CLAUDE.md` は `AGENTS.md` への symlink。`kasane/config.yaml` の `lint.identity.scope: [kasane, skills]` / `lint.comment-policy.exclude: [skills]` / `context` の 1 文を確認
- **標準 lint**: `python3 scripts/identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` いずれも exit 0
- **足場の凍結**: `git status --porcelain` で `kasane/changes/adopt-docs-refresh/` の追跡ファイル変更は `tasks.md` のみ (`[ ]` → `[x]`)。`proposal.md` / `specs/docs-refresh/spec.md` は無変更
- **後片付け**: fixture は scratchpad のみ。リポジトリ内に残骸なし。`/tmp/docs-refresh-targets.txt` は生成していない (python 断片は targets の読み先を scratchpad へ差し替えて実走した)
- 範囲外として扱った作業ツリーの差分: `kasane/lessons/inbox/translated-norm-needs-local-basis-and-fact-check.md` (未昇格の観測ファイルで、コンテキストパッケージが指定した diff 範囲に含まれない)

## アクションプラン

なし。前回の Suggestion (`/tmp/docs-refresh-*` の固定パスがリポジトリ間で衝突する) は phase-2 / 蒸留時の申し送りとして合意済みで、本サイクルでの対応は不要。
