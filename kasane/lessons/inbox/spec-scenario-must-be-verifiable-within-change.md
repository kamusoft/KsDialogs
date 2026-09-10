---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-10
last-seen: 2026-09-10
evidence:
  - add-consumer-verification (spec の Scenario「公開レジストリからの解決」が 4 形態の公開レジストリ解決とビルド成功を要求していたが、proposal Non-Goals と tasks は配布物が未公開であることを理由に実証を後続フェーズへ送っており、本 change の完了時点では Scenario を満たしたか判定できない形だった。相方 second-opinion-spec-001 #4 が Major で検出し、Scenario を削除して Requirement に「後続の change で立てる」旨を明記、「同じ version」Scenario も生成物と dry-run の解決結果で判定する形へ修正)
---

## ルール文

デルタスペックの Scenario を確定するとき、その THEN が本 change の完了時点で観測できるか (前提となる配布物・環境・外部状態が change の範囲内で成立するか) を 1 件ずつ確かめる。proposal の Non-Goals や tasks が「後続で実証する」と書いた事項を THEN に持つ Scenario は spec に残さず、Requirement の本文に後続の change で立てる旨を書くか、本 change で観測できる範囲 (生成物・設定・dry-run の解決結果) に THEN を限定して書き直す。守れたかは、verify の対応表に「実行証跡なし (後続待ち)」の Scenario が無いことから判定する。

## 経緯

- 2026-09-10 add-consumer-verification: 翻案元の spec を写した Scenario が、翻案先では未公開の配布物を前提にしていた。Non-Goals と spec の Scenario が同じ事項を逆向きに書いており、verify が判定不能になるところだった
