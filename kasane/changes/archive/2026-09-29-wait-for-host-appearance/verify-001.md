# Verify 001: wait-for-host-appearance

- 判定: **VALID**
- 検証日: 2026-09-28
- 対象: デルタスペック 8 能力 (dialog-contract・loading-contract・toast-contract・ios-native・android-native・maui-binding・kmp-facade・samples) の全 Requirement / Scenario と、コミット e2d4ac9 から作業ツリーまでの未コミットの変更すべて (`git diff e2d4ac9` の 97 ファイルと、`git status --short` の未追跡 43 件)
- 合意済みの差分: `deviation.md` の 5 項目 (Decision 4 の列の持ち主・列の入口までの順序と `@_spi` の順番札・PB-HW-06 の configure 中断時の順序・Decision 3 の器を載せられなかった場合の結末・[付随修正] Toast スイートの並列の間欠失敗)

## 1. テストの実行結果

### 1.1 この検証で実行したもの (端末を使わないもの)

| 対象 | コマンドの形 | 結果 |
|---|---|---|
| 仕様とテストの対応 | `python3 scripts/scenario-id-coverage.py --require-mirror` | 未網羅なし。両 Native ミラー (PB-HW・LD-HW・TS-HW を含む) OK |
| Android unit | `android/` で `./gradlew :ksdialogs-core:testDebugUnitTest --rerun-tasks` | 90 件、失敗 0、skip 0 (PB-HW-01〜08・PB-HA-01〜03・DM-AN-01 を含む) |
| MAUI managed | `maui/` で `dotnet test KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj` | 206 件、失敗 0、skip 0 (PB-MC-02・03・04・08、PB-MH-01、MB-MA-16 を含む) |
| MAUI 公開面の compile 検査 | `maui/` で `dotnet build KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj` | 成功 (警告 0・エラー 0)。PB-MC-01 の正の検査を含む |
| MAUI Android ブリッジ | `maui/android/native` で `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 41 件、失敗 0 (PB-MC-05・PB-MC-07・MB-MA-15 を含む) |
| KMP androidHostTest | `kmp/` で `./gradlew testAndroidHostTest --rerun-tasks` | 83 件、失敗 0 (DM-KM-02・PB-KC-04・PB-KT-09 を含む) |
| 標準 lint | `python3 scripts/local-path-lint.py`・`python3 scripts/identity-lint.py` | どちらも exit 0 |
| DM-KM-04 の静的 grep | `kmp/ksdialogs-kmp/src/{commonMain,androidMain,iosMain}/` のコメント以外の日本語文字列リテラル | 0 件。3 定数の英語文言も表のとおり (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt:98-99`・`IosLoadingGateway.kt:165`) |

### 1.2 前提として受け入れたもの (端末を使うため実行しない)

コンテキストパッケージの指示と `evidence/test-run-summary.md` に従い、次を成功として受け入れた: iOS (`xcodebuild test`) 354 件、MAUI iOS ブリッジ 17 件、KMP iosTest 86 件、Android instrumented 412 件 (skip 1 は既知の PB-SB-04)、MAUI 実配置テストホスト iOS 16 / Android 17 件。

## 2. 対応表

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離。実装・テストのパスはリポジトリ相対。

### 2.1 dialog-contract

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (MODIFIED) 呼び出しコンテキストの契約 | iOS `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:85-93` (紐付けの後の待ち)・`DialogHostWaitQueue.swift:60-109`。Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:84-89`・`DialogHostWaitQueue.kt:51`。旧来の即失敗 (`presentationHostUnavailable` / `PresentationHostUnavailable`) は両 Native のソース・テストに残っていない | 下記 | ✅ |
| UI スレッド外からの show が成立する (ID なし・既存) | 変更なし | `ios/Tests/KsDialogsTests/DialogCallContextTests.swift:10`・`android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogCallContextTests.kt:21` | ✅ |
| PB-HW-01 提示先が無い間は待ち、現れたら表示する | 同上 | `ios/Tests/KsDialogsTests/DialogCallContextTests.swift:57`・`android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogCallContextTests.kt:44` | ✅ |
| (ADDED) 提示先の出現を待つ Dialog の結末 | iOS: 登録の解決を待ちの前 `DialogPresenter.swift:33`、紐付けを待ちの前 `:72-81`、待ち `:85`、中身の生成を待ちの後 `:97`、列の先頭 1 枚ずつ `DialogHostWaitQueue.swift:149-159`、前の提示の完了後に次を明ける `DialogPresenter.swift:125-127`。Android: 同じ順 `DialogPresenter.kt:70-96`、器のウィンドウ追加後に次を明ける `:102`、列 `DialogHostWaitQueue.kt:138`。列はアプリ全体で 1 つ (⚠️ deviation: Decision 4 の列の持ち主) | 下記 | ✅ (⚠️ 列の持ち方は deviation 記録済み) |
| PB-HW-02 待っている間の打ち切り | iOS `DialogHostWaitQueue.swift:90-93` (打ち切りを cancelled で確定)、Android `DialogHostWaitQueue.kt` の `waitForTurn` の CancellationException 経路 | `ios/Tests/KsDialogsTests/DialogHostWaitTests.swift:45`・`android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogHostWaitTests.kt:54` | ✅ |
| PB-HW-03 待っている間の VM の報告 | iOS `DialogResultChannel.swift` の `observeSettlement`・`DialogHostWaitQueue.swift:84-88`、Android `DialogHostWaitQueue.kt` の `resultChannel.onSettle` | `DialogHostWaitTests.swift:67`・`DialogHostWaitTests.kt:78` | ✅ |
| PB-HW-04 未登録は待たずに失敗 | iOS `DialogPresenter.swift:33-35`、Android `DialogPresenter.kt:71` | `DialogHostWaitTests.swift:88`・`DialogHostWaitTests.kt:100` | ✅ |
| PB-HW-05 待っている間の再 show は「表示中」 | iOS `DialogPresenter.swift:72-78`、Android `DialogPresenter.kt:78-80` | `DialogHostWaitTests.swift:101`・`DialogHostWaitTests.kt:113` | ✅ |
| PB-HW-06 呼んだ順に表示、後が手前 | 上記の列。iOS の入口の順序保証は `Dialog.swift:35` ほかの `nonisolated(nonsending)`・`DialogPresenter.swift:11` の `reserveTurn`・`ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:97` (⚠️ deviation: 列の入口までの順序)。configure 中断時の順序は保証しない (⚠️ deviation: PB-HW-06 の型指定 show) | `DialogHostWaitTests.swift:123`・`:154`、`ios/Tests/KsDialogsTests/KsDialogsKmpFacadeTests.swift:276`、`DialogHostWaitTests.kt:133`、`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogHostWaitPresentationTests.kt:39`。実 UIKit の連続提示は `evidence/ios-dialog-sequential-presentation.md` と `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeHostWaitTests.swift` (tasks 5.4) | ⚠️ deviation 記録済み (範囲内は ✅) |
| PB-HW-08 待っている Dialog があるうちの show は後ろに並ぶ | iOS `DialogHostWaitQueue.swift:73` (列・提示中の番・未着の札があれば待つ)、Android `DialogHostWaitQueue.kt` の `waitForTurn` 冒頭 | `DialogHostWaitTests.swift:182`・`DialogHostWaitTests.kt:175` | ✅ |
| PB-HW-07 型指定 show の VM factory と configure は待つ前 | 両 Native の型指定 show が生成・configure の後に Presenter へ入る (待ちは Presenter 内) | `DialogHostWaitTests.swift:265`・`DialogHostWaitTests.kt:213` | ✅ |

### 2.2 loading-contract

| Requirement / Scenario | 実装 | テスト (iOS / Android) | 状態 |
|---|---|---|---|
| (ADDED) 提示先が無いまま始まった Loading の表示 | iOS `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:155` (`beginUse`: 開始時点で登録を解決、提示先があれば開始時点で生成)・`:261` (`waitForHost`: 中身の指定と VM を控える)・`:277-291` (出現で生成し、失敗は警告ログで表示だけを諦める)・`:294` / `:326` (終了・hide で待ちを解除)。Android は実装変更なし (tasks 6.5 の見込みどおり、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:417-421`・`:461`) | 下記 | ✅ |
| LD-HW-01 処理中なら入りのフックを経て表示 | 同上 | `ios/Tests/KsDialogsTests/LoadingHostWaitTests.swift:23` / `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingHostWaitTests.kt:49` | ✅ |
| LD-HW-02 先に処理が終われば表示しない | 同上 | `LoadingHostWaitTests.swift:66` / `LoadingHostWaitTests.kt:94` | ✅ |
| LD-HW-03 先に hide されれば表示しない | 同上 | `LoadingHostWaitTests.swift:87` / `LoadingHostWaitTests.kt:113` | ✅ |
| LD-HW-04 出現時の生成失敗は表示だけを諦める | 同上 | `LoadingHostWaitTests.swift:109` / `LoadingHostWaitTests.kt:131` | ✅ |
| LD-HW-05 表示の前の進捗も VM へ届く | 同上 | `LoadingHostWaitTests.swift:146` / `LoadingHostWaitTests.kt:175` | ✅ |
| LD-HW-07 未登録は開始の時点で失敗 | 同上 | `LoadingHostWaitTests.swift:201` / `LoadingHostWaitTests.kt:228` | ✅ |
| LD-HW-06 提示先があれば生成失敗は開始の失敗 | 同上 | `LoadingHostWaitTests.swift:181` / `LoadingHostWaitTests.kt:210` | ✅ |
| (前提) LD-CO-14 は変えない | action の実行の検査を残し、期待を「提示先が現れるまでは表示されない」に改訂 (tasks 4.2) | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift` | ✅ |

### 2.3 toast-contract

| Requirement / Scenario | 実装 | テスト (iOS / Android) | 状態 |
|---|---|---|---|
| (ADDED) Toast の中身は取り付けの時点で作る | iOS `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:95` (登録は受理の時点で解決)・`:168-190` (`attachIfPossible`: 期限を確かめてから中身を作る)・`:228` (提示面の口で待つ)。`ToastDisplay.swift` は中身ではなく指定を持つ。`NotificationCenter` の直接購読は削除済み。Android は実装変更なし (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:146`・`:250-254`) | 下記 | ✅ |
| TS-HW-01 出現時に表示、受理時点からの duration で消える | 同上 | `ios/Tests/KsDialogsTests/ToastContractTests.swift:226` / `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastContractTests.kt:234` | ✅ |
| TS-HW-02 満了した型指定経路は VM factory・configure・View factory とも呼ばれない | 同上 | `ToastContractTests.swift:265` / `ToastContractTests.kt:268` | ✅ |
| TS-HW-03 期限を過ぎた保留表示は出現が先でも表示されない | 同上 | `ToastContractTests.swift:295` / `ToastContractTests.kt:299` | ✅ |

### 2.4 ios-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (ADDED) 合図は提示先を選ぶ規則の隣で 2 つの通知から作る | `ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:26-47` (`UIWindow.didBecomeKeyNotification`・`UIScene.didActivateNotification`)、protocol `DialogKeyWindowProvider.swift:19-21`、3 機能の提示面の中継 (`DialogPresentationSurface.swift`・`LoadingPresentationSurface.swift`・`ToastPresentationSurface.swift`、`UIKitDialogPresentationSurface.swift:118-121`)。この 2 通知の購読は ios/Sources 内で provider の 1 か所だけ | 下記 | ✅ |
| PB-HI-01 2 つの通知で合図が届く・解除後は届かない | 同上 | `ios/Tests/KsDialogsTests/DialogHostAppearanceTests.swift:18` | ✅ |
| PB-HI-02 条件を満たさない合図では待ち続け、満たした合図で 3 機能とも表示 | 同上 | `DialogHostAppearanceTests.swift:42` | ✅ |
| PB-HI-03 待っている表示が無くなると購読が解除される | `ToastCoordinator.swift:246`・`LoadingCoordinator.swift:294`・`DialogHostWaitQueue.swift:162-166` | `DialogHostAppearanceTests.swift:154` | ✅ |
| (ADDED) 提示先が無いことを表す失敗は公開 API に無い | `ios/Sources/KsDialogs/Contract/DialogError.swift` から case 削除。`KsDialogsKmpError.swift` の理由の欠けた失敗の代替は `ios/Sources/KsDialogs/Kmp/KsDialogsKmpMissingFailureReason.swift` | `KsDialogsKmpFacadeTests.swift:353` (代替値の固定) | ✅ |
| PB-HI-04 `DialogError` の網羅 switch | 同上 | `DialogHostAppearanceTests.swift:125` | ✅ |
| (MODIFIED) iOS の失敗型メッセージは英語固定 | `DialogError.swift` (6 case) | 下記 | ✅ |
| DM-IO-01 6 case の英語文言 | 同上 | `ios/Tests/KsDialogsTests/DiagnosticMessageTests.swift:12` | ✅ |
| DM-IO-02 `KsDialogsKmpError` 2 case | 変更なし | `DiagnosticMessageTests.swift:51` | ✅ |

### 2.5 android-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (ADDED) Dialog の提示面は入れ替わりの購読口を持つ | `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresentationSurface.kt` (`observeHostChange`・`hostWaitQueue`)、`ActivityDialogPresentationSurface.kt:14`・`:48-51`。提示先の無い提示の要求は例外を投げず cancelled で確定 (`:43`、⚠️ deviation: Decision 3 の器を載せられなかった場合) | 下記 | ✅ |
| PB-HA-01 resume で表示、購読解除 | 同上 | `android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogActivityHostWaitTests.kt:78` | ✅ |
| PB-HA-02 破棄の通知だけでは表示されず、次の resume で表示 | 同上 | `DialogActivityHostWaitTests.kt:102` | ✅ |
| (ADDED) 提示先が無いことを表す例外は公開 API に無い | `DialogException.kt` からサブクラス削除 (sealed class) | 下記 | ✅ |
| PB-HA-03 網羅 when | 同上 | `android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogExceptionMessageTests.kt:49` | ✅ |
| (MODIFIED) Android の失敗型メッセージは英語固定 / DM-AN-01 (4 サブクラス) | `DialogException.kt` | `DialogExceptionMessageTests.kt:17` | ✅ |

### 2.6 kmp-facade

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (ADDED) 共有コードから待っている Dialog を打ち切れる | KMP 側に待ちの仕組みを足さず Native に委譲 (kmp の main は doc のみ変更)。iOS は互換面のハンドルの打ち切りが Presenter の Task を止める | 下記 | ✅ |
| PB-KC-04 打ち切りでキャンセルが伝播、iOS はハンドルの打ち切りが呼ばれる | 同上 | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt:163`・`kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidDialogGatewayContractTests.kt:238` | ✅ |
| (MODIFIED) KMP iOS ホストの gateway 固有メッセージは英語固定 | 定数は変更なし (`IosDialogGateway.kt:98-99`・`IosLoadingGateway.kt:165`) | 下記 | ✅ |
| DM-KM-01 互換面: 未登録は英語文言、登録済みは待ち打ち切りで cancelled | 同上 | `InteropBridgeContractTests.kt:86` | ✅ |
| DM-KM-03 共有コード: 未登録は英語文言素通し、登録済みは待ちキャンセル伝播 | 同上 | `InteropBridgeContractTests.kt:120` (登録済み)・`:132` (未登録) | ✅ |
| DM-KM-04 日本語リテラルが残らない | 静的 grep (除外表の既存登録で受け入れ) | 本検証で grep 0 件を確認 (1.1) | ✅ |
| (MODIFIED) KMP Android ホストは Native の英語文言を素通しする / DM-KM-02 | 変更なし | `AndroidDialogGatewayContractTests.kt:171` (文言の完全一致と cause の型、登録済みは待ちキャンセル) | ✅ |
| (MODIFIED) 共有コードの型指定 show — PB-KT-03〜08・13・14・LD-KT-02・TS-KT-01 | 変更なし | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowTests.kt:85`・`:109`・`:131`・`:147`・`:163`・`:185`・`:210`・`:271`、`LoadingTypedShowTests.kt:44`・`:80`・`:104`・`:142`・`:186`、`ToastTypedShowTests.kt:39`・`:69`・`:85`、`kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt:23` | ✅ |
| PB-KT-09 両 OS の gateway が型指定 show の VM を Native へ渡し、提示先を待つ | 同上 | `InteropBridgeContractTests.kt:147`・`InteropLoadingBridgeContractTests.kt:117`・`InteropToastBridgeContractTests.kt:79`、`AndroidDialogGatewayContractTests.kt:198`・`:216`、`AndroidLoadingGatewayContractTests.kt:243`、`AndroidToastGatewayContractTests.kt:145`。Toast / Loading の iosTest は design Decision 9 の判定材料 (互換面の受理の同期の結果・進捗の到達) に書き直し済み (tasks 7.4: `InteropToastBridgeContractTests.kt:41`・`:61`、`InteropLoadingBridgeContractTests.kt:73`・`:95`) | ✅ |

### 2.7 maui-binding

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (ADDED) Dialog の show は呼び出し元の打ち切りを受け付ける | `maui/KsDialogs.Maui/Presentation/IKsDialog.cs` の `ShowAsync` 7 本に `CancellationToken cancellationToken = default`、`Presentation/Dialog.cs:150` と `Internals/DialogPresenter.cs:97` (呼び出し時点の打ち切り済み判定)・`:122-126` (打ち切りを `OperationCanceledException` へ)、`Internals/DialogCallerCancellation.cs:53` (ハンドル取得前後の打ち切りの直列化)、両 OS の `Platforms/*/PlatformDialogGateway.cs:27`、iOS ブリッジ `maui/macios/native/KsDialogsMauiBridge/MauiDialogPresentation.swift:22`・`:40`・`:50`・`:61`、`MauiDialogBridge.swift:24`、Android ブリッジ `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogPresentation.kt:24`・`:45`・`:57`・`:68`、`MauiDialogBridge.kt:31`・`:63`・`:100-106`。binding の `Cancel` は `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs` | 下記 | ✅ |
| PB-MC-01 公開面の正の compile 検査 | 同上 | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogCancellationApiSurfaceChecks.cs` (7 本 × token なし / 位置引数 / 名前付き)。負の検査 `NegativeChecks/RejectsOptionsArgumentOnShow.cs`・`RejectsTransitionArgumentOnShow.cs` は据え置き | ✅ |
| PB-MC-02 待っている間の打ち切り | 同上 | `maui/KsDialogs.Maui.Tests/DialogCallerCancellationTests.cs:37` | ✅ |
| PB-MC-03 表示中の打ち切り | 同上 | `DialogCallerCancellationTests.cs:67` | ✅ |
| PB-MC-04 打ち切り済みなら何もしない (インスタンス渡し・型指定) | 同上 | `DialogCallerCancellationTests.cs:97` | ✅ |
| PB-MC-08 ハンドル取得前の打ち切りを取りこぼさない | 同上 | `DialogCallerCancellationTests.cs:165` | ✅ |
| PB-MC-05 Android ブリッジは cancelled を通知してから投げ直す。提示先不在の通知は無い | `MauiDialogClosureListener.kt` から `onPresentationHostUnavailable` を削除 | `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogCancellationTests.kt:64` (cancelled 1 回)、伝播は `MauiDialogClosureReportTests.kt` の「コルーチンのキャンセルは cancelled として通知してから伝播する」 | ✅ |
| PB-MC-06 iOS ブリッジは cancelled を届ける。閉鎖種別に提示先不在は無い | `MauiDialogClosure.swift`・`maui/macios/KsDialogs.Binding.iOS/StructsAndEnums.cs` から種別を削除 | `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeHostWaitTests.swift:122`・`:154` | ✅ |
| PB-MC-07 中身を作る前に閉じると一度も表示されない | 同上 | `MauiDialogCancellationTests.kt:86`・`MauiBridgeHostWaitTests.swift:210` | ✅ |
| (ADDED) MAUI は提示先を事前に判定せず、中身の供給の時点で文脈を解決 | 両 OS の `PlatformDialogGateway.cs` から事前判定を削除、`Platforms/*/PlatformDialogContent.cs` の `CreateInPresentationContext`、`Internals/DialogPresentationTarget.cs:37` (`InvalidOperationException`)、Loading・Toast の gateway も同じ口へ。`PresentationHostUnavailable` は `Internals/HostlessDialogGateway.cs:17` だけが使う | 下記 | ✅ |
| PB-MH-01 素の .NET はその場で失敗 | 同上 | `maui/KsDialogs.Maui.Tests/DialogCallContextTests.cs:68` | ✅ |
| PB-MH-02 文脈が取れなければ `InvalidOperationException` | `DialogPresentationTarget.cs:37` | 除外表で「コードレビューで受け入れる」。コードで確認済み (§3)。文言は `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs` の ID なしのテストでも固定 | ✅ |
| (MODIFIED) Android の managed/native 境界での失敗の受け止め / MB-MA-15 | 変更なし | `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogLoadingContentSupplyTests.kt:55`・`:78` | ✅ |
| MB-MA-16 既存の失敗経路 (素の .NET と利用者キャンセル) | 同上 | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:168` | ✅ |

### 2.8 samples

| Requirement / Scenario | 実装 | テスト / 受け入れ | 状態 |
|---|---|---|---|
| (ADDED) iOS 系 Sample の自動再生は最初の画面の表示時に始める | `samples/ios/KsDialogsSample/SampleMenuScreen.swift`・`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift` の `.onChange(of: scenePhase ...)` を `.onAppear { Task { await autoPlay() } }` へ | 下記 | ✅ |
| CA-SA-08 起動直後の自動再生で指定デモが出る | 同上 | 除外表で「実機証跡で受け入れる」。`evidence/after-fix-measurement.md` の 9.3 表で ios・kmp (iOS) の 4 デモ + `custom-toast` が全回表示、落ちた回なし (MAUI iOS / Android も同じ)。画像 `evidence/after-*.png` 20 枚 | ✅ |
| CA-SA-09 シーンの状態を待たない | 同上 | 除外表で「コードレビューで受け入れる」。両ファイルに `scenePhase`・`activationState`・`keyWindow`・`isKeyWindow`・`connectedScenes` が無いことを grep で確認 | ✅ |

## 3. 除外表 (`DEFAULT_ALLOW_MISSING`) の 3 ID の受け入れ手段

| ID | 登録 (`scripts/scenario-id-coverage.py`) | 受け入れ手段の存在 |
|---|---|---|
| CA-SA-08 | 理由付きで登録済み | `evidence/after-fix-measurement.md` (直す前 `evidence/before-fix-measurement.md` と並べた表・手順・画像一覧) が存在する |
| CA-SA-09 | 理由付きで登録済み | 上表のとおりコードで確認 |
| PB-MH-02 | 理由付きで登録済み | `maui/KsDialogs.Maui/Internals/DialogPresentationTarget.cs:37` の `DialogPresentationContext.Require` が `target?.MauiContext ?? throw new InvalidOperationException(UnavailableMessage)` で、両 OS の Dialog・Loading・Toast の供給がすべて `PlatformDialogContent.CreateInPresentationContext` を通る。`DialogException.PresentationHostUnavailable` は供給経路で使われていない |

## 4. 追加検査

- [x] **tasks.md**: 全タスク (1.1〜9.4) がチェック済み。対応表と突き合わせて、未実装のチェック済みは無い。5.4 は `evidence/ios-dialog-sequential-presentation.md`、1.2・1.3 は `evidence/before-fix-measurement.md`・`evidence/host-appearance-order.log`、9.2 は `evidence/test-run-summary.md`、9.3 は `evidence/after-fix-measurement.md`、9.4 は本検証の lint 実行 (1.1) で裏付けた
- [x] **逆流検査**: e2d4ac9 以降のコミットは無く、`git diff e2d4ac9 -- kasane/` の変更は `tasks.md` のみ。その差分はチェックボックスの `[ ]` → `[x]` だけ (本文の変更なし)。proposal・design・specs は未変更
- [x] **未記録乖離**: ❌ は 0 件
- [x] **付随修正**: deviation の [付随修正] (Toast スイートの並列の間欠失敗) に対応する diff は `ios/Tests/KsDialogsTests/Support/MainActorResponsiveTrait.swift` (新規) と Toast 系スイートの trait 付与 (`ToastAccessibilityTests.swift`・`ToastAttributeTests.swift`・`ToastMultiDisplayTests.swift`・`ToastNonModalTests.swift`・`ToastSwiftUIContentTests.swift`・`ToastTransitionTests.swift`・`ToastTypedShowTests.swift`・`KsToastKmpTests.swift` の `@Suite` への `.awaitsMainActorResponsive` の付与、各 1 行)。記録済みなので乖離としない。なお `DialogStatusBarAppearanceTests.swift` の 1 行は付随修正ではなく、提示面の `present` に完了通知の引数を足した (tasks 5.1) ことへの呼び出しの追随
- [x] **deviation の実装照合**: Decision 4 の列 (`DialogHostWaitQueue.application` と Android の `DialogHostWaitQueue.application`)、入口の順序 (`nonisolated(nonsending)`・`reserveTurn`・`@_spi(KsDialogsBridge)` は `ios/Sources/KsDialogs/Presentation/Dialog.swift:125`・`:133`・`DialogShowReservation.swift:9`、MAUI iOS ブリッジ `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:2`)、Decision 3 の器を載せられなかった場合 (iOS `DialogPresenter.swift:125-133`・`UIKitDialogPresentationSurface.swift:43`・`:93`、Android `ActivityDialogPresentationSurface.kt:43`、テスト `ios/Tests/KsDialogsTests/DialogPresentationRefusalTests.swift`・`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationRefusalTests.swift`) は、いずれも記録の内容どおりに実装されている
- [x] **UI 変更**: ui/ アーティファクトを持たない change のため対象外
- [x] **テスト**: 端末を使わないものは本検証で実行して失敗 0 (1.1)。端末を使うものは前提として受け入れた (1.2)

## 5. 所見 (判定には影響しない)

判定の根拠にはしないが、レビュー・蒸留・drift で拾えるよう記録する。

1. **Sample のコメントに「提示先不在」の記述が残っている**: `samples/ios/KsDialogsSample/SampleMenuModel.swift:92` ほか同ファイルの計 7 か所、`SampleLayoutPanelModel.swift:49`、`SampleTransitionPanelModel.swift:52` の catch 節のコメント「未登録・提示先不在は Sample の組み立ての誤り」。iOS の Dialog は提示先の不在では失敗しなくなったので、このコメントは古い。コードの振る舞いは samples のデルタの対象外で、Scenario には抵触しない
2. **`evidence/test-run-summary.md` の Android の出現表の区分**: 「Android instrumented」の PB-HW-01〜08 (9 本) と PB-HA-01〜03 は、実際には PB-HW-06 の 1 本 (`DialogHostWaitPresentationTests.kt:39`) を除いて unit (`src/test`) のテスト。件数の合計や成否には影響しない
3. **PB-KC-04 (iosTest) の呼び出し口**: Scenario の GIVEN は `Dialog.instance.show` だが、iosTest は互換面の打ち切りを数えるため `GatewayKsDialog(IosDialogGateway(<記録用の面>))` から呼んでいる (`InteropBridgeContractTests.kt:163`)。`Dialog.instance` と同じ gateway の経路で、実互換面 (`InteropDialogShowSurface(bridge)`) を包んでいる。androidHostTest の PB-KC-04 は `Dialog.instance.show` から呼ぶ
4. **DM-KM-03 は 2 本に分割**: 1 つの Scenario の未登録側と登録済み側を、同じ ID の 2 本のテスト (`InteropBridgeContractTests.kt:120`・`:132`) で検証している。合わせて Scenario の THEN を満たす

## 6. 判定

**VALID**。全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。❌ は 0 件。虚偽チェックなし、足場の逆流なし、実行したテストはすべて成功。
