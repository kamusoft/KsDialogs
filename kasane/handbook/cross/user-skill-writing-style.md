---
kind: rule
applies-when:
  always: false
  paths: ["skills/**"]
  tasks: [skills/ の references の新設・改稿, ja → en の同期]
title: 利用者向け Skill の記述スタイル
description: skills/ の references が持つ節の型 (API 選択表 → サンプル分割 → 構成ミスの表)、機能名と用語の表記、ja を正とした en 同期の規則
timestamp: 2026-09-07
---

# 利用者向け Skill の記述スタイル

この文書は、利用者向け Agent Skills (`skills/{en,ja}/`) の本文をどう書くかを定める。読むと、references を新設・改稿するときに揃えるべき節の型と、機能名・用語の表記、ja から en へ同期するときの規則が分かる。何を載せるか (API の網羅と除外) は [user-skill-api-listing.md](user-skill-api-listing.md) が扱い、本規約は載せ方だけを扱う。出典は change proofread-user-skills-ja でオーナーが確定した決定事項。

## references の節の型

機能ごとの references (dialogs / loading / toast) は、読み手が呼び出しを書く順に次の節を持つ。layout / transitions / view-models も、選択表を属性表・preset 表に読み替えて同じ順に並べる。

| 節 | 到達状態 |
|---|---|
| API 選択表 | 公開 show / start / hide 系の全 overload が「シグネチャ / 何をする / いつ選ぶ / 必要な登録」の表に 1 行ずつある。表の下に、既存サンプルがどの行に当たるかを 1〜2 文で示す |
| 最小例 | サンプルに現れない overload のそれぞれに 1〜数行のコード例があり、各例の直前の 1 文で引数の特徴からどの overload か分かる |
| サンプル分割 | ViewModel / content / 起動時の登録 / 呼び出し元 の順に別ブロックで示す。content は platform の実際の書き方 (MAUI は XAML + code-behind、iOS は SwiftUI と UIKit、Android は Compose と View)。登録は起動点 (`MauiProgram` / `App.init` / `Application.onCreate`)、呼び出し元は Page・画面 model・ViewModel が既定入口か注入で受け取る形 |
| 結果型の例 | dialogs.md に、bool 以外の結果型を返す例が 1 つある |
| 構成ミスの失敗 | 例外 (case) ごとに「メッセージ / 原因と対処」の表があり、失敗する構成のコード例と catch して扱うコード例が添えてある。呼び出し元へ返らない失敗は表に入れず本文で注記する |

サンプルは static helper・free function・top-level suspend fun の形にしない。明示型は各言語の型推論 (`var` 等) に寄せる。各コード例の直前に「この例が何を示すか」の 1 文を置く。

地の文は 1 文 1 事項で、列挙・対応関係・優先順位・細則は表か箇条書きにする。「facade」「契約」「消費側」「配線」のような読み手が意味を推測する語は、「使う側のクラス」「constructor で受け取る」「登録する」のような動作の語に置き換える。

## 機能名と用語

- プロダクトの機能名は「Dialog」「Loading」「Toast」と書く (ja で「ダイアログ」と書かない)。一般語として画面の種類を指す場合は例外
- 構成ミスの表のメッセージ列は、実装の英語文言 ([diagnostic-message-language.md](diagnostic-message-language.md)) をそのまま引用し、型名が埋まる箇所は en / ja とも `{TypeName}` のプレースホルダで書く。表の直後に「メッセージは現在の実装値で、安定 API ではない」の 1 文を添える
- Dialog の接頭語「型付き」は付けない。結果や API を修飾する「型付き結果」「型付き `DialogNotifier`」は残す
- 組み込みの文言だけの Toast は「組み込みのメッセージ Toast」(KMP は「組み込みの非対話メッセージ Toast」)。「ピル」「pill」は使わない
- 「覆い」は en で "overlay" (API 名 `overlayColor` / `OverlayDuration` と揃える)。「組み込み」「内蔵」は en で "built-in"
- references へのリンクラベルは ja「[Dialog]」、en "[Dialog]" の単数

## ja を正とした en 同期

ja を書き直したら、同じ change で en を次の状態にする。

- 節の並び・見出しの数と順序・表の列構成が ja と一致している (見出しは英訳)。ja で削除・統合した節は en でも同じ
- コードブロックは ja と byte 一致で、数と順序も一致している (user-skill-api-listing.md「コード例のコメント」)
- 地の文と表のセルは ja の内容の英訳で、ja で事実を補正した箇所は必ず追随している
- 例外メッセージの列は en / ja で byte 一致させる (実装の英語文言をそのまま引用し、訳さない・言い換えない)。添える 1 文 (安定 API ではない旨) は英訳する
- "typed dialog" の接頭語は付けない ("typed result" は残す)

確定前に docs-refresh の scripts (`code-block-parity-check.py` / `heading-parity-check.py` / `link-resolution-check.py` / `frontmatter-check.py`) と `scripts/local-path-lint.py` / `scripts/identity-lint.py` を通し、差分ゼロにする。

複数のワーカーへ platform 別に同期を分けるときは、上の用語表をコンテキストパッケージに含める (含めないと platform ごとに訳語が割れる)。

## してはいけないこと

- 実装で確認できない API 名・既定値・例外・挙動を書かない: 選択表と構成ミスの表は、実装 (公開面のソース) と `kasane/concepts/<platform>/api/` で裏取りしてから確定する
- en だけ、または ja だけを直して終えない: 同じ change で両方を上の状態にする
- 例外メッセージを翻訳・言い換えしない: 実装の英語リテラルを en / ja ともそのまま引用する (ja で和訳を添えない)

## 関連

- [user-skill-api-listing.md](user-skill-api-listing.md) — 何を載せるか (網羅と除外) とコード例のコメント規約
- [diagnostic-message-language.md](diagnostic-message-language.md) — 診断表が引用する実装文言の言語 (英語固定) と、文言が互換契約でないこと
- docs-refresh (`.agents/skills/docs-refresh/SKILL.md`) — 検査 scripts と追従更新の手順
- 出典: `kasane/changes/archive/2026-09-06-proofread-user-skills-ja/summary.md`
