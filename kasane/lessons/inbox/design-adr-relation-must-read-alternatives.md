---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-08
last-seen: 2026-09-08
evidence:
  - add-maui-nuget-distribution (design Decision 6 と「ADR 候補」節が、Android の Dialog / Loading gateway に預かり口を配線する変更を「core/ADR-0033 (accepted) の決定からの乖離の解消」と位置づけ、蒸留時の作業を「現行照合 footer の更新」と申し送った。しかし ADR-0033 は同じ配線を Alternatives で「採らず」と明示し、その理由 (Kotlin 側の catch (Throwable) で観察可能挙動は成立) と Consequences の「派生: 3 面の対称化は将来課題」を本文に持っていた。実装は却下案の採用にあたり、却下理由は本 change の証跡 (型が失われメッセージだけになる) が反証している。review-001 Minor 1 が検出し、蒸留で amends (core/ADR-0036) を起票した)
---

## ルール文

design が accepted ADR との関係を「その決定に従う」「決定からの乖離を解消する」と書くとき、当該 ADR の Decision だけでなく Alternatives Considered と Consequences (派生・将来課題の行) まで読み、これから採る案がそこで却下・保留されていないかを確かめる。却下・保留されていたら「乖離の解消」ではなく「却下案の採用」であり、却下理由が崩れた根拠を design に書いたうえで、蒸留の申し送りを footer 更新ではなく amends / supersede のドラフト起票にする。守れたかは、design の ADR 参照箇所に「Alternatives に同案の却下あり / なし」の 1 語が添えてあることから判定する。

## 経緯

- 2026-09-08 add-maui-nuget-distribution: ADR-0033 の Decision 本文は一般形 (MAUI 境界は例外を値に変える) で、Android 側を除外したのは Alternatives と Consequences の側だった。Decision だけを読むと「乖離」に見え、Alternatives まで読めば「却下案の採用」と分かる。footer 更新だけで済ませると、accepted な ADR に「採らず」と「将来課題」が残り、次に同じ判断を参照する読み手を誤らせる (review-001 Minor 1)。近縁: [[merge-must-apply-adrs-accepted-meanwhile]] (合流時に accepted ADR へ追随する)
