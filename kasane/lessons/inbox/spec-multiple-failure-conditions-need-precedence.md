---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-10-20
last-seen: 2026-10-20
evidence:
  - guard-kmp-swift-int-result-type (Requirement が show の失敗の条件を「指定した結果型が Int / UInt、または登録された結果型が Int / UInt」と 2 つ並べ、失敗の値 (`expected` / `actual`) を条件ごとに決めていたが、両方が別の型で当たる組 (登録 `Int` × 指定 `UInt`) の値が未定義だった。ホストの自己レビュー 2 周は通過し、相方 second-opinion-spec-001 が Minor で検出。指定した型を先に判定する順を Requirement に書き、その組の Scenario を足した)
---

## ルール文

Requirement が同じ失敗を複数の条件で起こし、失敗の値 (エラーの中身・メッセージ) が条件ごとに違うとき、提案の自己レビューで、条件が同時に当たる組を列挙し、そのときの値を決める判定の順が Requirement に書いてあり、その組の Scenario があるかを確かめる。守れたかは、同時に当たる組ごとに、期待する失敗の値を書いた THEN が spec にあることから判定する。

## 経緯

- 2026-10-20 guard-kmp-swift-int-result-type: 条件を 1 つずつ Scenario にしたため、組み合わせの値が決まっていないことに気づかなかった
