---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-19
last-seen: 2026-08-19
evidence:
  - expand-api-surface (改訂 propose が phase-5-2 agenda の申し送り「MAUI スナップショット配線の実機確認」をタスク 7.3 として追加したが、相方スペックレビュー spec-002 Minor で add-layout-spec 内に実施済み証跡 (verification/maui-snapshot-wiring/ + review-004 #4 の解消判定) があると判明し撤回。agenda 側の申し送りが古いままだった)
---

## ルール文

agenda・申し送りから提案へタスクを写すとき、その項目の出典と近傍の証跡 (archive された change の verification / review の解消判定) を突き合わせ、既に解消済みでないかを確かめてから採録する。解消済みと判明したら、タスク化を見送るだけでなく申し送り元 (agenda) も訂正する。

## 経緯

- 2026-08-19 expand-api-surface: add-layout-spec の実装後半で実機確認が実施され review-004 で解消判定されていたのに、agenda の申し送り (実装前に書かれたもの) は「未実施」のまま残っていた。改訂 propose はこの申し送りを鵜呑みにしてタスクを追加し、相方スペックレビューが archive の証跡と突き合わせて重複を検出。申し送りは書かれた時点の事実であり、消化する側が現在の証跡で鮮度を確かめる必要がある
