# 検証結果: add-page-layout-area (001 回目)

**日付**: 2026-09-26
**判定**: VALID

検証範囲: HEAD (c4936a8) からの未コミットの作業ツリー全体 (`git diff HEAD` と未追跡ファイル)。デルタスペック 5 能力 (dialog-contract / ios-native / android-native / maui-binding / samples)、tasks.md、deviation.md、ui/brief.md を突き合わせた。

## 対応表

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離。実装・テストのパスはリポジトリ相対。

### dialog-contract

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 基準領域「表示中のページ」 / タブバーを持つページで End 配置がタブバーを避ける | `ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift:27` (`.currentPage` → ページの inset)、`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:208` / `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutResolver.kt:33`、`DialogLayoutHost.kt:242` | iOS `DialogCurrentPageTests.tabAndNavigationEndPlacementAvoidsTabBar`、Android `DialogCurrentPageTests.登録した_View_で_End_配置が下部バーを避ける`、ケース表 C24 | ✅ |
| 同 / 同じ画面で visibleArea はタブバーに重なる | 同上 | iOS `visibleAreaOverlapsTabBarOnSameScreen`、Android `同じ画面で_VISIBLE_AREA_は下部バーに重なり_CURRENT_PAGE_より下に出る` | ✅ |
| 同 / 全画面のページでは可視領域と一致する | 同上 (ページ ∩ 可視領域) | iOS `fullScreenPageMatchesVisibleArea`、ケース表 C26 (iOS / Android) | ✅ |
| 同 / 比率サイズもページの矩形を基準にする | 同上 (R を差し替えるだけで比率計算は既存) | iOS `proportionalHeightUsesPageRect`、ケース表 C25 (iOS / Android) | ✅ |
| 同 / バーの下まで伸びるページでも safe area の内側を採る | `ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageGeometry.swift:21` (`safeAreaLayoutGuide.layoutFrame`) / Android は可視領域との共通部分 (`DialogLayoutHost.kt:249`) | iOS `tabAndNavigationEndPlacementAvoidsTabBar` (ページの View がタブバーの下まで伸びることを前提条件で確認)、ケース表 C27、Android `edge_to_edge_でもシステムバーを含まない` | ✅ |
| 現在ページは登録された provider から得る / 登録した provider の View が基準になる | iOS `CurrentPage/DialogRegisteredCurrentPageSource.swift`、Android `DialogCurrentPageSource.kt:150` | iOS `DialogCurrentPageTests` (登録の優先と解除)、Android `登録した_View_で_End_配置が下部バーを避ける` | ✅ |
| 同 / 登録の差し替えは次の表示から | iOS `DialogCurrentPageResolver.capturingRegistration` を器の生成時に固定 (`DialogLayoutApplier.swift:63`)、Android `DialogLayoutHost` の既定引数 (`DialogLayoutHost.kt:42`) | iOS `登録の差し替えは次の表示から効き…`、Android `登録の差し替えは次の表示から効き表示中のダイアログは動かない` | ✅ |
| 同 / 窓の寸法が変わると provider に問い合わせ直す | iOS `DialogLayoutApplier.swift:139` (入力変化で再問い合わせ + 次の main の番の再問い合わせ)、Android `DialogLayoutHost.kt:190` | iOS `[PB-WN-01] 回転でページの矩形が変わると…`、Android `PB_WN_01_回転でページの矩形が変わると…` | ✅ |
| 同 / modifier の台帳が登録 provider に勝つ | iOS `DialogCurrentPageResolver.swift:18` (台帳 → 登録 → 既定)、Android `DialogCurrentPageResolver.kt:46` (台帳 → 登録) | iOS `DialogCurrentPageSwiftUITests.ledgerWinsOverRegisteredProviderThenFallsBack`、Android `ComposeCurrentPageTests.台帳が登録した関数に勝ち_台帳が空になると登録した関数へ進む` | ✅ |
| 同 / 台帳が空なら登録 provider へ進む | 同上 | 同上 | ✅ |
| 同 / 登録 provider が null なら既定 provider へ進む | iOS 同上、MAUI `maui/KsDialogs.Maui/Internals/DialogCurrentPageLocator.cs:42` | iOS `登録した provider が nil を返すと既定の取得元へ進む`、MAUI `DialogCurrentPageLocatorTests.RegisteredProviderFallsBackToTheDefaultPage` | ✅ |
| 同 (本文) / 候補は提示先と同じ window (Android は同じ Activity のウィンドウ) | iOS `DialogCurrentPageGeometry.swift:17`、Android `DialogCurrentPageSource.kt:99` (`isInPresentingActivity`) | iOS `viewInAnotherWindowIsNotUsed`、Android `別の_Activity_のウィンドウに載った…`・`同じ_Activity_のモーダルのウィンドウに載った…`・`KsDialogs_の器に載った…` | ⚠️ deviation 2 (Android の「同じ Activity のウィンドウ」の解釈)。実装は記述と 1 対 1 で一致 |
| 同 (本文) / 窓座標の矩形を返す provider は公開しない | iOS `DialogCurrentPageSource` は internal。Android `DialogCurrentPageSource` は internal、台帳の入口と印は `@KsDialogsInternalApi` の public | (公開面の正の検査のみ) | ⚠️ deviation 1。`KsDialogsInternalApi.kt` が `@RequiresOptIn(level = ERROR)`、`DialogCurrentPageLedger` / `DialogCurrentPageMarker` に付与されており記述どおり |
| 未解決時は可視領域へ落ちる / 未登録なら可視領域 | Android `DialogUnregisteredCurrentPageSource` + `DialogLayoutHost.kt:260` (`Log.w`) | Android `未登録なら可視領域と一致し未登録を示す診断ログが_1_件出る` | ✅ |
| 同 / provider が null を返す | iOS / Android の登録 source | Android `登録した関数が_null_を返すと可視領域と一致する`、iOS `provider が nil を返し既定の取得元も解決できないときは…` | ✅ |
| 同 / provider が例外を投げても表示は失敗しない | iOS `DialogRegisteredCurrentPageSource.swift:16`、Android `DialogCurrentPageSource.kt:157` | Android `登録した関数が例外を投げても表示は失敗せず…`、iOS `provider がエラーを投げても表示は失敗せず…` | ✅ |
| modifier の台帳規則 / 1 つだけ付けたとき | iOS `CurrentPage/DialogCurrentPageLedger.swift`、Android `DialogCurrentPageLedger.kt:53` + `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogCurrentPage.kt` | iOS `DialogCurrentPageLedgerTests` (1 つだけ)、Android `Scaffold_の_content_枠に付けると下部バーを避ける`、JVM `DialogCurrentPageSelectionTests` | ✅ |
| 同 / 画面遷移で付けた画面が消えたら残りへ戻る | iOS `DialogCurrentPageMarkerView.didMoveToWindow`、Compose `onDetach` | iOS `名乗った枠が画面から外れると…`、Android `遷移で枠が離脱すると台帳から外れ_VISIBLE_AREA_と一致する` | ✅ |
| 同 / 戻る遷移で再び付けた画面が基準になる | 同上 | iOS `画面へ戻った枠は再び基準になる`、Android `戻る遷移で再び付けた画面が基準になる` | ✅ |
| 同 / 入れ子は内側が勝つ | iOS `DialogCurrentPageLedger.swift:61`、Android `chooseCurrentPage` | iOS `入れ子の枠は、後から外側が載っても内側が勝つ`、Android `入れ子は後から外側が配置されても内側が勝つ`、JVM | ✅ |
| 同 / 窓の外に配置されたものは候補にならない | iOS `DialogCurrentPageGeometry.swift:26`、Android `chooseCurrentPage` の `intersects` | iOS `窓の外に置かれた枠は…`、Android `窓の外に配置された枠は後から配置されても候補にならない`、JVM | ✅ |
| 同 (iOS 追加規則) / 画面に表示されていない印を外す | iOS `DialogCurrentPageLedger.swift:81` (`isShownOnScreen`: 自身と祖先の `isHidden` / `alpha < 0.01`、問い合わせ時点) | iOS `非表示の祖先を持つ枠は…`・`不透明度 0 の祖先を持つ枠は…`・`TabView の切り替え中に去るタブの印が残っていても…` | ⚠️ deviation 3。記述と 1 対 1 で一致 (iOS のみ、Compose 側には無い) |
| 共通ケース表の拡張 / 追加ケースが iOS / Android の実 frame 検証に載る | `core/layout-spec/cases.json` C24〜C28 (期待値を手計算で再導出し一致を確認) | iOS `DialogLayoutCaseTableTests` / `DialogSwiftUIAttributeDslTests` / Loading・Toast のケース表、Android `DialogLayoutCaseTableTests` ほか (結果 XML に C24〜C28 が現れる) | ✅ |
| 同 / 既存ケースは変わらない | cases.json の差分は追加のみ (C01〜C23 不変) | 同上 (全ケース成功) | ✅ |

### ios-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| currentPage と既定 provider / tab + navigation の階層で End 配置がタブバーを避ける | `ios/Sources/KsDialogs/CurrentPage/DialogDefaultCurrentPageSource.swift` | `DialogCurrentPageTests.tabAndNavigationEndPlacementAvoidsTabBar` | ✅ |
| 同 / presented された画面が現在ページになる | 同 `currentPageViewController(from:)` | `presentedViewControllerBecomesCurrentPage` | ✅ |
| 同 / 表示中の器は現在ページにならない | 同 `isKsDialogsContainer` | `presentedDialogContainerIsNotCurrentPage` | ✅ |
| 同 / 別ウィンドウの View は候補にならない | `DialogCurrentPageGeometry.swift:17` | `viewInAnotherWindowIsNotUsed` | ✅ |
| provider の上書き / 上書きした provider が既定より優先される | `Contract/DialogCurrentPage.swift`、`DialogCurrentPageResolver.swift` | `登録した provider の View が既定より優先され、登録を外すと既定に戻る` | ✅ |
| 同 / 登録を外すと既定に戻る | 同上 | 同上 | ✅ |
| SwiftUI の modifier / TabView + NavigationStack で content 枠の modifier が基準になる | `SwiftUI/View+DialogCurrentPage.swift`、`SwiftUI/DialogCurrentPageMarker.swift` | `DialogCurrentPageSwiftUITests.swiftUITabViewAndNavigationStackUseModifier` | ✅ |
| 同 / modifier を付けた枠が既定 provider に勝つ | 同上 | `modifierWinsOverDefaultAndFallsBackAfterDetach` | ✅ |
| 同 / 枠が画面から外れると既定 provider に戻る | 同上 | 同上 | ✅ |
| ケース表の追加ケースへの適合 (iOS) / 追加ケースの実 frame 検証が通る | `ios/Tests/KsDialogsTests/Support/DialogLayoutPageHostViewController.swift` (pageArea を safe area で再現) | `DialogLayoutCaseTableTests` 他 | ✅ |

### android-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| CURRENT_PAGE と provider の登録 / 登録した View で End 配置が下部バーを避ける | `DialogLayoutArea.kt`、`DialogCurrentPage.kt`、`DialogLayoutHost.kt` | `DialogCurrentPageTests.登録した_View_で_End_配置が下部バーを避ける` | ✅ |
| 同 / 未登録なら可視領域と診断ログ | `DialogUnregisteredCurrentPageSource`、`DialogLayoutHost.kt:260` | `未登録なら可視領域と一致し未登録を示す診断ログが_1_件出る` | ✅ |
| 同 / 器のウィンドウと Activity のウィンドウの原点が違っても同じ場所を指す | `DialogLayoutHost.kt:242` (器の root の `getLocationOnScreen` を引く) | `器のウィンドウと_Activity_のウィンドウの原点が違っても同じ場所を指す` | ✅ |
| 同 / edge-to-edge でもシステムバーを含まない | `DialogLayoutHost.kt:249` (辺ごとに可視領域の余白以上) | `edge_to_edge_でもシステムバーを含まない` | ✅ |
| Compose の modifier / Scaffold の content 枠に付けると下部バーを避ける | `compose/KsDialogCurrentPage.kt` | `ComposeCurrentPageTests.Scaffold_の_content_枠に付けると下部バーを避ける` | ✅ |
| 同 / 遷移で枠が離脱すると台帳から外れる | 同 `onDetach` | `遷移で枠が離脱すると台帳から外れ_VISIBLE_AREA_と一致する` | ✅ |
| 同 / 入れ子は内側が勝つ | `chooseCurrentPage` | `入れ子は後から外側が配置されても内側が勝つ` | ✅ |
| ケース表の追加ケースへの適合 (Android) / 追加ケースの instrumented 検証が通る | `support/CurrentPageAreaFixture.kt` (Activity 側にページ領域の View) | `DialogLayoutCaseTableTests` 他 (C24〜C28 が結果 XML に現れる) | ✅ |

### maui-binding

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| CurrentPage のパススルー / CurrentPage が Native へ届く | `maui/KsDialogs.Maui/Contract/DialogLayoutArea.cs`、両 OS の `PlatformDialogContent.ToBridgeLayoutArea`、bridge 2 つ、`maui/macios/KsDialogs.Binding.iOS/StructsAndEnums.cs` | `DialogLayoutPassthroughTests.CurrentPageDeclaredInXamlReachesTheGateway` / `CurrentPageAttachedInCodeReachesTheGateway`、`MauiDialogLayoutPassthroughTests.基準領域はどの値も同じ意味の値へ写る`、`MauiBridgeCurrentPageTests` | ✅ |
| MAUI 層の既定 provider / TabbedPage の中の NavigationPage | `Internals/DialogCurrentPageLocator.cs:84` (`FindDefaultPage`)、`Platforms/{iOS,Android}/PlatformCurrentPage.cs` | `DialogCurrentPageLocatorTests.TabbedPageWithNavigationPageResolvesToTheTopContentPage` + 実配置テストホスト `tabbed-navigation` (両 OS) | ✅ |
| 同 / Shell | 同上 | `ShellResolvesToItsCurrentPage` + `shell` | ✅ |
| 同 / モーダルが優先される | 同上 (ModalStack の最後を起点) | `ModalPageWinsOverTheRootPage` / `ModalNavigationPageResolvesToItsTopPage` + `modal` / `modal-navigation` | ⚠️ deviation 2 (Android でモーダルのダイアログウィンドウを候補に含める)。一致 |
| 同 / FlyoutPage の Detail | 同上 | `FlyoutPageResolvesToTheTopOfItsDetail` + `flyout` | ✅ |
| 同 / 素の ContentPage は可視領域と一致する | 同上 | `PlainContentPageResolvesToItself` + `plain-content-page` (`sameAsVisibleArea=True`) | ✅ |
| 同 (本文) / 未描画は未解決・iOS Native の既定 provider は使わない | `DialogCurrentPageLocator.cs:151` (Handler なし)、iOS `PlatformCurrentPage.cs:64` (未解決時は提示先 window を返し Native の VC 走査へ進ませない) | `UnrenderedDefaultPageIsUnresolved`、`MauiBridgeCurrentPageTests.presentationWindowAsPageMatchesVisibleArea` | ✅ |
| provider の上書き (MAUI) / 上書きした要素が基準になる | `Presentation/DialogCurrentPage.cs`、`DialogCurrentPageLocator.Select` | `RegisteredElementWinsOverTheDefaultPage` + `provider-override` (両 OS) | ✅ |

MAUI の「実配置」(ダイアログがその矩形を基準に置かれる) は `maui/KsDialogs.Maui.PlacementHost` (手動起動のテストホスト) が担保する。本検証でも両 OS で起動し直して確認した (下記「テスト実行」)。

### samples

| Requirement / Scenario | 実装 | テスト (証跡) | 状態 |
|---|---|---|---|
| レイアウトデモ項目 / Current page で End/End がタブバーを避ける | 4 ルートの `SampleLayoutPanelScreen` / `SampleLayoutPanelTabbedPage.cs`、iOS 系は `.ksDialogCurrentPage()`、Android 系は Compose の modifier、MAUI は既定 provider | `ui/verification/*-current-page-end-end.png` (6 面) | ✅ |
| 同 / Visible area で End/End はタブバーに重なる | 同上 | `ui/verification/*-visible-area-end-end.png` (6 面) | ✅ |
| 同 / Current page で Start/Start はナビゲーションバーの下に出る | 同上 | `ui/verification/*-current-page-start-start.png` (6 面) | ✅ |
| 同 / Info タブでは画面の上端に出る | 同上 (Info タブの content 枠にも名乗り) | `ui/verification/*-info-current-page-start-start.png` (6 面) | ✅ (iOS 系の成立は deviation 3 に依存) |
| 同 / Info タブへ切り替えてもパネルの設定は保たれる | iOS `@State model` を TabView の外側、Android `SampleLayoutPanelState` を Activity が保持、MAUI はパネルのページを TabbedPage が保持 | `*-info-current-page-start-start.png` (パネルで設定した Start/Start が Info タブで効いている) | ✅ |
| 同 / 既存のパネル操作は変わらない | 同上 | `*-visible-area-end-end.png`、`*-panel-initial.png` | ✅ |
| 同 (本文) / 文言・初期値・タブ名は ui/brief.md の文言表と 4 ルート一致 | 4 ルートの `SampleText` | `kasane/handbook/cross/sample-parity.md` 文言表 (brief と一致)、`*-panel-initial.png`・`*-info-tab.png` | ✅ |

## 追加検査

- [x] tasks.md: 全 23 タスクがチェック済み。対応表と突き合わせて虚偽チェックなし (6.1 の件数は `evidence/distill-handoff.md`、5.2 は `evidence/panel-accessibility-*.txt` 6 面、5.3 は `ui/verification/` 36 枚で実在を確認)
- [x] 逆流検査: `git diff HEAD` で change 内の追跡ファイルの差分は tasks.md のチェックボックスのみ。proposal.md / exploration.md / specs/ / ui/brief.md / core/ADR-0038 は無変更
- [x] 未記録乖離: なし
- [x] 付随修正: deviation.md の `[付随修正]` 2 件 (Android Native / KMP Android の `SampleOffsetField.kt` の `setHorizontallyScrolling(false)` + `maxLines = 1`、両ルートの `MainActivity.kt` の API 35 以上だけ `isAppearanceLightStatusBars = true`) は diff と 1 対 1 で一致
- [x] UI 変更: ui/brief.md に承認モック (mock-b2.html → approved.png、2026-09-26 オーナー承認) と実装時の裁量の記録あり
- [x] テスト全件成功 (下記)

## テスト実行 (本検証で実行した結果)

作業用 Simulator (KsDialogs-add-page-layout-area) と未起動だった AVD (Small_Phone、API 35) を起動して使い、終了後に停止した。共用の端末は使っていない。

| ビルドルート | 実行 | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -parallel-testing-enabled NO` | Swift Testing 316 件 / 54 suites 成功 (XCTest 側 `Executed 0 tests`) |
| android/ | `./gradlew test --rerun-tasks` | 75 件 / 失敗 0 (`verifyNoDeclarativeUiDependency`・`:api-surface-check:compileDebugKotlin` を含む) |
| android/ instrumented | `ANDROID_SERIAL=<専用 AVD> ./gradlew connectedDebugAndroidTest` | `:ksdialogs-core` 350 件 / 失敗 0 / skip 1 (`PB_SB_04`、API 29 専用)。`:ksdialogs` 52 件 / 失敗 0 |
| maui/ | `dotnet test` | 192 件 / 失敗 0 (`KsDialogs.Maui.ApiSurfaceCheck` のビルドを含む) |
| maui/macios/native/ | `xcodebuild test -scheme KsDialogsMauiBridge` | Swift Testing 9 件 / 5 suites 成功 |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 38 件 / 失敗 0 (`MauiDialogCurrentPageRegistrationTests`・`MauiDialogLayoutPassthroughTests` を含む) |
| kmp/ | `./gradlew allTests --rerun-tasks` | iosSimulatorArm64 85 件 + androidHostTest 81 件 / 失敗 0 |
| MAUI 実配置テストホスト (Android) | `maui/KsDialogs.Maui.PlacementHost` を専用 AVD で起動し logcat を読む | SUMMARY 行が passed=7 / failed=0 (7 シナリオすべて PASS) |
| MAUI 実配置テストホスト (iOS) | 同を作業用 Simulator で `simctl launch --console-pty` | SUMMARY 行が passed=7 / failed=0 (7 シナリオすべて PASS) |

`python3 scripts/scenario-id-coverage.py` は「未網羅なし」、`comment-policy-lint.py` は禁止 0 件、診断文言の日本語リテラル検査 (handbook cross/diagnostic-message-language.md の grep) は出力なし。

初回の instrumented 実行は AVD の画面が消灯していたため `:ksdialogs` が `No screen is available to present the Dialog.` で全件失敗した。画面を点けて (`input keyevent KEYCODE_WAKEUP` / `svc power stayon true`) 再実行した結果が上表で、実装の問題ではない。

## 判定

VALID — 全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」(deviation 3 項は記述と実装が 1 対 1 で一致)。虚偽チェック・逆流・未記録乖離なし、全ルートのテストが成功。
