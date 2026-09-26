---
scope: process
kind: pain
severity: normal
count: 3
first-seen: 2026-09-10
last-seen: 2026-09-25
evidence:
  - add-consumer-verification (deviation.md のオーナー判断 A の項が「発行後に `git checkout --` で復元する」という修正前の機構のまま残り、review-001 / 相方 Major の指摘で復元の主体・コマンド (`git checkout HEAD --`)・発行前検査と `EXIT` trap へ改めた後も更新されていなかった。review-002 Minor 2 が検出。deviation.md は蒸留がそのまま長命層へ運ぶ記録のため、古い機構が concepts に持ち込まれる寸前だった)
  - fix-release-published-wait (exploration の決定事項「再実行の分岐 (枠ごとの状態照会) は変わらない」に対し、review-001 Major の修正で再実行時の PUBLISHING 枠の待ちを後段の一括待ち step へ移した (取り消せない操作との順序も変わる) が、deviation.md が作られないまま修正サイクルを終えた。review-002 Minor が検出し、蒸留前に 1 項目として記録された)
  - define-loading-action-thread (完了確認の全件実行で LD-TH-07 が間欠的に落ち、オーケストレーターの修正依頼で Android の報告経路を「1 本の列 + 終了前の区切り」へ作り直した。design Decision 3 の前提 (切り替えを action の呼び出しの内側に入れれば順序の保証はそのまま) が崩れた機構変更だったが、deviation.md に記録しないまま review-001 に回し、Minor で検出された。その後の受理ループの失敗の閉じ込めも review-002 の Suggestion で deviation に追記した)
---

## ルール文

修正サイクル (レビュー指摘・テスト失敗・オーナー指摘のいずれが起点でも) で、design や deviation.md に書かれた仕組み (順序・待ち・復元・分岐の位置) を動かしたら、同じサイクルが終わる前に deviation.md の項を書く (既存の項があれば今の実装に書き直し、無ければ作る。判断そのものと理由は変えない)。守れたかは、修正後のレビューの解消確認表に deviation の項が含まれ、各項が実装と 1 対 1 で照合できることから判定する。

## 経緯

- 2026-09-10 add-consumer-verification: 修正の証跡 (evidence/review-fix-001) は書かれたが、記録 (deviation.md) は修正前のままだった。証跡は「その時点の実行」を残す文書で書き換えないが、deviation.md は現行の合意を表す文書なので追随が要る
- 2026-09-10 fix-release-published-wait: 2 件目。今回は deviation.md 自体が無く、修正が合意スコープの字面 (「変わらない」) からはみ出したことがどの成果物にも残っていなかった。レビュー指摘の修正で「機構の位置・順序」を動かしたら、記録の有無にかかわらず deviation を書く (無ければ作る)
- 2026-09-25 define-loading-action-thread: 3 件目。今回はレビュー指摘ではなく、完了確認のテスト失敗を受けた修正で機構を動かした。修正のきっかけ (レビュー / テスト失敗 / オーナー指摘) によらず、機構の位置・順序を動かした修正サイクルの終わりに deviation を書く必要がある
- 2026-09-26 昇格 (process L-003、オーナー承認)。ルール文を修正のきっかけを問わない形 (レビュー指摘に限らない) に直して転記した
