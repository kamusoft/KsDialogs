# セカンドオピニオン: adopt-docs-refresh (code-005)
**相方**: codex / **label**: so-code-adopt-docs-refresh-b (turn 2、code-004 Minor の修正確認) / **日付**: 2026-09-04 / **対象**: 未コミット作業ツリー (修正は `.agents/skills/docs-refresh/SKILL.md:309` / `:480` の注記の言い換えのみ。ベース HEAD 3a147e2)
---
## 1. 判定と総評

**APPROVED**

前回の Minor 指摘は解消されています。

- tasks 2.9 の指定パターンで再検索した結果、ヒットは許容箇所である `.agents/skills/docs-refresh/SKILL.md:12` の翻案元出典のみでした。許容箇所以外は 0 件です。
- `.agents/skills/docs-refresh/SKILL.md:309` の「冒頭の翻案元」への言い換え後も、姉妹リポジトリ間で `/tmp` の予定 manifest・対象一覧が衝突し、誤った適合判定を招くリスクが明確に説明されています。
- `.agents/skills/docs-refresh/SKILL.md:480` は、`lint.identity.scope` がパス第1セグメントを基準に対象を絞ること、scope 外パスは素通りすること、scope 拡張は別 change とすることを維持しています。「scope に未登録の先頭セグメント配下」は従来例より一般化され、意味も正確です。
- 記録済みの3件の乖離は `deviation.md` と一致し、新たな未記録乖離は確認されませんでした。

## 2. 指摘一覧

指摘なし。

| 重要度 | 件数 |
|---|---:|
| Critical | 0 |
| Major | 0 |
| Minor | 0 |
| Suggestion | 0 |

## 3. verify

### Scenario 対応表

| # | Scenario | 実装・証跡 | 結果 |
|---:|---|---|---|
| 1 | docs-refresh の配置と2つの入口 | Skill本体、参照資料、8スクリプト、Claude symlinkを確認。link-resolution-check のみ記録済み乖離 | 適合 |
| 2 | 翻案元の契約維持 | 翻案元との比較および出典注記を確認 | 適合 |
| 3 | 自動発動しない | AGENTS.md の明示実行条件を確認 | 適合 |
| 4 | 不正な予定manifestでは書き込まない | Skill手順の停止条件を確認 | 適合 |
| 5 | `skills/` 不在時に停止する | 事前条件を確認 | 適合 |
| 6 | 追従対象を特定する | 一時対象一覧と判断フローを確認 | 適合 |
| 7 | 翻案元だけの変更では追従しない | 対象判定条件を確認 | 適合 |
| 8 | 新規API変更を追従対象へ追加する | API判定・対象追加手順を確認 | 適合 |
| 9 | 新概念を検出して警告する | architecture drift の警告手順を確認 | 適合 |
| 10 | アーキテクチャ変更を対象へ割り当てる | 利用者判断を反映する導線を確認 | 適合 |
| 11 | source values が存在する | 設定値と参照先を確認 | 適合 |
| 12 | KMP catalog が切り離されていれば停止する | 整合性チェックを確認 | 適合 |
| 13 | version catalog 変更を対象へ追加する | 変更判定手順を確認 | 適合 |
| 14 | wrapper 不整合を別問題として報告する | 独立した報告手順を確認 | 適合 |
| 15 | MAUI下限変更を対象へ追加する | バージョン検出手順を確認 | 適合 |
| 16 | 内部用語の漏出を検出する | identity lint の適用を確認 | 適合 |
| 17 | 機械的なsurface漏出を検出する | API listing検査を確認 | 適合 |
| 18 | Skill外への相対リンクを検出する | link-resolution-check の処理を確認 | 適合 |
| 19 | 不正identifierを検出する | identifier検査を確認 | 適合 |
| 20 | 正当なidentifierを受理する | 許容規則を確認 | 適合 |
| 21 | AGENTS.md / CLAUDE.md の宣言を一致させる | symlinkと宣言内容を確認 | 適合 |
| 22 | identity lint のscopeを有効化する | 3値化および6-⑦の注記を確認。README追加は記録済み乖離 | 適合 |

### 横断確認

| 確認項目 | 結果 |
|---|---|
| tasks 2.9 残留grep | `.agents/skills/docs-refresh/SKILL.md:12` の許容出典のみ |
| 未完了タスク | なし |
| proposal/specへの逆流 | なし |
| 未記録の仕様乖離 | なし |
| scripts 8本の翻案元差分 | `link-resolution-check.py` の環境変数と既定値に関する2箇所のみ |
| 一時ファイルパスの整合 | `/tmp/docs-refresh-ksdialogs-*` の生成側・読み側が整合 |
| identity scope注記 | 3値化した実効scopeと整合 |
| `.gitignore` | 既存項目を維持し、Kasane証跡を救済。`.agents/` は無視されない |
| lint | identity / local-path / comment-policy が成功 |
| 製品テスト | proposal Impactに記録された合意済み省略 |

**verify 判定: VALID**
