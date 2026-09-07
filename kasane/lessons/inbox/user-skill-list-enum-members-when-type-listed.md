---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - split-concepts-platform-surface (task 6.2 の仕分けで `DialogAlignment` / `DialogTransitionEdge` の列挙子 `Center` / `Fill` / `Top` と `ToastStyle.BuiltinBackgroundColor` を「機械的に導出できる名前」として除外リストに載せたところ、オーナーが「掲載に倒す」と判断を覆した。兄弟の `End` / `Bottom` / `BuiltinDefaultDuration` が既に Skill に載っていた)
---

## ルール文

利用者向け Skill の API 名網羅検査の候補を仕分けるとき、候補が「Skill に既に載っている型の列挙子」または「Skill に既に載っている名前と同じ命名規則の兄弟 (`BuiltinDefaultDuration` に対する `BuiltinBackgroundColor` など)」で、その兄弟のいずれかが Skill に現れているなら、「機械的に導出できる名前」で除外せず、列挙を揃えて掲載する。除外の「機械的に導出」は、1 つの規則から全数が導けて Skill にその規則が書かれている場合に限る。

## 経緯

- 2026-09-06 split-concepts-platform-surface: 除外リストの書き直し (design Decision 4) で 4 名を暫定除外し、ワーカーもレビュアー (review-004 Suggestion) も「掲載側にも読める」と留保していた。完了報告で提示したところオーナーが掲載を選択。一部の兄弟だけが偶然載っている状態は利用者のエージェントから見て網羅に見えないため、揃える側に倒すのが既定
