---
scope: process
kind: pain
severity: normal
count: 3
first-seen: 2026-09-09
last-seen: 2026-09-28
evidence:
  - fix-android-instrumented-toast-ime-hide-flake (CI の間欠失敗の根本対応で、一般化 (共通プリミティブ・handbook 規約・他テストへの適用) を別 change に起票する切り方を提案したところ、オーナーが却下 — 「ワークフロー系の問題は全部ここで片付けてくれ。小分けにしてもキリがない」。旧 inbox: ci-flake-work-batched-not-split)
  - define-loading-action-thread (完了報告で、handbook cross/test-execution.md の負の検査の表と実測の食い違いを「起票 (drift の棚卸しに回すなら見送り)」の選択肢で提示したところ、オーナーが「分かってるなら今直す」と指示。旧 inbox: known-small-discrepancy-fix-in-change-not-ticket)
  - wait-for-host-appearance (実装中に見つかった、変更前からある iOS の既定の並列実行での Toast テストの間欠失敗を、オーケストレーターは原因が未解明のため「起票」を推奨して提示したが、オーナーは「同梱」を選んだ。原因は MainActor の混み合いで期限の短い Toast が捨てられていたことで、テスト側の trait で直した)
---

## ルール文

実装フェーズで見つかったスコープ外の不具合・食い違い (変更前からある flake、handbook の表と実測の食い違い、同じ族の失敗の一般化など) をオーナーに「同梱 / 起票 / 見送り」で諮るときは、原因が未解明でも「同梱」を推奨の既定にし、作業量の見込み (切り分けに要る実測・直す範囲) を添える。「起票」を推奨にするのは、直す範囲が現在の change の能力やドメインの外へ広がり、同梱すると proposal の Impact を書き換える規模になる場合に限り、その理由を推奨に書く。守れたかは、判断依頼の推奨が「同梱」になっているか、「起票」なら範囲が広がる理由が書かれていることから判定する。

## 経緯

- 2026-09-09 fix-android-instrumented-toast-ime-hide-flake: 小分けの起票は探索・提案・レビューのコストが change ごとに付き、オーナーの時間感覚と食い違った
- 2026-09-25 define-loading-action-thread: 値が分かっている表の食い違いを起票の選択肢に回した。ksn-orchestrator の「付随修正」判定では同梱条件を満たす類型だった
- 2026-09-28 wait-for-host-appearance: ksn-orchestrator 自体に「起票を既定にしない」とあるのに、原因が未解明であることを理由に起票を推奨した。オーナーは同梱を選び、切り分けの結果は小さなテスト側の修正で閉じた。原因が未解明でも、切り分けに要る実測の量は見込みとして示せる。3 件は別々の inbox 項目で数えられていたが、「小分けにした起票より今直す」という同じ好みとして統合した (オーナー承認 2026-09-29)
