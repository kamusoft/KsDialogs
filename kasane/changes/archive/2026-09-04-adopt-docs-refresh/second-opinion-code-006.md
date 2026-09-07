# セカンドオピニオン: adopt-docs-refresh (code-006)
**相方**: codex / **label**: so-code-adopt-docs-refresh-b (turn 3、review-004 残指摘 3 件の修正確認) / **日付**: 2026-09-04 / **対象**: 未コミット作業ツリー (修正は `.agents/skills/docs-refresh/SKILL.md:329` / `:480` の注記と `.gitignore` の証跡例外。ベース HEAD 3a147e2)
---
## 1. 判定と総評

**CHANGES_REQUESTED**

(1) と (2) は正しく修正されています。

- `.agents/skills/docs-refresh/SKILL.md:329` の警告は、実際の6-⑥のコマンド (`:457`) および `link-resolution-check.py` の既定値 (`scripts/link-resolution-check.py:21`) と整合しています。
- `.agents/skills/docs-refresh/SKILL.md:480` のscope列挙は `kasane/config.yaml:19` と一致し、「追従対象に関わるのは後ろ3つ」も正確です。
- `.gitignore` の新しい否定パターンは、`kasane/` 直下、change、archive、roadmap、さらにサブディレクトリ内の `.log` を正しく救済します。通常の `foo.log` や `kasane/.../not-verification/a.log` は引き続き無視されます。

ただし、否定パターンが末尾にあるため、機密・クラッシュログ向けの既存ignoreまで上書きする問題が1件残っています。

照合規約は `comment-policy.md`（always）、`test-execution.md`（完了判定）、および `.gitignore` の `evidence/` を扱うためKasaneの証跡規約です。

## 2. 指摘一覧

### [🟠 Major] 証跡例外が機密・クラッシュログのignoreも解除する

**該当箇所**: `.gitignore:89`、`.gitignore:90`、`.gitignore:92`、`.gitignore:101`

**問題点**: Gitのignoreは最後に一致した規則が優先されるため、末尾の証跡例外が先行する以下の保護を上書きします。

- `secrets.*`
- `.env.*`
- `hs_err_pid*.log`

実測では次がすべて非無視になりました。

- `kasane/roadmaps/x/verification/secrets.log`
- `kasane/roadmaps/x/evidence/secrets.log`
- `kasane/roadmaps/x/verification/.env.log`
- `kasane/roadmaps/x/evidence/.env.log`
- `kasane/roadmaps/x/verification/hs_err_pid123.log`

特に `verification/**/*.log` は `kasane/config.yaml:17` でlint対象外でもあるため、誤って配置された機密ログに対する防波堤が弱くなります。今回の `kasane/**` への拡張により、この解除範囲もchange配下からKasane全体へ広がっています。

また、`.gitignore:100` の「範囲は lint.exclude と揃える」は `verification` についてのみ正しく、`evidence` は `lint.exclude` に含まれていません。証跡規約上、`evidence` はsanitize済み抜粋を置いてlintを通す場所です。

**推奨修正**: 証跡例外より後ろで、少なくとも次の保護パターンを再適用してください。

```gitignore
!kasane/**/verification/**/*.log
!kasane/**/evidence/**/*.log

# 証跡ディレクトリ内でも機密・生クラッシュログは救済しない
secrets.*
.env.*
hs_err_pid*.log
```

コメントも「`verification` の範囲は `lint.exclude` と一致し、`evidence` はsanitize済み抜粋を追跡する」と区別すると実態と一致します。

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
| link-resolution-check差分 | 環境変数の説明と既定値読取りの2箇所のみ |
| `identity-lint.py` | exit 0 |
| `local-path-lint.py` | exit 0 |
| `comment-policy-lint.py` | 0件 |
| 追跡中ファイルのignore判定 | 出力なし |
| `.agents/` | 非無視 |
| 製品テスト | proposal Impactの合意済み例外として省略 |

**verify判定: VALID**

`.gitignore` の指摘は、deviation記録済みの付随修正に対する品質上の問題であり、デルタスペックとの未記録乖離ではありません。このためレビューは `CHANGES_REQUESTED`、一致検証は `VALID` です。

## 突き合わせ結果

ホスト側 review-004.md (APPROVED) の残指摘 3 件の修正に対する相方の確認 (2026-09-04)。相方 006 は CHANGES_REQUESTED (Major 1)、修正確認の 007 で APPROVED / VALID。

| 指摘 | 出典 | 採否 |
|---|---|---|
| review-004 の残指摘 3 件 (env var 付け忘れ警告 / 6-⑦ scope 列挙 / 証跡例外の範囲拡張) の解消 | 相方が確認 | 解消として確定 |
| `.gitignore` の証跡例外 (末尾の否定パターン) が後勝ちで `secrets.*` / `.env.*` / `hs_err_pid*.log` の保護まで verification / evidence 配下で解除する | 相方のみ Major (実測 5 パスで非無視を再現) | **採用** — ホスト側の見逃し。例外の直後に 3 パターンを再適用し、コメントの lint.exclude との対応 (verification のみ一致、evidence は sanitize 済み抜粋) も訂正 (直接修正、相方 007 で解消確認、check-ignore で追跡中ファイルの巻き添え 0 を再確認) |

確定 0 / 採用 1 / 降格 0 / 未解決 0。両者 APPROVED / VALID で収束。
