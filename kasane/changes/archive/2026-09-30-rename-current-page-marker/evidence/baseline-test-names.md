# 改名前のテスト名の一覧 (tasks 0.1)

改名前 (develop の 077da71 時点) のテスト宣言を、ソースから列挙したもの。1.4・2.4 で改名後の実行結果と突き合わせる基準にする。

## iOS: `DialogCurrentPageSwiftUITests` (`ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift`、Swift Testing、4 件)

| # | 関数名 | 表示名 (`@Test`) |
|---|---|---|
| 1 | `swiftUITabViewAndNavigationStackUseModifier()` | SwiftUI の TabView + NavigationStack で content 枠の modifier が基準になり、タブバーとナビゲーションバーを避ける |
| 2 | `swiftUITabViewUsesMarkerOfShownTabDuringSwitch()` | TabView の切り替え中に去るタブの印が残っていても、表示中のタブの印が基準になる |
| 3 | `modifierWinsOverDefaultAndFallsBackAfterDetach()` | 既定の取得元が届く UIKit の画面でも、modifier を付けた枠が既定に勝つ。枠が画面から外れると既定に戻る |
| 4 | `ledgerWinsOverRegisteredProviderThenFallsBack()` | modifier の台帳が登録した provider に勝ち、台帳が空になると登録した provider へ進む |

## Android: `jp.kamusoft.ksdialogs.compose.ComposeCurrentPageTests` (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt`、instrumented、7 件)

| # | テスト名 |
|---|---|
| 1 | `Scaffold_の_content_枠に付けると下部バーを避ける` |
| 2 | `遷移で枠が離脱すると台帳から外れ_VISIBLE_AREA_と一致する` |
| 3 | `戻る遷移で再び付けた画面が基準になる` |
| 4 | `入れ子は後から外側が配置されても内側が勝つ` |
| 5 | `窓の外に配置された枠は後から配置されても候補にならない` |
| 6 | `同じ_Activity_で出したモーダルの中の枠は後から配置されると候補になり画面上の位置が基準になる` |
| 7 | `台帳が登録した関数に勝ち_台帳が空になると登録した関数へ進む` |
