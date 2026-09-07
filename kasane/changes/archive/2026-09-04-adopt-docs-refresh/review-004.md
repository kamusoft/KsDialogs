# レビュー結果: adopt-docs-refresh (004 回目)

**日付**: 2026-09-04
**判定**: APPROVED

## サマリー

review-003 (APPROVED) 以降のオーナー追加指示 3 件 — ①`/tmp` 一時ファイルの KsDialogs 固有化と `link-resolution-check.py` の `DOCS_REFRESH_TARGETS` 対応、②`lint.identity.scope` へのルート README 2 枚の追加、③`.gitignore` の整備 ([付随修正]) — をレビューした。いずれも deviation.md に記録済みの合意済み差分で、実装は指示どおり。

一時 fixture (`skills/{en,ja}/5 Skill` + manifest v3、リポジトリ直下に一時構築 → `trash` で撤去) を組み、SKILL.md の Step 3〜6 を新パスで頭から通した。**8 検査すべてが新パスで動作し、正例は素通り・負例は検出**した。`scripts/` は 7 本が翻案元と byte 一致、`link-resolution-check.py` の差分は環境変数 + 既定値 + docstring の 3 箇所に収まる。`.gitignore` は追跡中ファイルを 1 件も無視せず (証跡ログ 22 本を含む)、`.agents/` / `.claude/skills/` / `.claude/settings.json` はいずれも追跡対象のまま。標準 lint 3 本は exit 0。

Critical / Major なし。低優先の Minor 1 件と Suggestion 3 件のみのため APPROVED とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — コメント構文を持つファイルは翻案元から無改変コピーした `.py` 8 本のみ (今回差分を持つ `link-resolution-check.py` の追加コメントも既存の docstring 内)。`comment-policy-lint.py` は `exclude: [skills]` の下で exit 0 (検査対象 919 ファイル / 違反 0)。fixture で `skills/` を実在させた状態でも同数・exit 0
- `kasane/handbook/cross/test-execution.md` (きっかけ: 変更の完了判定) — 全ビルドルート全件実行は proposal の Impact どおり合意済み例外。diff が製品コード・テストに触れないことを再確認して適用外とした
- 適用外と判定: `sample-parity.md` (`samples/` 無変更)・`runtime-behavior-verification.md` (実行時挙動に触れない)・`local-development-setup.md` (ビルド・環境構築を行わない)・`aiforms-origin-reference.md` (未移植機能の実装・調査でない)
- `kasane/lessons/code-review.md` は存在しない。`impl.md` L-001 (証跡実体の突き合わせ) / `process.md` L-001 (姉妹面の照合) を読み、本 change には該当なし (証跡画像なし・姉妹面実装なし) と判断した
- `ksn-core` SKILL.md「付随修正」/ `references/delta-spec.md` (deviation の意味論)・`references/paths.md`
- `deviation.md` 記録済みの 4 件 (tasks 2.2 の差分 / scope へのルート README 追加 / `/tmp` 固有化と `DOCS_REFRESH_TARGETS` / `.gitignore` の [付随修正]) は合意済み差分として扱い、違反として指摘していない
- 降格・申し送り済み (外部 URL の意味レビュー工程) は本レビューでも再掲しない

## 重点確認の結果

### (1) `/tmp` パスの分離と Step 3〜6 の通し実行 — 問題なし

- **旧パスの残留なし**: `SKILL.md` 内の `/tmp/docs-refresh-*` 19 箇所すべてが `docs-refresh-ksdialogs-{decisions.json, manifest-planned.json, targets.txt}` の 3 種に統一されている。`ksdialogs` を含まない `/tmp` パスは `scripts/link-resolution-check.py:11` / `:21` (既定値。deviation で合意済み) のみ
- **生成側と読み側の整合**: 生成側 (`:314` decisions / `:322` planned-manifest / `:335` targets-list) と読み側 (`:334` `:345` `:356` `:367` `:378` の 6-①〜④、`:393` `:409` の 6-⑤、`:457` `:461` `:468` の 6-⑥、`:471-472` の 6-⑦、`:498` の 6-⑧、`:522` の Step 7) が同一のファイル名を指す。突き合わせで食い違いなし
- **通し実行**: fixture (5 Skill × 2 言語 = 16 ファイル + `skills/README{,_ja}.md` + manifest v3、targets 8 キー / concepts 10 本 / excluded 1 本) をリポジトリ直下に一時構築し、SKILL.md の記述どおりに実行:

| 手順 | 結果 |
|---|---|
| 3c concepts 網羅 | `concepts coverage OK` (excluded 1 + targets 9 = 実在 10 本と一致) |
| 3d コード正チェック | 4 行の取得元すべてが非空値 (`agp=9.3.0` / `kotlin=2.4.10` / `minSdk=24` / `compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / OS 下限 `17.0`・`24.0` / `Microsoft.Maui.Controls 10.0.1`)。`kmp/settings.gradle.kts:32` の catalog 共有前提も成立 |
| 3e API 名網羅 | 9 行の未掲載候補を報告 (fixture が API 名を持たないため。報告のみの位置づけどおり exit 0) |
| Step 6 前段 | `planned-manifest.py` → 7 キーの予定 manifest、`targets-list.py` → 19 行の対象一覧を新パスへ出力 |
| 6-① concepts 網羅 | `concepts coverage OK` |
| 6-② en/ja 節構成 | `en/ja heading structure OK` → ja 側だけに節を足すと `heading levels differ en=[1,2,2] ja=[1,2,2,2]` を検出 |
| 6-③ コードブロック | `code blocks byte-identical` |
| 6-④ frontmatter | `frontmatter OK` |
| 6-⑤ ①②③ | 正例のみでは 0 件。負例 (地の文の `` `kasane/...` ``・「」形・`**` 強調形・`ADR-0004`・`KsDialogsInteropBridge` / `KsDialogsInteropResultType`・別 Skill へのインラインリンクと参照定義) をすべて検出。素通り側 (`https://example.com/kasane/spec.md`・`mykasane/foo`・同一 Skill 内の `./SKILL.md`) に誤検出なし |
| 6-⑥ 内部リンク解決 | `DOCS_REFRESH_TARGETS=/tmp/docs-refresh-ksdialogs-targets.txt` を渡して `All internal links resolve` (対象には実在のルート `README.md` を含む) |
| 6-⑦ lint | `local-path-lint.py --paths` / `identity-lint.py --paths` とも exit 0 |
| 6-⑧ 表記ゆれ | 正例 (`KsDialogs` / `jp.kamusoft:ksdialogs` / `ksdialogs-compose` / `ksdialogs-kmp` / `jp.kamusoft.ksdialogs.compose` / `KsDialogs.Maui` / `KsDialogAttributes`) は 0 件、誤例 (`Ksdialogs` / `ks-dialogs` / `com.kamusoft` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui` / 単数形 `KsDialog`) を検出 |
| `--readme-only` 分岐 | `DOCS_REFRESH_README_ONLY=1` で対象一覧が `readmes` 3 行に絞られ、`heading-parity-check.py` が `targets` 分を外して `en/ja heading structure OK` |

- fixture は `trash` で撤去済み。`git status --porcelain` は fixture 構築前と同一。`/tmp/docs-refresh-ksdialogs-*` 3 本も `trash` 済み。`/tmp/docs-refresh-manifest-planned.json` (KsSettingsView 側の残骸、11755 bytes) は指示どおり触っていない — この残骸の実在が、`/tmp` 固有化の必要性そのものの裏付けになっている

### (2) `scripts/` 8 本の翻案元との一致 — 問題なし

- `cmp` で 7 本が byte 一致 (api-coverage / code-block-parity / concepts-coverage / frontmatter / heading-parity / planned-manifest / targets-list)
- `link-resolution-check.py` の `diff -u` は 2 ハンク・追加 4 行 / 削除 2 行のみ: docstring の入力説明 (`:9-11`) と `targets_path = os.environ.get("DOCS_REFRESH_TARGETS", "/tmp/docs-refresh-targets.txt")` + `open(targets_path)` (`:21-22`)。`os` は元から import 済みで、判定ロジック・出力文言に変更なし。deviation の記述 (env var + 既定値 + docstring) と一致する

### (3) `.gitignore` — 問題なし

- **追跡中ファイルの巻き添えなし**: `git ls-files -z | xargs -0 git check-ignore --no-index` の出力が空。証跡ログは `git ls-files | grep '\.log$'` で 22 本あり、全件が無視されていない
- **例外パターンの実効**: `!kasane/changes/**/verification/**/*.log` / `!kasane/changes/**/evidence/**/*.log` は `verification/foo.log` (サブディレクトリ 0 段) と `verification/sub/foo.log` (1 段以上) の両方を救う (`git check-ignore` で確認)。`ui/verification/` 配下も `**` が `<id>/ui` を吸収するため救われる
- **既存エントリの維持**: 旧 `.gitignore` の 20 エントリはすべて新版に含まれる (`.swiftpm/` を含む。KsSettingsView 版に無い KsDialogs 固有エントリも落ちていない)。KsSettingsView 版の `openspec/changes/dummy.txt` は該当ディレクトリが無いため持ち込んでおらず正しい
- **`.agents/` / `.claude/skills/` / `.claude/settings.json`**: いずれも `git check-ignore` で「not ignored」。`.claude/settings.local.json` のみ `.gitignore:76` で無視 (意図どおり、コメントも添えられている)
- **無視集合の実差分**: 旧 `.gitignore` では見えていたが新版で消える実ファイルは 0 件 (`git ls-files --others --exclude-from=<旧版>` の差分はすべて空ディレクトリか、本 change が新設した `.agents/` / `.claude/skills/` / 未追跡の証跡 md)

### (4) `lint.identity.scope` の 4 値化 — 問題なし

- `identity-lint.py` の `in_scope()` は `rel.split("/", 1)[0] in self.scope` で判定するため、ファイル名 `README.md` / `README_ja.md` も第 1 セグメントとして正しく一致する。実測: `README.md` `README_ja.md` `kasane/x.md` `skills/en/a/SKILL.md` → True、`samples/README.md` `core/foo.kt` → False (ルート README だけを拾い、`samples/` 配下の開発者向け README は拾わない)
- **未存在パスによる無音化なし**: lint モードは `git grep --untracked ... -- kasane skills README.md README_ja.md` を使うが、存在しない pathspec (`skills` / `README_ja.md`) があっても git grep は rc=0 で正常終了し、`kasane` 側のヒットを従来どおり返す (実測 489 行)。`git grep` が失敗して stdout が空になり「違反 0 件」に化ける経路は生じていない
- `python3 scripts/identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` はいずれも exit 0 (fixture で `skills/` を実在させた状態でも同じ)
- SKILL.md 6-⑦ の注記 (`:475` / `:480`) と config の対応は成立している (下の Suggestion #2 に文言の精度の指摘あり)

### 全体の再確認

- **翻案元契約の保持**: Requirement が列挙する 11 項目を本文で 1 件ずつ再照読し全件残存を確認 (自動発動禁止 `:3` + Guardrails `:544` / manifest 検証 4 ケース `:94-100` / 停止案内 `:102-115` / 承認前無変更 `:260` / 器 `ksn-implementer` 固定 `:55` `:269` `:280` `:552` / 最大 3 並列 `:271` / `--all` と `--readme-only` の同時指定エラー `:63` / `--readme-only` の concepts スナップショット非更新 / manifest を最後に書く / 旧ハッシュ保持 `:521` `:523`)。Guardrails 17 項目も欠落なし
- **構造の保持**: 翻案元 SKILL.md との見出し比較で、増えたのは「移行 Skill の源泉規則」1 節、変わったのは 6-⑤ の見出し名のみ。節の欠落なし
- **閉世界性検査・識別子検査**: (1) の fixture 実走のとおり検出・素通りとも後退なし
- **2 つの入口**: `.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` の symlink。両入口の `SKILL.md` は同一 inode (100700763)
- **初期生成前の停止**: fixture 撤去後に `concepts-coverage-check.py` を実行 → `FileNotFoundError: 'skills/.manifest.json'` / rc=1、実行前後の `git status --porcelain` が同一 (書き換えなし)
- **規約記述**: `AGENTS.md:11-12` の 2 行に実行手順・フラグは含まれず、`CLAUDE.md` は `AGENTS.md` への symlink (`git ls-files -s` で mode `120000`) のため宣言は必ず一致する
- **足場の凍結**: `proposal.md` / `specs/docs-refresh/spec.md` は無変更。`kasane/changes/adopt-docs-refresh/` の追跡ファイル変更は `tasks.md` (`[ ]` → `[x]`) のみ
- **付随修正の同梱条件**: `.gitignore` は本務で触るファイルでも docs-refresh 能力内でもないため ksn-core の同梱条件 ① を厳密には超える。ただし deviation.md に「オーナーが指示」と経緯つきで記録済みの合意済み差分であり、担保 (追跡ファイル 0 件の無視・無視集合の実差分 0 件) も本レビューで取れているため、指摘としては起こさない

## 指摘事項

### [🟡 Minor] `DOCS_REFRESH_TARGETS` の付け忘れが姉妹リポジトリの対象一覧を黙って読む経路が残っている (低優先)

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:329`、`.agents/skills/docs-refresh/scripts/link-resolution-check.py:21`

**問題点**: `:309` の注記が「片方の実行が書いた予定 manifest や**対象一覧**をもう片方が読んでしまうと、他リポジトリの状態を検査したまま『適合』と読み違える」と述べているのに対し、`link-resolution-check.py` の既定値は翻案元と共有の `/tmp/docs-refresh-targets.txt` のままである (deviation 記録どおりで、既定値そのものは合意済み)。`:329` の「付け忘れると既定値のディスク manifest へ**エラーを出さずにフォールバックする**」という警告は `DOCS_REFRESH_MANIFEST` を必要とする 5 箇所だけを列挙しており、`DOCS_REFRESH_TARGETS` (6-⑥) は列挙外。KsSettingsView 側が直前に docs-refresh を走らせて `/tmp/docs-refresh-targets.txt` を残していた場合、6-⑥ で env var を付け忘れると他リポジトリのファイル一覧に対して `All internal links resolve` を返し、失敗せずに素通りする。実際に `/tmp/docs-refresh-manifest-planned.json` (KsSettingsView 側の残骸) がこの環境に残っており、残骸が発生する運用であることは裏が取れている。なお対象一覧が存在しない場合は `FileNotFoundError` で落ちるため、危険なのは「姉妹リポジトリの残骸がある」ケースに限られる。

**推奨修正**: `:329` の一文に `DOCS_REFRESH_TARGETS` (6-⑥) を含める。例:「予定 manifest を読ませたいスクリプト (…) と、対象一覧を読ませる 6-⑥ (`DOCS_REFRESH_TARGETS=/tmp/docs-refresh-ksdialogs-targets.txt`) はすべて、コマンド行の先頭に環境変数を付けて起動する。付け忘れるとどちらも姉妹リポジトリと共有の既定パスへエラーなしでフォールバックする」。既定値そのものは deviation の合意 (翻案元と同じ) を保つ。

### [🔵 Suggestion] tasks 2.9 の残留 grep が「許容箇所 1 箇所」を満たさなくなっている

**該当箇所**: `tasks.md:29` (tasks 2.9)、`.agents/skills/docs-refresh/SKILL.md:309`、`:480`

**問題点**: tasks 2.9 は「許容箇所 (冒頭の翻案元出典の注記 1 箇所) 以外に残っていないこと」を完了条件としているが、今回の追加指示で `KsSettingsView` が `:309` (一時ファイル名の由来説明) に、`docs/` が `:480` (scope 外パスの例示) に新たに現れ、grep のヒットは 3 件になった。いずれも意図的で正当な記述 (前者は `/tmp` 衝突回避の根拠、後者は「scope 外のパスを `readmes` に足すと素通りする」の例示) であり、Requirement 側の Scenario (「KsSettingsView の Skill 名 `kssettingsview-*` は本文に残っていない」) は満たしている。ただし記録が無いと、後で tasks 2.9 を再実行した人が回帰と読み違える。

**推奨修正**: deviation.md に 1 行足す (例: 「tasks 2.9 の残留 grep の許容箇所は、`/tmp` 固有化に伴い `SKILL.md:12` に加えて `:309` (衝突回避の根拠として KsSettingsView に言及)・`:480` (scope 外パスの例示としての `docs/`) の計 3 箇所になった。いずれも翻案元の残留ではなく KsDialogs 向けの意図的な記述」)。`:480` の `docs/` は、KsDialogs に実在しない語で例示するのが紛らわしければ `samples/` 等の実在する scope 外セグメントに置き換えてもよい。

### [🔵 Suggestion] 6-⑦ の注記が `lint.identity.scope` の実値を 1 つ落として書いている

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:480`

**問題点**: 「現状の scope は `skills` / `README.md` / `README_ja.md` なので」とあるが、`kasane/config.yaml:19` の実値は `[kasane, skills, README.md, README_ja.md]` の 4 値。docs-refresh の追従対象に効くのは挙げた 3 つで結論 (追従対象 4 枚と Skill 本体はすべて実効する) は正しいが、config を確認した読者には食い違って見える。

**推奨修正**: 「現状の scope は `kasane` / `skills` / `README.md` / `README_ja.md` で、このうち追従対象に効くのは後ろの 3 つ」等、実値を落とさない書き方にする。

### [🔵 Suggestion] `.gitignore` の証跡例外が `kasane/changes/` 配下に限られている

**該当箇所**: `.gitignore:98-101`

**問題点**: `*.log` の例外は `!kasane/changes/**/verification/**/*.log` と `!kasane/changes/**/evidence/**/*.log` の 2 本で、`kasane/roadmaps/<id>/phases/<p>/artifacts/` 等に置かれるログは無視される (`git check-ignore` で確認)。`kasane/config.yaml:15` の `lint.exclude` は `kasane/**/verification/**/*.log` と changes に限定していないため、両者の想定範囲がずれている。現時点で該当ファイルは 0 件のため実害はないが、ロードマップのフェーズ artifacts にログを置いた時点で無音の取りこぼしになる。

**推奨修正**: 例外を `!kasane/**/verification/**/*.log` / `!kasane/**/evidence/**/*.log` に広げる (`kasane/` 配下は元々コミット対象であり、範囲を広げても副作用は無い)。または現状で意図どおりであることをコメントに明記する。

## アクションプラン

1. (低優先) Minor: `SKILL.md:329` の env var 付け忘れ警告に `DOCS_REFRESH_TARGETS` を含める。1 行の追記で済み、本 change 内で対応してもよいし phase-2 へ送ってもよい
2. (任意) Suggestion #1: deviation.md に tasks 2.9 の許容箇所が 3 箇所になった旨を追記する (蒸留時でも可)
3. (任意) Suggestion #2 / #3: `SKILL.md:480` の scope 実値の書き方、`.gitignore` の証跡例外の範囲。いずれも実害が出ていないため、蒸留時または phase-2 でまとめて扱ってよい

いずれも APPROVED を妨げない。
