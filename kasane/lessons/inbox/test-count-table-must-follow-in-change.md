---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-09-06
last-seen: 2026-09-08
evidence:
  - add-maui-nuget-distribution (facade 155 → 160・maui/android/native 31 → 34 に育ったのに、実装 6 グループ・review-001・review-002 のいずれも kasane/handbook/cross/test-execution.md の件数表 (2026-09-07 実測) の更新を求めなかった。verify-001 が INVALID の注記で検出し、オーケストレーターが change 内で 3 行を更新して deviation.md に付随修正として記録した。あわせて、修正サイクルで Kotlin テストが 1 件増えたのに修正前に書いた証跡 evidence/test-routes/ が 33 件のまま残り、同じ verify-001 が ❌ として検出した — 件数の証跡・件数表は修正サイクルの後に再計測して確定させる必要がある)
  - add-loading-toast-typed-show (テストが ios 251 → 275・android instrumented 305 → 333・maui 133 → 153 と育ったのに、kasane/handbook/cross/test-execution.md の件数表は 2026-08-28 実測のまま。同規約は「テスト構成が育って実態が変わったら本規約を実測で更新する」と定め、先例 (rename-dialog-contract-singular の負の検査表・fix-android-instrumented の instrumented 行) は実装側で追随していた。review-002 Minor 2 が検出し、蒸留で 3 行を更新した)
---

## ルール文

テストを追加・削除した change は、完了ゲート (絞り込みなしの全件実行) の実測値で `kasane/handbook/cross/test-execution.md` の件数表と負の検査表の該当行を、同じ change の中で更新する (実測日を添える)。守れたかは、change の diff にその行の更新が含まれること、または deviation.md に「件数表は蒸留で更新」と理由つきで記録されていることから判定する。

## 経緯

- 2026-09-06 add-loading-toast-typed-show: 3 ルートで計 72 件テストが増えたが、実装・review-001・verify-001 のいずれも件数表の更新を求めなかった。表は次の change の完了判定の基準になるため、放置すると「件数が合わない」を古い基準で判断することになる
- 2026-09-08 add-maui-nuget-distribution: 2 件目。今回はレビュー 2 周でも拾われず verify が拾った。派生の観測として、レビュー指摘の修正でテストが増えると、修正前に書いた件数の証跡 (evidence/) も同時に古くなる — 完了判定の件数 (3 ルート全件実行) は修正サイクルが閉じてから計測するか、修正のたびに証跡を更新する規律が要る
