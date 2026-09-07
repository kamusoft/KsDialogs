# セカンドオピニオン: adopt-docs-refresh (code-001)
**相方**: codex / **label**: so-code-adopt-docs-refresh / **日付**: 2026-09-04 / **対象**: 未コミット作業ツリー (`.agents/skills/docs-refresh/` 一式・`.claude/skills/docs-refresh`・`AGENTS.md`・`kasane/config.yaml`。ベース HEAD 3a147e2)
---
# 1. 判定

**判定: CHANGES_REQUESTED**

翻案元の主要契約、8スクリプトの byte 一致、symlink、取得元、規約記述は概ね正しく実装されています。一方、閉世界性検査には実際にすり抜け可能な経路があり、主要 Requirement を保証できないため Major 1件としました。

指摘件数: **Critical 0 / Major 1 / Minor 1 / Suggestion 0**

確認結果:

- `scripts/` 8本は翻案元 commit `33ce94e` と byte 一致
- `.agents/skills/docs-refresh` と `.claude/skills/docs-refresh` は同じ実体
- proposal / spec にベースからの変更なし（足場逆流なし）
- `identity-lint.py` / `local-path-lint.py`: exit 0
- `concepts-coverage-check.py`: manifest 不在により exit 1、書き換えなし
- 3d の全取得元から値を取得でき、Gradle wrapper は両方 9.7.0
- 製品ビルドルートは proposal 記録済みの合意例外に従い省略
- 指定範囲外の未追跡 lesson はレビュー対象外

# 2. 指摘一覧

### [🟠 Major] 閉世界性検査を回避でき、仕様上の「レビュー」も実装されていない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:395`、`.agents/skills/docs-refresh/SKILL.md:411`、`.agents/skills/docs-refresh/SKILL.md:423`、`.agents/skills/docs-refresh/SKILL.md:433`

**問題点**: 次の生成物は閉世界性に違反しますが、現在の8検査を通過できます。

```markdown
[内部仕様][source]
[source]: ../../../../kasane/concepts/core/api/loading-semantics.md
```

- `kasane/` grep は直前が `/` の場合を除外するため、`../../kasane/...` を検出しません。
- 相対リンク検査と `link-resolution-check.py` はインライン形式 `[text](path)` だけを解析し、上記の参照形式リンクを解析しません。
- 同じ方法で、別Skillへの参照形式リンクも回避できます。
- 任意の外部解説URLも一律にスキップされます。spec は外部参照禁止を「生成プロンプトとレビュー」で担保するとしていますが、生成後に外部URLを分類するレビュー工程がStep 5〜6にありません。

生成エージェントが指示に違反した場合の防波堤がなく、Requirement「閉世界性と機械面の漏れ検査」を満たしません。

**推奨修正**: Step 6のPython検査を、インラインリンク・参照形式リンク・裸の相対パスを一括解析する実装へ変更してください。外部URLについても一覧化し、配布座標・`metadata.source` 以外を意味レビューする工程をStep 6に明記してください。少なくとも上記形式、別Skillへの参照形式リンク、許可URL、禁止URLをfixtureへ追加します。

### [🟡 Minor] ルートREADMEには identity-lint が実効しない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:451`、`.agents/skills/docs-refresh/SKILL.md:468`、`kasane/config.yaml:18`

**問題点**: ルート `README.md` / `README_ja.md` はdocs-refreshの更新対象ですが、`identity-lint.py` はパスの先頭セグメントを `lint.identity.scope` と照合するため、現在の `[kasane, skills]` では両ファイルを検査しません。実装自身もこの空白を認識していますが、別changeへの先送りだけで、docs-refreshが更新する公開成果物を保護できていません。

**推奨修正**: 明示された `--paths` に対してscopeを回避できる生成物専用モードを用意するか、ルートREADMEを検査対象として扱えるscope設定と検出規則を追加してください。

# 3. verify

**判定: INVALID**

閉世界性Requirementの2 Scenarioに未記録の欠落があります。deviation.mdは存在しないため、合意済み乖離として扱えるものはありません。

| Requirement / Scenario | 実装・検証対応 | 状態 |
|---|---|---|
| 2つの入口が同じ実体を指す | `.claude/skills/docs-refresh`、`tasks.md:32`。同一実体と8本のbyte一致を再確認 | ✅ 一致 |
| 翻案元の契約の保持 | `.agents/skills/docs-refresh/SKILL.md:49`、`:61`、`:260`、`:269`、`:307`、`:502`。commit固定の翻案元との差分を照合 | ✅ 一致 |
| concepts更新後の非発動 | `.agents/skills/docs-refresh/SKILL.md:3`、`:530` | ✅ 一致 |
| 異常なmanifestでも書き換えない | `.agents/skills/docs-refresh/SKILL.md:92`。4異常類型と停止案内を保持 | ✅ 一致 |
| skills/未生成での起動 | `.agents/skills/docs-refresh/SKILL.md:94`、`tasks.md:33`。実行はmanifest不在でexit 1、変更なし | ✅ 一致 |
| 追従対象の特定 | `.agents/skills/docs-refresh/SKILL.md:22`。5 Skill × 2言語、KMP・移行Skill、README 4枚を明記 | ✅ 一致 |
| 移植元の変更は要追従にならない | `.agents/skills/docs-refresh/SKILL.md:139`。移植元をmanifest源泉に含めない | ✅ 一致 |
| 新API側の変更で対応先が追従する | `.agents/skills/docs-refresh/SKILL.md:145`、`:167` | ✅ 一致 |
| architecture配下の新concept | `.agents/skills/docs-refresh/SKILL.md:171`、`:179` | ✅ 一致 |
| 利用者に効くarchitecture conceptはtargetsへ回せる | `.agents/skills/docs-refresh/SKILL.md:173`、`:179` | ✅ 一致 |
| 取得元が実在して値を返す | `.agents/skills/docs-refresh/SKILL.md:195`、`tasks.md:34`。全項目を実読確認 | ✅ 一致 |
| KMPのcatalog共有が外れた | `.agents/skills/docs-refresh/SKILL.md:193` | ✅ 一致 |
| version catalogの変更 | `.agents/skills/docs-refresh/SKILL.md:199`、`:204` | ✅ 一致 |
| Gradle wrapperの食い違い | `.agents/skills/docs-refresh/SKILL.md:200` | ✅ 一致 |
| MAUI本体下限の変更 | `.agents/skills/docs-refresh/SKILL.md:202`、`:204` | ✅ 一致 |
| 内部用語の漏れ | `.agents/skills/docs-refresh/SKILL.md:395`、`:411`。参照形式リンク＋`../../kasane/...` が全検査を回避する | ❌ 乖離 |
| 機械面の漏れ | `.agents/skills/docs-refresh/SKILL.md:397` | ✅ 一致 |
| Skillルート外への相対リンク | `.agents/skills/docs-refresh/SKILL.md:411`。インラインリンクのみで参照形式リンクを検出しない | ❌ 乖離 |
| 誤表記の検出 | `.agents/skills/docs-refresh/SKILL.md:488`。Scenario列挙の5誤例を検出 | ✅ 一致 |
| 正しい識別子は素通り | `.agents/skills/docs-refresh/SKILL.md:474`、`:488`。正典の識別子を照合 | ✅ 一致 |
| 宣言の一致 | `AGENTS.md:11`。CLAUDE.mdはAGENTS.mdへのsymlinkで同一内容 | ✅ 一致 |
| skills/がidentity-lintの範囲に入る | `kasane/config.yaml:18`。YAML parseとlint exit 0を確認 | ✅ 一致 |

❌ 2件はいずれも実装修正が妥当です。仕様上すでに閉世界性を要求しているため、deviationとして緩和するより検査を補強すべきです。

## 突き合わせ結果

ホスト側 review-001.md / verify-001.md と突き合わせ (2026-09-04)。採否規則は ksn-second-opinion Step 3。

| 指摘 | 出典 | 採否 |
|---|---|---|
| 6-⑤ ① `kasane/` grep が `./kasane/` `../kasane/` の相対パス形を取りこぼす (SKILL.md:395) | 双方一致 (ホスト Minor / 相方 Major) | **確定・Major** (相方が高い方を主張) |
| 6-⑤ ③ 相対リンク検査が `[label]: path` の参照形式リンクを解析せず、別 Skill / `kasane/` への参照が素通りする (SKILL.md:411) | 相方のみ・根拠強 (具体例と回避経路を特定) | **採用** — ホスト側の見逃し。上と同じ修正サイクルで扱う |
| 外部 URL を一覧化して配布座標以外を意味レビューする工程を Step 6 に明記する | 相方のみ | **降格** — デルタスペックが外部 URL を機械検査の対象外と明記し、文書としての外部参照の禁止は生成プロンプトの内容規約と承認レビューで担保する設計。報告に出典付きで残す |
| ルート README が identity-lint の scope (`[kasane, skills]`) の外 (config.yaml:18) | 相方のみ Minor | **降格** — Requirement「規約記述と lint 範囲」の要求外で README_ja.md は未存在。phase-2 (skills/ と README の初期生成) への申し送り候補として完了報告に載せる |
| 6-⑧ `KsDialog([^s]\|$)` が公開 Compose API `KsDialogAttributes` を誤検出 (SKILL.md:489) | ホストのみ Major (fixture で再現) | **確定・Major** |
| python 断片の空リンク例外と空チェックの挙動不一致 (SKILL.md:409 / :422) | ホストのみ Suggestion | 同じ断片を直すため同梱修正 |
| tasks 2.2 の文言と実装の食い違い (実装側が正) | ホストのみ Suggestion | deviation.md に記録 (既定 A、オーナー確認待ち) |
| `/tmp/docs-refresh-targets.txt` の残骸 | ホストのみ Suggestion | 同梱 (trash で片付け) |

確定 2 / 採用 1 / 降格 2 / 未解決 0。
