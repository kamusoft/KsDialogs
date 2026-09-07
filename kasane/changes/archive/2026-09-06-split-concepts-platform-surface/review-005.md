# レビュー結果: split-concepts-platform-surface (005 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

review-004 (APPROVED) の後に入った小修正 — オーナー判断による除外 4 名 (`Center` / `Fill` / `Top` / `ToastStyle.BuiltinBackgroundColor`) の掲載への切り替え — だけを対象にした確認レビュー。追記された散文 3 箇所は en / ja とも実装 (`maui/KsDialogs.Maui/Contract/DialogAlignment.cs` / `DialogTransitionEdge.cs` / `ToastStyle.cs`) と源泉 concept に一致し、handbook の除外リスト 20 行と `api-coverage-check.py` の候補 21 行 (34 名前) は自分で再実行して 1:1 (未分類 0・余剰 0) を確認した。機械検査 4 種と lint はすべて exit 0。

Critical / Major なし。Suggestion 1 件は源泉 concept 側の表現に由来するもので、利用者向け成果物としての正しさには影響しない。

## 照合した規約

- `.agents/skills/docs-refresh/references/prompt-skill.md` の内容規約 ⑤ 閉世界性 / ⑦ 個数の地の文 / ⑧ 全称表現 — 追記 3 箇所を節ごとに照合 (結果は下表)
- `kasane/handbook/cross/user-skill-api-listing.md` (適用のきっかけ: `skills/**` を更新するとき / API 名網羅検査の仕分け) — 方針節「簡潔でも網羅」・除外基準表・「してはいけないこと」は今回不変。除外リストの 2 行削除は「オーナー判断で掲載に倒した」経路 (規約が求める判断者) で行われている
- `kasane/lessons/code-review.md` — 不在。重点観点・指摘しないことの持ち込みなし

## 実行した検証 (すべて自分で再実行)

| 検査 | 結果 |
|---|---|
| 内容規約 ⑤ 閉世界性 | 追記 3 箇所に `kasane/` 参照・ADR 番号・Skill ルート外リンクなし |
| 内容規約 ⑦ 個数の地の文 | 追記が新たに埋め込んだ個数なし (既存の「animation の2行」は review-004 の範囲) |
| 内容規約 ⑧ 全称表現 | `すべて` / `always` / `every` による挙動の断定なし |
| `api-coverage-check.py` | 21 行。`verification/api-coverage-after.txt` 追記の記録と一致 (aiforms の `layout-surface.md` 行・`toast-surface.md` 行が消え、`transition-surface.md` 行から `Top` が落ちた) |
| 除外リスト 20 行 ↔ 候補 21 行の突き合わせ | 1:1。未分類 0・余剰 0。内訳 iOS 5 / Android 5 / MAUI 6 / KMP 8 / AiForms migration 10 = 34 名前 (掲載前 38 名前 − 4) で、追記の記載と一致 |
| `skill-forbidden-tokens.py` | exit 0。検査 [1] [2] とも 5 Skill 範囲 0 件、負の検査 一致 0 件 |
| `heading-parity-check.py` / `code-block-parity-check.py` / `frontmatter-check.py` | 3 種すべて OK (exit 0) |
| `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | すべて exit 0。`doc-structure` の残存注意は本 change が触らない既存ファイルのみで、`handbook/cross/user-skill-api-listing.md` と `concepts/log.md` は含まれない |
| `DialogAlignment` の照合 | 列挙子 4 種と順序 (`Start` / `Center` / `End` / `Fill`)、`Start` / `End` が物理方向で RTL に追随しないこと、`Fill` が位置とサイズを有効領域へ広げること、比率サイズがある軸で `Fill` が `Center` 扱いになること — 実装の XML doc および `kasane/concepts/core/api/layout-semantics.md` のサイズ優先順 (比率指定 > fill 配置 > 内容サイズ) と手順 5 に一致 |
| `DialogTransitionEdge` の照合 | 列挙子 4 種と順序 (`Top` / `Bottom` / `Start` / `End`)、`Start` / `End` がレイアウト方向に追随して RTL で入れ替わり `Top` / `Bottom` が物理方向のままであること — `Contract/DialogTransition.cs:265-274` が `EffectiveFlowDirection.RightToLeft` で `Start` ⇄ `End` を実際に入れ替え `Top` / `Bottom` を素通しすることを確認。`kasane/concepts/maui/api/transition-surface.md:75` とも一致 |
| `ToastStyle.BuiltinBackgroundColor` の照合 | `Contract/ToastStyle.cs:34` の static プロパティで、`BackgroundColor` の既定値 (`:37`) かつ null 時の拠り所 (`:60`)。`BuiltinDefaultDuration` (`:31`) が `DefaultDuration` (`:51`) に対して持つ関係と同型で、散文の「同じ位置づけ」は正しい |
| 追記の en / ja 同等性 | 3 行のバッククォート識別子を多重集合で比較。差は en 290 行の `BackgroundColor` 1 個のみで、ja が主語の反復を省いた散文差 (内容は同等)。構造は heading / code-block parity で機械確認済み |
| 他 Skill との無矛盾 | `skills/ja/ksdialogs-maui/references/layout.md:17` (比率が `Fill` に勝ち `Fill` は中央配置)、`references/transitions.md:12` (`Start` / `End` は layout 方向追随、`Top` / `Bottom` は物理) と移行 Skill の新規散文は同じことを言っている |
| 追随記録 | `kasane/concepts/log.md:42` の 1 行 (22 行 → 20 行・追記した 3 概念・timestamp 据え置き) は現物と一致。deviation.md 末尾の task 6.2 の行にも掲載への切り替えと理由が記録されている |

## 指摘事項

### [🔵 Suggestion] `BuiltinBackgroundColor` を「半透明」と呼ぶ表現が実値に対してやや強い

**該当箇所**: `skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:290` (`半透明のダークグレー`) / `skills/en/.../api-mapping.md:290` (`translucent dark grey`)

**問題点**: 実装値は `Color.FromRgba(0x32, 0x32, 0x32, 0xEB)` (`maui/KsDialogs.Maui/Contract/ToastStyle.cs:34`) で、alpha 0xEB は約 92% 不透明。「半透明」は誤りではない (完全不透明ではない) が、読者は半分程度の透過を想像しうる。ただしこの語は源泉 concept `kasane/concepts/maui/api/toast-surface.md:61` の記述をそのまま引いたものであり、Skill 側は正本に忠実。docs-refresh の内容規約 ⑥ (正本を独断で書き換えない) からも Skill 単独で直すべきではない。

**推奨修正**: 対応不要。次に concept を触るとき (蒸留時など) に、`maui/api/toast-surface.md` の既定値欄を「わずかに透過するダークグレー」等へ寄せると、派生物の Skill も自然に揃う。

## アクションプラン

1. (任意・最低優先) 上記 Suggestion の concept 側表現の見直し — 本 change での対応は不要
2. 本 change の実装作業は完了とみなしてよい。未追跡の `verification/skill-forbidden-tokens.py` / `verification/api-coverage-after.txt` を commit で取り込むこと (review-004 の記録事項の再掲)
