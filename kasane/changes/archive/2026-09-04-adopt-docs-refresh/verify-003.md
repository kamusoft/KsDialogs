# 検証結果: adopt-docs-refresh (003 回目)

**日付**: 2026-09-04
**判定**: VALID (❌ 0 件)

対象: `specs/docs-refresh/spec.md` の ADDED Requirement 9 本 / Scenario 22 本。`deviation.md` に記録済みの乖離 1 件 (tasks 2.2 の文言と実装の差) は Requirement を持たないため対応表の対象外。

verify-002 の ❌ 1 件 (Requirement「閉世界性と機械面の漏れ検査」の ① — 全角区切り・強調記号の直後の `kasane/` を検出しない) は解消した。今サイクルで新たな ❌ は生じていない。

「実装」は本 change の成果物の該当箇所、「検証」は本サイクルで再実走した結果 (製品コード・テストに触れない change のため、テストの代わりに tasks 5.x の検証手段を fixture で再実走した)。fixture は scratchpad に置き、リポジトリ内には残していない。

## 対応表

### Requirement: docs-refresh 一式の配置と起動経路

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 2 つの入口が同じ実体を指す | `.agents/skills/docs-refresh/` (SKILL.md + references 2 + scripts 8)、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` | `stat -f %i` で両入口の SKILL.md が同一 inode (100700537)。`cmp` で scripts 8/8 が翻案元と byte 一致 | ✅ 一致 |
| 翻案元の契約の保持 | frontmatter `description` (`SKILL.md:3`)、`:63` (`--all` / `--readme-only` 同時指定エラー)、`:94-100` (manifest 検証 4 ケース)、`:102-115` (停止案内)、`:55` / `:269` / `:549` (器 `ksn-implementer` 固定)、`:271` (最大 3 並列)、Guardrails (`:538-557`) の承認前無変更・`--readme-only` の concepts スナップショット非更新・manifest を最後に書く・`--all` の扱い、`:519` (予定 manifest と同一内容)、`:518` (旧ハッシュ保持) | 今サイクルの編集は 6-⑤ ① のパターン (`:395`) と ① の注記 (`:448`) の 2 箇所のみ。契約項目を本文で 1 件ずつ再照読し、欠落・改変なしを確認 | ✅ 一致 |
| concepts 更新後の非発動 | `SKILL.md:3` (description) と Guardrails の「自発的な自動発動をしない」 | 両方に残存を確認 | ✅ 一致 |

### Requirement: 初期生成前の停止

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 異常な manifest でも書き換えない | `SKILL.md:94-100` の 4 ケース (不在 / parse 不能 / `version` ≠ 3 / 必須キー欠落・型不正)、`:102-115` の停止案内が「承認を伴う変更フロー (Kasane の change) の実装として行う」へ誘導 | 本文照読で 4 ケースと誘導文を確認 | ✅ 一致 |
| skills/ 未生成での起動 | 翻案元無改変の `scripts/concepts-coverage-check.py` | 実行 → `FileNotFoundError: 'skills/.manifest.json'` / exit=1。実行前後の `git status --porcelain` が同一 (書き換えなし) | ✅ 一致 |

### Requirement: 追従対象の規範

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 追従対象の特定 | `SKILL.md:24-32` (5 Skill 表)、`:34-41` (README 4 枚 + `readmes` が正 + 構成見直しは守備範囲外) | 翻案元固有語の残留 grep (`KsSettingsView` / `kssettingsview` / `KsColor` / `KsFont` / `ADR-0022` / `ADR-0023` / `user-skill-api-listing` / `public-identifiers` / `openspec`) を SKILL.md と prompt 2 本に掛け、許容箇所 (`:12` 翻案元出典) の 1 件のみ | ✅ 一致 |

### Requirement: 移行 Skill の源泉は新 API 側の concepts のみ

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 移植元の変更は要追従にならない | `SKILL.md:139-145` の源泉規則 (旧 API 側は concepts に源泉を持たず対象外・書き起こしは初期生成時の一度限り・clone は `cross/reference/reference-repositories.md` で解決) | 本文照読。今サイクルの編集は触れていない | ✅ 一致 |
| 新 API 側の変更で対応先が追従する | `SKILL.md:145` / `:161-169` (3b の逆引き規則) | 本文照読 | ✅ 一致 |

### Requirement: excluded の初期値と architecture カテゴリの既定

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| architecture 配下の新 concept | `SKILL.md:177` (excluded 初期値 = `cross/reference/reference-repositories.md` 1 本 + 理由)、`:179` (architecture は除外候補の既定・自動除外しない)、`:238-242` (Step 4 提示例) | 本文照読。`kasane/concepts/**/architecture/` は現時点 0 本のため机上確認 | ✅ 一致 |
| 利用者に効く architecture concept は targets へ回せる | `SKILL.md:179` 末尾 + `:238-242` の両選択肢並記 | 本文照読 | ✅ 一致 |
| (Requirement 本文: excluded 1 本 + 残り 9 本が targets) | 同上 | 実 concepts は index/log/rules を除いて 10 本 (core/api 8 + cross/reference 1 + maui/api 1)。excluded 1 + targets 9 と一致 | ✅ 一致 |

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
| 内部用語の漏れ | `SKILL.md:395` (① `kasane/`)、`:396` (② `ADR-[0-9]{4}`) | fixture 実走で全形を検出 — リンク形 `](../../../kasane/…)`、相対パス形 `./kasane/…` / 行頭 `../kasane/…`、参照形式 `[source]: ../../../../kasane/…`、行頭素形 `kasane/…`、**地の文の言及** (`**kasane/…**` / `「kasane/…」` / `（kasane/…）` / `*kasane/…*` / `“kasane/…”` / `【kasane/…】` / `・kasane/…` / `詳細はkasane/…`)、`skills/README_ja.md` の「」形とリンク形。`ADR-0004` も検出。verify-002 の ❌ は解消 | ✅ 一致 |
| 機械面の漏れ | `SKILL.md:397` の `KsDialogsInteropBridge\|KsDialogsInteropResultType` | fixture 実走で 2 行とも検出 | ✅ 一致 |
| Skill ルート外への相対リンク | `SKILL.md:401-434` の python 断片 (インライン形式 + 行頭の参照定義 `[label]: path`) | fixture 実走: インライン `[other](../../ksdialogs-android/SKILL.md)`・参照定義 `[o]: ../../ksdialogs-android/SKILL.md`・`[source]: ../../../../kasane/…`・`[spec](../../../kasane/…)` の 4 件を検出。同一 Skill 内の `[SKILL](../SKILL.md)` と `[ref]: ../SKILL.md` は報告なし。`skills/README_ja.md` は `len(parts) < 4` で対象外。`[]( )` と外部 URL で例外・誤検出なし | ✅ 一致 |
| (Requirement 本文 ①: `kasane/` 配下への**参照**の検出。リンク形に限定しない) | `SKILL.md:395` `(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/` (否定クラス)、`:448` の注記 | 上記のとおり地の文の言及 8 形をすべて検出 (verify-002 で ❌ だった 6 形を含む)。素通り側も後退なし — `https://example.com/kasane/spec.md` / `mykasane/foo` / `my_kasane/foo` / `skills/en/x/kasane/y.md` を含むファイルは 0 件報告。`LC_ALL=C` と `LC_ALL=ja_JP.UTF-8` で検出件数が同一 (16 件) であることも確認 | ✅ 一致 |
| (Requirement 本文: 生成プロンプトの内容規約) | `references/prompt-skill.md:47-52` / `prompt-readme.md:46-49` の規約⑤ | `kasane/` 内部文書・ADR 番号・機械面・他 Skill / リポジトリ内ファイルへの参照の禁止と「配布座標の URL は可」が両方に入っていることを確認 | ✅ 一致 |
| (Requirement 本文: `docs/` / `openspec` 検査を持ち込まない) | — | `SKILL.md` / prompt 2 本に `docs/` `openspec` の残留 0 件 | ✅ 一致 |

### Requirement: 配信識別子の表記ゆれ検査

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 誤表記の検出 | `SKILL.md:499-501` の grep | fixture 実走: Scenario 列挙 5 種 (`Ksdialogs` / `ks-dialogs` / `com.kamusoft` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui`) に加え、単数形 `KsDialog ` / `KsDialogs.MAUI` / `Ks_Dialogs` / `KSDialogs` / `ksDialogs` / `KSdialogs` の計 11 行をすべて検出 | ✅ 一致 |
| 正しい識別子は素通り | `SKILL.md:500` のパターン (`KsDialog([^sA-Za-z0-9_]\|$)`) + `:490` の注記 | fixture 実走: spec 列挙の正しい識別子 (SwiftPM `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose`・`ksdialogs-kmp` / Kotlin `jp.kamusoft.ksdialogs`・`.compose`・`.kmp` / NuGet `KsDialogs.Maui` / namespace `KsDialogs`) と `KsDialogAttributes`・`jp.kamusoft.ksdialogs.compose.KsDialogAttributes`・配布座標 URL のみを含むファイルは 0 件報告 (exit 1) | ✅ 一致 |
| (Requirement 本文: 正典 2 本の参照と識別子表) | `SKILL.md:481-490` | `kasane/decisions/cross/0005-public-identifier-mapping.md` と `kasane/decisions/android/0001-compose-api-separate-module.md` の実在を確認 | ✅ 一致 |

### Requirement: 規約記述と lint 範囲

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 宣言の一致 | `AGENTS.md:11-12` の 2 行。`CLAUDE.md` は `AGENTS.md` への symlink | `git diff HEAD -- AGENTS.md` で追加 2 行を確認。実行手順・フラグは含まれず、スキル本体のパスのみを指す | ✅ 一致 |
| skills/ が identity-lint の範囲に入る | `kasane/config.yaml:19` `scope: [kasane, skills]` | 該当行を確認。`lint.comment-policy.exclude` は `[skills]`。`identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` いずれも exit 0 | ✅ 一致 |
| (Requirement 本文: config の `context` に同趣旨) | `kasane/config.yaml:9-10` | 本文照読 | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 6 節 20 項目すべて `[x]`。5.1〜5.6 は本検証でも再実走して裏付けを取った。**虚偽チェックなし** |
| 逆流検査 (足場凍結) | `git status --porcelain` で `kasane/changes/adopt-docs-refresh/` の追跡ファイル変更は `tasks.md` のみ (`[ ]` → `[x]`)。`proposal.md` / `specs/docs-refresh/spec.md` / `second-opinion-spec-001.md` は無変更。今サイクルの編集は `.agents/skills/docs-refresh/SKILL.md` の 2 箇所 (`:395` / `:448`) のみ |
| 記録済み乖離との突き合わせ | `deviation.md` の 1 件 (tasks 2.2 の文言 vs 実装) は Requirement を持たないため対応表の対象外。合意済み差分として扱った |
| 未記録乖離 | なし |
| 付随修正 | diff に Scenario 非対応の変更はなし (`.agents/` / `.claude/skills/` / `AGENTS.md` / `kasane/config.yaml` / `tasks.md` のみ)。`kasane/lessons/inbox/translated-norm-needs-local-basis-and-fact-check.md` は教訓機構の未昇格観測であり、コンテキストパッケージが指定した diff 範囲外 |
| UI 変更 | なし (`ui/` を持たない change) |
| テスト全件実行 | 製品コード・テストに変更がないことを確認したうえで、proposal の Impact どおり**合意済み例外**として省略。代替として scripts 8/8 の byte 一致、`concepts-coverage-check.py` の起動確認、標準 lint 3 本の exit 0、6-⑤ ①②③ / 6-⑧ の fixture 実走 (ロケール 2 種を含む) を行った |
| 作業の後片付け | 検証 fixture は scratchpad のみ。リポジトリ内に残骸なし。`/tmp/docs-refresh-targets.txt` は生成していない (③ の python 断片は targets の読み先を scratchpad へ差し替えて実走した) |

## ❌ の一覧と見立て

なし。verify-002 の ❌ 1 件は解消し、新たな乖離は検出されなかった。
