---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-10-20
last-seen: 2026-10-20
evidence:
  - guard-kmp-swift-int-result-type (フェーズ担当が提案のコミット (f98ed41) の後に proposal.md の Impact の 1 行を互換の主張を絞る形に書き換え、コミットに含めず deviation.md にも書かないまま実装を委ねた。review-001 が提案のコミットとの差分を未記録の足場の書き換えと判定して CHANGES_REQUESTED にし、deviation.md への記録で review-002 APPROVED。修正サイクル 1 周)
---

## ルール文

提案をコミットしたあと実装ワーカーへ委ねる前に足場 (proposal・design・specs・tasks の本文) を書き換えたら、委ねる前にその書き換えをコミットし、委ねたあとに書き換えたなら同じ作業で deviation.md に行を足す。守れたかは、実装を委ねる時点で `git status` に足場の差分が無いこと、または `git diff <提案のコミット> -- <足場>` の差分ごとに deviation.md の行があることから判定する。

## 経緯

- 2026-10-20 guard-kmp-swift-int-result-type: 書き換え自体は lessons process L-002 (互換の主張を実証範囲に絞る) に従った正しいものだったが、記録が無かったため凍結の規律に当たった。近縁: [[review-fix-must-not-rewrite-frozen-scaffold]] (レビュー対応の中で足場を書き換える側)
