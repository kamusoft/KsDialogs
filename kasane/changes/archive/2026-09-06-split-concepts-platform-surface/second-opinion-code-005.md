# セカンドオピニオン: split-concepts-platform-surface (code-005)
**相方**: codex / **label**: so-code-final-split-concepts-platform-surface / **日付**: 2026-09-06 / **対象**: 最終レビュー — 作業ツリーの未コミット分 (tasks 5.2〜7.3: skills/ 再生成・manifest・verification 6.1・handbook 除外リスト・docs-refresh 3e 注記・ADR-0014・deviation) + コミット 4e81fd8 (文脈)
---
# レビュー結果: split-concepts-platform-surface

**判定**: CHANGES_REQUESTED
**指摘件数**: Critical 0 / Major 1 / Minor 1 / Suggestion 0

## サマリー

manifest、生成済み Skills、公開 API、禁止トークン fixture、除外リストは、deviation の合意済み差分を除いて概ね整合しています。ただし docs-refresh の新しい運用指示が、仕様で認められた KMP・移行 Skill の cross-platform source 構成を配置違反と誤判定させるため、修正が必要です。

提示された整合性チェック結果を証拠として扱い、検査は再実行していません。

## 照合した規約

- `comment-policy.md`（always）
- `test-execution.md`（変更の完了判定）
- `user-skill-api-listing.md`（`skills/**` の生成・API 候補の仕分け）
- cross/ADR-0011、cross/ADR-0014
- Swift / Kotlin / Compose / C# / MAUI / Native Binding の解決済みレビュー観点

## 指摘事項

### [🟠 Major] 正当な他 platform source を concepts 配置違反として扱ってしまう

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:214`
**関連箇所**: `skills/.manifest.json:193`、`skills/.manifest.json:213`、`specs/user-skills-manifest/spec.md:21`

**問題点**:
追加された指示は、API 名網羅検査で他 platform の `<platform>/api/` が源泉になっていた場合、concepts 側の配置違反を疑って concepts を直すよう案内しています。

しかし承認済み構成では、次が意図的な例外です。

- KMP の `layout.md` / `transitions.md` は iOS・Android surface を参照する
- KMP の `android-host.md` は Android surface 5本を参照する
- AiForms migration は MAUI surface 6本を参照する

実際、`api-coverage-after.txt:34-38` にも KMP に対する Android・iOS source の候補が正常系として現れています。現在の記述では、将来の docs-refresh が正しい manifest を配置違反と判断し、正本である concepts の不適切な修正を提案する可能性があります。

**推奨修正**:
他 platform source を違反とする前に manifest の承認済み target 構成を確認するよう改め、少なくとも KMP の3経路と AiForms migration の MAUI source を明示的な例外として記載してください。これらの候補は、配置違反ではなく「host API の掲載漏れ」または掲載除外規約で仕分けるべきです。

### [🟡 Minor] 負の検査証跡の対象ファイル数が実態と一致しない

**該当箇所**: `verification/api-coverage-after.txt:3`

**問題点**:
証跡は対象を「62ファイル」としていますが、仕様と manifest は33ファイル×2言語＝66ファイルで、検査スクリプトも各 Skill ディレクトリ全体を走査します。62は未コミット差分で変更されたファイル数であり、検査対象数ではありません。

結果自体は66ファイルを走査しているため無効ではありませんが、証跡だけを読むと4ファイルが検査対象外だったように見えます。

**推奨修正**:
「検査対象66ファイル（うち内容変更62ファイル）」と訂正し、全ファイルを検査したことを明確にしてください。

## アクションプラン

1. docs-refresh 3e に承認済み cross-platform source の例外を明記する。
2. `api-coverage-after.txt` の対象件数を66へ訂正する。
3. 修正後、docs-refresh の関連チェックと `git diff --check` を再確認する。

**最終判定: CHANGES_REQUESTED**


## 突き合わせ結果 (ホスト側 review-004 = APPROVED、Minor 2 / Suggestion 2 との照合。2026-09-06)

| 相方の指摘 | 採否 | 処置 |
|---|---|---|
| Major: docs-refresh 3e 注記が KMP の 3 側・移行 Skill の MAUI 源泉 (manifest 承認済み構成) を配置違反と読ませる | **採用** (相方のみ・根拠強: 該当箇所と実害シナリオが具体的、`api-coverage-after.txt` の KMP 行が正常系として現れている事実と整合) | `.agents/skills/docs-refresh/SKILL.md` 3e 注記を「まず manifest の `targets` を見る。承認済み源泉なら掲載漏れ / 除外規約で仕分け。KMP の 3 側と移行 Skill の MAUI 源泉を明示的な例外として列挙。配置違反を疑うのは core に platform 識別子が残る場合と targets に無い他 platform 源泉が現れた場合に限る」へ改訂 |
| Minor: `verification/api-coverage-after.txt` の対象件数 62 が検査対象数 (66) と一致しない | **採用** | 「検査対象 66 ファイル (内容変更 62、残り 4 も走査対象)」へ訂正 |

ホスト側のみの指摘: Minor (kmp 集合の `A.min` 非対称 → baseline.md に理由 1 行追記で対処) / Minor (ADR-0014 候補列挙の `DialogException` / `LayoutArea` 残存 → 確定一覧の名前へ差し替え、条件を満たさなかった理由を併記) / Suggestion 2 件 (「非 API token」基準の定義と収容内容のずれ、migration Skill の `Center` / `Fill` / `Top` 除外基準) は記録のみ (蒸留・オーナー判断の材料)。

未解決: なし。判定の矛盾: なし (相方の CHANGES_REQUESTED は修正で解消、修正後の確認を同 label の send で依頼)。

## 修正確認 (2 ターン目、同 label の send。2026-09-06)

前回の 2 指摘はいずれも解消されています。新たなブロッキング問題はありません。

- **Minor** — `kasane/changes/split-concepts-platform-surface/verification/baseline.md:362`
  問題点: `A.min` も architecture への移動漏れ検出と説明していますが、実際には `core/api/layout-semantics.md` の fenced code block に残る数式です。また、説明段落が件数表の途中に入り、`:363` の `aiforms-migration` 行が表から分離しています。
  推奨修正: `aiforms-migration` 行を `kmp` 行の直後に移し、説明を表の後へ配置してください。`A.min` は「core 数式の Skill への転記漏れ検出」、残る 4 件は「architecture 記述の移動漏れ検出」と区別すると正確です。

確認結果:

- `.agents/skills/docs-refresh/SKILL.md:214` は manifest の `targets` を先に確認する手順、KMP と移行 Skill の例外、配置違反を疑う二条件を明記しており、実際の manifest 構成とも一致しています。前回 Major は解消済みです。
- `verification/api-coverage-after.txt:3` は 66 ファイル、内容変更 62 ファイル、残り 4 ファイルも走査対象と正しく区別しています。実ファイル数 66、差分対象 62 とも一致し、前回 Minor は解消済みです。
- ADR-0014 の候補列挙は `core-contract-check.md` と整合し、不成立の `DialogException`／`LayoutArea` の理由も明確です。
- KMP 禁止集合を維持する判断自体は、合意済みの fixture 解釈と現在の成果物に整合しています。

**最終判定: APPROVED**

`ksn-review` の判定基準上、残件は説明・Markdown構造に限られる非ブロッキングの Minor です。


突き合わせ: 2 ターン目の Minor (baseline.md の説明段落が件数表を分断、`A.min` の説明) は **採用** し、段落を表の後ろへ移して `A.min` (core 数式の転記漏れ検出) と残り 4 件 (architecture 移動漏れ検出) を区別して書き直した。最終判定 APPROVED で相方側も収束。
