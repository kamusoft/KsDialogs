---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-25
last-seen: 2026-09-25
evidence:
  - define-loading-action-thread (完了報告で、handbook cross/test-execution.md の負の検査の表と実測 (android `loadingShowOptions` の診断が 1 件) の食い違いを「起票 (drift の棚卸しに回すなら見送り)」の選択肢で提示したところ、オーナーが「分かってるなら今直す」と指示)
---

## ルール文

実装フェーズで見つかったスコープ外の食い違いのうち、箇所と正しい値が実測で分かっていて、直す作業が文書の表・数行の書き換えと再実測で閉じるもの (handbook の期待値の表・件数表・証跡の記述など) は、「同梱 / 起票 / 見送り」の選択肢としてオーナーに回さず、同じ change の中で直して deviation.md に `[付随修正]` として記録する。同じ前提 (同じ機構・「同上」の参照) に依存する隣の行も同時に実測して揃える。守れたかは、完了報告の判断依頼に「値が分かっている表の食い違い」が残っていないことと、deviation.md に対応する `[付随修正]` の行があることから判定する。

## 経緯

- 2026-09-25 define-loading-action-thread: 完了確認のワーカーが負の検査の診断の食い違いを「確認」として報告し、review-001 も「範囲外・drift 行き」の Suggestion にした。オーケストレーターはそれをそのまま起票の選択肢として完了報告に並べた。ksn-orchestrator の「付随修正」判定では同梱条件を満たす類型で、起票すると探索・提案のコストが付くだけだった。関連: ci-flake-work-batched-not-split (小分けの起票を嫌うオーナーの時間感覚)
