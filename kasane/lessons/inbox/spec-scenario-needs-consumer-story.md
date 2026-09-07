---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-15
last-seen: 2026-08-15
evidence:
  - add-vertical-slice (verify-001 ❌2 の Scenario「Swift から await した結果の型と値が正しい」をオーナーが「主従が逆では」と指摘。ダイアログを出す主役は共有コードであり、Swift 直接 await の消費者ストーリーが提案時に裏付けられていなかった。deviation で phase-5 送りに)
---

## ルール文

デルタスペックの Scenario を起こすとき、境界面 (interop・facade・binding) の Scenario には「誰がその経路を使うのか」の消費者ストーリーが proposal / design 側にあることを確認する。消費者ストーリーの裏付けがない Scenario は spec に入れず、後続 phase の設計論点として Non-Goals または申し送りに書く。

## 経緯

- 2026-08-15 add-vertical-slice: kmp-facade の spec に「Swift から KMP の show を直接 await」の Scenario が入っていたが、KMP 利用アプリでは show の主役は共有コードで、Swift 側の自然な役割は View 登録。実装後の verify で「実証コードがリポジトリに1つもない」ことが発覚し、オーナー指摘で Scenario 自体が先走りと判明。Swift 向け KMP 面 (登録経路 + show + 型付き結果) は一体の設計テーマとして phase-5 へ送られた。提案時に消費者ストーリーを確認していれば、この Scenario は最初から Non-Goals に置けた
