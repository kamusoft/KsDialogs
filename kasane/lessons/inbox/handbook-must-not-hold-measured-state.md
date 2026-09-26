---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - define-loading-action-thread (実装フェーズで handbook cross/test-execution.md の件数表 7 行を実測値へ更新し、蒸留で「件数表の更新漏れ」の教訓 (count 3) を handbook の完了判定へ昇格させる案を出したところ、オーナーが「handbook に実測件数を載せること自体が間違ってる。この表に何の意味があるのか」と指摘。件数の実測値を handbook から外した)
---

## ルール文

handbook (規範) に、テストや構成が育つと変わる実測値 (テストの件数・本数・所要時間・「現在は N 件」のような今の状態) を書かない・更新しない。handbook の値が実態と食い違っているのを見つけたら、値を実測で直す前に「その値は守る決まりか、今の状態か」を判定し、今の状態なら handbook から外して、その変更の証跡 (`kasane/changes/<id>/evidence/`) か concepts へ移す。同じ値の更新漏れが教訓として繰り返し数えられたら、ルールを足す前に値の置き場所を疑う。守れたかは、変更の diff で handbook/ に数値の実測値 (件数・本数・実測日つきの値) が増えていないことから判定する。

## 経緯

- 2026-09-26 define-loading-action-thread: 件数表の更新漏れが 3 つの change で続き、inbox の count が 3 に達した。オーケストレーターは「どこで更新を強制するか」だけを考え、表が規範 (handbook) に状態を持ち込んでいること自体を問わなかった。ksn-core の規範 / 記述の割り方 (食い違ったら直すのがコードの側なら handbook) に照らせば、件数は文書の側を直す値で handbook の対象外だった
