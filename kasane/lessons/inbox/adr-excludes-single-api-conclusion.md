---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-30
last-seen: 2026-09-30
evidence:
  - rename-current-page-marker (探索中に起票した core/ADR-0045 の下書きで、一般則「接頭辞 ks は OS の View 型への後付け属性とその SwiftUI の対に限る」に加えて、議論のきっかけになった API 1 つ (表示中のページの印) の扱いと命名方針を Decision の段落・タイトル・却下案・Consequences に書いた。オーナーが「これは書かなくて良い。限定的すぎる決定はいらない」と指摘し、該当箇所をすべて外した)
---

## ルール文

会話中の決定から ADR を起こすとき、議論のきっかけになった個別の API・画面・ファイル 1 つだけに効く結論 (その API に規則を当てはめた結果・その API の名前の付け方) を、タイトル・Decision・Alternatives Considered・Consequences に書かない。きっかけは Context に 1 文だけ残し、その API の結論は exploration.md の決定事項に書く。守れたかは、ADR のタイトルと Decision 節からきっかけの API 名を消しても、決定の意味が変わらないことから判定する。

## 経緯

- 2026-09-30 rename-current-page-marker: `ks` の付け分けを決めた論点で、一般則と「印には付けない」を同じ ADR に並べた。後者は一般則から導ける当てはめで、ADR の決定を細かくするだけだった
