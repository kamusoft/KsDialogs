# 検証結果: adopt-docs-refresh (001 回目)

**日付**: 2026-09-04
**判定**: INVALID (❌ 1 件)

対象: `specs/docs-refresh/spec.md` の ADDED Requirement 9 本 / Scenario 18 本。`deviation.md` は存在しない (記録済み乖離なし)。

## 対応表

「実装」は本 change の成果物における該当箇所、「テスト」は tasks 5.x の検証手段 (本 change は製品コード・テストに触れないため、テストの代わりに検証手段を再実走した結果を記す)。

### Requirement: docs-refresh 一式の配置と起動経路

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 2 つの入口が同じ実体を指す | `.agents/skills/docs-refresh/` (SKILL.md + references 2 + scripts 8)、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` | `stat -f %i` で両入口の SKILL.md が同一 inode (100700467)。`cmp` で scripts 8/8 が翻案元 `33ce94e` と byte 一致。翻案元は `33ce94e..HEAD` で当該ディレクトリに差分なし | ✅ 一致 |
| 翻案元の契約の保持 | `SKILL.md:92-116` (manifest 検証 4 ケース)、`:260` (承認前無変更)、`:55` / `:271` (器 `ksn-implementer` 固定・最大 3 並列・委譲不能時停止)、`:61-63` (`--all` / `--readme-only` / 同時指定エラー)、`:545` / Guardrails (`--readme-only` で concepts スナップショット非更新)、`:212-220` (3e は報告のみ)、`:307-325` (予定 manifest に対する整合性チェック)、`:504-509` (manifest を最後に書く・旧ハッシュ保持) | 翻案元との `diff -u` で削除された行は翻案元固有値のみ (KsSettingsView 名・ADR-0022/0023・`KsColor`/`KsFont`・`docs/`/`openspec`・`user-skill-api-listing`・過去実績件数)。契約 11 項目を本文で 1 件ずつ確認 | ✅ 一致 |
| concepts 更新後の非発動 | frontmatter `description` (`SKILL.md:3`) と Guardrails (`:530`) の双方に自動発動禁止 | 本文照読で両方に残存を確認 | ✅ 一致 |

### Requirement: 初期生成前の停止

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 異常な manifest でも書き換えない | `SKILL.md:94-100` に 4 ケース (不在 / parse 不能 / `version` ≠ 3 / 必須キー欠落・型不正) を列挙、`:102-115` の停止案内が「承認を伴う変更フロー (Kasane の change) の実装として行う」へ誘導 | 本文照読で 4 ケースと誘導文を確認 | ✅ 一致 |
| skills/ 未生成での起動 | 翻案元無改変の `scripts/concepts-coverage-check.py` | `python3 .agents/skills/docs-refresh/scripts/concepts-coverage-check.py` → `FileNotFoundError: 'skills/.manifest.json'` / exit=1。直後の `git status --porcelain` が実行前と同一 (書き換えなし) | ✅ 一致 |

### Requirement: 追従対象の規範

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 追従対象の特定 | `SKILL.md:24-32` (5 Skill 表。KMP は 1 本で 3 側・ホスト側レシピは `references/`、移行は `SKILL.md` + `references/api-mapping.md`・Toast は対応先なし)、`:34-41` (README 4 枚 + `readmes` が正 + 構成見直しは守備範囲外) | 5 Skill 名・構成・README 4 枚を本文で確認。`kssettingsview` の残留 grep は 0 件 (許容箇所である `:12` の翻案元出典 1 箇所のみ `KsSettingsView` が残る) | ✅ 一致 |

### Requirement: 移行 Skill の源泉は新 API 側の concepts のみ

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 移植元の変更は要追従にならない | `SKILL.md:139-145`「移行 Skill の源泉規則」— 旧 API 側は concepts に源泉を持たず差分検出・要追従判定の対象外、書き起こしは初期生成時の一度限り、clone は `kasane/concepts/cross/reference/reference-repositories.md` で解決 | 本文照読。fixture manifest で `ksdialogs-aiforms-migration/references/api-mapping.md` の `targets` を新 API 側 (`maui/api/di-registration.md` + `core/api/toast-semantics.md`) のみにしても網羅検査が `concepts coverage OK` を返す | ✅ 一致 |
| 新 API 側の変更で対応先が追従する | `SKILL.md:145` (逆引きは通常どおり働く)、`:161-169` (3b の逆引き規則) | fixture の `targets-list.py` 実走で `skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md` が en/ja ペアで展開されることを確認 | ✅ 一致 |

### Requirement: excluded の初期値と architecture カテゴリの既定

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| architecture 配下の新 concept | `SKILL.md:177` (excluded 初期値 = `cross/reference/reference-repositories.md` 1 本 + 理由)、`:179` (architecture は除外候補の既定・自動除外しない・確定まで manifest を変更しない)、`:238-242` (Step 4 提示例に「architecture カテゴリのため除外候補 (既定)」) | 本文照読。現時点で `kasane/concepts/**/architecture/` は 0 本のため机上確認 | ✅ 一致 |
| 利用者に効く architecture concept は targets へ回せる | `SKILL.md:179` 末尾 (ユーザーの選択で `targets` へ回す)、`:238-242` の提示文が両選択肢を並記 | 本文照読 | ✅ 一致 |
| (Requirement 本文: excluded 1 本 + 残り 9 本が targets) | 同上 | fixture manifest (excluded 1 + targets 9 本の実在 concept) で `concepts-coverage-check.py` が `concepts coverage OK`。リポジトリの実 concepts は index/log/rules を除いて 10 本で一致 | ✅ 一致 |

### Requirement: コード正の機械チェック (ツール最低バージョン) の取得元

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 取得元が実在して値を返す | `SKILL.md:195-199` の 4 行表 | 実読: `agp=9.3.0` / `kotlin=2.4.10` / `android-minSdk=24` / `android-compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / `SupportedOSPlatformVersion` ios `17.0`・android `24.0` / `Microsoft.Maui.Controls` `10.0.1` — 全項目が非空 | ✅ 一致 |
| KMP の catalog 共有が外れた | `SKILL.md:193`「先に前提を確認する」— 外れていたら android 側の値を KMP の正として読まず報告して停止 | `kmp/settings.gradle.kts:30-34` が `from(files("../android/gradle/libs.versions.toml"))` を持つことを確認 (前提は現状成立)。停止規定は本文照読 | ✅ 一致 |
| version catalog の変更 | `SKILL.md:195` ① 行 + `:201`「突合先の読み方」(まずルート README 群、値を導入節に持つ Skill は en/ja ペア) | 本文照読。Step 4 提示例 (`:244-245`) も AGP 変化 → `README.md, README_ja.md` になっている | ✅ 一致 |
| Gradle wrapper の食い違い | `SKILL.md:196` ② 行「食い違う場合は README 突合とは別に食い違いそのものを報告する」 | 本文照読。現状 2 本とも `gradle-9.7.0-bin.zip` で一致 | ✅ 一致 |
| MAUI 本体下限の変更 | `SKILL.md:198` ④ 行 + `:201` の例 (`Microsoft.Maui.Controls` の版が食い違えば `ksdialogs-maui/SKILL.md` の en/ja) | 本文照読。csproj の該当 3 種が ios/android 条件つき `PropertyGroup` と `PackageReference` に実在することを確認 | ✅ 一致 |
| (Requirement 本文: module の build.gradle.kts は取得元にしない) | `SKILL.md:203` の注記 | 本文照読 | ✅ 一致 |

### Requirement: 閉世界性と機械面の漏れ検査

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 内部用語の漏れ | `SKILL.md:382-397` の ① `(^|[^A-Za-z0-9_./-])kasane/` と ② `ADR-[0-9]{4}` | fixture 実走: 空白直後の `kasane/concepts/...` と `ADR-0004` を含む Skill ファイルが両方とも報告された。相対パス形 (`](../kasane/...)`) は ① を素通りするが、同じ Skill ファイルに対しては ③ が「Skill ルート外への相対リンク」として報告する (Scenario の GIVEN は Skill ファイル) | ✅ 一致 (※ README 側の穴は review-001.md の Minor) |
| 機械面の漏れ | `SKILL.md:397` の `KsDialogsInteropBridge\|KsDialogsInteropResultType` | fixture 実走で該当行が報告された。両名は `ios/Sources/KsDialogs/Interop/` に現存する public 宣言であり、Guardrails (`:541`) が「廃止と書かない」で扱っていることも確認 | ✅ 一致 |
| Skill ルート外への相対リンク | `SKILL.md:401-426` の python 断片 | fixture 実走: `skills/en/ksdialogs-kmp/SKILL.md` の `../ksdialogs-android/SKILL.md` と `../../../kasane/concepts/index.md` を検出 (2 件)、同一 Skill 内の `references/ios-host.md` → `../SKILL.md` は報告なし。README (`skills/README.md`) は `len(parts) < 4` で対象外 | ✅ 一致 |
| (Requirement 本文: 生成プロンプトの内容規約) | `references/prompt-skill.md` ⑤ / `references/prompt-readme.md` ⑤ | 翻案元との diff で、`kasane/` 内部文書・ADR 番号・機械面・他 Skill / リポジトリ内ファイルへの相対リンクの禁止と「配布座標の URL は可」が両方に入っていることを確認 | ✅ 一致 |
| (Requirement 本文: `docs/` / `openspec` 検査を持ち込まない) | — | `SKILL.md` / prompt 2 本に `docs/` `openspec` の残留 0 件 | ✅ 一致 |

### Requirement: 配信識別子の表記ゆれ検査

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 誤表記の検出 | `SKILL.md:485-491` の grep | fixture 実走: `Ksdialogs` / `ks-dialogs` / `com.kamusoft.sample` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui` を含む行が報告された | ✅ 一致 |
| 正しい識別子は素通り | 同上 (`:489` のパターン) | fixture 実走: spec が列挙する識別子 (SwiftPM `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose`・`ksdialogs-kmp` / Kotlin `jp.kamusoft.ksdialogs`・`.compose`・`.kmp` / NuGet `KsDialogs.Maui` / namespace `KsDialogs`) のみのファイルは素通りした。**しかし現存する公開 Compose API `KsDialogAttributes` は `KsDialog([^s]\|$)` に当たり報告される** (`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:30` の public 宣言。`kasane/concepts/core/api/{layout,registration-show,transition}-semantics.md` に記載があり android / kmp Skill の源泉になる) | ❌ 乖離 |
| (Requirement 本文: 正典 2 本の参照と識別子表) | `SKILL.md:472-478` | `kasane/decisions/cross/0005-public-identifier-mapping.md` の Decision 表および `android/ksdialogs-compose/build.gradle.kts` (`group = "jp.kamusoft"` / `namespace = "jp.kamusoft.ksdialogs.compose"`) と一致 | ✅ 一致 |

### Requirement: 規約記述と lint 範囲

| Scenario | 実装 | 検証 (再実走) | 状態 |
|---|---|---|---|
| 宣言の一致 | `AGENTS.md:11-12` の 2 行。`CLAUDE.md` は `AGENTS.md` への symlink | `ls -la CLAUDE.md` で symlink を確認 (内容は定義上一致)。追加 2 行に docs-refresh の実行手順・フラグは含まれず、スキル本体のパスのみを指す | ✅ 一致 |
| skills/ が identity-lint の範囲に入る | `kasane/config.yaml:19` `scope: [kasane, skills]` | `yaml.safe_load` で `['kasane', 'skills']` を確認。`lint.comment-policy.exclude` は `['skills']`。`python3 scripts/identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` いずれも exit 0 (違反 0 件) | ✅ 一致 |
| (Requirement 本文: config の `context` に同趣旨) | `kasane/config.yaml:9-10` | 本文照読 | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 6 節 20 項目すべて `[x]`。5.1〜5.6 は本検証で再実走して裏付けを取り、5.7 は phase-1 agenda の決定事項 5 件 (KMP 1 本 / 移行独立 / 移行の源泉 / excluded 既定 / 3d 4 行) と翻案元契約 11 項目を照合して確認した。**虚偽チェックなし** |
| 逆流検査 (足場凍結) | `git status` で `kasane/changes/adopt-docs-refresh/` の変更は `tasks.md` のみ。`git diff --word-diff` で差分は `[ ]` → `[x]` だけ。`proposal.md` / `specs/docs-refresh/spec.md` / `second-opinion-spec-001.md` は無変更 |
| 未記録乖離 | ❌ 1 件 (下記)。`deviation.md` は存在しないため、記録済み乖離との突き合わせは対象なし |
| 付随修正 | diff に Scenario 非対応の変更はなし (変更範囲は `.agents/` / `.claude/skills/` / `AGENTS.md` / `kasane/config.yaml` / `tasks.md` のみ) |
| UI 変更 | なし (`ui/` アーティファクトを持たない change) |
| テスト全件実行 | 製品コード・テストに変更がないことを diff で確認したうえで、proposal の Impact どおり**合意済み例外**として省略。代替として scripts 8/8 の byte 一致、8 スクリプトの起動確認、標準 lint 3 本の exit 0、tasks 5.x の再実走を行った |
| 作業の後片付け | リポジトリ内に fixture の残骸なし (`git status` の未追跡は `.agents/` `.claude/skills/` と本 change 外の `kasane/lessons/inbox/translated-norm-needs-local-basis-and-fact-check.md` のみ)。検証 fixture は scratchpad 内に置いた |

## ❌ の一覧と見立て

### ❌ Requirement「配信識別子の表記ゆれ検査」/ Scenario「正しい識別子は素通り」

**乖離**: `SKILL.md:489` の `KsDialog([^s]|$)` が、現存する公開 Compose API `KsDialogAttributes` を誤検出する。Scenario の GIVEN「正しい識別子のみを含む Skill ファイル」に `KsDialogAttributes` を含む生成物が該当し、THEN「6-⑧ は何も報告しない」が成立しない。

**見立て**: **実装を直すべき**。deviation として合意する筋ではない — この API は phase-2 で `ksdialogs-android` / `ksdialogs-kmp` のレシピに載ることが決まっている源泉 concepts (3 本) に記載されており、放置すると整合性チェックが正しい生成物に対して恒常的に失敗し、SKILL.md:491 の「行が出たら再修正対象に追加する」規律のもとで抜け出せない再修正ループになる。修正は文字クラスの拡張 1 箇所 (`KsDialog([^sA-Za-z0-9_]|$)`) で済み、真の誤表記 (単数形の `KsDialog` 単独出現) の検出力は失われないことを実走で確認済み。詳細と推奨修正は `review-001.md` の Major 指摘を参照。
