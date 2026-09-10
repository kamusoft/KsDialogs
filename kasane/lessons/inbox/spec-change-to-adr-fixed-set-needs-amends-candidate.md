---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-10
last-seen: 2026-09-10
evidence:
  - add-consumer-verification (spec が lint job の検査を 7 から 8 へ増やすのに、accepted の cross/ADR-0021 が「lint job は 7 検査、検査を無断で足すことはできない」と固定していることを design の ADR 候補が「なし」としていた。同じ提案で cross/ADR-0018 の MAUI 本体の版 (10.0.70) を写した Requirement が maui/ADR-0004 と現行コード (10.0.20) に衝突していた。ホストの自己レビュー 2 周は通し、相方 second-opinion-spec-001 #1 / #2 が Major で検出。cross/ADR-0022 (amends 0021) をオーナー判断で起票し、ADR-0018 の改訂は蒸留へ申し送り)
---

## ルール文

提案・design・spec が既存の accepted ADR が数値や集合として固定している事項 (検査の本数・job の集合・版の表・座標の一覧) を変える、または写すときは、確定前に当該 ADR の Decision を開いて「置き換える範囲」を特定し、置き換えるなら design の ADR 候補に amends のドラフトを、写すなら現行コードと同じ対象を扱う他の ADR (別ドメインを含む) との一致を確かめた結果を書く。守れたかは、design の ADR 候補節に「触れた accepted ADR とその扱い (維持 / amends / 出典の値と現行値の一致)」が列挙されていることから判定する。

## 経緯

- 2026-09-10 add-consumer-verification: 翻案元の agenda 決定事項に従って書いたため「議論済み」に見えたが、置き換えられる側の ADR を開いていなかった。同じ提案で、写した値 (10.0.70) が翻案元固有で、翻案先では別 ADR が却下した値だった
