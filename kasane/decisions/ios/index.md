# ios ADR 一覧

iOS 系統 (UIKit/Swift ネイティブ実装) の決定記録。

| ID | タイトル | status | date |
|---|---|---|---|
| [0001](0001-dialog-show-call-order-at-entry.md) | iOS の Dialog の show は呼び出し元の実行文脈のまま待ちの列に着き、同じ UI スレッドから続けて呼んだ順を入口で保証する | accepted | 2026-09-29 |
| [0002](0002-swiftui-content-receives-standard-safe-area.md) | iOS の器に載せた SwiftUI の中身には、窓全体基準で端に付けた軸の安全領域との重なりを、器の大きさを変えずに標準の安全領域として届ける | accepted | 2026-10-20 |
| [0003](0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md) | iOS の器で端に付けた SwiftUI の中身が安全領域の内側に収まらないときは、器の内側の辺にそろえて器いっぱいに置く | accepted | 2026-10-21 |
