---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-04
last-seen: 2026-09-04
evidence:
  - fix-ios-toast-presentation-wait-flake (対応済み実行条件で iOS 全件を計 14 回反復して再現せず、製品コード・テストコードを変えないと決定した後、change 自体の破棄を提案した。オーナー指摘により、再現条件・不採用案・見送り根拠を含む探索記録には再発時の比較材料として価値があるため、蒸留して archive する方針へ訂正した)
---

## ルール文

探索の結論が「実装修正なし」でも、再現条件、不採用案、判断根拠、再発時の入口が記録されている change は破棄しない。完了した調査として蒸留し、将来の再探索で比較できる状態のまま archive する。中身のない scaffold を取り下げる場合と、調査証拠を持つ no-code change を区別する。

## 経緯

- 2026-09-04 fix-ios-toast-presentation-wait-flake: 対応済み実行条件で再現しないため修正を見送ったことを、change も不要という意味に誤って広げた。オーナーの「修正は不要でも trash は不適切」という指摘で、実装要否と調査記録の保存要否は別判断だと確定した
