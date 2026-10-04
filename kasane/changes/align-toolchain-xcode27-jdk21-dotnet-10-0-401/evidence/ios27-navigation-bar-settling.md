# iOS 27.0: SwiftUI のナビゲーションバーの枠が落ち着くまでの観測

対象は `ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift` の「SwiftUI の TabView + NavigationStack で content 枠の modifier が基準になり、タブバーとナビゲーションバーを避ける」。2026-10-04、Xcode 27.0 / iOS 27.0 の Simulator (iPhone 17) で、対象テストに一時的な計測出力を入れて測った (実装ワーカーの報告からの転記。計測コードは撤去済みで、生ログは残していない)。

| 時点 | ナビゲーションバーの枠 (ウィンドウ座標) | バーの下端 | 中身の safe area 上端・台帳の矩形の上端 |
|---|---|---|---|
| 台帳に候補が載った直後 (修正前のテストが測っていた時点) | (0, 50, 400, 106)。presentation layer は未生成 | 156 | 104 |
| `window.layoutIfNeeded()` を 1 回呼んだ直後 (1〜2 ms 後) | (0, 50, 400, 54) | 104 | 104 |
| その後 1.5 秒間、5 ms 間隔 | (0, 50, 400, 54) のまま。presentation layer も同じ枠 | 104 | 104 |

- `UINavigationBar` は window 内に 1 本だけだった
- 計測は 5 回行い、毎回同じ値だった
- 製品が基準にする矩形の上端は最初から 104 で、落ち着いたバーの下端 104 と一致する
- 高さ 106 の枠については、その時点で presentation layer が無かったことまでを観測した。画面に描かれないことを直接確かめたわけではない
- iOS 26 で同じ途中の枠が出るかは未確認 (開発機に iOS 26 の runtime が無い)
