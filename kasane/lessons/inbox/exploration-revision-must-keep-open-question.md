---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - fix-android-instrumented-toast-back-loading-coalescing (簡易起票時の exploration.md にあった未決の論点「同種の待ち方をしている他の instrumented テストにも同じ脆さがないか (L-001 の姉妹面照合)」が、探索の確定時に「未決の論点」節を書き換えた際に答えのないまま消えた。review-001 が気づいて iOS の同名テストを照合 (穴なし) し、結果を review に残した)
---

## ルール文

探索の確定で exploration.md の「未決の論点」を書き換えるときは、簡易起票時・前回の論点を 1 つずつ「答えた (決定事項へ)」「残す (未決のまま)」「不要 (理由)」のいずれかに振り分けてから書き換える。答えも理由も書かずに論点を落とさない。守れたかは、改訂前の論点がすべて改訂後の決定事項・未決の論点・却下理由のどこかに現れることから判定する。

## 経緯

- 2026-09-06 fix-android-instrumented-toast-back-loading-coalescing: 探索は原因特定に集中し、起票時の論点を新しい「原因」「決定事項」節に置き換える形で改訂したため、姉妹面照合の論点が答えなしで消えた。実害はレビューが拾って防いだが、レビューが拾わなければ蒸留に届かなかった
