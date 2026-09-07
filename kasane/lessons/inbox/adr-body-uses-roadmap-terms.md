---
scope: process
kind: pain
severity: normal
count: 2
first-seen: 2026-08-19
last-seen: 2026-08-19
evidence:
  - add-layout-spec (蒸留の accepted 昇格時に ADR 本文からロードマップ用語の除去指示)
  - expand-api-surface (蒸留の昇格レビューでオーナーが「phase-x-x など初見で理解できない用語は ADR で使用禁止。ロードマップは短命なので参照の意味がない」と再指摘 — Decision 節は除去済みだったが Context 節に残っていた)
---

## ルール文

ADR の本文 (Context / Decision / Alternatives / Consequences) にロードマップ用語 (phase-x-x・フェーズ名・論点記号) を書かない。時期や経緯は change-id・ADR 参照・「決定当時」などの自足する表現で書き、ロードマップ由来の出どころは footer の出典行 (ファイルパス) だけに置く。ドラフト時点から適用し、昇格時の除去作業にしない。

## 経緯

- 2026-08-19 add-layout-spec: 蒸留の accepted 昇格で本文からロードマップ用語を除去した (オーナー指示)
- 2026-08-19 expand-api-surface: 昇格対象 8本のドラフトで Decision 節の phase 用語は置換したが Context 節に残し、オーナーが使用禁止として差し戻し。ロードマップは archive されるとフェーズ名が初見の読者に解決不能になる — ADR はプロジェクト存続期間の文書であり、短命層への参照は腐る
