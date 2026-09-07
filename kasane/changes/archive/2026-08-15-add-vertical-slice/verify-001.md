# 一致検証結果: add-vertical-slice (001 回目)

**日付**: 2026-08-15
**判定**: **INVALID** (❌ 3 件 — うち 1 件はオーナー判断待ちの既知項目、2 件は本検証で独立に検出)

デルタスペック (specs/ 6 能力 / 20 Requirement / 31 Scenario) と実装・テストを機械的に突き合わせた。
品質評価は行っていない (それは review-001 / review-002 の領分)。

## サマリー

- **全 Scenario の 4 形態展開 (計 62 セル)** のうち、59 セルが実装・テストまたは実機証跡で裏付けられている
- **テストは全ルート実行して全件 green** — ios 35 / android 34 / kmp 32 (iosSimulatorArm64 17 + androidHostTest 15) / maui 19 / maui bridge 5。コンテキストパッケージの期待値と一致
- **逆流なし** — proposal / design / specs 6 件は提案作成コミット (36f75f2) 以降 未変更。working tree で変更されているのは実績欄を持つ tasks.md と ui/brief.md のみ
- **tasks.md の虚偽チェックなし** — 全 26 項目に対応する実体を確認した
- **deviation.md は存在しない** ため、下記 ❌ 3 件はいずれも「未記録の乖離」に当たる

## ❌ の要約 (詳細は「未充足項目」節)

| # | 対象 | 種別 |
|---|---|---|
| ❌1 | kmp-facade「Swift async からの直接呼び出し」の**復元失敗の報告** | 既知 (オーナー判断待ち。証跡 review-001 Major 3 / second-opinion-code-001 #1) |
| ❌2 | kmp-facade「Swift async からの直接呼び出し」の Scenario **「Swift から await した結果の型と値が正しい」の実証欠落** | 本検証で検出 |
| ❌3 | samples「Sample の consumer 境界」の**「内部実装を直接参照しない」** (samples/kmp/iosApp が互換面を直接使用) | 本検証で検出 |

---

## 対応表

### dialog-contract (全形態に適用)

Scenario ごとに 4 形態 (iOS Native / Android Native / KMP / MAUI) の対応を並べる。
KMP / MAUI の「N」は Native 継承 + adapter 契約テスト (verification-matrix.md 凡例) を根拠とするセル。

#### Requirement: 型付き結果の show

| Scenario | 形態 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| 完了操作で completed が返る | iOS | `ios/Sources/KsDialogs/Presentation/Dialog.swift:23-43` | `DialogResultRouteTests.swift` 「完了操作で completed が返る」 | ✅ |
| 〃 | Android | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:17-28` | `DialogResultRouteTests.kt` 「完了操作で completed が返る」 | ✅ |
| 〃 | KMP | `kmp/.../commonMain/.../DialogGateway.kt:38-51` (GatewayKsDialogs) | commonTest `DialogResultRouteTests.kt` 「完了操作で completed が返る」(両ターゲット) | ✅ |
| 〃 | MAUI | `maui/KsDialogs.Maui/Presentation/Dialog.cs:42-56` | `DialogResultRouteTests.cs` `CompleteOperationReturnsCompleted` | ✅ |
| キャンセル操作で cancelled が返る | iOS | 同上 + `Contract/DialogNotifier.swift:19-21` | `DialogResultRouteTests.swift` 「キャンセル操作で cancelled が返る」 | ✅ |
| 〃 | Android | 同上 + `DialogNotifier.kt:24-26` | `DialogResultRouteTests.kt` 「キャンセル操作で cancelled が返る」 | ✅ |
| 〃 | KMP | 同上 | commonTest `DialogResultRouteTests.kt` 「キャンセル操作で cancelled が返る」 | ✅ |
| 〃 | MAUI | 同上 + `Contract/DialogNotifier.cs:25` | `DialogResultRouteTests.cs` `CancelOperationReturnsCancelled` | ✅ |
| 外側タップで cancelled が返る (既定) | iOS | `Presentation/DialogContainerViewController.swift:48-50, 72-91` | `DialogResultRouteTests.swift` 「外側タップで cancelled が返る (既定)」/ 実機 `ui/verification/ios-native-outside-tap-cancelled.png` | ✅ |
| 〃 | Android | `DialogContainer.kt:62-65, 100-128` | `DialogResultRouteTests.kt` 「外側タップで cancelled が返る (既定)」/ 実機 `android-native-outside-tap-cancelled.png` | ✅ |
| 〃 | KMP | N (Native の器が報告) | 実機 `kmp-ios-outside-tap-cancelled.png` / `kmp-android-outside-tap-cancelled.png` | ✅ |
| 〃 | MAUI | N (Native の器が報告) + `Platforms/*/PlatformDialogGateway.cs` の閉鎖通知結線 | `DialogResultRouteTests.cs` `OutsideTapReturnsCancelledByDefault` / 実機 `maui-ios-outside-tap-cancelled.png` / `maui-android-outside-tap-cancelled.png` | ✅ |
| (Requirement 本文) 呼び出し側が VM と異なる結果型を指定できる API 面を公開しない | 4形態 | `KsDialogs.swift:13` / `KsDialogs.kt:19` / `kmp/KsDialogs.kt:25` / `IKsDialogs.cs:29` — いずれも結果型は VM から導出 | 負のコンパイル検証: iOS `DialogTypedResultCompileChecks.swift` (`-DKSDIALOGS_NEGATIVE_COMPILE_CHECK`) / android `src/testNegativeCompileCheck/` / kmp `src/commonTestNegativeCompileCheck/` / maui `NegativeCompileChecks/` (`-p:KsDialogsNegativeCompileCheck=true`) | ✅ |

#### Requirement: 契約 interface と既定 singleton の両対応とレジストリ共有

| Scenario | 形態 | 実装 | テスト | 状態 |
|---|---|---|---|---|
| 片方の入口の登録がもう片方から見える | iOS | `Presentation/KsDialogs.swift` (protocol) + `Dialog.swift:6-21` + `Registry/DialogViewRegistry.swift:12` (`shared`) | `DialogRegistryTests.swift` 「片方の入口の登録がもう片方から見える」 | ✅ |
| 〃 | Android | `KsDialogs.kt` + `Dialog.kt:9-33` + `DialogViewRegistry.kt:48-51` (`shared`) | `DialogRegistryTests.kt` 「片方の入口の登録がもう片方から見える」 | ✅ |
| 〃 | KMP | `kmp/KsDialogs.kt:12-26` + `Dialog.kt:10-13` (expect) / `Dialog.android.kt` / `Dialog.ios.kt` | commonTest `DialogEntryPointTests.kt` 「既定エントリと契約 interface 経由でレジストリを共有する」/ androidHostTest 「既定エントリのレジストリは Native ライブラリの共有レジストリを指す」/ iosTest 「既定エントリの show は互換面と同じレジストリを引く」 | ✅ |
| 〃 | MAUI | `IKsDialogs.cs` + `Dialog.cs:13-39` + `DialogViewRegistry.cs:21` (`Shared`) | `DialogRegistryTests.cs` `RegistrationFromOneEntryPointIsVisibleFromTheOther` / `DefaultEntryPointUsesSharedRegistry` | ✅ |

#### Requirement: 結果確定とダイアログの閉鎖

| Scenario | 形態 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| 結果確定で自分のダイアログだけが閉じる | iOS | `DialogPresenter.swift:29-44` + `UIKitDialogPresentationSurface.swift:22-26` | `DialogMultiDisplayTests.swift` 「結果確定で自分のダイアログだけが閉じる」/ 実機再観測 (verification-matrix 実績メモ 2026-08-15) | ✅ |
| 〃 | Android | `DialogPresenter.kt:41-50` + `ActivityDialogPresentationSurface.kt:27-30` | `DialogMultiDisplayTests.kt` 「結果確定で自分のダイアログだけが閉じる」/ 実機 `android-native-md-a-recheck-lower-survived.png` | ✅ |
| 〃 | KMP | N | androidHostTest / iosTest の adapter 契約テスト (委譲 1:1) + 実機 7.2 | ✅ |
| 〃 | MAUI | N + `Platforms/*/PlatformDialogGateway.cs:42-45` (自分の presentation のみ Dismiss) | `DialogGatewayContractTests.cs` `StackedShowsReceiveTheirOwnResults` / `DelegatedContentAndChannelBelongToTheSameShow` + 実機 7.2 | ✅ |
| (Requirement 本文) プログラムから閉じる公開 API を提供しない | 4形態 | 公開契約は registry + show の 2 面のみ (`KsDialogs.swift:5-14` / `KsDialogs.kt:8-20` / `kmp/KsDialogs.kt:12-26` / `IKsDialogs.cs:12-30`) | 公開面の目視確認 (dismiss 相当の公開 API なし) | ✅ |

#### Requirement: 呼び出しコンテキストの契約

| Scenario | 形態 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| UI スレッド外からの show が成立する | iOS | `DialogPresenter.swift:8` (`@MainActor` 境界で提示へマーシャリング) | `DialogCallContextTests.swift` 「UI スレッド外からの show が成立する」 | ✅ |
| 〃 | Android | `DialogPresenter.kt:24` (`withContext(Dispatchers.Main.immediate)`) | `DialogCallContextTests.kt` 「UI スレッド外からの show が成立する」 | ✅ |
| 〃 | KMP | N — `GatewayKsDialogs` / `AndroidDialogGateway` / `IosDialogGateway` はスレッドを変えず素通しし、マーシャリングは Native が担う | adapter 契約テスト (委譲 1:1) + Native 側の同名テスト | ✅ (KMP 固有の自動テストはなし。Native 継承として成立) |
| 〃 | MAUI | `Platforms/*/PlatformDialogGateway.cs:30-39` (`MainThread.InvokeOnMainThreadAsync` 内で文脈解決 + Present) | `DialogCallContextTests.cs` `ShowFromWorkerThreadSucceeds` + 実機ワーカースレッド実測 (verification-matrix 実績メモ 2026-08-15) | ✅ |
| 提示 host 不在の show は即失敗する | iOS | `DialogPresenter.swift:18-20` + `ApplicationKeyWindowProvider.swift:22-28` | `DialogCallContextTests.swift` 「提示 host 不在の show は即失敗する」/ `ApplicationKeyWindowProviderTests.swift` 4 件 | ✅ |
| 〃 | Android | `DialogPresenter.kt:30-32` + `ActivityDialogPresentationSurface.kt:14-18` | `DialogCallContextTests.kt` 「提示 host 不在の show は即失敗する」 | ✅ |
| 〃 | KMP | `AndroidDialogGateway.kt:28-31` / `IosDialogGateway.kt:50-52` (Native の失敗を `DialogException` へ載せ替え) | commonTest 「構成エラーでは結果を返さずに失敗する」/ iosTest 「既定エントリの show は互換面と同じレジストリを引く」(提示先不在の失敗で判定) | ✅ |
| 〃 | MAUI | `Internals/HostlessDialogGateway.cs:15-16` + `Platforms/*/PlatformDialogGateway.cs:33-35` | `DialogCallContextTests.cs` `ShowWithoutPresentationHostFailsImmediately` / bridge `提示先不在はそれと分かる形で通知される` | ✅ |

#### Requirement: 結果はちょうど1回だけ確定する

| Scenario | 形態 | 実装 | テスト | 状態 |
|---|---|---|---|---|
| 確定後の再報告は無効 | iOS | `Contract/DialogResultChannel.swift:15-30` (NSLock + isSettled) | `DialogNotifierTests.swift` 「確定後の再報告は無効」/「受け取り側の登録より先に確定した結果も1回だけ届く」 | ✅ |
| 〃 | Android | `DialogResultChannel.kt:21-35` | `DialogNotifierTests.kt` 同名 2 件 | ✅ |
| 〃 | KMP | N (互換面 / Native lib が保持) | iosTest `InteropBridgeContractTests` + androidHostTest adapter 契約テスト | ✅ |
| 〃 | MAUI | `Contract/DialogResultChannel.cs` (`TrySetResult`) | `DialogResultRouteTests.cs` `ReportsAfterSettlementAreNoOp` / bridge `閉鎖の通知はちょうど1回だけ届く` 5 件 | ✅ |

#### Requirement: VM 型キーによる View 解決と毎回生成

| Scenario | 形態 | 実装 | テスト | 状態 |
|---|---|---|---|---|
| 登録済み VM 型の show で View が表示される | iOS | `Registry/DialogViewRegistry.swift:21-44` + `DialogViewModelKey.swift:5-11` | `DialogRegistryTests.swift` 「登録済み VM 型の show で View が表示される」 | ✅ |
| 〃 | Android | `DialogViewRegistry.kt:29-46` (KClass キー) | `DialogRegistryTests.kt` 同名 | ✅ |
| 〃 | KMP | N — レジストリ実体は Native 側 1 個 (`DialogViewRegistry.kt` commonMain はハンドル) | iosTest 「Swift 側登録の View factory が共有コードの ViewModel で解決される」/ androidHostTest 「共有コードの ViewModel が Native レジストリのキーとして通用する」 | ✅ |
| 〃 | MAUI | `Registry/DialogViewRegistry.cs:37-61` (Type キー) | `DialogRegistryTests.cs` `RegisteredViewModelTypeResolvesItsView` | ✅ |
| 未登録 VM 型の show は即エラー | iOS | `DialogPresenter.swift:15-17` (View 生成前に throw) | `DialogRegistryTests.swift` 「未登録 VM 型の show は即エラー」/ `KsDialogsInteropBridgeTests.swift` 「未登録の ViewModel は互換面では error 判別で返る」 | ✅ |
| 〃 | Android | `DialogPresenter.kt:26-29` | `DialogRegistryTests.kt` 「未登録 VM 型の show は即エラー」 | ✅ |
| 〃 | KMP | `IosDialogGateway.kt:50-52` / `AndroidDialogGateway.kt:28-31` (NSError → `DialogException`) | iosTest 「未登録の ViewModel の show は結果を返さずに失敗する」 | ✅ |
| 〃 | MAUI | `Internals/DialogPresenter.cs:28-32` | `DialogResultRouteTests.cs` `ShowWithUnregisteredViewModelTypeFailsImmediately` | ✅ |
| show ごとに View は新規生成される | iOS | `DialogViewRegistry.swift:25-28` (factory を毎回呼ぶ) | `DialogRegistryTests.swift` 「show ごとに View は新規生成される」 | ✅ |
| 〃 | Android | `DialogViewRegistry.kt:33-38` | `DialogRegistryTests.kt` 同名 | ✅ |
| 〃 | KMP | N | iosTest 「互換面へ渡す View factory は実体のある View を返す」+ Native 継承 | ✅ |
| 〃 | MAUI | `DialogViewRegistry.cs:42-44` + `DialogPresenter.cs:36` | `DialogRegistryTests.cs` `EachShowCreatesItsOwnView` | ✅ |
| 同一 VM インスタンスの再 show は独立した重ね出しになる | iOS | `DialogPresenter.swift:22-27` (show ごとに channel / container 新規) | `DialogRegistryTests.swift` 「同一 VM インスタンスの再 show は独立した重ね出しになる」 | ✅ |
| 〃 | Android | `DialogPresenter.kt:34-40` | `DialogRegistryTests.kt` 同名 | ✅ |
| 〃 | KMP | N | commonTest 「重ねて show しても各呼び出しが自分の結果を受け取る」 | ✅ |
| 〃 | MAUI | `DialogPresenter.cs:34-38` | `DialogRegistryTests.cs` `ReshowingSameViewModelInstanceStacksIndependently` | ✅ |

#### Requirement: 多段表示の基本保証

| Scenario | 形態 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| MD-a 2枚重ねて上から順に閉じる | iOS | `DialogContainerViewController.swift` (`.overFullScreen` 重ね提示) + `UIKitDialogPresentationSurface.swift:29-38` | `DialogMultiDisplayTests.swift` 「MD-a 2枚重ねて上から順に閉じる」/ 実機 `ios-native-md-a-*.png` | ✅ |
| 〃 | Android | `DialogContainer.kt` (1枚=1ウィンドウ) | `DialogMultiDisplayTests.kt` 「MD-a 2枚重ねて上から順に閉じる」/ 実機 `android-native-md-a-*.png` | ✅ |
| 〃 | KMP | N | adapter 契約テスト (重ねた show が各自の結果を受け取る) | ✅ |
| 〃 | MAUI | N | `DialogGatewayContractTests.cs` `StackedShowsReceiveTheirOwnResults` | ✅ |
| MD-c 重ね出し中の外側タップは手前のみ | iOS | `DialogContainerViewController.swift:72-91` | `DialogMultiDisplayTests.swift` 「MD-c 重ね出し中の外側タップは手前のみ」/ 実機 `ios-native-md-c-outside-tap.png` | ✅ |
| 〃 | Android | `DialogContainer.kt:62-65, 100-128` | `DialogMultiDisplayTests.kt` 同名 / 実機 `android-native-md-c-outside-tap.png` | ✅ |
| 〃 | KMP | N | 単独表示の外側タップは実機 6 セルで確認 (`kmp-*-outside-tap-cancelled.png`)。重ね出しは Native 2 セル基準 (シナリオ表 §5 の規約) | ✅ |
| 〃 | MAUI | N | 同上 (`maui-*-outside-tap-cancelled.png`) | ✅ |
| (Requirement 本文) 重なりの枚数・一覧を管理せず公開しない | 4形態 | 公開契約に枚数・一覧の API なし | 公開面の目視確認 | ✅ |

### ios-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **Swift 公開 API での貫通** — `async throws` + enum 結果 / メタタイプキー | `Presentation/KsDialogs.swift:13` (`async throws -> DialogResult<ViewModel.Result>`) / `Contract/DialogResult.swift:3-8` (enum) / `Registry/DialogViewModelKey.swift:5-11` | — | ✅ |
| Scenario: Swift から show して結果を受け取る | 同上 | `DialogResultRouteTests.swift` 「Swift から show して結果を受け取る」 | ✅ |
| **KMP 委譲向け互換面の提供** — `@objc` / 型消去輸送 / 公開 API と分離 / exactly-once 素通し | `Interop/KsDialogsInteropBridge.swift:13-65` (`@objc(KSDInteropDialogBridge)`) / `KsDialogsInteropResult.swift:8-36` (kind + value + error) / `KsDialogsInteropNotifier.swift:9-29` | — | ✅ |
| Scenario: ObjC 互換面経由の呼び出しが同一レジストリに到達する | `KsDialogsInteropBridge.swift:34-44` (同一 `DialogViewRegistry.shared`、同一 `DialogViewModelKey` 空間) | `KsDialogsInteropBridgeTests.swift` 「ObjC 互換面経由の呼び出しが同一レジストリに到達する」 | ✅ |
| Scenario: 互換面経由でも結果は1回だけ届く | `KsDialogsInteropNotifier.swift:20-27` → `DialogResultChannel.swift:15-30` | `KsDialogsInteropBridgeTests.swift` 「互換面経由でも結果は1回だけ届く」 | ✅ |

### android-native

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| **Kotlin 公開 API での貫通** — suspend + sealed / KClass キー | `KsDialogs.kt:19` (`suspend fun <R> show(...): DialogResult<R>`) / `DialogResult.kt:10-19` (sealed) / `DialogViewRegistry.kt:29-31` | — | ✅ |
| Scenario: Kotlin から show して結果を受け取る | 同上 | `DialogResultRouteTests.kt` 「Kotlin から show して結果を受け取る」 | ✅ |
| **戻るボタンによるキャンセル (通常時)** | `DialogContainer.kt:41, 45, 67-70` (`setCancelable(true)` + `setOnCancelListener` → cancelled) | `DialogBackPressTests.kt` 「戻るボタンで cancelled が返る (キーボード非表示時)」 | ✅ |
| Scenario: 戻るボタンで cancelled が返る (キーボード非表示時) | 同上 | 上記 + 実機 `android-native-back-cancelled.png` / `maui-android-back-cancelled.png` / `kmp-android-back-cancelled.png` | ✅ |
| (本文) MD-d の実挙動記録が存在すること | — | `common-spec-scenarios.md` §5 MD-d に実機記録あり (証跡 `android-native-md-d-*.png` 3 枚) | ✅ |

### kmp-facade

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| **commonMain からの show 貫通** | `commonMain/KsDialogs.kt:12-26` + `DialogResult.kt` + `DialogViewRegistry.kt` + `Dialog.kt:10-13` (expect object) | — | ✅ |
| Scenario: 共有コードからの show が Android で動作する | `androidMain/Dialog.android.kt:9-12` + `AndroidDialogGateway.kt:17-31` + `DialogViewModel.android.kt:9` (typealias) | androidHostTest `AndroidDialogGatewayContractTests` 5 件 / 実機 `kmp-android-dialog.png` ほか | ✅ |
| Scenario: 共有コードからの show が iOS で動作する | `iosMain/Dialog.ios.kt:12-16` + `IosDialogGateway.kt:26-55` (cinterop 委譲) | iosTest `InteropBridgeContractTests` 5 件 / 実機 `kmp-ios-dialog.png` ほか | ✅ |
| **Swift 側登録とのキー同一性** | `iosMain/DialogViewModel.ios.kt:9` (共有 VM クラスがそのままキー) + `IosDialogGateway.kt:31-42` (包み直さず素通し) | iosTest 「共有コードの ViewModel は ObjC クラスとして見える」/「Swift 側登録の View factory が共有コードの ViewModel で解決される」 | ✅ |
| Scenario: Swift 登録の View が共有コードの show で表示される | `samples/kmp/iosApp/.../SampleDialogRegistration.swift:14-22` | 実 framework 越しの実機確認 (verification-matrix 実績メモ 2026-08-15: ObjC export 名 `SampleSharedBasicDialogViewModel` で解決、`結果: completed(true)`) | ✅ |
| **Swift async からの直接呼び出し** — 型付き結果が正しい型・値で届く | `commonMain/KsDialogs.kt:24-25` (`@Throws` + suspend)。Swift 側呼び出し元はリポジトリ内に不在 | 4.5 の実測記録 (verification-matrix:112) のみ。**自動テスト・実機証跡ともに直接経路の実証なし** | **❌2** |
| Scenario: Swift から await した結果の型と値が正しい | 同上 | 同上 | **❌2** |
| (本文) **復元失敗**と構成エラーは Kotlin 例外 → NSError で届く | `commonMain/DialogGateway.kt:46-50` — `@Suppress("UNCHECKED_CAST") outcome.value as R` のみ。復元失敗の検出なし | なし | **❌1** |
| **テスト差し替え** | `commonMain/KsDialogs.kt:12` (interface) + `commonTest/support/FakeKsDialogs.kt` | commonTest `FakeDialogsSubstitutionTests.kt` 2 件 (両ターゲット) | ✅ |
| Scenario: fake 実装で Presenter を単体テストできる | 同上 | 同上 | ✅ |

### maui-binding

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| **MAUI 公開 API での貫通** — `Task` + 型付き結果 / VM 型キー / platform view 実体化 | `IKsDialogs.cs:29` + `Dialog.cs:42-56` / `Registry/DialogViewRegistry.cs:37-61` / `Platforms/Android/PlatformDialogGateway.cs:77-81` (`ToPlatform`) / `Platforms/iOS/PlatformDialogGateway.cs:80-81, 119-148` | — | ✅ |
| Scenario: C# から show して結果を受け取る | 同上 | `DialogResultRouteTests.cs` `CompleteOperationReturnsCompleted` / `CompletedValueKeepsDeclaredResultType` | ✅ |
| Scenario: MAUI View がダイアログとして表示される | `Platforms/*/PlatformDialogGateway.cs` (Bridge 経由の提示) | 実機 `maui-ios-dialog.png` / `maui-android-dialog.png` / `maui-ios-completed.png` / `maui-android-completed.png` | ✅ |
| **結果経路の platform 非依存検証** (gateway seam) | `Internals/DialogGateway.cs:15-28` (internal interface) + `DialogGatewayFactory.cs:10-15` + `HostlessDialogGateway.cs` | 素の net10.0 で 19 件全通過 (`dotnet test`)。`DialogGatewayContractTests.cs` 5 件が adapter 契約を固定 | ✅ |
| Scenario: fake 実装で結果経路をユニットテストできる | `KsDialogs.Maui.Tests/Support/TestDialogGateway.cs` | `DialogResultRouteTests.cs` / `DialogRegistryTests.cs` / `DialogCallContextTests.cs` (シミュレータ不要で完了) | ✅ |

### samples

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| **4ルートの Basic Dialog デモ項目** (6セル) | `samples/ios` / `samples/android` / `samples/maui` / `samples/kmp` の各 Sample | `ui/brief.md`「6セル × 経路の確認結果」表 (6 セル × 表示 / 完了 / キャンセル / 外タップ / 戻る) | ✅ |
| Scenario: 完了操作の結果が画面に表示される | `SampleText.completedResult` (4ルート同一実装) | `maui-ios-completed.png` / `maui-android-completed.png` / brief.md 表 (6セル 済) | ✅ |
| Scenario: キャンセルの結果が画面に表示される | `SampleText.CANCELLED_RESULT` (4ルート同一) | `*-cancelled.png` / `*-outside-tap-cancelled.png` (6セル分) | ✅ |
| **Sample の consumer 境界** — 参照方式 | ios = `XCLocalSwiftPackageReference "../../ios"` (pbxproj:140) / android = `settings.gradle.kts` `includeBuild("../../android")` + `dependencySubstitution` / maui = `ProjectReference` 1本 (csproj:48) / kmp = `includeBuild("../../kmp")` + shared が facade を消費 | ✅ 4ルートとも仕様どおり | ✅ |
| Scenario: Sample が公開 API だけでビルド・動作する | 同上 | tasks 7.1 (BuildProbe 削除後の全ルートビルド + Sample 実行) | ✅ |
| (本文) **Sample から本体の内部実装を直接参照しない** | `samples/kmp/iosApp/.../SampleDialogRegistration.swift:14` が `KsDialogsInteropBridge.shared` を直接使用 | — | **❌3** |
| **4ルートのパリティ** | `samples/*/SampleText.*` 4 ファイルの文言が完全一致 (menuTitle / basicDialogItem / basicDialogMessage / OK / キャンセル / 直近の結果 / 結果表示) を突き合わせ済み。SampleTheme RGBA は `samples/README.md` に一覧 | `ui/brief.md` + `ui/verification/notes.md` (承認モック mock-b との構造・トークン・意図の一致を 6 セルで確認) | ✅ |
| Scenario: 4ルートのデモ項目が一致する | 同上 | `*-menu.png` (6セル) | ✅ |

---

## 未充足項目 (❌) と見立て

### ❌1 kmp-facade「Swift async からの直接呼び出し」— 型消去輸送からの**復元失敗が検出されない**

**該当**: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:46-50`

```kotlin
is DialogOutcome.Completed -> {
    @Suppress("UNCHECKED_CAST")
    DialogResult.Completed(outcome.value as R)
}
```

spec は「iosMain actual は…**復元を担い、復元失敗**と構成エラーは Kotlin 例外 → NSError 変換で Swift 側に届く SHALL」と定めるが、`R` は実行時に消去されるためこのキャストは復元失敗を検出しない。iOS 経路では互換面 `KsDialogsInteropNotifier.complete(_ value: Any)` (`ios/Sources/KsDialogs/Interop/KsDialogsInteropNotifier.swift:20`) が型を持たないため、不整合な値が入り得る経路が実在する。`DialogException` の KDoc (`DialogException.kt:6`) は「型消去輸送からの結果値の復元に失敗した」を内訳に挙げているが、それを投げるコードは存在しない。

**状態**: コンテキストパッケージのとおり**オーナー判断待ちで意図的に保留**。証跡は review-001 Major 3 / second-opinion-code-001 突き合わせ結果 #1。

**見立て**: 修正方針の選択が spec / 契約に触れる (どこで型を持たせるか) ため、**deviation.md への記録が必要**。実装で直すなら (a) `DialogViewModel<R>` に結果型を実行時に持たせる手段を足す、(b) iosMain の gateway で復元検査を行い失敗を `DialogException` にする、のいずれか。**現状は「保留」の合意自体が deviation.md に記録されていない**点が形式上の欠落であり、蒸留前に記録するのが望ましい。

### ❌2 kmp-facade「Swift から await した結果の型と値が正しい」— 直接経路の実証がない

**該当**: リポジトリ内に **Swift から KMP facade の `show` を await するコードが存在しない**。

- `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift:18` が await しているのは共有 Presenter `SamplePresenter.showBasicDialog()` (`samples/kmp/shared/.../SamplePresenter.kt:25-31`) で、`show` の呼び出しと `DialogResult` の分岐は **Kotlin 側に閉じている**。Swift へ渡るのは `String` である
- iosTest 側 (`InteropBridgeContractTests` / `InteropValueTransportTests`) はいずれも Kotlin から見た経路の検証で、Swift からの await ではない
- `verification-matrix.md:112` の A4.5 記録は「framework の生成ヘッダと Swift の型検査で実測」— すなわち**シグネチャの利用可能性**の実測であり、同記録自身が「値が正しく届くかの最終確認は 7.2 の手動確認 (samples/kmp) で行う」としている。しかし 7.2 が確認したのは上記の Presenter 経路である

加えて、同記録は Requirement 本文「型付き結果が**正しい型**・値で届く」に対し「**sealed の網羅分岐は失われ**、**結果型のジェネリクスも消える** (`DialogResultCompleted<AnyObject>` / `value` は `AnyObject?`)」と、契約とのずれ自体を記録している (フォールバックは「解消しないため」未発動)。

**見立て**: 2 通りある。
- **(a) 実装を直す**: kmp/ADR-0001 のフォールバック (自前 `@objc` completion ラッパー) を発動する。ただし実測記録は「ジェネリクス消失は ObjC 面の制約でフォールバックでも解消しない」としており、Requirement 本文どおりの「静的に型付いた結果」は達成できない見込み
- **(b) deviation として合意する** (推し): 「ObjC 面の制約により Swift 側では結果型のジェネリクスが消え、判別は `as?` で行う」ことを合意済み差分として記録する。**そのうえで値の正しさを実証する証跡を 1 本足す**のが筋 — samples/kmp/iosApp に Swift から facade の `show` を直接 await する経路を 1 つ設ける (デモ項目を増やさずとも検証用の一時経路の記録で足りる) か、Swift 側の最小テストを置く

いずれにせよ**現状は Scenario の THEN が未実証**であり、verification-matrix の KI セル `[x] A4.5 M7.2` は根拠を実態より強く書いている (A4.5 に対応する自動テストはリポジトリに存在しない)。

### ❌3 samples「Sample から本体の内部実装を直接参照しない SHALL」— KMP iOS Sample が互換面を直接使用

**該当**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleDialogRegistration.swift:14-22`

```swift
KsDialogsInteropBridge.shared.registerViewFactory(forViewModelClass: BasicDialogViewModel.self) { ... }
```

`KsDialogsInteropBridge` は specs/ios-native が「**内部用として公開 API と分離される SHALL** (design Decision 12)」と定めた型であり、実装自身の doc コメントも「この型は内部境界であり、Swift から直接使う公開 API ではない (型付きの入口は `Dialog`)」(`KsDialogsInteropBridge.swift:12`) と明記している。また kmp の `DialogViewRegistry` KDoc は「iOS は **Swift ライブラリのレジストリ**が受け口になる」(`commonMain/DialogViewRegistry.kt:8`) としており、Sample の実装はこの記述とも食い違う。

一方で **KMP iOS 消費者に型付きの公開登録経路が用意されていない**ことも事実である (Swift の `DialogViewRegistry.register` は Swift protocol `DialogViewModel` への準拠を要求し、Kotlin 由来の VM クラスはそのままでは準拠しない)。

**見立て**: 2 通り。
- **(a) 実装を直す**: Swift 側で Kotlin 由来 VM クラスへの retroactive conformance (`extension BasicDialogViewModel: DialogViewModel { typealias Result = Bool }`) が成立するか実測し、成立するなら Sample を公開入口 (`Dialog.shared.registry.register`) へ寄せる。レジストリキーはどちらの入口も `ObjectIdentifier(クラス)` なので同一性は保たれる見込み
- **(b) deviation として合意する**: 「KMP iOS の View 登録は現状 `@objc` 互換面が唯一の経路であり、Sample もそれを使う」ことを合意済み差分として記録し、**KMP iOS 向け公開登録経路の整備を phase-5 (API 表面の突き合わせ) へ申し送る**

どちらを採るかは公開 API の形に関わるため**オーナー判断**が要る。なお `KsDialogsInteropBridge` は cinterop のため `public` である必要があり、可視性そのものは design Decision 12 に沿っている (命名分離は満たしている)。

---

## 追加検査

### tasks.md (全 26 項目 [x])

**虚偽のチェックは見当たらない。** 抜き取りで実体を確認した:

| タスク | 確認した実体 |
|---|---|
| 1.1 / 1.2 | `common-spec-scenarios.md` (MD-a〜MD-d、受け入れ / 調査の区分あり) / `verification-matrix.md` (6セル × 20 Requirement、全セル記入済み) |
| 2.1〜2.5 | `ios/Sources/KsDialogs/` 23 ファイル / `ios/Tests/` 35 テスト。MD-a / MD-c / MD-b の同名テストあり |
| 3.1〜3.4 | `android/ksdialogs/src/main/` 16 ファイル / 34 テスト。MD-a / MD-c / MD-b の同名テストあり |
| 4.1〜4.6 | commonMain 7 / androidMain 3 / iosMain 3 ファイル、32 テスト。疎通確認の実測は matrix 実績メモ 111〜113 行 |
| 4.7 | matrix 実績メモ 129 行に `minos 17.0` の実測記録あり (非保証フラグ依存の事実も記載) |
| 5.1 / 5.2 | `maui/macios/KsDialogs.Binding.iOS` (XcodeProject) / `maui/android/KsDialogs.Binding.Android` (gradlew フォールバック) + `kasane/decisions/maui/0003-build-wiring-xcodeproject-and-gradle-exec.md` 起票済み |
| 5.3〜5.5 | `maui/KsDialogs.Maui/` 一式 + 19 テスト + bridge 5 テスト |
| 6.1〜6.4 | `samples/` 4ルート、参照方式は仕様どおり (上表参照) |
| 6.5 / 6.6 | `ui/verification/` 62 ファイル (png 61 + notes.md) / `samples/README.md` のパリティ表 |
| 7.1 | BuildProbe 8 ファイルが 4ルートすべてで削除済み (`git status` の D 行 8 件) |
| 7.2 | `ui/brief.md`「最終照合の記録 (tasks 7.2)」6セル表 + `kasane/roadmaps/.../artifacts/verification-evidence-link.md` からのリンク |
| 7.3 | `kasane/config.yaml:63-82` に `ui.screenshot` 手順あり (実測日 2026-08-15) |
| 7.4 | `common-spec-scenarios.md` §6「tasks 7.4 でまとまった追記候補」3 件 |
| 7.5 | matrix 全セル `[x]`。ただし **KI / Swift async からの直接呼び出しの `A4.5`** は対応する自動テストがリポジトリに存在しない (→ ❌2) |

**留意 (虚偽ではないが根拠の書き過ぎ)**: verification-matrix の KI セル `Swift async からの直接呼び出し` = `[x] A4.5 M7.2`。判定手段 A (自動テスト) の実体がなく、M7.2 も別経路 (共有 Presenter) での確認である。

### 逆流検査

- `proposal.md` / `design.md` / `specs/` 6 件 — **未変更**。`git log` 上の最終更新は提案作成コミット `36f75f2` (2026-08-14) のみで、実装期間中の書き換えはない
- working tree で変更されているのは `tasks.md` (実績欄) と `ui/brief.md` (照合記録) のみ。いずれも実績を書く欄であり、規約に沿っている

### 未記録乖離

`kasane/changes/add-vertical-slice/deviation.md` は**存在しない**。したがって ❌1 / ❌2 / ❌3 はいずれも未記録の乖離に当たる (❌1 は「保留する」という合意自体が未記録)。

### UI 変更の確認

- `ui/brief.md:34` に承認モックの記録あり — `mock/mock-b.html` 採用、`approved.png`、**2026-08-14 オーナー承認**
- 合意済み妥協の記録あり (`ui/brief.md:69-78`) — MAUI iOS の当たり判定 / MAUI・KMP 4セルでの多段表示未観測 / 初期状態の結果表示エリア非表示 (実装者判断のままオーナー確認が残る旨を明記) / ボタン高さ 38pt-dp
- モックとの視覚照合の証跡は `ui/verification/` (61 枚 + notes.md)

### テスト実行 (絞り込みなしの全件)

`kasane/concepts/cross/conventions/test-execution.md` のコマンドで実行し、件数まで確認した。

| ルート | コマンド | 結果 |
|---|---|---|
| ios | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **35 tests / 9 suites passed** (`Test run with 35 tests in 9 suites passed`)。XCTest 側は `Executed 0 tests` (規約どおり2系統) |
| android | `./gradlew test --rerun-tasks` | **34 tests / 0 failures** (`testDebugUnitTest` の TEST-*.xml 9 ファイル集計) |
| kmp | `./gradlew allTests --rerun-tasks` | **32 tests / 0 failures** (iosSimulatorArm64Test 17 + testAndroidHostTest 15) |
| maui | `dotnet test` | **19 tests / 0 failures** (`合格: 19、失敗: 0`) |
| maui bridge | `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **5 tests / 0 failures** |

いずれもコンテキストパッケージの期待値 (ios 35 / android 34 / kmp 32 / maui 19 + bridge 5) と一致し、全件 green。

---

## 参考所見 (判定に影響しない)

- **`ui/brief.md:66` の MD-b 行が手当て前の事実のまま**: 「iOS = 上の show は未完了のまま」と書かれているが、`common-spec-scenarios.md:96` と `verification-matrix.md:135` は手当て後の再観測 (上の show は cancelled で確定) まで追随している。MD-b はデルタスペックの Scenario ではない**調査ケース**のため判定には影響しないが、review-002 Minor 2 と同型 (記録の陳腐化) であり、蒸留前に揃えるのが望ましい
- **`kmp-facade` spec は「iosMain actual が復元を担う」としているが、実装の復元は commonMain の `GatewayKsDialogs`** にある。挙動の契約ではなく配置の記述であり、❌1 の修正方針を決めるときに合わせて整理すると spec と実装が揃う

---

## 判定

**INVALID** — ❌ 3 件。

- ❌1 はオーナー判断待ちの既知項目 (コンテキストパッケージで申し送り済み)。ただし**「保留する」合意が deviation.md に記録されていない**
- ❌2 / ❌3 は本検証で独立に検出したもので、いずれも**実装を直すか deviation として合意するかの判断がオーナーに要る**

3 件とも「テストが落ちている」「タスクが未実施」ではなく、**契約の文言と実装・証跡の間のずれ**である。deviation.md の起票 (または実装修正) が済めば VALID になる性質のもの。
