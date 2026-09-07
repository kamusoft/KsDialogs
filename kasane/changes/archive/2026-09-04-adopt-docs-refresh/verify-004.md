# 検証結果: adopt-docs-refresh (004 回目)

**日付**: 2026-09-04
**判定**: VALID (❌ 0 件)

対象: `specs/docs-refresh/spec.md` の ADDED Requirement 9 本 / Scenario 22 本。`deviation.md` に記録済みの乖離 4 件のうち、Requirement を持つ 2 件 (`lint.identity.scope` へのルート README 追加 / `scripts/` の byte 一致に対する `link-resolution-check.py` の差分) は「⚠️ deviation 記録済み」として扱い、Requirement を持たない 2 件 (tasks 2.2 の文言差 / `.gitignore` の [付随修正]) は対応表の対象外とした。

verify-003 は ❌ 0 件で VALID。今サイクル (オーナー追加指示 3 件の反映後) でも新たな ❌ は生じていない。

「実装」は本 change の成果物の該当箇所、「検証」は本サイクルで実走した結果。製品コード・テストに触れない change のため、テストの代わりに tasks 5.x の検証手段を一時 fixture で頭から再実走した (fixture は `skills/{en,ja}/<5 Skill>` + manifest v3 をリポジトリ直下に一時構築し、`trash` で撤去。`git status --porcelain` は構築前と同一に復帰)。

## 対応表

### Requirement: docs-refresh 一式の配置と起動経路

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 2 つの入口が同じ実体を指す | `.agents/skills/docs-refresh/` (SKILL.md + references 2 + scripts 8)、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` | `stat -f %i` で両入口の SKILL.md が同一 inode (100700763)。`cmp` で scripts 7/8 が翻案元と byte 一致、`link-resolution-check.py` のみ差分 | ⚠️ deviation 記録済み |
| (Requirement 本文: `scripts/` 8 本は無改変) | `scripts/link-resolution-check.py:9-11` (docstring)、`:21-22` (`DOCS_REFRESH_TARGETS` と既定値) | `diff -u` は 2 ハンク・追加 4 行 / 削除 2 行のみ。判定ロジック・出力文言に変更なし。既定値は翻案元と同じ `/tmp/docs-refresh-targets.txt`。deviation.md 3 件目の記述と一致 | ⚠️ deviation 記録済み |
| 翻案元の契約の保持 | `:3` (description の自動発動禁止) / `:63` (`--all` と `--readme-only` の同時指定エラー) / `:94-100` (manifest 検証 4 ケース) / `:102-115` (停止案内) / `:55` `:269` `:280` `:552` (器 `ksn-implementer` 固定) / `:271` (最大 3 並列) / `:260` (承認前無変更) / `:521` `:523` (旧ハッシュ保持) / `:522` (予定 manifest と同一内容) / Guardrails `:541-560` (`--readme-only` の concepts スナップショット非更新・manifest を最後に書く・`--all` の扱い) | 契約 11 項目・Guardrails 17 項目を本文で 1 件ずつ再照読し全件残存。翻案元との見出し比較でも、増えたのは「移行 Skill の源泉規則」1 節、変わったのは 6-⑤ の見出し名のみ (節の欠落なし) | ✅ 一致 |
| concepts 更新後の非発動 | `SKILL.md:3` (description) と Guardrails `:544` | 両方に残存を確認 | ✅ 一致 |

### Requirement: 初期生成前の停止

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 異常な manifest でも書き換えない | `SKILL.md:94-100` の 4 ケース (不在 / parse 不能 / `version` ≠ 3 / 必須キー欠落・型不正)、`:102-115` の停止案内が「承認を伴う変更フロー (Kasane の change) の実装として行う」へ誘導 | 本文照読で 4 ケースと誘導文を確認 | ✅ 一致 |
| skills/ 未生成での起動 | 翻案元無改変の `scripts/concepts-coverage-check.py` | fixture 撤去後に実行 → `FileNotFoundError: 'skills/.manifest.json'` / rc=1。実行前後の `git status --porcelain` が同一 (書き換えなし) | ✅ 一致 |

### Requirement: 追従対象の規範

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 追従対象の特定 | `SKILL.md:24-32` (5 Skill 表)、`:34-41` (README 4 枚 + `readmes` が正 + 構成見直しは守備範囲外) | 5 Skill 名 (`ksdialogs-{ios,android,maui,kmp,aiforms-migration}`) と KMP 1 本構成・移行 Skill 構成・README 4 枚を照読。`kssettingsview-*` の Skill 名は本文に 0 件 (残留 grep のヒットは `:12` 出典注記・`:309` `/tmp` 衝突回避の根拠説明の 2 箇所のみで、いずれも KsDialogs 向けの意図的な記述) | ✅ 一致 |

### Requirement: 移行 Skill の源泉は新 API 側の concepts のみ

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 移植元の変更は要追従にならない | `SKILL.md:136-145` の源泉規則 (旧 API 側は concepts に源泉を持たず対象外・書き起こしは初期生成時の一度限り・clone は `cross/reference/reference-repositories.md` で解決) | 本文照読。fixture の manifest で `ksdialogs-aiforms-migration/*` の `targets` に新 API 側 concepts のみを置いて 3b/3c/6-① を通し、旧 API 側の源泉が要らない構成で `concepts coverage OK` になることを確認 | ✅ 一致 |
| 新 API 側の変更で対応先が追従する | `SKILL.md:145` (逆向きは通常どおり働く) / `:161-169` (3b の逆引き規則) | fixture の `targets` で `ksdialogs-aiforms-migration/references/api-mapping.md` ← `maui/api/di-registration.md` の逆引きが成立することを確認 (`targets-list.py` が en/ja ペアで展開) | ✅ 一致 |

### Requirement: excluded の初期値と architecture カテゴリの既定

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| architecture 配下の新 concept | `SKILL.md:177` (excluded 初期値 = `cross/reference/reference-repositories.md` 1 本 + 理由)、`:179` (architecture は除外候補の既定・自動除外しない)、`:238-242` (Step 4 提示例) | 本文照読。`kasane/concepts/**/architecture/` は現時点 0 本のため机上確認 | ✅ 一致 |
| 利用者に効く architecture concept は targets へ回せる | `SKILL.md:179` 末尾 + `:238-242` の両選択肢並記 | 本文照読 | ✅ 一致 |
| (Requirement 本文: excluded 1 本 + 残り 9 本が targets) | 同上 | 実 concepts は index/log/rules を除いて 10 本 (core/api 8 + cross/reference 1 + maui/api 1)。fixture の manifest を excluded 1 + targets 9 で組むと `concepts-coverage-check.py` が `concepts coverage OK` を返す (3c / 6-① とも) | ✅ 一致 |

### Requirement: コード正の機械チェック (ツール最低バージョン) の取得元

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 取得元が実在して値を返す | `SKILL.md:195-199` の 4 行表 | 実読: `agp=9.3.0` / `kotlin=2.4.10` / `android-minSdk=24` / `android-compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / `SupportedOSPlatformVersion` `17.0`・`24.0` / `Microsoft.Maui.Controls 10.0.1` — 全項目が非空 | ✅ 一致 |
| KMP の catalog 共有が外れた | `SKILL.md:193` (前提を先に確認し、外れていたら報告して停止) | `kmp/settings.gradle.kts:32` が `from(files("../android/gradle/libs.versions.toml"))` を持つ (前提は現状成立)。停止規定は本文照読 | ✅ 一致 |
| version catalog の変更 | `SKILL.md:195` ① 行 + `:204` の突合先の読み方 | 本文照読 | ✅ 一致 |
| Gradle wrapper の食い違い | `SKILL.md:196` ② 行 (食い違いそのものを別途報告) | 実読。現状 2 本とも `gradle-9.7.0-bin.zip` で一致 | ✅ 一致 |
| MAUI 本体下限の変更 | `SKILL.md:198` ④ 行 + `:204` の例 (`ksdialogs-maui/SKILL.md` の en/ja) | 実読。csproj の該当 3 種 (`:4` / `:17` `:21` / `:25`) が実在 | ✅ 一致 |
| (Requirement 本文: module の build.gradle.kts は取得元にしない) | `SKILL.md:206` の注記 | 本文照読 | ✅ 一致 |

### Requirement: 閉世界性と機械面の漏れ検査

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 内部用語の漏れ | `SKILL.md:395` (① `kasane/`)、`:396` (② `ADR-[0-9]{4}`) | fixture 実走 (新パス `/tmp/docs-refresh-ksdialogs-targets.txt` 経由): バッククォート内の `` `kasane/concepts/...` ``、「」形、`**` 強調形、`ADR-0004` をすべて検出。素通り側 (`https://example.com/kasane/spec.md` / `mykasane/foo`) は 0 件報告で後退なし | ✅ 一致 |
| 機械面の漏れ | `SKILL.md:397` の `KsDialogsInteropBridge\|KsDialogsInteropResultType` | fixture 実走で該当行を検出 | ✅ 一致 |
| Skill ルート外への相対リンク | `SKILL.md:401-434` の python 断片 (targets の読み先は `/tmp/docs-refresh-ksdialogs-targets.txt`) | fixture 実走: インライン `[other](../../ksdialogs-android/SKILL.md)` と参照定義 `[o]: ../../ksdialogs-android/SKILL.md` の 2 件をルート外として検出。同一 Skill 内の `references/*.md` → `../SKILL.md`、Skill ルートの `./SKILL.md` は報告なし。`skills/README_ja.md` は `len(parts) < 4` で対象外、README 群 (ルート `README.md`) も `skills/` 前置なしで対象外 | ✅ 一致 |
| (Requirement 本文: 生成プロンプトの内容規約) | `references/prompt-skill.md:47-53` / `prompt-readme.md:46-49` の規約⑤ | `kasane/` 内部文書・ADR 番号・機械面・他 Skill / リポジトリ内ファイルへの参照の禁止と「配布座標の URL は可」が両方に入っていることを確認 (翻案元との `diff` で規約⑤ が置き換わっていることも確認) | ✅ 一致 |
| (Requirement 本文: `docs/` / `openspec` 検査を持ち込まない) | — | `SKILL.md` / prompt 2 本に `openspec` 0 件。`docs/` は検査としては存在せず、`SKILL.md:480` に「scope 外パスの例示」として 1 件現れるのみ (検査の持ち込みではない) | ✅ 一致 |

### Requirement: 配信識別子の表記ゆれ検査

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 誤表記の検出 | `SKILL.md:499-501` の grep (対象一覧は新パス) | fixture 実走: Scenario 列挙 5 種 (`Ksdialogs` / `ks-dialogs` / `com.kamusoft` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui`) と単数形 `KsDialog ` を含む行を検出 | ✅ 一致 |
| 正しい識別子は素通り | `SKILL.md:500` のパターン (`KsDialog([^sA-Za-z0-9_]\|$)`) + `:490` の注記 | fixture 実走: spec 列挙の正しい識別子 (SwiftPM `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose`・`ksdialogs-kmp` / Kotlin `jp.kamusoft.ksdialogs.compose` / NuGet `KsDialogs.Maui` / namespace `KsDialogs`) と `KsDialogAttributes` のみの行、配布座標 URL の行は報告なし | ✅ 一致 |
| (Requirement 本文: 正典 2 本の参照と識別子表) | `SKILL.md:481-490` | `kasane/decisions/cross/0005-public-identifier-mapping.md` と `kasane/decisions/android/0001-compose-api-separate-module.md` の実在を確認 | ✅ 一致 |

### Requirement: 規約記述と lint 範囲

| Scenario | 実装 | 検証 (実走) | 状態 |
|---|---|---|---|
| 宣言の一致 | `AGENTS.md:11-12` の 2 行。`CLAUDE.md` は `AGENTS.md` への symlink | `git diff HEAD -- AGENTS.md` で追加 2 行を確認。`git ls-files -s CLAUDE.md` が mode `120000` (symlink) で、宣言は構造上必ず一致する。実行手順・フラグは含まれず、スキル本体のパスのみを指す | ✅ 一致 |
| skills/ が identity-lint の範囲に入る | `kasane/config.yaml:19` `scope: [kasane, skills, README.md, README_ja.md]` | `kasane` と `skills` の両方を含む (Scenario の要求を満たす)。指示によりルート README 2 枚が追加されている点は deviation 記録済み。`in_scope()` の実測で `README.md` / `README_ja.md` / `kasane/**` / `skills/**` が True、`samples/README.md` / `core/**` が False。未存在 pathspec (`skills` / `README_ja.md`) があっても `git grep` は rc=0 で正常終了し検査が無音化しないことを確認。`identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` いずれも exit 0 | ⚠️ deviation 記録済み |
| (Requirement 本文: config の `context` に同趣旨) | `kasane/config.yaml:9-10` | 本文照読 | ✅ 一致 |
| (Requirement 本文: `lint.comment-policy.exclude` は `skills`) | `kasane/config.yaml:31` | `exclude: [skills]`。fixture で `skills/` を実在させた状態でも `comment-policy-lint.py` は検査対象 919 ファイル / 違反 0 で exit 0 | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 6 節 20 項目すべて `[x]`。5.1〜5.6 は本検証でも fixture で再実走して裏付けを取った。**虚偽チェックなし**。ただし 5.x の完了条件のうち tasks 2.9 の「許容箇所 1 箇所」は、追加指示後の本文では 3 箇所になっている (いずれも KsDialogs 向けの意図的な記述で翻案元の残留ではない。review-004 の Suggestion #1 で deviation への追記を推奨) |
| 逆流検査 (足場凍結) | `git status --porcelain` で `kasane/changes/adopt-docs-refresh/` の追跡ファイル変更は `tasks.md` のみ (`[ ]` → `[x]`)。`proposal.md` / `specs/docs-refresh/spec.md` / `second-opinion-spec-001.md` は無変更 |
| 記録済み乖離との突き合わせ | `deviation.md` の 4 件すべてが実装と一致 — ① tasks 2.2 の文言差 (`SKILL.md:41`)、② `lint.identity.scope` へのルート README 追加 (`config.yaml:19`)、③ `/tmp` の KsDialogs 固有化と `link-resolution-check.py` の `DOCS_REFRESH_TARGETS` (既定値は翻案元と同じ)、④ `.gitignore` の [付随修正] |
| 未記録乖離 | なし。diff の全ファイル (`.agents/` 一式 / `.claude/skills/` symlink / `AGENTS.md` / `kasane/config.yaml` / `.gitignore` / `tasks.md`) が Scenario または deviation 記録に対応する |
| 付随修正 | `.gitignore` の 1 件が `[付随修正]` として記録済み。Requirement を持たないため対応表の対象外。担保: `git ls-files -z \| xargs -0 git check-ignore --no-index` が空 (追跡中 22 本の証跡ログを含め巻き添え 0 件)、旧 `.gitignore` との無視集合の実差分 0 件、`.agents/` / `.claude/skills/` / `.claude/settings.json` はいずれも非無視 |
| UI 変更 | なし (`ui/` を持たない change) |
| テスト全件実行 | 製品コード・テストに変更がないことを確認したうえで、proposal の Impact どおり**合意済み例外**として省略。代替として scripts 7/8 の byte 一致 + 1 本の差分確認、`concepts-coverage-check.py` の manifest 不在時停止、標準 lint 3 本の exit 0、fixture による Step 3c〜6-⑧ の全 8 検査の通し実走 (正例・負例・`--readme-only` 分岐を含む) を行った |
| 作業の後片付け | fixture (リポジトリ直下 `skills/`) と `/tmp/docs-refresh-ksdialogs-{decisions.json,manifest-planned.json,targets.txt}` を `trash` で撤去。`git status --porcelain` は fixture 構築前と同一。`/tmp/docs-refresh-manifest-planned.json` (KsSettingsView 側の残骸) は指示どおり未接触 |

## ❌ の一覧と見立て

なし。9 Requirement / 22 Scenario のすべてが「✅ 一致」または「⚠️ deviation 記録済み」で、未記録の欠落・乖離・虚偽チェック・逆流は検出されなかった。
