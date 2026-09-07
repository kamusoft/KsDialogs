---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - rollout-user-docs (repository-docs spec の Scenario「廃止した README への参照の解消」が `kasane/changes/archive/` とロードマップ過去記録だけを除外して「全文字列 0 件」を要求したが、`kasane/concepts/log.md`・`kasane/lessons/inbox/`・進行中 change の足場・accepted ADR の本文と過去の現行照合が移送元パスを履歴として保持するため達成不能だった。合格条件を「active な decisions・handbook・concepts・実装・公開文書で 0 件」に倒して deviation 記録)
---

## ルール文

デルタスペックの Scenario が「〜への参照・文字列がリポジトリ内に残らない (0 件)」という残存検査を書くとき、grep の対象から append-only の履歴 (`kasane/concepts/log.md`、`kasane/lessons/inbox/` と `details/`、`kasane/changes/` の進行中 change の足場、accepted ADR の本文と過去の現行照合行、ロードマップの history) を明示的に除外し、検査対象を「active な decisions・handbook・concepts・実装・公開文書」と書く。除外を書かない 0 件条件は、spec-review で達成可能性を疑って差し戻す。

## 経緯

- 2026-09-05 rollout-user-docs: 実装の最終段で発覚し、レビュー 2 周 (022 → 023) と deviation 記録のコストが掛かった。近縁: [[review-fix-must-not-rewrite-frozen-scaffold]] (達成不能と分かったときに spec を書き換えない側)
