---
scope: process
kind: pain
severity: normal
count: 2
first-seen: 2026-09-10
last-seen: 2026-09-10
evidence:
  - add-consumer-verification (deviation.md のオーナー判断 A の項が「発行後に `git checkout --` で復元する」という修正前の機構のまま残り、review-001 / 相方 Major の指摘で復元の主体・コマンド (`git checkout HEAD --`)・発行前検査と `EXIT` trap へ改めた後も更新されていなかった。review-002 Minor 2 が検出。deviation.md は蒸留がそのまま長命層へ運ぶ記録のため、古い機構が concepts に持ち込まれる寸前だった)
  - fix-release-published-wait (exploration の決定事項「再実行の分岐 (枠ごとの状態照会) は変わらない」に対し、review-001 Major の修正で再実行時の PUBLISHING 枠の待ちを後段の一括待ち step へ移した (取り消せない操作との順序も変わる) が、deviation.md が作られないまま修正サイクルを終えた。review-002 Minor が検出し、蒸留前に 1 項目として記録された)
---

## ルール文

レビュー指摘への修正で deviation.md に記録済みの機構 (復元・ガード・順序などの「実際はこうする」の部分) を変えたときは、同じ修正サイクルの中で当該 deviation 項の記述を現行の実装へ書き直す (判断そのものと理由は変えない)。守れたかは、修正後の deviation.md の各項が実装と 1 対 1 で照合できること (レビューの解消確認表に deviation の項が含まれること) から判定する。

## 経緯

- 2026-09-10 add-consumer-verification: 修正の証跡 (evidence/review-fix-001) は書かれたが、記録 (deviation.md) は修正前のままだった。証跡は「その時点の実行」を残す文書で書き換えないが、deviation.md は現行の合意を表す文書なので追随が要る
- 2026-09-10 fix-release-published-wait: 2 件目。今回は deviation.md 自体が無く、修正が合意スコープの字面 (「変わらない」) からはみ出したことがどの成果物にも残っていなかった。レビュー指摘の修正で「機構の位置・順序」を動かしたら、記録の有無にかかわらず deviation を書く (無ければ作る)
