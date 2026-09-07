---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-15
last-seen: 2026-08-15
evidence:
  - add-vertical-slice (蒸留の ADR 更新で「実装結果 (change-id) — 日付」の履歴節を Consequences に追加し、オーナーが「ADR は履歴を書くものではない。ノイズになる」として全13件の是正を指示)
---

## ルール文

ADR に実装履歴・検証経緯・日付付き追記を書かない。実装で判明したことのうち「この決定を採る限り常に成り立つ帰結」だけを Consequences に無時制の帰結文として統合し、決定が精緻化された場合は Decision 本文を現在の決定に書き直す。検証の経緯・テスト結果・証跡参照は change の review / verify ファイルと history に残す。

## 経緯

- 2026-08-15 add-vertical-slice: 蒸留 Step 3c で proposed ADR 13件に「実装結果 (add-vertical-slice) — 2026-08-15」節 (検証経緯・実測値・レビュー顛末) を追加したところ、オーナーが ADR フォーマット逸脱として却下。ADR は決定の無時制の記録であり、履歴は足場 (review/verify/history) の責務という境界の再確認になった
