---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - add-loading-toast-typed-show (テストが ios 251 → 275・android instrumented 305 → 333・maui 133 → 153 と育ったのに、kasane/handbook/cross/test-execution.md の件数表は 2026-08-28 実測のまま。同規約は「テスト構成が育って実態が変わったら本規約を実測で更新する」と定め、先例 (rename-dialog-contract-singular の負の検査表・fix-android-instrumented の instrumented 行) は実装側で追随していた。review-002 Minor 2 が検出し、蒸留で 3 行を更新した)
---

## ルール文

テストを追加・削除した change は、完了ゲート (絞り込みなしの全件実行) の実測値で `kasane/handbook/cross/test-execution.md` の件数表と負の検査表の該当行を、同じ change の中で更新する (実測日を添える)。守れたかは、change の diff にその行の更新が含まれること、または deviation.md に「件数表は蒸留で更新」と理由つきで記録されていることから判定する。

## 経緯

- 2026-09-06 add-loading-toast-typed-show: 3 ルートで計 72 件テストが増えたが、実装・review-001・verify-001 のいずれも件数表の更新を求めなかった。表は次の change の完了判定の基準になるため、放置すると「件数が合わない」を古い基準で判断することになる
