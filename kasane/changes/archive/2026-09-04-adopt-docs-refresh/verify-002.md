# 検証結果: adopt-docs-refresh (002 回目)

**日付**: 2026-09-04
**判定**: INVALID (❌ 1 件)

対象: `specs/docs-refresh/spec.md` の ADDED Requirement 9 本 / Scenario 22 本。`deviation.md` に記録済みの乖離 1 件 (tasks 2.2 の文言と実装の差) は Requirement を持たないため対応表の対象外。

verify-001 の ❌ 1 件 (Scenario「正しい識別子は素通り」) は解消。代わりに Requirement「閉世界性と機械面の漏れ検査」の ① に、今回の修正で入った取りこぼしが ❌ 1 件。

「実装」は本 change の成果物の該当箇所、「検証」は本サイクルで再実走した結果 (製品コード・テストに触れない change のため、テストの代わりに tasks 5.x の検証手段を fixture で再実走した)。fixture は scratchpad に置き、リポジトリ内には残していない。

## 対応表

### Requirement: docs-refresh 一式の配置と起動経路

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 2 つの入口が同じ実体を指す | `.agents/skills/docs-refresh/` (SKILL.md + references 2 + scripts 8)、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` | `stat -f %i` で両入口の SKILL.md が同一 inode (100700537)。`cmp` で scripts 8/8 が翻案元と byte 一致。翻案元 `../KsSettingsView` は `33ce94e..HEAD` で当該ディレクトリに差分なし | ✅ 一致 |
| 翻案元の契約の保持 | frontmatter `description` (`SKILL.md:3`)、`:94-100` (manifest 検証 4 ケース)、`:102-115` (停止案内)、Guardrails (`:538-557`) の承認前無変更・器 `ksn-implementer` 固定・`--readme-only` の concepts スナップショット非更新・manifest を最後に書く・`--all` の扱い、`:519` (予定 manifest と同一内容)、`:518` (旧ハッシュ保持) | 今回の編集は 6-⑤ / 6-⑧ とその注記に限られる。契約項目を本文で 1 件ずつ再照読し、欠落・改変なしを確認 | ✅ 一致 |
| concepts 更新後の非発動 | `SKILL.md:3` (description) と Guardrails の「自発的な自動発動をしない」 | 両方に残存を確認 | ✅ 一致 |

### Requirement: 初期生成前の停止

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 異常な manifest でも書き換えない | `SKILL.md:94-100` の 4 ケース (不在 / parse 不能 / `version` ≠ 3 / 必須キー欠落・型不正)、`:102-115` の停止案内が「承認を伴う変更フロー (Kasane の change) の実装として行う」へ誘導 | 本文照読で 4 ケースと誘導文を確認 | ✅ 一致 |
| skills/ 未生成での起動 | 翻案元無改変の `scripts/concepts-coverage-check.py` | 実行 → `FileNotFoundError: 'skills/.manifest.json'` / exit=1。実行前後の `git status --porcelain` が同一 (書き換えなし) | ✅ 一致 |

### Requirement: 追従対象の規範

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 追従対象の特定 | `SKILL.md:24-32` (5 Skill 表)、`:34-41` (README 4 枚 + `readmes` が正 + 構成見直しは守備範囲外) | 5 Skill 名・KMP 1 本構成・移行 Skill 構成・README 4 枚を確認。`kssettingsview` の残留は 0 件、`KsSettingsView` は許容箇所 (`:12` 翻案元出典) の 1 箇所のみ | ✅ 一致 |

### Requirement: 移行 Skill の源泉は新 API 側の concepts のみ

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 移植元の変更は要追従にならない | `SKILL.md:139-145` の源泉規則 (旧 API 側は concepts に源泉を持たず対象外・書き起こしは初期生成時の一度限り・clone は `cross/reference/reference-repositories.md` で解決) | 本文照読 | ✅ 一致 |
| 新 API 側の変更で対応先が追従する | `SKILL.md:145` / `:161-169` (3b の逆引き規則) | 本文照読。逆引きは manifest の `targets` に対する通常経路で、今回の編集は触れていない | ✅ 一致 |

### Requirement: excluded の初期値と architecture カテゴリの既定

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| architecture 配下の新 concept | `SKILL.md:177` (excluded 初期値 = `cross/reference/reference-repositories.md` 1 本 + 理由)、`:179` (architecture は除外候補の既定・自動除外しない)、`:238-242` (Step 4 提示例) | 本文照読。`kasane/concepts/**/architecture/` は現時点 0 本のため机上確認 | ✅ 一致 |
| 利用者に効く architecture concept は targets へ回せる | `SKILL.md:179` 末尾 + `:238-242` の両選択肢並記 | 本文照読 | ✅ 一致 |
| (Requirement 本文: excluded 1 本 + 残り 9 本が targets) | 同上 | リポジトリの実 concepts は index/log/rules を除いて 10 本 (core/api 8 + cross/reference 1 + maui/api 1)。excluded 1 + targets 9 と一致 | ✅ 一致 |

### Requirement: コード正の機械チェック (ツール最低バージョン) の取得元

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 取得元が実在して値を返す | `SKILL.md:195-199` の 4 行表 | 実読: `agp=9.3.0` / `kotlin=2.4.10` / `android-minSdk=24` / `android-compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / `SupportedOSPlatformVersion` `17.0`・`24.0` / `Microsoft.Maui.Controls 10.0.1` — 全項目が非空 | ✅ 一致 |
| KMP の catalog 共有が外れた | `SKILL.md:193` (前提を先に確認し、外れていたら報告して停止) | `kmp/settings.gradle.kts:32` が `from(files("../android/gradle/libs.versions.toml"))` を持つ (前提は現状成立)。停止規定は本文照読 | ✅ 一致 |
| version catalog の変更 | `SKILL.md:195` ① 行 + `:204` の突合先の読み方 | 本文照読 | ✅ 一致 |
| Gradle wrapper の食い違い | `SKILL.md:196` ② 行 (食い違いそのものを別途報告) | 本文照読。現状 2 本とも `gradle-9.7.0-bin.zip` | ✅ 一致 |
| MAUI 本体下限の変更 | `SKILL.md:198` ④ 行 + `:204` の例 (`ksdialogs-maui/SKILL.md` の en/ja) | 本文照読。csproj の該当 3 種が実在 | ✅ 一致 |
| (Requirement 本文: module の build.gradle.kts は取得元にしない) | `SKILL.md:206` の注記 | 本文照読 | ✅ 一致 |

### Requirement: 閉世界性と機械面の漏れ検査

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 内部用語の漏れ | `SKILL.md:395` (① `kasane/`)、`:396` (② `ADR-[0-9]{4}`) | fixture 実走: `](../../../kasane/concepts/index.md)` / `./kasane/index.md` / `[source]: ../../../../kasane/…` / `ADR-0004` をすべて検出。`skills/README.md` の `](../kasane/…)` も検出 (前回 ❌ だった相対パス形は解消)。URL 中の `…/kasane/…` と `mykasane/` は素通り | ✅ 一致 (Scenario の GIVEN「リンク」の範囲では成立) |
| 機械面の漏れ | `SKILL.md:397` の `KsDialogsInteropBridge\|KsDialogsInteropResultType` | fixture 実走で該当行を検出 | ✅ 一致 |
| Skill ルート外への相対リンク | `SKILL.md:401-434` の python 断片 (インライン形式 + 行頭の参照定義 `[label]: path`) | fixture 実走: `../../../kasane/concepts/index.md` (インライン)、`[source]: ../../../../kasane/…` (参照形式)、`[o]: ../ksdialogs-android/SKILL.md` (別 Skill・参照形式) の 3 件を検出。同一 Skill 内の `[SKILL](../SKILL.md)` と `[ref]: ../SKILL.md` は報告なし。`skills/README.md` は `len(parts) < 4` で対象外。`[]( )` を含む行で例外は発生しない (前回 ❌ だった参照形式の未解析は解消) | ✅ 一致 |
| (Requirement 本文 ①: `kasane/` 配下への**参照**の検出) | `SKILL.md:395` の許可リスト型文字クラス | fixture 実走: 全角区切り・強調記号で囲んだ形 (`「kasane/…」`・`（kasane/…）`・`**kasane/…**`・`*kasane/…*`・`“kasane/…”`・`【kasane/…】`) が 6 件すべて未検出。修正前のパターン `(^|[^A-Za-z0-9_./-])kasane/` では 6 件とも検出できていたため、今サイクルの修正で入った取りこぼし。`skills/ja/` と `skills/README_ja.md` に効く | ❌ 乖離 |
| (Requirement 本文: 生成プロンプトの内容規約) | `references/prompt-skill.md:47-52` / `prompt-readme.md:46-49` の規約⑤ | `kasane/` 内部文書・ADR 番号・機械面・他 Skill / リポジトリ内ファイルへの参照の禁止と「配布座標の URL は可」が両方に入っていることを確認 | ✅ 一致 |
| (Requirement 本文: `docs/` / `openspec` 検査を持ち込まない) | — | `SKILL.md` / prompt 2 本に `docs/` `openspec` の残留 0 件 | ✅ 一致 |

### Requirement: 配信識別子の表記ゆれ検査

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 誤表記の検出 | `SKILL.md:499-501` の grep | fixture 実走: `Ksdialogs` / `ks-dialogs` / `com.kamusoft.sample` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui` の Scenario 列挙 5 種に加え、単数形 `KsDialog ` / `KsDialogs.MAUI` / `Ks_Dialogs` / `KSDialogs` / `ksDialogs` / `KSdialogs` を検出 | ✅ 一致 |
| 正しい識別子は素通り | `SKILL.md:500` のパターン (`KsDialog([^sA-Za-z0-9_]\|$)`) + `:490` の注記 | fixture 実走: spec 列挙の正しい識別子 (SwiftPM `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose`・`ksdialogs-kmp` / Kotlin `jp.kamusoft.ksdialogs`・`.compose`・`.kmp` / NuGet `KsDialogs.Maui` / namespace `KsDialogs`) と **`KsDialogAttributes`**・配布座標 URL のみを含むファイルは 0 件報告。verify-001 の ❌ は解消 | ✅ 一致 |
| (Requirement 本文: 正典 2 本の参照と識別子表) | `SKILL.md:481-490` | `kasane/decisions/cross/0005-public-identifier-mapping.md` と `kasane/decisions/android/0001-compose-api-separate-module.md` の実在、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:30` の `public fun KsDialogAttributes(` を確認 | ✅ 一致 |

### Requirement: 規約記述と lint 範囲

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 宣言の一致 | `AGENTS.md:11-12` の 2 行。`CLAUDE.md` は `AGENTS.md` への symlink | `ls -la CLAUDE.md` で symlink を確認 (内容は定義上一致)。追加 2 行に実行手順・フラグは含まれず、スキル本体のパスのみを指す | ✅ 一致 |
| skills/ が identity-lint の範囲に入る | `kasane/config.yaml:19` `scope: [kasane, skills]` | 該当行を確認。`lint.comment-policy.exclude` は `[skills]`。`identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` いずれも exit 0 | ✅ 一致 |
| (Requirement 本文: config の `context` に同趣旨) | `kasane/config.yaml:9-10` | 本文照読 | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 6 節 20 項目すべて `[x]`。5.1〜5.6 は本検証でも再実走して裏付けを取った。**虚偽チェックなし** |
| 逆流検査 (足場凍結) | `git status --porcelain` で `kasane/changes/adopt-docs-refresh/` の追跡ファイル変更は `tasks.md` のみ (`[ ]` → `[x]`)。`proposal.md` / `specs/docs-refresh/spec.md` / `second-opinion-spec-001.md` は無変更。今サイクルの編集は `.agents/skills/docs-refresh/SKILL.md` のみ |
| 記録済み乖離との突き合わせ | `deviation.md` の 1 件 (tasks 2.2 の文言 vs 実装) は Requirement を持たないため対応表の対象外。合意済み差分として扱った |
| 未記録乖離 | ❌ 1 件 (下記) |
| 付随修正 | diff に Scenario 非対応の変更はなし (`.agents/` / `.claude/skills/` / `AGENTS.md` / `kasane/config.yaml` / `tasks.md` のみ) |
| UI 変更 | なし (`ui/` を持たない change) |
| テスト全件実行 | 製品コード・テストに変更がないことを確認したうえで、proposal の Impact どおり**合意済み例外**として省略。代替として scripts 8/8 の byte 一致、`concepts-coverage-check.py` の起動確認、標準 lint 3 本の exit 0、6-⑤ / 6-⑧ の fixture 実走を行った |
| 作業の後片付け | 検証 fixture は scratchpad のみ。リポジトリ内に残骸なし。`/tmp/docs-refresh-targets.txt` は解消済み (`/tmp/docs-refresh-manifest-planned.json` に KsSettingsView 側の残骸があるが本 change の生成物ではない。review-002.md の Suggestion で扱う) |

## ❌ の一覧と見立て

### ❌ Requirement「閉世界性と機械面の漏れ検査」/ ① (Requirement 本文)

**乖離**: `SKILL.md:395` の検出パターンが許可リスト型の文字クラス (`[[:space:]([<"'\`,|:=]`) になっているため、全角区切り・強調記号の直後に現れる `kasane/` 参照 (`「kasane/…」`・`（kasane/…）`・`**kasane/…**` 等) を検出しない。Requirement は ① を「`kasane/` 配下への参照」としてリンク形に限定せず無条件の SHALL で要求しているため、この取りこぼしは契約を満たしていない。生成物の半分は `skills/ja/` であり、`skills/README_ja.md` には ③ (相対リンク検査) が適用されないため代替の防波堤もない。

**見立て**: **実装を直すべき**。deviation として合意する筋ではない — 修正前のパターン (`(^|[^A-Za-z0-9_./-])kasane/`) はこれらを検出できており、前サイクルの指摘 (相対パス形の取りこぼし) を直す過程で入った回帰にあたる。修正は文字クラスを否定形へ戻す 1 箇所 (`(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/`) で済み、前サイクルで直した相対パス形の検出と URL / 単語内一致の素通りは維持されることを fixture で確認済み。詳細と推奨修正は `review-002.md` の Major 指摘を参照。
