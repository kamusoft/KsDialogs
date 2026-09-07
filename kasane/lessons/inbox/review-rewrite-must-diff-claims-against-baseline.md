---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - split-concepts-platform-surface (review-001 が core/api 8 本の書き直しを「新規に増えた主張なし」と判定したが、相方 second-opinion-code-001 が Major 2 件で検出 — 1 引数 factory の適用範囲が登録経路からインライン show へ拡張されていた主張 (model-binding-semantics.md)、Android の Compose 登録経路とインライン表示経路を混同した表 (android/api 3 本)。いずれも baseline の本文には無い主張で、修正サイクル 1 周が発生)
---

## ルール文

「意味を改訂しない言い換え・再配置」を目的とする文書変更 (proposal の Non-Goals に挙動・保証の変更が無い change の concepts / handbook の書き直し) をレビューするとき、baseline (着手時点のコミット) の本文と新本文を節ごとに突き合わせ、新本文にだけある主張 (適用範囲の拡張・経路の統合・条件の欠落) を列挙してから、各主張を実装コードで確認する。「言い換えに見える」ことを理由に主張の比較を省かない。守れたかは、review に「新規主張 N 件 (箇所と実装での確認結果)」の記録があることから判定する。

## 経緯

- 2026-09-05 split-concepts-platform-surface: 書き直しは表を散文に落とす作業だったため、ホスト側は文面の同値性を読み比べただけで主張の増減を数えず、相方は実装 (各形態のインライン show の引数・Compose モジュールの関数群) と突き合わせて新規主張を検出した
