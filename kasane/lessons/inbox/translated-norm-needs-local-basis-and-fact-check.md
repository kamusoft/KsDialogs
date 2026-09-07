---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-04
last-seen: 2026-09-04
evidence:
  - adopt-docs-refresh (tasks 2.2 が翻案元 KsSettingsView の「platform / Sample ディレクトリに README を置かない」規範を「KsDialogs の phase-2 踏襲決定への参照に直す」と指示したが、その決定は phase-1 agenda / history に存在せず、しかも `samples/README.md` 等 6 本の README が実在し `kasane/config.yaml` の `ui.screenshot` がそれを正として参照している。実装ワーカーが停止報告形式で発見し、規範を落として追従対象の範囲だけを述べる形に倒した)
---

## ルール文

姉妹プロジェクトの文書を翻案する提案・tasks が、翻案元の規範 (「〜を置かない」「〜は禁止」) を翻案先にも書かせるとき、(1) その規範の根拠となる決定が翻案先の decisions / agenda 決定事項に実在するか、(2) 規範が翻案先の現状 (該当ファイルの実在・config からの参照) と矛盾しないかを、`ls` / `grep` の最小確認で確かめてから採録する。根拠が無い・現状と矛盾する規範は、翻案先の記述から落とす (追従対象の範囲だけを述べる) か、論点として agenda に立ててから書く。未成立の決定 (「phase-N で決まる予定」) を根拠に規範を書かない。

## 経緯

- 2026-09-04 adopt-docs-refresh: 翻案元の SKILL.md にあった README 設置の禁止規範は KsSettingsView の cross/ADR-0023 が根拠だったが、KsDialogs には対応する決定が無い。tasks は「phase-2 踏襲決定への参照に直す」と書いたが phase-2 はまだ議論されておらず、samples/ 配下の README は現役文書だった。近縁: [[handoff-item-not-checked-against-evidence]] (写す項目を現在の証跡と突き合わせる) / [[tool-premise-decision-needs-existence-probe]] (前提の存在プローブ)
