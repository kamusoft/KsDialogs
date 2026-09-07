# セカンドオピニオン: adopt-docs-refresh (code-007)
**相方**: codex / **label**: so-code-adopt-docs-refresh-b (turn 4、code-006 Major の修正確認) / **日付**: 2026-09-04 / **対象**: 未コミット作業ツリー (修正は `.gitignore` の証跡例外直後の保護再適用のみ。ベース HEAD 3a147e2)
---
## 1. 判定と総評

**APPROVED**

前回のMajor指摘は解消されています。

- `.gitignore:102-103` で通常のKasane証跡ログを救済した後、`.gitignore:105-107` で `secrets.*`、`.env.*`、`hs_err_pid*.log` を再適用しており、Gitの後勝ち規則どおり機能します。
- change、archive、roadmap、直下およびサブディレクトリの通常証跡ログは非無視です。
- 一般ログ、Kasane外のログ、類似名ディレクトリのログ、機密名・クラッシュログは無視されます。
- `.gitignore:100-101` のコメントも、`verification/` は `lint.exclude` と一致し、`evidence/` はsanitize済み抜粋を置いてlintを通す場所であることを正確に区別しています。
- 末尾に追加の否定規則はなく、再適用した保護が後続規則で再度解除される経路もありません。

照合規約は `comment-policy.md`（always）、`test-execution.md`（完了判定）、Kasaneの証跡規約です。

## 2. 指摘一覧

指摘なし。

| 重要度 | 件数 |
|---|---:|
| Critical | 0 |
| Major | 0 |
| Minor | 0 |
| Suggestion | 0 |

### `.gitignore` 実測結果

| 分類 | 代表パス | 結果 |
|---|---|---|
| 一般ログ | `foo.log` | 無視 |
| Kasane通常ログ | `kasane/root.log` | 無視 |
| change検証ログ | `kasane/changes/x/verification/a.log` | 非無視 |
| change検証ログ・下位階層 | `kasane/changes/x/verification/sub/a.log` | 非無視 |
| archive検証ログ | `kasane/changes/archive/demo/verification/sub/a.log` | 非無視 |
| roadmap検証ログ | `kasane/roadmaps/x/verification/a.log` | 非無視 |
| roadmap証跡ログ | `kasane/roadmaps/x/evidence/a.log` | 非無視 |
| Kasane外のevidenceログ | `other/evidence/a.log` | 無視 |
| 類似名ディレクトリ | `kasane/roadmaps/x/not-verification/a.log` | 無視 |
| 機密名 | `verification/secrets.log`・`evidence/secrets.log` | 無視 |
| 環境ファイル名 | `verification/.env.log`・`evidence/.env.log` | 無視 |
| JVMクラッシュログ | `verification/hs_err_pid123.log`・`evidence/hs_err_pid123.log` | 無視 |

## 3. verify

### Scenario対応表

| # | Scenario | 対応状況 | 状態 |
|---:|---|---|---|
| 1 | 2つの入口が同じ実体を指す | Skill本体とClaude symlinkを確認 | ✅ |
| 2 | 翻案元の契約の保持 | 7スクリプト一致、link-resolution-checkのみ記録済み差分 | ⚠️ deviation記録済み |
| 3 | concepts更新後の非発動 | frontmatterとAGENTS.mdで自動発動禁止 | ✅ |
| 4 | 異常なmanifestでも書き換えない | manifest検証と停止条件あり | ✅ |
| 5 | skills/未生成での起動 | 初期生成changeへの誘導あり | ✅ |
| 6 | 追従対象の特定 | 5 Skill×2言語とREADME 4枚を定義 | ✅ |
| 7 | 移植元の変更は要追従にならない | 新API側conceptsのみをsourceとして定義 | ✅ |
| 8 | 新API側の変更で対応先が追従する | targets逆引きあり | ✅ |
| 9 | architecture配下の新concept | 自動除外せず候補として提示 | ✅ |
| 10 | 利用者に効くarchitecture concept | ユーザー判断でtargetsへ追加可能 | ✅ |
| 11 | 取得元が実在して値を返す | 4取得元と抽出方法を定義 | ✅ |
| 12 | KMPのcatalog共有が外れた | 前提確認後に停止 | ✅ |
| 13 | version catalogの変更 | README・該当Skillを追従対象化 | ✅ |
| 14 | Gradle wrapperの食い違い | README突合と別に報告 | ✅ |
| 15 | MAUI本体下限の変更 | README・MAUI Skillを追従対象化 | ✅ |
| 16 | 内部用語の漏れ | 6-⑤のgrepで検出 | ✅ |
| 17 | 機械面の漏れ | Interop名を検出 | ✅ |
| 18 | Skillルート外への相対リンク | 解決検査を実装 | ✅ |
| 19 | 誤表記の検出 | ADR由来の誤表記パターンあり | ✅ |
| 20 | 正しい識別子は素通り | 正当な識別子を除外 | ✅ |
| 21 | 宣言の一致 | AGENTS.mdとCLAUDE.mdが同一実体 | ✅ |
| 22 | skills/がidentity-lintの範囲に入る | `kasane, skills, README.md, README_ja.md` | ⚠️ deviation記録済み |

### 追加検査

| 確認項目 | 結果 |
|---|---|
| 未完了タスク | なし |
| tasks 2.9 残留grep | 許容箇所 `.agents/skills/docs-refresh/SKILL.md:12` のみ |
| 足場への逆流 | proposal/specに差分なし |
| 未記録の仕様乖離 | なし |
| 追跡中ファイルのignore判定 | 出力なし |
| 追跡中のKasaneログのignore判定 | 出力なし |
| `.agents/` | 非無視 |
| `identity-lint.py` | exit 0 |
| `local-path-lint.py` | exit 0 |
| `comment-policy-lint.py` | 0件 |
| `git diff --check` | 問題なし |
| 製品テスト | proposal Impactの合意済み例外として省略 |

**verify判定: VALID**
