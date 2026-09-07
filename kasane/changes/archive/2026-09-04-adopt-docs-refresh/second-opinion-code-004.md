# セカンドオピニオン: adopt-docs-refresh (code-004)
**相方**: codex / **label**: so-code-adopt-docs-refresh-b (新セッション、オーナー追加指示後の最終確認) / **日付**: 2026-09-04 / **対象**: 未コミット作業ツリー (`.agents/skills/docs-refresh/` 一式・`.claude/skills/docs-refresh`・`AGENTS.md`・`kasane/config.yaml`・`.gitignore`。ベース HEAD 3a147e2)
---
## 1. 判定と総評

**判定: CHANGES_REQUESTED**

Critical 0件、Major 0件、Minor 1件です。

主要実装は仕様と整合しています。特に以下は確認できました。

- `/tmp/docs-refresh-ksdialogs-*` の生成側・参照側は一致
- 旧 `/tmp/docs-refresh-*` は `link-resolution-check.py` の互換既定値以外に残存なし
- 7スクリプトは翻案元 commit `33ce94e` と byte 一致
- `link-resolution-check.py` の差分は `DOCS_REFRESH_TARGETS` の説明と取得処理のみ
- `.gitignore` は既存エントリを維持し、verification/evidence の直下・サブディレクトリの `.log` を救済
- `.agents/` は無視対象外
- `lint.identity.scope` は `kasane`・`skills`・ルート README 2枚に実効
- proposal/spec の逆流変更なし
- `identity-lint.py`、`local-path-lint.py`、`comment-policy-lint.py` は exit 0
- 製品テスト全件実行は proposal 記録済みの合意済み例外として省略

ただし、完了済みとされた残留語検査が実際には通らないため、verify は **INVALID** です。修正範囲は局所的です。

照合した規約:

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（変更の完了判定）
- cross/ADR-0005
- android/ADR-0001
- package-distribution phase-1 agenda

## 2. 指摘一覧

### [🟡 Minor] 完了済みの翻案元残留検査に未許容の `docs/` が残っている

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:480`、`tasks.md:18`

**問題点**: `tasks.md` 2.9 は、SKILL.md と prompt 2本に対して `docs/` を含む翻案元固有語を検索し、許容箇所以外に残っていないことを完了条件としています。しかし、SKILL.md 6-⑦の注記に「例: `docs/` 配下」が残っており、指定された grep が現在もヒットします。

`.agents/skills/docs-refresh/SKILL.md:309` の `KsSettingsView` は一時ファイル衝突を説明する記録済み deviation に必要な記述ですが、480行目の `docs/` は deviation に記録された例外ではありません。したがって、2.9 の `[x]` は現状では客観的に成立していません。

**推奨修正**: 480行目の例を、KsDialogs に存在しない旧ディレクトリ名を使わない表現（例:「scope に未登録の先頭セグメント配下」）へ置き換え、`tasks.md:18` 記載の残留 grep を再実行してください。

## 3. verify

### Scenario 対応表

| Requirement / Scenario | 実装 | テスト・確認根拠 | 状態 |
|---|---|---|---|
| docs-refresh配置 / 2つの入口 | `.agents/skills/docs-refresh/SKILL.md:1`、`.claude/skills/docs-refresh` | symlink の `readlink`・同一実体判定、8スクリプトの blob 比較 | ⚠️ deviation記録済み |
| docs-refresh配置 / 翻案元契約の保持 | `.agents/skills/docs-refresh/SKILL.md:43`、`:57`、`:92`、`:147`、`:262`、`:284`、`:514` | 翻案元との差分照合、`tasks.md:38` | ✅ 一致 |
| docs-refresh配置 / concepts更新後の非発動 | `.agents/skills/docs-refresh/SKILL.md:3`、`:544` | 自動起動用hook・設定追加なし | ✅ 一致 |
| 初期生成前の停止 / 異常manifest | `.agents/skills/docs-refresh/SKILL.md:92` | 不在・parse・version・必須キー/型の4ケースを本文照合 | ✅ 一致 |
| 初期生成前の停止 / skills未生成 | `.agents/skills/docs-refresh/SKILL.md:94`、`scripts/concepts-coverage-check.py:19` | `skills/` 不在でスクリプトが書換えなし・exit 1 | ✅ 一致 |
| 追従対象 / 対象の特定 | `.agents/skills/docs-refresh/SKILL.md:22` | 5 Skill、KMP構成、移行構成、README 4枚を照合 | ✅ 一致 |
| 移行源泉 / 移植元変更は非追従 | `.agents/skills/docs-refresh/SKILL.md:139`、`:159` | 旧APIをtargetsに含めない規則とconcept逆引きを照合 | ✅ 一致 |
| 移行源泉 / 新API変更で追従 | `.agents/skills/docs-refresh/SKILL.md:141`、`:167` | targets値からの逆引き・en/ja展開を照合 | ✅ 一致 |
| excluded / architecture新規concept | `.agents/skills/docs-refresh/SKILL.md:171`、`:177` | `tasks.md:36` のfixture確認、提示例 `SKILL.md:238` | ✅ 一致 |
| excluded / targetsへの振り分け | `.agents/skills/docs-refresh/SKILL.md:173`、`:179` | ユーザー確定前にmanifestを変更しない規則を照合 | ✅ 一致 |
| 取得元 / 全取得値が実在 | `.agents/skills/docs-refresh/SKILL.md:193`、`:197` | catalog、wrapper 2本、Package.swift、csprojを実読 | ✅ 一致 |
| 取得元 / KMP catalog共有解除 | `.agents/skills/docs-refresh/SKILL.md:193` | 前提不成立時の停止と非推測を本文照合 | ✅ 一致 |
| 取得元 / version catalog変更 | `.agents/skills/docs-refresh/SKILL.md:191`、`:199`、`:204` | README・該当Skillへの追加規則を照合 | ✅ 一致 |
| 取得元 / wrapper不一致 | `.agents/skills/docs-refresh/SKILL.md:200` | 2本を読み、不一致自体を別報告する規則を照合 | ✅ 一致 |
| 取得元 / MAUI本体下限変更 | `.agents/skills/docs-refresh/SKILL.md:202`、`:204` | csprojの取得箇所とmaui Skillへの対応を照合 | ✅ 一致 |
| 閉世界性 / 内部用語 | `.agents/skills/docs-refresh/SKILL.md:384`、`:397` | `kasane/`・ADRパターンを静的照合 | ✅ 一致 |
| 閉世界性 / 機械面 | `.agents/skills/docs-refresh/SKILL.md:399`、`:448` | 2つのInterop名の検出規則を照合 | ✅ 一致 |
| 閉世界性 / Skill外相対リンク | `.agents/skills/docs-refresh/SKILL.md:403` | 同一Skill内・別Skillリンクの解決ロジックを照合 | ✅ 一致 |
| 配信識別子 / 誤表記 | `.agents/skills/docs-refresh/SKILL.md:482`、`:502` | spec例5件をパターンへ入力し全件検出 | ✅ 一致 |
| 配信識別子 / 正しい識別子 | `.agents/skills/docs-refresh/SKILL.md:486`、`:510` | 正規識別子と `KsDialogAttributes` が非検出 | ✅ 一致 |
| 規約記述 / 宣言一致 | `AGENTS.md:11`、`CLAUDE.md` | CLAUDE.mdがAGENTS.mdへのsymlink、実行手順なし | ✅ 一致 |
| 規約記述 / identity-lint範囲 | `kasane/config.yaml:19`、`.agents/skills/docs-refresh/SKILL.md:463` | 設定parseと `Settings.in_scope()` の実効判定 | ⚠️ deviation記録済み |

### 追加検査

| 検査 | 結果 |
|---|---|
| tasks完了・虚偽チェック | ❌ `tasks.md:18` の残留語検査が未成立 |
| 足場逆流 | ✅ proposal/specはベースから変更なし |
| 未記録乖離 | ❌ SKILL.md:480 の `docs/` 残留 |
| 記録済みdeviation | ✅ link script、identity scope、`.gitignore`を違反扱いしていない |
| `.gitignore` | ✅ 既存規則維持、証跡ログ救済、`.agents/`非除外 |
| 合意された静的lint | ✅ すべてexit 0 |
| 製品テスト | ⚠️ proposal記録済みの合意済み例外で省略 |

**verify判定: INVALID**

全Scenarioの実装対応自体は揃っていますが、チェック済みタスク2.9が未成立であり、未記録の残留が1件あるためです。`SKILL.md:480` の表現を修正して残留grepを通せば、VALIDへ収束できる状態です。

## 突き合わせ結果

ホスト側 review-004.md / verify-004.md (APPROVED / VALID) と突き合わせ (2026-09-04)。相方 004 は CHANGES_REQUESTED (Minor 1)、修正確認の 005 で APPROVED / VALID。

| 指摘 | 出典 | 採否 |
|---|---|---|
| 6-⑦ 注記の例示 `docs/` が残り tasks 2.9 の残留 grep が通らない (SKILL.md:480) | 相方 Minor + ホスト Suggestion (許容箇所が 3 に増えた) で双方一致 | **確定・Minor** — `docs/` の例示を一般化し、309 行目の翻案元固有名も言い換えて許容箇所を 12 行目の 1 箇所に戻した (オーケストレーター直接修正、相方 005 で解消確認) |
| env var 付け忘れ警告 (SKILL.md:329) に 6-⑥ の `DOCS_REFRESH_TARGETS` が漏れている | ホストのみ Minor (根拠強: 既定値が翻案元と共通で姉妹リポジトリの残骸を黙って読む経路) | **確定・Minor** — 1 文追記 (直接修正、相方に確認依頼) |
| 6-⑦ 注記の scope 列挙から `kasane` が落ちている | ホストのみ Suggestion | 同梱修正 (直接) |
| `.gitignore` の証跡例外が `kasane/changes/**` 限定で `lint.exclude` (`kasane/**/verification/**/*.log`) と範囲がずれる | ホストのみ Suggestion | 同梱修正 — `kasane/**` に広げて lint.exclude と揃えた (check-ignore で追跡中ファイルの巻き添え 0 を再確認) |

確定 2 / 採用 0 / 降格 0 / 未解決 0。
