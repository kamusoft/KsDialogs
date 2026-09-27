---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-27
last-seen: 2026-09-27
evidence:
  - fix-ios-sample-demo-autoplay (蒸留で Loading の提示先不在時の OS 差を見つけたとき、「直すか承認するかが未決なので concepts は今の記述のまま」を推奨したが、オーナーが「concepts はコードの抽象化なので、今の状態をそのまま起こせばよい」と指摘し、承認済みでない OS 差として記述した)
---

## ルール文

蒸留・棚卸しで、コードの現在の振る舞いが concepts の記述より細かい、または OS 間で違うと分かったら、直す予定や承認の判断が未決であっても、concepts には今のコードの振る舞いをそのまま書き起こす。未決であることは「承認済みの差ではない」などの注記で示し、直す・承認するの判断は change (簡易起票) の側に置く。守れたかは、蒸留のサマリで見つかった振る舞いの差ごとに、concepts の記述箇所が挙がっていることから判定する。

## 経緯

- 2026-09-27 fix-ios-sample-demo-autoplay: Android の Loading は提示先が現れた時点で表示し直し、iOS は表示しないまま終わる差を蒸留中に発見。オーナーの判断で、core の loading-semantics に「承認済みの差ではない」OS 差として記述し、直すか承認するかは fix-ios-toast-wait-host-at-launch の探索に追記した
