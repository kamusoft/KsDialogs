---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - rollout-user-docs (review-021 までの指摘対応の過程で、承認済み proposal.md / specs/repository-docs/spec.md の文言と accepted な cross/ADR-0006・0007 の本文・過去の現行照合が直接書き換えられ、review-022 が NEEDS_DISCUSSION で差し戻した。足場は Git から凍結版へ復元し、乖離は deviation.md に、ADR は現行照合 footer の追記だけに倒して review-023 で解消)
---

## ルール文

レビュー指摘への修正で「spec / proposal と成果物が食い違う」「accepted ADR の記述が現状と違う」と分かったとき、proposal / design / specs (実装中は凍結) と accepted ADR の本文・過去の現行照合行は書き換えない。食い違いは `deviation.md` への追記で、ADR の現在地は現行照合 footer の追記だけで表す。修正後は `git diff <提案作成 commit> -- proposal.md design.md specs/` と accepted ADR の diff が footer 追記以外で空であることを確認してから再レビューに出す。

## 経緯

- 2026-09-05 rollout-user-docs: 廃止した README への参照が append-only の履歴に残るため spec の「全文字列 0 件」が達成できず、レビュー対応で spec 側の文言を合格条件に合わせて書き換えた。レビュー 1 周分 (022 → 023) の手戻りになった。近縁: [[spec-residual-check-must-exclude-append-only-history]] (そもそも達成不能な Scenario を書かない側)
