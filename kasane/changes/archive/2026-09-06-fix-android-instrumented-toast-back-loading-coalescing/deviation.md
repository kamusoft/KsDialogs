# deviation: fix-android-instrumented-toast-back-loading-coalescing

- [付随修正] `kasane/handbook/cross/test-execution.md` の「API レベルで走る / 走らない Scenario」節: 合意スコープは実測値行の更新だけだが、同節の「件数表の 222 は 1 台分」が現行の件数表 (305) と食い違っていたため、同じ文書内の数字の追随として 305 に直した。理由: 実測値行を更新すると同節の 222 だけが取り残されて誤読を招く (2026-09-06)
- [レビュー指摘の処置] review-001 Minor: Android の `LoadingCoordinator.isDismissing` が参照 0 件になる件は、オーナー判断で **そのまま残す** (iOS 側の同名観察面と揃えたまま。本体不変の合意を維持。起票もしない) (2026-09-06)
