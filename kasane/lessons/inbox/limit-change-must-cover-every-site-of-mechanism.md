---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-09-10
last-seen: 2026-09-13
evidence:
  - backport-registry-wait-hardening (自己テストが「待機の止まらない誤実装」を停止ではなく失敗として表すかの確認で、レビューに名指しされた 1 種類 (`PUBLISHED` の枠を待機対象から外さない) だけを再現して塞いだ。同じ機構の他の終端 (`FAILED` / `NOT_FOUND` を終端として扱わない) が残り、review-002 が実測で 2 件を検出。その修正がさらに既存検査の判別力を食い (上限判定を丸ごと外しても緑)、review-003 が検出。収束まで 3 サイクル)
  - fix-release-published-wait (公開待ちの上限を 30 分 → 90 分へ上げる修正で、publish job にある 3 つの待ち (新設の一括待ち step・Android 枠と KMP 枠の再実行経路の `wait-published` 分岐) のうち 1 つだけを変え、残る 2 つは `central-portal.sh` の既定 1800 秒のままだった。しかも 2 つは step が別で枠ごとに直列に待つため、この change が解消したはずの「足し算になる直列待ち」が再実行時に 30 分 × 2 で復活していた。review-001 Major が検出)
---

## ルール文

待ちの上限・間隔・件数のような運用値、または同じ機構の分岐 (再実行経路・失敗経路を含む) を変えるときは、その値・機構が現れる箇所を `grep` で列挙し (workflow の step・スクリプトの既定値・再実行経路の分岐)、すべてに同じ変更を当てるか、1 箇所 (スクリプト側の専用の既定値) に集約して workflow から個別指定を消す。守れたかは、変更後に旧値・旧分岐の `grep` が 0 件であることと、報告に「出現箇所 N 件のうち N 件を変更 / 集約先」が書かれていることから判定する。

## 経緯

- 2026-09-10 fix-release-published-wait: 修正は「上限を専用変数 `KSR_PUBLISHED_TIMEOUT_SECONDS` (既定 5400) へ寄せ、再実行で引き継ぐ枠の待ちを一括待ち step へ回す」形で解消した (review-002 APPROVED)。上限が 1 箇所に集約されたことで、workflow の指定漏れで 30 分に落ちる経路そのものが消えた
