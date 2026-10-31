# レビュー結果: add-root-readme-language-links (001 回目)

**日付**: 2026-10-31
**判定**: APPROVED

## サマリー
ルート `README.md` / `README_ja.md` の H1 直下に、空行を挟んで相手言語版への GitHub 絶対 URL のリンクを 1 行ずつ足す差分で、exploration.md の決定事項 4 点をそのまま満たしている。姉妹リポジトリ KsSettingsView の形 (位置・リンク文言・`blob/main` の絶対 URL) と一致し、見出し・コードブロックには触れていないため docs-refresh の英日構成検査と README 系の lint はすべて通る。指摘事項はない。

## 照合した規約
- `kasane/handbook/cross/install-examples.md` — 適用のきっかけ: diff が `README.md` / `README_ja.md` を触る (applies-when.paths)。インストール例には触れていないことを確認し、`scripts/install-example-lint.py` も通過
- `kasane/handbook/cross/comment-policy.md` (always) — Markdown はコメント構文を持つ対象ファイルではないため適用なし
- `kasane/decisions/cross/0012-readme-root-only-and-developer-knowledge-in-handbook-concepts.md` (accepted) — README はルート 2 枚・翻訳ロックステップ・利用者の入口に純化
- `.agents/skills/docs-refresh/references/prompt-readme.md` — ペアの見出し階層・コードブロックの一致の規約

ロードしたスキル: なし (`kasane/config.yaml` の `skills.code-review` / `skills.domain` / `skills.impl` は空、diff は README 2 枚のみで domain-skills の該当なし。パッケージの解決済みリストに従った)
相手の拠り所: 節なし (比較用に `../KsSettingsView/README.md`・`../KsSettingsView/README_ja.md` の冒頭を読んだ)

## 確認した観点

**仕様充足 (合意スコープ = exploration.md「決定事項」)**
- `README.md:3` に `[日本語](https://github.com/kamusoft/KsDialogs/blob/main/README_ja.md)` が H1 (`README.md:1`) の直下、空行 (`README.md:2`) を挟んで置かれている — 決定事項 1 と一致
- `README_ja.md:3` に `[English](https://github.com/kamusoft/KsDialogs/blob/main/README.md)` が同じ位置に置かれている — 決定事項 2 と一致
- 対象はルート README の 2 枚だけで、見出しは増えていない (`git diff` は 2 ファイル各 +2 行のみ) — 決定事項 3 と一致
- `.agents/skills/docs-refresh/references/prompt-readme.md` は変更されていない — 決定事項 4 と一致
- tasks.md・deviation.md は S 級のため無い。足場 (exploration.md) の書き換えは無い

**テスト / 機械検査** (ドキュメントのみの差分のため、ビルドとテストの代わりに README に効く検査を実行)
- `git diff --check`: 空白の異常なし。CR 混入なし
- `python3 .agents/skills/docs-refresh/scripts/heading-parity-check.py`: `en/ja heading structure OK`
- `python3 .agents/skills/docs-refresh/scripts/code-block-parity-check.py`: `code blocks byte-identical`
- `python3 scripts/install-example-lint.py`: 16 ファイル 24 行が契約を満たす (exit 0)
- `python3 scripts/readme-example-lint.py`: README の最小例 4 件が消費者検証のソースと一致 (exit 0)
- `python3 scripts/identity-lint.py README.md README_ja.md` / `python3 scripts/local-path-lint.py README.md README_ja.md`: exit 0
- `link-resolution-check.py` は相対リンクだけを見て `https://` を飛ばす設計のため、今回の 2 リンクはその検査の外。代わりにリンク先を手で確かめた: `origin` は `https://github.com/kamusoft/KsDialogs.git`、`origin/main` に `README.md` と `README_ja.md` が両方ある

**設計品質**
- 絶対 URL の選択: `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:22` の `PackageReadmeFile` と `:34` の同梱でルート `README.md` が NuGet パッケージに入るため、相対リンクでは nuget.org で切れる。絶対 URL (`blob/main`) は exploration.md の却下理由どおりで、同じ README 内の既存リンク (`README.md:170`・`:185`・`:189`・`:193` など) とも形がそろう
- 姉妹リポジトリとの一貫性: `../KsSettingsView/README.md:3`・`../KsSettingsView/README_ja.md:3` と位置・リンク文言・URL の形が一致
- ADR-0012 との照合 (節ごと): ルート 2 枚のみ (README の新設なし)、翻訳ロックステップ (2 枚を同時に更新)、利用者の入口への純化 (開発者向け手順を足していない)、platform / Sample 別 README の新設なし — いずれも反しない。ADR-0012 の Decision が並べる載せる項目に言語切替リンクは無いが、これは節ではなく導線の 1 行で、見出し構成を変えないため列挙への違反とは判断しない
- オーバーエンジニアリングなし (各ファイル 1 行 + 空行 1 行)
- 機密情報・ローカル絶対パスの混入なし

**lessons/code-review.md**
- L-001 (証跡手順の判別力): 本レビューは証跡手順や受け入れ条件を推奨しないため該当なし
- L-002 (全件成功の主張と実行表の突き合わせ): 実装報告に「テスト全件成功」の主張は無く、差分にコードが無いため該当なし

## 指摘事項

なし

## アクションプラン

なし (このまま蒸留・コミットへ進めてよい)
