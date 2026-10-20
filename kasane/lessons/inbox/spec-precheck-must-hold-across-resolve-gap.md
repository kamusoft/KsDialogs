---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-10-20
last-seen: 2026-10-20
evidence:
  - guard-kmp-swift-int-result-type (spec が「show は登録された結果型が `Int` / `UInt` なら表示せず、View factory を呼ばない」と約束したが、show の判定のあと提示は別の Task でレジストリを引き直すため、その間の再登録で判定を通った show が拒否対象の factory を呼びえた。ホストの自己レビュー 2 周は通過し、相方 second-opinion-spec-001 が Major で検出。仕組みは足さず、spec の約束を「show を呼んだ時点の登録で判定し、その後の再登録は既存の結果復元時の照合に任せる」に絞った)
---

## ルール文

デルタスペックが「X の時点で共有状態 (レジストリ・設定) を判定し、条件に当たれば Y をしない」と約束するとき、提案の自己レビューで、判定してから Y が共有状態を使うまでの経路 (別 Task・待ち・引き直し) を実装で辿り、その間に共有状態を書き換えられるかを確かめる。書き換えられるなら、判定と使用を同じスナップショットにする設計を書くか、約束を判定の時点に絞る文を Requirement に書く。守れたかは、その Requirement に、判定の時点と、それより後の書き換えの扱いが書いてあることから判定する。

## 経緯

- 2026-10-20 guard-kmp-swift-int-result-type: 判定を show の入口に置くことだけを見て、提示の経路がレジストリを引き直すことを提案の段階で読んでいなかった。近縁: [[review-check-snapshot-contract-on-accessors]] (同じ型の破れをコードレビューの段階で見る側)
