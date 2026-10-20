# レビュー結果: switch-docs-refresh-worker-to-scribe (001 回目)

**日付**: 2026-10-20
**判定**: APPROVED

## サマリー
docs-refresh が指定する器名を `ksn-implementer` から `ksn-scribe` に差し替える変更で、3 ファイル 8 行 (8+/8-) の置換だけになっている。summary.md の申告どおり、器名以外の文言には触れていない。手本の `../KsSettingsView` commit `458aa23` とも、同じファイル・同じ 8 箇所・同じ置換で一致した。Critical / Major の指摘は無い。器の性質が変わることに伴う受け取り側の扱いについて、Suggestion を 1 件挙げる。

## 照合した規約
- handbook: `kasane/handbook/cross/index.md` を確かめた。今回の作業 (docs-refresh スキル本体の器名の差し替え) を適用のきっかけに含む文書は無い。`docs-refresh-timing.md` は「docs-refresh を走らせるとき」の規約で、器に関する記述は無かった。`comment-policy.md` (常時) はコメント構文を持つファイルが対象で、Markdown だけの本 diff は対象外
- ksn-core: `references/change-scope.md` (足場凍結・deviation・付随修正)、`references/paths.md` (成果物のパス表記)
- lessons: `kasane/lessons/code-review.md` (L-001 / L-002)。L-002 は対象外 (テスト全件成功の主張が無い)。L-001 も対象外 (証跡手順や受け入れ条件を推奨しない)
- decisions / concepts: docs-refresh の器に関する ADR・概念は無い (`kasane/decisions/index.md` と `kasane/concepts/index.md` には該当する記述が無い)
ロードしたスキル: なし (パッケージ指定どおり。`skills.code-review` は空で、対象は Markdown のスキル文書であり、ドメインのスキルも当たらない)

## 確認した観点
- 仕様充足 (S 級・合意スコープ): session.md の決定事項「docs-refresh のサブエージェントの器は ksn-scribe (手本 458aa23 と同形)」を満たす。`git diff -- .agents/skills/docs-refresh/` を手本 `458aa23` の diff と行ごとに見比べ、置換箇所 (SKILL.md の 6 箇所と prompt-skill.md / prompt-readme.md の各 3 行目) と置換の内容が一致することを確かめた。食い違いは行番号のずれだけ
- 置換漏れ: `.agents/skills/docs-refresh/` に `implement` を含む文字列は残っていない。リポジトリ全体でも `ksn-implementer` が残るのは `kasane/roadmaps/ios-safe-area-and-kmp-int-result/autopilot-log.md` の 2 行だけで、これは過去の点検記録 (append-only のログ) なので差し替えの対象外
- 器定義パスの実在: SKILL.md:67 に挙げた確認手段のうち `~/.claude/agents/ksn-scribe.md` と `~/.codex/agents/ksn-scribe.toml` が実在する (`.claude/agents/` はプロジェクトに無いが、「片方が成立すれば配置済み」の規定なので問題ない)
- 器の前提との整合: SKILL.md:280 が器に求める性質 (読むべきスキルを読む・コンテキストパッケージなしでは作業しない) は、`ksn-scribe` の定義の 1・2 項と一致する。テンプレートの「読むべきスキル: なし」も、scribe の定義と矛盾しない
- 足場の書き換え: 足場 (proposal / spec) は無い。summary.md・session.md の記述は diff と一致する (申告した行番号 53, 67, 72, 269, 280, 561 を grep で確かめた)
- 無断の逸脱・付随修正: deviation.md は無い。diff は器名の置換だけで、スコープ外の変更は含まれない (作業ツリーの `skills/**` の未コミット変更は、パッケージの指示どおりレビュー対象外とした)
- テスト: 対象はスキル文書だけで、ビルド・テストのルートは無い。代わりに `scripts/local-path-lint.py` と `scripts/identity-lint.py` を、変更した 3 ファイルと change の成果物 (summary.md / session.md) にかけ、どちらも exit 0 だった
- 設計品質: 器名をスキル内に直書きで固定する既存方針 (SKILL.md:53「器名が変われば本スキルの記述を直す」) の範囲内での値の差し替えで、オーバーエンジニアリングは無い。summary.md の「ADR 候補: なし」は妥当
- パス表記: summary.md / session.md はリポジトリ相対パスと `~/` 表記だけで書かれており、ローカル絶対パスは無い

## 指摘事項

### [🔵 Suggestion] scribe が報告する「未記入の箇所」を受け取る手順が docs-refresh に無い
**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:282` (Step 5 のバッチ完了処理)、`.agents/skills/docs-refresh/SKILL.md:548` (Step 8 の完了サマリ)、`.agents/skills/docs-refresh/references/prompt-skill.md` の「指示」4、`.agents/skills/docs-refresh/references/prompt-readme.md` の「指示」4
**問題点**: `ksn-scribe` の定義の 3 項は、「テンプレート・既存資料から埋められない箇所は自分で決めて埋めず、未記入の箇所とその理由を報告に列挙する」と定めている (`ksn-implementer` にはこの規定が無い)。一方 docs-refresh は、テンプレートの指示 4 で返させる報告を「変更要約と drift 所見」に限っている。Step 5 は「各バッチ完了後、該当ファイルを完了にマークする」だけで、Step 8 の完了サマリも drift 所見の一覧しか持たない。このため、scribe が一部を未記入のまま返すと、メインはその報告を拾う手順を持たないまま進捗を完了にしてしまう。Step 6 の 8 検査 (網羅・節構成・byte 一致・閉世界性など) は、レシピ本文が書きかけで止まっていることを検出する仕組みではない。その結果、未記入のまま承認・manifest 更新に至る経路が開いている。また SKILL.md:53 は、委譲作業を「concepts 本文と en/ja 一式を読み書きする最も重い作業」と表現している。scribe の担当範囲 (考える量の少ない書き込み) とどう対応するかは、文書から読み取れない。手本の `../KsSettingsView` にも同じ空白がある。
**推奨修正**: 合意スコープ (器名だけの差し替え) の外なので、本 change では直さず、後続の簡易起票を提案する。直す方向は 2 つある。(a) テンプレートの指示 4 に「未記入の箇所とその理由」を報告項目として足し、Step 5 で未記入の報告があった単位は完了にせずに再委譲または Step 8 で報告すると明記する。(b) 姉妹リポジトリの docs-refresh とそろえて直すかを、オーナーが判断する。

## アクションプラン
1. 本 change は APPROVED のまま蒸留へ進めてよい (修正は不要)
2. Suggestion の件は、必要ならオーナーの判断で簡易起票する (KsSettingsView 側との同時対応も含めて検討する)
