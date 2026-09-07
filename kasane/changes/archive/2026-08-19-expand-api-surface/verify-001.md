# 一致検証: expand-api-surface (001 回目)

**日付**: 2026-08-19
**対象**: `kasane/changes/expand-api-surface/specs/` のデルタスペック6本 (dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples) と、HEAD (154d4fb) に対する未コミットの作業ツリー差分
**deviation.md**: 存在しない (= オーナー合意済みの乖離はゼロ)
**判定**: **VALID**

Scenario 総数 **53**。内訳は ✅ 一致 53 / ⚠️ deviation 記録済み 0 / ❌ 欠落・乖離 0。

パスはすべてリポジトリルート基準の相対表記。

---

## 1. dialog-contract (ADDED 3 Requirements / 11 Scenarios)

### Requirement: 真偽値結果の省略形

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 省略形 VM の結果は真偽値で返る | iOS `ios/Sources/KsDialogs/Contract/DialogViewModel.swift:14` (`associatedtype Result: Sendable = Bool`) / Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/SimpleDialogViewModel.kt:14` / MAUI `maui/KsDialogs.Maui/Contract/DialogViewModel.cs:32` + `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:56` / KMP Swift `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:36`,`:64`,`:91` | iOS `DialogApiSurfaceCompileChecks.swift:40,49` / Android `DialogSimpleViewModelFaceTests.kt:18` / MAUI `DialogSimpleViewModelFaceTests.cs:20,41,59,87` / KMP `KsDialogsKmpFacadeTests.swift:51` `omittedResultTypeDefaultsToBool` | ✅ |
| カスタム型の宣言経路は従来どおり | 既存経路不変。iOS `register(_:factory:)` / Android `DialogViewModel<R>` / MAUI `Register<TViewModel,TResult>` `DialogViewRegistry.cs:37` / KMP `result:` ラベル `KsDialogsKmp.swift:48` | iOS `DialogApiSurfaceCompileChecks.swift:69` / Android `ComposeDialogContentTests.kt:69` / MAUI `DialogExpandedApiSurfaceChecks.cs:25,59` / KMP `KsDialogsKmpFacadeTests.swift:27` | ✅ |

KMP 共有 VM の「commonMain では `DialogViewModel<Boolean>` を明示する」条件も維持されている (expect interface に型引数デフォルトの導入なし)。

### Requirement: 技術別登録の挙動同一性

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 宣言的 UI 登録のダイアログも同じ結果経路を通る | 内部表現の1本化: `ios/Sources/KsDialogs/Registry/DialogContent.swift` / `DialogViewFactory.swift:28-37`、Android `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogRegistration.kt:23` + `DialogComposeContentView.kt:37` | iOS `DialogSwiftUIContentTests.swift:25` / Android `ComposeDialogContentTests.kt:51`,`:69` | ✅ |
| 宣言的 UI にもレイアウト規則が適用される | iOS `DialogContainerViewController.swift:250-256` (合成) / Android `DialogComposeContentView.kt:132-135` | iOS `DialogSwiftUIAttributeDslTests.swift:22` (共通ケース表全件を引数化) / Android `ComposeDialogLayoutParityTests.kt:59` (`cases()` = ケース表全件) | ✅ |
| 添付 DSL と従来添付は同じ結果になる (非レイアウト属性を含む) | iOS `ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:15-53` / Android `KsDialogAttributes.kt:27` + `DialogAttributeCollector.kt` | iOS `DialogSwiftUIAttributeDslTests.swift:48` `非レイアウト属性も添付 DSL で届く` / Android `ComposeDialogLayoutParityTests.kt:59` (View 系との外形一致を全ケース) + `ComposeDialogAttributeTests.kt:96` (overlayColor の実ピクセル読み取り・外側タップ注入) | ✅ |
| show 引数の placement は添付 DSL より優先される | iOS `DialogContainerViewController.swift:250-256` (オブジェクト単位置換) / interop 面 `KsDialogsInteropBridge.swift:85-106` | `KsDialogsKmpFacadeTests.swift:142` `sharedCodeShowPlacementOverridesAttachment` (SwiftUI 添付 Start/Start に対し show 引数 Start+offsetX10/End が矩形 `(10,664,280,180)` でまるごと採用) + 従来添付側 `DialogAttributeSupplyTests.swift:70,88` | ✅ |
| インライン show でも placement 上書きが成立する | iOS `Dialog.swift:44-66` / Android `Dialog.kt:26` / MAUI `Dialog.cs:60,77` | iOS `DialogInlineShowTests.swift:147` `インライン show の placement は添付をまるごと置換する` / Android `DialogInlineShowTests.kt:172` / MAUI `DialogInlineShowTests.cs:80` | ✅ |

### Requirement: インライン factory show

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未登録の VM をその場で表示できる | iOS `Presentation/Dialog.swift:69-81` (`showInline`、registry 非経由) / Android `Dialog.kt:26` / Compose `ComposeDialogShow.kt:23` / MAUI `Dialog.cs:60,77` | iOS `DialogInlineShowTests.swift:13,37` / Android `DialogInlineShowTests.kt:26` / Compose `ComposeDialogContentTests.kt:87` / MAUI `DialogInlineShowTests.cs:22` | ✅ |
| インライン show はレジストリを汚さない | 同上 (登録・削除を一切行わない、design Decision 5) | iOS `DialogInlineShowTests.swift:85` / Android `DialogInlineShowTests.kt:90` / Compose `ComposeDialogContentTests.kt:145` / MAUI `DialogInlineShowTests.cs:161` | ✅ |
| 既存登録と共存する | 同上 | iOS `DialogInlineShowTests.swift:55` / Android `DialogInlineShowTests.kt:59` / Compose `ComposeDialogContentTests.kt:108` / MAUI `DialogInlineShowTests.cs:103,131` | ✅ |
| 並行インライン show の独立 | 同上 | iOS `DialogInlineShowTests.swift:109` / Android `DialogInlineShowTests.kt:110,141` / MAUI `DialogInlineShowTests.cs:184,221` | ✅ |

---

## 2. ios-native (ADDED 5 Requirements / 11 Scenarios)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **SwiftUI 登録オーバーロード** / 宣言表どおりの利用コードがコンパイルできる | `Registry/DialogViewRegistry.swift:37-45`、`Presentation/KsDialogs.swift:30-42`、`Presentation/Dialog.swift:44-66`、`SwiftUI/DialogAttributeAttachment.swift:41,49` | `Tests/KsDialogsTests/DialogApiSurfaceCompileChecks.swift` (`registersSwiftUIContent`:59 / `registersSwiftUIContentWithCustomResult`:69 / `registersUIViewContent`:76 / `showsInlineUIViewContent`:85 / `showsInlineSwiftUIContent`:95 / `showsInlineContentWithPlacement`:108 / `attachesAttributesToSwiftUIContent`:126)。非 `@testable` な `import KsDialogs` で公開性も同時に固定 | ✅ |
| **SwiftUI 登録オーバーロード** / SwiftUI コンテンツの登録と表示 | `DialogViewRegistry.swift:37-45` → `Registry/DialogViewFactory.swift:28-37` | `DialogSwiftUIContentTests.swift:25` | ✅ |
| **SwiftUI ホストの所有と破棄** / 全閉鎖経路でホストが解放される | child containment `Presentation/DialogContainerViewController.swift:128-134`、解放 `:288-294`、合流 `:263-281`、ホスト `SwiftUI/DialogSwiftUIHost.swift:10-28` | `DialogSwiftUIContentTests.swift:77` `全閉鎖経路でホストが解放される` (completed / cancelled / callerCancelled の3引数)、`:49` (containment)、`:109` (器破棄) | ✅ |
| **SwiftUI 添付 DSL** / 添付値が初回表示から反映される | `SwiftUI/DialogAttributeAttachment.swift:15-32`、`DialogSwiftUIHost.swift:44-74`、`DialogSwiftUIContentView.swift:40-44` | `DialogSwiftUIAttributeDslTests.swift:22` (共通ケース表全件を `layoutCase` で駆動) | ✅ |
| **SwiftUI 添付 DSL** / 非レイアウト属性も添付 DSL で届く | `DialogContainerViewController.swift:127,244,301-304` | `DialogSwiftUIAttributeDslTests.swift:48` | ✅ |
| **SwiftUI 添付 DSL** / 添付なしは契約既定値 | 同期解決経路 `DialogSwiftUIHost.swift:63-69`、到達判定 `DialogContainerViewController.swift:184-187` | `DialogSwiftUIAttributeDslTests.swift:74` (`isLayoutSnapshotFrozen` で待ちが起きないことも固定) | ✅ |
| **SwiftUI 添付 DSL** / 到達上限後は既定値と警告 | `DialogContainerViewController.swift:34` (`maxAttributeSupplyWaitPasses = 1`)、`:193-211` (警告は `:205` `Logger.warning`) | `DialogSwiftUIAttributeDslTests.swift:97` + プローブ `Tests/.../Support/NeverResolvingAttributeSupplyView.swift` | ✅ (所見1) |
| **SwiftUI 添付 DSL** / 同一属性の重畳は外側勝ち | `DialogAttributeAttachment.swift:20-31` (`reduce`) | `DialogSwiftUIAttributeDslTests.swift:123` | ✅ |
| **SwiftUI 添付 DSL** / 提示後の添付変更は反映されない | `DialogContainerViewController.swift:56`,`:238-248` (固定後は早期 return) | `DialogSwiftUIAttributeDslTests.swift:148` (placement / overlayColor / 外側タップの3面。供給自体は届き続けることを先に確認する構成) | ✅ |
| **Result のデフォルト** / Result 宣言なしの VM | `Contract/DialogViewModel.swift:14` | `DialogApiSurfaceCompileChecks.swift:40` `notifierDefaultsToBool`、`:49`。負検査 `DialogTypedResultCompileChecks.swift:31` | ✅ |
| **インライン show** / SwiftUI のインライン表示 | `Presentation/KsDialogs.swift:38-42` / `Dialog.swift:56-66` | `DialogInlineShowTests.swift:13` (UIView 版は `:37`) | ✅ |

**design Decision 6 (iOS 宣言表) との照合**: `register` (SwiftUI) / `show` (UIView・SwiftUI、`placement:` 付き) / `associatedtype Result = Bool` / `View.ksDialogOptions` `View.ksDialogPlacement` の4項目とも一致。総称パラメータ名 (`VM` → `ViewModel`) と `public extension` 表記の差のみで、宣言レベルの形は同一。

---

## 3. android-native (ADDED 5 Requirements / 10 Scenarios)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **Compose 登録** / 宣言表どおりの利用コードがコンパイルできる | `android/ksdialogs-compose/.../ComposeDialogRegistration.kt:23`、`ComposeDialogShow.kt:23`、`android/ksdialogs/.../KsDialogs.kt:47`、`SimpleDialogViewModel.kt:14` | `android/api-surface-check/.../DialogExpandedApiSurfaceChecks.kt` (`acceptsSimpleViewModelRegistration`:32 / `acceptsSimpleViewModelShow`:41 / `acceptsInlineShow`:45 / `acceptsInlineShowWithPlacement`:53 / `acceptsInlineShowWithDeclaredResultType`:60 / `acceptsComposeRegistration`:68 / `acceptsComposeInlineShow`:80 / `acceptsComposeInlineShowWithPlacement`:87)。friend path を持たない別モジュール | ✅ |
| **Compose 登録** / 本体は Compose 非依存のまま | `android/ksdialogs/build.gradle.kts:92` (`verifyNoDeclarativeUiDependency`、debug/release × compile/runtime の4 classpath を推移的に走査) | 同 `:134` (`check` 結線) / `:141` (`test` 結線) — 規約の全件実行 `./gradlew test --rerun-tasks` に乗る。`concepts/cross/conventions/test-execution.md:159-171` に節あり | ✅ |
| **Compose 登録** / Compose コンテンツの登録と表示 | `ComposeDialogRegistration.kt:23`、`DialogComposeContentView.kt:37` | `ComposeDialogContentTests.kt:51`、`:69` (カスタム結果型) | ✅ |
| **Compose ホストの lifecycle と破棄** / 全閉鎖経路で composition が破棄される | `DialogComposeContentView.kt:82-103` (owner 据え付け)、`:105-111` (`onDetachedFromWindow` → `disposeComposition()`)、`:121-129` (owner 復帰) | `ComposeDialogContentTests.kt:165` / `:172` / `:179` (Scenario の3経路) + `:197` `画面破棄で閉じたときに組み立てが破棄される` (Requirement 本文の4つ目) | ✅ |
| **Compose 添付 DSL** / 添付値が初回表示から反映される | `KsDialogAttributes.kt:27` (`SideEffect`)、`DialogAttributeCollector.kt:16,42` (`staticCompositionLocalOf`)、`DialogComposeContentView.kt:67-73` (`OnPreDrawListener`)、`:132-135` | `ComposeDialogAttributeTests.kt:78`、`ComposeDialogLayoutParityTests.kt:59` (`@Parameterized`、`cases()`:138 でケース表全件) | ✅ (所見2) |
| **Compose 添付 DSL** / 非レイアウト属性も添付 DSL で届く | `DialogComposeContentView.kt:132-135` | `ComposeDialogAttributeTests.kt:96` (overlayColor は実ピクセル読み取り、`isCanceledOnTouchOutside=false` は実タップ注入) | ✅ |
| **Compose 添付 DSL** / 添付なしは契約既定値 | `DialogAttributeCollector.kt:27-34`、`DialogComposeContentView.kt:133-134` | `ComposeDialogAttributeTests.kt:70` | ✅ |
| **Compose 添付 DSL** / 提示後の添付変更は反映されない | `DialogComposeContentView.kt:67-73` (preDraw で一度だけ取り込みリスナ自己解除) | `ComposeDialogAttributeTests.kt:132` (placement / overlayColor / 外側タップの3面) | ✅ |
| **bool 既定の VM 契約の顔** / 型引数なしの VM 宣言 | `android/ksdialogs/.../SimpleDialogViewModel.kt:14` | `DialogSimpleViewModelFaceTests.kt:18` + compile 検査 `DialogExpandedApiSurfaceChecks.kt:32,41` | ✅ |
| **インライン show** / Compose のインライン表示 | `ComposeDialogShow.kt:23` (View 版は本体 `KsDialogs.kt:47-51` / `Dialog.kt:26`) | `ComposeDialogContentTests.kt:87`,`:108`,`:145` / View 版 `DialogInlineShowTests.kt` 9本 | ✅ |

Lazy スコープ制約の契約明記 (design Decision 11 の要求): `KsDialogAttributes.kt:18-20` の KDoc に `LazyColumn` / `LazyRow` を名指しで記載済み。

**design Decision 6 (Android 宣言表) との照合**: `SimpleDialogViewModel` / インライン `show` / `registerCompose` / `showCompose` / `KsDialogAttributes` の5項目とも、型引数・引数名・既定値まで一致 (`explicitApi()` 由来の `public` 修飾のみ付加)。`ksdialogs-compose` モジュールは `android/settings.gradle.kts:36` に登録済み。

---

## 4. maui-binding (ADDED 2 Requirements / 4 Scenarios)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **bool 既定の登録** / 全呼び出し形式の compile 検査 | `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:37`,`:56`、`Contract/DialogViewModel.cs:32` | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogExpandedApiSurfaceChecks.cs` (`AcceptsBothTypeArgumentsOnRegister`:25 / `AcceptsViewModelOnlyTypeArgumentOnRegister`:36 / `AcceptsInferredRegisterForSimpleFace`:48 / `AcceptsInferredRegisterForCustomResult`:59) — 4形式すべて | ✅ |
| **bool 既定の登録** / 意図しない形式は negative compile check で拒否される | `maui/KsDialogs.Maui.ApiSurfaceCheck/NegativeChecks/RejectsCustomResultViewModelOnSimpleRegister.cs:11-16`、フラグ結線 `KsDialogs.Maui.ApiSurfaceCheck.csproj:49-51` | コンパイル失敗が期待結果のためテスト関数なし。規約側は `concepts/cross/conventions/test-execution.md:155` に行追加、件数は `:26` `:114` `:118` の3か所とも 16 → **17 本**へ更新済み (tasks 4.1 の要求どおり) | ✅ |
| **bool 既定の登録** / 型引数1つの登録の動作 | `DialogViewRegistry.cs:56`、`Presentation/Dialog.cs:46` | `maui/KsDialogs.Maui.Tests/DialogSimpleViewModelFaceTests.cs:20,41,59,87` | ✅ |
| **インライン show** / 未登録 VM のインライン表示 | `Presentation/IKsDialogs.cs:67`,`:88`、`Presentation/Dialog.cs:60`,`:77` | `maui/KsDialogs.Maui.Tests/DialogInlineShowTests.cs` 9本 (`:22` ほか) + compile 検査 `DialogExpandedApiSurfaceChecks.cs:77,88,103` | ✅ |

**design Decision 6 (MAUI 宣言表) との照合**: 4宣言すべて厳密一致 (`placement = null` の既定値を含む)。

---

## 5. kmp-facade (ADDED 6 Requirements / 10 Scenarios)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **呼び出し元キャンセルでの閉鎖** / Swift Task キャンセルで閉じる | `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:127-145` (`withTaskCancellationHandler`)、`Kmp/KmpShowCancellation.swift:8`、`Interop/KsDialogsInteropShowHandle.swift:13,24`、ハンドル返却 `Interop/KsDialogsInteropBridge.swift:68,85` | `KsDialogsKmpCancellationTests.swift` (`callerCancellationClosesDialog`:20 / `cancellationSettlesResultExactlyOnce`:35 / `cancellationClosesOnlyItsOwnDialog`:58 / `cancellationBeforePresentationLeavesNothing`:80)。証跡 `verification/kmp-task-cancellation/` | ✅ |
| **Swift 向け型付き登録** / 型付き登録と show の一連 | `KsDialogsKmp.swift:48-57`、`Kmp/DialogViewFactory+Kmp.swift:11-22` | `KsDialogsKmpFacadeTests.swift:27` | ✅ |
| **Swift 向け型付き登録** / result 省略は真偽値 | `KsDialogsKmp.swift:36-41`,`:64-69` | `KsDialogsKmpFacadeTests.swift:51` + compile 検査 `KmpApiSurfaceCompileChecks.swift:29,49` | ✅ |
| **Swift 向け型付き登録** / SwiftUI 添付 DSL は KMP 経路でも有効 | `KsDialogsKmp.swift:64,72`、`DialogViewFactory+Kmp.swift:26-36` | `KsDialogsKmpFacadeTests.swift:113` `swiftUIAttachmentIsAppliedThroughKmpFace` (実効値を矩形で固定) | ✅ |
| **Swift 向け型付き登録** / 共有コードの show 引数 placement が優先 | `KsDialogsInteropBridge.swift:66-78`,`:85-106`、`KsDialogsKmp.swift:136` | `KsDialogsKmpFacadeTests.swift:142` (offsetX ごと置換されることでオブジェクト単位採用を示す) | ✅ |
| **Swift からの型付き show** / Swift から await して型付き結果を得る | `KsDialogsKmp.swift:91-96`,`:103-123` | `KsDialogsKmpFacadeTests.swift:69` `swiftTypedShowReturnsDeclaredResultType`、`:91` `swiftTypedShowReturnsCancelled` | ✅ |
| **型不一致の型付きエラー** / 誤申告の検出 | `KsDialogsKmp.swift:111-116`、`Kmp/KsDialogsKmpError.swift:24-34` (`publicError(from:)`) | `KsDialogsKmpFacadeTests.swift:182` `showRejectsResultOfUndeclaredType`、`:207` `showSurfacesRegistrationResultTypeMismatchAsTypedError` | ✅ |
| **機械面の利用者非公開** / 公開経路だけで登録が完結する | 利用者面 `Presentation/Dialog.swift:16` (`public let kmp`)、Sample 移行 `samples/kmp/iosApp/.../SampleDialogRegistration.swift:17,25,39,48` | `KmpApiSurfaceCompileChecks.swift` 全体 (非 `@testable` の `import KsDialogs`)、`KsDialogsKmpFacadeTests.swift:252` | ✅ |
| **機械面の利用者非公開** / cinterop 委譲は引き続き成立する | `Interop/KsDialogsInteropBridge.swift:15-16` (`@objc(...) public final class` 維持) + doc コメント `:12-14` (「KMP cinterop 委譲専用・アプリコードから直接使用しない」)、委譲側 `kmp/.../IosDialogGateway.kt:32` | `kmp/.../iosTest/.../InteropBridgeContractTests.kt` 6本 + `InteropValueTransportTests.kt` / `InteropPlacementTransportTests.kt`、Swift 側 `KsDialogsInteropBridgeTests.swift` | ✅ |
| **宣言表どおりの Swift 公開面** / 宣言表どおりの利用コードがコンパイルできる | `KsDialogsKmp.swift:36,48,64,72,91,103`、`Kmp/KsDialogsKmpError.swift:12-17` | `KmpApiSurfaceCompileChecks.swift` (登録4形 `:29,38,49,59` / show 2形 `:77,85` + `:94,107` / エラー網羅 `handlesEveryKmpError`:115) | ✅ |

**`KsDialogsKmpError` の照合**: `notRegistered(viewModelType:)` / `resultTypeMismatch(expected:actual:)` の **2 case のみ**で design Decision 6 と厳密一致 (相方レビュー `second-opinion-code-001.md` の Major は是正済み)。`handlesEveryKmpError` が `default` 枝のない `switch` であるため、case 集合の無断拡張はコンパイルで検出される。提示先不在は公開契約型 `DialogError.presentationHostUnavailable` のまま伝播し、`KsDialogsKmpFacadeTests.swift:241` で固定されている。

**register/show の照合**: 型引数・`result:` ラベル・`placement: DialogPlacement? = nil` の既定値まで一致。差は所見3のみ。

---

## 6. samples (ADDED 4 Requirements / 7 Scenarios)

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| **API 表面の新デモ項目** / 宣言的 UI デモ | android `SampleDialogRegistration.kt:45-51` + `DeclarativeDialogCard.kt` / ios `SampleDialogRegistration.swift:32-38` / kmp-android `:51-57` / kmp-ios `:39-45` / maui `SampleDialogRegistration.cs:42-46` | 4ルート揃い。`ui/verification/{android,ios,kmp-android,kmp-ios,maui}-declarative.png` | ✅ |
| **API 表面の新デモ項目** / カスタム結果型デモ | android `:54-61` / ios `:41-47` / kmp-android `:60-67` / kmp-ios `:48-54` (`result: String.self`) / maui `:49-53` | `ui/verification/*-textinput.png`、`verification/sample-walkthrough/` の結果表で `completed("Hello")` を確認 | ✅ |
| **API 表面の新デモ項目** / インライン show デモ | android `MainActivity.kt:84-97` / ios `SampleMenuModel.swift:71-90` / kmp-android `MainActivity.kt:77-95` / kmp-ios `SampleMenuModel.swift:55-75` / maui `SampleMenuPage.xaml.cs:54-68` | 事前登録なしの経路であることをコードで確認 — `InlineDialogViewModel` は5アプリすべてでローカル宣言のみ、どの `SampleDialogRegistration` にも登録なし。`ui/verification/*-inline.png` | ✅ |
| **属性調整パネル操作部の読み上げ対応** / パネル操作部が読み上げで識別できる | android `SampleAlignmentSegmentsView.kt:58-59` (複合名 `"$axisLabel ${choice.label}"` + role 委譲) `SampleAccessibility.kt:13-18` / kmp-android 同型 / ios `SampleAlignmentSegments.swift:29-30`、`SampleOffsetField.swift:12`、`SampleLayoutPanelScreen.swift:51` / kmp-ios 同型 / maui `SampleAlignmentSegmentsView.xaml.cs:63,74-89` + `SampleLayoutPanelPage.xaml(.cs)` | `verification/panel-accessibility/` に4ルート分 (android / kmp-android / ios / kmp-ios / maui-android / maui-ios)。uiautomator XML に `content-desc="Horizontal Start"`… `class="android.widget.Button"`、XCUITest dump に `Button … label: 'Horizontal Center'` を確認 | ✅ |
| **属性調整パネル操作部の読み上げ対応** / 操作部の状態が読み上げに含まれる | 上記 + android `SampleLayoutPanelView.kt:211,228` / maui は platform 側属性で補完 (`ButtonRoleDelegate:115`) | 同証跡の initial/changed 2状態: `Horizontal End selected="true"`、`EditText content-desc="OffsetX" text="24"`、`Switch content-desc="Use visible area" checked="false"` / iOS 側 `Selected`・`value: 0`・`value: 1` | ✅ |
| **KMP iOS Sample の公開 API 化** / 機械面直接利用の解消 | `samples/kmp/iosApp/KsDialogsSampleKmp/SampleDialogRegistration.swift:17,25,39,48` (`Dialog.shared.kmp.register`) | `samples/kmp/iosApp` の Swift / pbxproj への `KsDialogsInteropBridge` grep が **0件** | ✅ |
| **MAUI Sample の登録2スタイル提示** / 2スタイルが実登録に存在する | 型引数明示 `SampleDialogRegistration.cs:19,25,42` / 明示型付きラムダ (型引数なし) `:49-53` | 両方とも実登録。`:9-12` の XML doc と `:41,48` のコメントで意図説明 | ✅ |

文言のパリティ: `ui/brief.md:33-41` の文言表と4ルートの実装文言 (`SampleText.*`) が一字一句一致することを確認 (メニュー3項目・本文・プレースホルダ・ボタン・結果表記)。

---

## 7. 追加検査

### 7.1 tasks.md の虚偽チェック

全23タスクが `[x]`。対応表と突き合わせた結果、**未実装のまま完了印が付いているタスクはない**。

- 1.1 core 契約文書 → `concepts/core/api/registration-show-semantics.md` (新規193行) に3契約すべて記載 (`:32-48` 省略形 / `:50-81` 挙動同一性 / `:96-109` インライン)。`layout-semantics.md:68-75` に SwiftUI / Compose 行が追加され、「宣言的 UI 向けの添付イディオムはまだ提供していない」は concepts 配下から消えている (残存は変更アーティファクト側の経緯記述のみ)
- 4.1 の「負検査 16 本 → 更新」→ `test-execution.md` の3か所とも 17 本へ更新済み
- 6.5 は `[x]` だが本文で「照合作業は完了・**オーナーの最終承認は未取得**」と明示されており、`ui/brief.md` の記載と一致する。実体と表示の食い違いではない (下記 7.4)
- 7.3 → `handoff-distill.md` に5項目の申し送りあり

### 7.2 逆流検査 (足場アーティファクトの凍結)

`git diff HEAD -- proposal.md design.md specs/` は **空**。実装期間中の書き換えなし。

作業ツリーで変更されている change 配下のファイルは `tasks.md` (チェック更新のみ) と `ui/brief.md` (ksn-core の ui/ 規約が求める照合結果・合意済み妥協の追記のみ) の2つで、いずれも逆流に当たらない。

### 7.3 未記録乖離の洗い出し

❌ が0件のため、**deviation.md に記録すべき未記録乖離はない**。design Decision 6 の宣言表と実装の差は所見3の1点のみで、これは宣言表自身が「名前は仮」と明記している範囲に収まる。

### 7.4 UI 変更の検査

- 承認モックの記録: `ui/brief.md`「承認モック」節に `mock/mock-api.html` (approved.png、2026-08-17 オーナー承認) の記録あり — **あり**
- 合意済み妥協の記録: 「合意済み妥協 (platform 制約)」節に Text Input 入力欄の高さ (iOS 44 / Android 48) の1件 — **あり**
- 照合の証跡: `ui/verification/` に24枚 (4ルート + maui iOS 側) と `comparison-sheet.png`
- **オーナーの最終承認 (照合結果に対する) は未取得** — `ui/brief.md` と `tasks.md:46` の双方に明記されている。デルタスペックの Requirement / Scenario ではなくプロセスゲートのため判定 (VALID/INVALID) には算入しないが、**アーカイブ前に閉じるべき残ゲート**として記録する

### 7.5 テストの全件実行 (本検証で実測、2026-08-19)

`concepts/cross/conventions/test-execution.md` の全件実行コマンドで6ルートすべてを実行した。

| ビルドルート | コマンド | 結果 | 規約の記載値 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **90 tests / 19 suites / 0 failures** (`** TEST SUCCEEDED **`) | 90 / 19 一致 |
| android/ | `./gradlew test --rerun-tasks` | **50 tests / 0 failures** (`:ksdialogs` 50) | 50 一致 |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` | **94 tests / 0 failures** (`:ksdialogs` 62 + `:ksdialogs-compose` 32) | 94 (62+32) 一致 |
| kmp/ | `./gradlew allTests --rerun-tasks` | **48 tests / 0 failures** (iosSimulatorArm64 26 + androidHostTest 22) | 48 一致 |
| maui/ | `dotnet test` | **44 tests / 0 failures** (`成功! - 失敗: 0、合格: 44`) | 44 一致 |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **9 tests / 0 failures** | 9 一致 |

instrumented は1台目 (Pixel 4a / API 33) で `:ksdialogs` の 41/62 件目付近に `Instrumentation run failed due to Process crashed` が出て中断した。個別テストの failure ではなく instrumentation プロセスの異常終了で、結果 XML も生成されなかった。2台目 (Pixel 6a / API 36) で同じタスクを再実行し **62 tests / 0 failures** で完走したため、端末依存の実行環境事象と判断する (`:ksdialogs-compose` の 32 件は1台目でも 0 failures で完走済み)。判定には算入しないが、実行環境の注意点として記録する。

負のコンパイル検査 17 本は、フラグを付けた個別実行が必要なため本検証では再実行していない (新設分 `KsDialogsNegativeCheckSimpleRegister` の期待診断 CS0311 での失敗は `review-001.md:122` で実測記録あり)。

---

## 8. 判定

**VALID**

- デルタスペック6本の全 **53 Scenario** が「✅ 一致」。❌ は0件
- tasks.md の虚偽チェックなし
- 足場アーティファクト (proposal / design / specs) の逆流なし
- 未記録乖離なし (deviation.md 不在と矛盾しない)
- テストは6ルートすべてを実測して 0 failures

---

## 9. 所見 (判定に算入しない記録)

1. **iOS S18「到達上限後は既定値と警告」の警告ログが直接アサートされていない**: 実装 (`DialogContainerViewController.swift:205` の `Logger.warning`) は存在し、テストは同一分岐で立つ代理指標 `didExhaustAttributeSupplyWait` を検証している。また上限到達の状況作りが SwiftUI コンテンツではなくテスト専用 UIView プローブ (`NeverResolvingAttributeSupplyView`) 経由である。Scenario の主眼 (既定値で提示され、提示が停止しない) は満たされているため ✅ とした
2. **Android の添付 DSL のケース表適合は間接検証**: 期待矩形と直接照合せず「従来 View 系の添付と同じ外形になる」をケース表全件で示し、View 系のケース表適合テスト (`DialogLayoutCaseTableTests.kt`) との推移で従わせている。論拠は `ComposeDialogLayoutParityTests.kt:40-50` の KDoc に明記されており、Scenario の THEN (共通ケース表の該当ケースに適合) を満たす
3. **design Decision 6 の KMP 宣言表に現れる `KmpDialogNotifier<R>` という型は実装に存在せず、iOS Native と同じ `DialogNotifier<R>` に一本化されている**。宣言表の当該ブロックは「Swift パッケージの新公開型 (**名前は仮**)」と明記されているため乖離として扱わなかったが、Decision 6 の前文は「名前の微修正は deviation として記録」とも書いており両者は緊張関係にある。**蒸留時に design 側の表記を実装へ合わせるのが素直** (`review-001.md:126` も同種の差 (`Dialog.shared.kmp` / `KsDialogsKmp`) を「名前は仮」を根拠に乖離なしと判定している)
4. **`show` 引数 placement が添付 DSL に勝つこと (dialog-contract) の直接テストは KMP / SwiftUI 経路にある**。Android の `showCompose(placement = ...)` と `KsDialogAttributes` を組み合わせた同趣旨のテストは見当たらない (供給合成は View 系の既存テストで固定済み)。Scenario 自体は満たされているため ✅ だが、両 Native で検証範囲が非対称
5. **`verification/sample-walkthrough/` は4ルート全通しではなく ios / kmp-ios の代表2ルート**。README に「グループ6実装時に4ルート通し済みのため代表ルートの再通し」と範囲と選定理由が明記されており、tasks 7.2 の実体は満たされている
6. **`concepts/cross/index.md` の test-execution.md の1行説明が「正/負16本」のまま**で 17 本と食い違う。`handoff-distill.md` 4節が蒸留への残棚卸しとして明示済み
7. **Kotlin 側 `IosDialogGateway` は呼び出し元キャンセルで表示が残る既存挙動のまま**。本変更のデルタスペックは KMP の Swift 公開面に限定されているためスコープ外で、`handoff-distill.md` 5節が phase-5-3 以降へ申し送っている。なお同ファイルの doc コメント (`kmp/.../IosDialogGateway.kt:41-42`) が「取り消しの操作を持たない」と書いたままで、機械面がハンドルを返すようになった実態と食い違っている
