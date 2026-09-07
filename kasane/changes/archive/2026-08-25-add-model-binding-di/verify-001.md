# Verify 001: add-model-binding-di

- 検証日: 2026-08-25
- 基準コミット: `5a2c0c3` に対する作業ツリーの全変更
- 対象デルタスペック: `kasane/changes/add-model-binding-di/specs/` の6能力 (dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples)
- Scenario ID 体系: `MB-<領域>-<NN>` (design Decision 7)
- 合意済み差分: `kasane/changes/add-model-binding-di/deviation.md` (21 項目。うち `[付随修正]` 4 項目)

---

## 判定

**VALID**

全 36 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。虚偽チェックなし・逆流なし・全ビルドルートのテスト成功。未記録乖離は 0 件。

---

## 1. dialog-contract — Requirement「notifier の VM 供給」

挙動は iOS / Android / MAUI の3実装で同名検証する (ios-native / android-native / maui-binding デルタの前文)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-NI-01 VM 引数のみの factory が VM 経由の notifier で報告 | ios: `ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:52` (1引数登録) + `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:49` (View 生成前の紐付け)<br>android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt:50` + `DialogPresenter.kt:75`<br>maui: `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:95` + `maui/KsDialogs.Maui/Internals/DialogPresenter.cs:71` | `ios/Tests/KsDialogsTests/DialogNotifierSupplyTests.swift:16`<br>`android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogNotifierSupplyTests.kt:23`<br>`maui/KsDialogs.Maui.Tests/DialogNotifierSupplyTests.cs:18` | ✅ 一致 (3形態とも factory 実行中の取得を検証しており、「factory 完了後の紐付けでは通らない」条件を満たす) |
| MB-NI-02 show 前は取得できない | ios: `ios/Sources/KsDialogs/Contract/DialogViewModel.swift:30`<br>android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModel.kt:39`<br>maui: `maui/KsDialogs.Maui/Contract/DialogViewModelExtensions.cs:31` | 同上3ファイルの MB-NI-02 | ✅ 一致 |
| MB-NI-03 結果配送後は取得できない | ios: `DialogPresenter.swift:58` (`defer` 除去)<br>android: `DialogPresenter.kt:95` (`finally` 除去)<br>maui: `DialogPresenter.cs` の `finally` 除去 | 同上3ファイルの MB-NI-03 | ✅ 一致 (呼び出し元へ結果が渡る前に除去されることを show の await 後に検証) |
| MB-NI-04 同一 VM インスタンスの並行 show は構成ミス失敗 | ios: `DialogPresenter.swift:49` (`bind` 失敗 → `DialogError.viewModelAlreadyShowing`)<br>android: `DialogPresenter.kt:75` (`DialogException.ViewModelAlreadyShowing`)<br>maui: `DialogPresenter.cs` (`DialogException.ViewModelAlreadyShowing`) | 同上3ファイルの MB-NI-04 (先行 show の存続と結果配送も検証) | ✅ 一致 |
| MB-NI-05 2引数 factory でも同じ配送先 | 3形態とも紐付けを factory 解決方式によらず提示共通入口で行う | 同上3ファイルの MB-NI-05 | ✅ 一致 |
| MB-NI-06 等価な別インスタンスは干渉しない | ios: `ios/Sources/KsDialogs/Contract/DialogNotifierBindings.swift` (`ObjectIdentifier` キー + 弱参照)<br>android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogNotifierBindings.kt` (identity hash + `WeakReference` の自作キー)<br>maui: `maui/KsDialogs.Maui/Contract/DialogNotifierBindings.cs` (`ConditionalWeakTable`) | 同上3ファイルの MB-NI-06 (等価一致・別インスタンスを事前確認) | ✅ 一致 (design Decision 1 の3機構どおり) |
| MB-NI-07 異常終了でも除去され再 show できる | ios: `defer` / android: `finally` / maui: `finally` | 同上3ファイルの MB-NI-07 | ✅ 一致 |

VM 契約の参照型限定:

- Swift: `ios/Sources/KsDialogs/Contract/DialogViewModel.swift:14` — `AnyObject` 制約化 ✅
- C#: `maui/KsDialogs.Maui/Presentation/Dialog.cs` / `Registry/DialogViewRegistry.cs` の `where TViewModel : class` ✅ (notifier 取得の受け手のみ制約なし → deviation 14 記録済み ⚠️)
- Kotlin: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModelTypeCheck.kt` の実行時拒否 ✅

## 2. dialog-contract — Requirement「型指定呼び出し」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-TS-01 生成 → configure → 表示 → 結果 | ios: `ios/Sources/KsDialogs/Presentation/Dialog.swift:68`<br>android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:41`<br>maui: `maui/KsDialogs.Maui/Presentation/Dialog.cs` `ShowTypedAsync` | `ios/Tests/KsDialogsTests/DialogTypedShowTests.swift:29`<br>`android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogTypedShowTests.kt:36`<br>`maui/KsDialogs.Maui.Tests/DialogTypedShowTests.cs:19` | ✅ 一致 |
| MB-TS-02 非同期 configure 完了まで View 生成なし | 3形態とも「VM 生成 → configure await → View factory」の順序を固定 | 同上3ファイルの MB-TS-02 (門を閉じた状態で生成数 0・提示なしを検証) | ✅ 一致 |
| MB-TS-03 VM factory 未登録は構成ミス失敗 | ios: `viewModelFactoryNotRegistered`<br>android: `DialogException.ViewModelFactoryNotRegistered`<br>maui: `maui/KsDialogs.Maui/Internals/DialogResolution.cs:68` | 同上3ファイルの MB-TS-03 (提示に進まないことも検証) | ✅ 一致 |
| MB-TS-04 configure 省略 | 3形態とも `configure` 省略可 | 同上3ファイルの MB-TS-04 | ✅ 一致 |
| MB-TS-05 configure の例外は提示に進まず伝播 | 3形態とも紐付け前に生成・configure を実行 | 同上3ファイルの MB-TS-05 (その後の型指定 show の正常動作も検証) | ⚠️ deviation 記録済み (Swift シグネチャが `async throws` クロージャ — deviation 3) |
| MB-TS-06 再登録はスロット単位の後勝ち | ios: `ios/Sources/KsDialogs/Registry/DialogRegistryEntry.swift`<br>android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogRegistryEntry.kt`<br>maui: `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs` `UpdateEntry` | 同上3ファイルの MB-TS-06 | ✅ 一致 |

VM factory 登録 API の名前は3形態とも `registerViewModel` / `RegisterViewModel` (design の「`register(VM 型, viewModel:)` 相当」から改名 — deviation 4・12 記録済み ⚠️。iOS のみ `register(_:viewModel:)` 相当のオーバーロード `DialogViewRegistry.swift:78`)。

## 3. ios-native — Requirement「Swift 公開面の VM 供給と型指定呼び出し」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-IO-01 正の compile 検査 | `ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:52,63,78`・`Presentation/KsDialogs.swift` の既定引数版 | `ios/Tests/KsDialogsTests/DialogModelBindingCompileChecks.swift` (非 `@testable` import。UIKit / SwiftUI 1引数登録・VM factory 登録・`vm.notifier` の宣言結果型・configure あり/なし/async・placement) | ✅ 一致 (本検証で `xcodebuild test` に同梱されコンパイル成功) |
| MB-IO-02 値型 VM の準拠拒否 | `ios/Sources/KsDialogs/Contract/DialogViewModel.swift:14` | 同ファイル `#if KSDIALOGS_NEGATIVE_CHECK_VALUE_TYPE_VIEW_MODEL` | ✅ 一致 (本検証で実行: `non-class type 'ConsumerValueDialogViewModel' cannot conform to class protocol 'DialogViewModel'` で `TEST BUILD FAILED` = 期待結果) |
| MB-IO-03 SwiftUI 登録でも同じに働く | `DialogViewRegistry.swift:63` (SwiftUI 1引数登録) | `ios/Tests/KsDialogsTests/DialogTypedShowTests.swift:156` | ✅ 一致 |

## 4. android-native — Requirement「Kotlin 公開面の VM 供給と型指定呼び出し」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-AN-01 正の compile 検査 | `DialogViewRegistry.kt:50,67`・`ComposeDialogRegistration.kt:23`・`KsDialogs.kt:48,70` | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/DialogModelBindingApiSurfaceChecks.kt` (View / Compose 両系統・コンストラクタ参照 `::ConsumerModelBindingView`・suspend configure・placement) | ✅ 一致 (`./gradlew test` に `:api-surface-check:compileDebugKotlin` として同梱、成功) |
| MB-AN-02 Compose 登録でも同じに働く | `ComposeDialogRegistration.kt:23` | `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogModelBindingTests.kt` | ✅ 一致 (instrumented で実行) |
| MB-AN-03 value class の VM の構成ミス拒否 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModelTypeCheck.kt` (`box-impl` の有無で判定)・`DialogViewRegistry.kt` の登録時検査・`DialogPresenter.kt:63` の提示共通入口 | `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogTypedShowTests.kt:167,196,219` (登録 / VM factory 登録 / 型指定 show / インライン show / インスタンス渡し show の5経路) | ⚠️ deviation 記録済み (拒否点を全 show 経路へ拡大・失敗種別が `ValueClassViewModel` に変化 — deviation 20) |

## 5. maui-binding

### Requirement「C# 公開面の VM 供給と型指定呼び出し」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-MA-01 正の compile 検査 | `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs`・`Presentation/Dialog.cs:85-117` (2型引数形 4 オーバーロード)・`Registry/DialogViewRegistry.cs:81,95,114,128` | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogModelBindingApiSurfaceChecks.cs` (bool 省略形 / カスタム結果型・同期 / 非同期 configure・既存インスタンス渡し / インライン show との同居) | ✅ 一致 (`dotnet test` が ProjectReference 経由でビルド、成功。design Open Question 2 = 重複解決の成立が実証された) |
| MB-MA-02 負の compile 検査 | 各所の `where TViewModel : class` 制約・`IDialogViewModel` 制約・`DialogNotifier<TResult>` の型付け | `maui/KsDialogs.Maui.ApiSurfaceCheck/NegativeChecks/RejectsValueTypeViewModel.cs` / `RejectsNonContractTypedShow.cs` / `RejectsMismatchedNotifierResultType.cs` | ⚠️ deviation 記録済み (notifier 取得の受け手に class 制約なし — deviation 14。本検証で3フラグとも期待診断 CS0452×2 / CS0311 / CS0029 で失敗 = 期待結果) |

拡張プロパティ (`vm.Notifier`) は `maui/KsDialogs.Maui/Contract/DialogViewModelExtensions.cs:31` で成立 — design Open Question 1 は「拡張プロパティで書ける」で決着 ✅。

### Requirement「1行登録 (RegisterForDialog)」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-MA-03 1行登録ペアがインスタンス渡し show で表示 | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs` `RegisterPair` (TryAddTransient・2スロット配線) | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:31` | ✅ 一致 |
| MB-MA-04 1行登録だけで型指定 show が DI 解決の VM で動く | 同上 `StoreViewModelFactory` (`GetRequiredService<TViewModel>`) | 同ファイル `:55` | ✅ 一致 |
| MB-MA-09 コンストラクタに同一インスタンスが1回だけ | 同ファイル `CreateView` / `TakesViewModel` (`ActivatorUtilities.CreateInstance(provider, viewType, viewModel)`) | 同ファイル `:81` (`CreatedCount` が 0 = 追加生成なし) | ✅ 一致 |

`RegisterForDialog<TView, TViewModel, TResult>` の対追加は deviation 13 記録済み ⚠️ (非破壊)。

### Requirement「fallback resolver」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-MA-05 明示登録が fallback より優先 | `maui/KsDialogs.Maui/Internals/DialogResolution.cs:26,57` | `DialogDependencyInjectionTests.cs:109` (fallback 呼び出し数 0 も検証) | ✅ 一致 |
| MB-MA-06 未登録型が View fallback で解決・BindingContext 設定 | `DialogResolution.cs:35-41` | 同ファイル `:140` | ✅ 一致 |
| MB-MA-07 fallback が解決できない場合は構成ミス失敗 | `DialogResolution.cs:38,44` | 同ファイル `:164` | ⚠️ deviation 記録済み (失敗タイミングが View factory 実行時 — deviation 15。観察可能な挙動は spec どおり) |
| MB-MA-08 VM fallback の既定実装 | `maui/KsDialogs.Maui/Hosting/KsDialogsOptions.cs` `UseViewModelFallback()` | 同ファイル `:177` | ✅ 一致 |
| MB-MA-10 明示 VM factory + View fallback | `DialogResolution.cs` のスロット独立判定 | 同ファイル `:202` | ✅ 一致 |

`AddKsDialogs` の provider ホルダは `maui/KsDialogs.Maui/Hosting/KsDialogsInitializer.cs` + `Internals/DialogServiceProvider.cs` (どちらも internal。static な公開差し込み口なし ✅)。スロット単位の合成は `DialogViewRegistry.MergeFallbacks` で成立し、追加テスト2本 (引数なし再呼び出しで消えない / 別々の呼び出しで両方残る) と初期化サービスの冪等性テストが固定している。

## 6. kmp-facade

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MB-KM-01 KMP 面の1引数登録とアクセサ | `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の1引数 `register` (UIView / SwiftUI × 結果型あり/なし) | `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift:18` | ⚠️ deviation 記録済み (テスト置き場が Kotlin iosTest ではなく Swift 側 — deviation 6。共有コードからの show は `KsDialogsInteropBridge` 経由で再現) |
| MB-KM-04 結果型不一致は typed error | `KsDialogsKmp.swift` の `notifier(for:result:)` + `ios/Sources/KsDialogs/Registry/DialogDeclaredResultType.swift` | 同ファイル `:49,81` (show 前の nil・不一致の typed error・show 時点の型固定を検証) | ⚠️ deviation 記録済み (同 deviation 6) |
| MB-KM-02 共有 VM に Native 拡張の notifier | 追加実装なし (typealias。`android/ksdialogs/.../DialogViewModel.kt:39` がそのまま効く) | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/KmpViewModelSupplyTests.kt` | ⚠️ deviation 記録済み (androidHostTest は提示先を持てないため「報告結果が呼び出し元へ届く」部分は Native 同名契約テストと Sample 通しに委譲 — deviation 7) |
| MB-KM-03 UI 層参照なしの共有コードが型付き結果を受け取る | commonMain の呼び出し面は変更なし (proposal の Non-Goals どおり) | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/SharedLayerCallTests.kt` (iOS / Android 両ターゲットで実行) + `kasane/changes/add-model-binding-di/verification/kmp-model-binding/README.md` の実 framework 越し証跡 | ✅ 一致 |

## 7. samples

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| MB-SM-01 Model Dialog の完了経路 | 文言: `samples/ios/KsDialogsSample/SampleText.swift:32`・`samples/android/.../SampleText.kt:48`・`samples/maui/KsDialogs.Sample.Maui/SampleText.cs:49`・`samples/kmp/shared/.../SampleText.kt:48` (4ルートとも `Model Dialog` / `ViewModel から表示しています`)<br>中身: 各ルートの `ModelDialogCardView` / `ModelDialogCard` | `evidence/ios-model-dialog-completed.png`・`android-`・`maui-`・`kmp-ios-`・`kmp-android-` の completed 5枚。ダイアログ表示は `ui/verification/*-model-dialog.png` | ⚠️ 自動テスト対象外 (deviation 17 で `scripts/scenario-id-coverage.py` の除外に登録済み。PB-SM-01〜03 の先例踏襲) |
| MB-SM-02 4ルートで同一デモ | 経路: ios `samples/ios/KsDialogsSample/SampleMenuModel.swift:52` (型指定 show)・android `samples/android/.../MainActivity.kt:74` (型指定 show)・maui `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:39` (型指定 show)・kmp `samples/kmp/shared/.../SamplePresenter.kt:49` (インスタンス渡し) — spec の「ルートごとの主経路」どおり | 上記 completed 5枚 + cancelled 5枚 | ⚠️ deviation 記録済み (MAUI は Android 面のみで通し。iOS 面は Xcode バージョン制約でビルド不可 — deviation 9) + 自動テスト対象外 (deviation 17) |
| MB-SM-03 MAUI が DI チェーン構成で全デモを通す | `samples/maui/KsDialogs.Sample.Maui/MauiProgram.cs:25` (`AddKsDialogs().RegisterForDialog<ModelDialogCardView, ModelDialogViewModel>()`) | `evidence/maui-model-dialog-completed.png` / `-cancelled.png` | ⚠️ 自動テスト対象外 (deviation 17)・MAUI iOS 面未実施 (deviation 9) |

---

## 8. 追加検査

### 8.1 tasks.md の突き合わせ

`kasane/changes/add-model-binding-di/tasks.md` の全 25 タスクが `[x]`。対応表と突き合わせた結果、**未実装なのにチェック済みの虚偽は 0 件**。

- 1.1〜1.6 (契約と iOS Native): 実装・テスト・compile 検査すべて確認 ✅
- 2.1〜2.6 (Android Native): 同上 ✅
- 3.1〜3.7 (MAUI): 3.7 の「不成立なら実装を止めて報告」は不要 — 2型引数形でオーバーロード解決が成立し、MB-MA-01 が既定ビルドで通る ✅
- 4.1〜4.3 (KMP): 4.3 の証跡は `verification/kmp-model-binding/README.md` + `evidence/kmp-*` ✅
- 5.1〜5.6 (Samples): 5.6 の視覚照合結果は `ui/brief.md` の「照合結果」節と `ui/verification/*.png` 10枚 ✅
- 6.1〜6.3 (横断検証): 本検証で再実行して確認 ✅

### 8.2 逆流検査 (足場アーティファクトの書き換え)

`git diff 5a2c0c3 -- kasane/changes/add-model-binding-di/` の結果、変更されているのは:

- `tasks.md` — チェックボックスのみ (許容)
- `ui/brief.md` — 「照合結果」節と「未解決 (オーナー確認待ち)」節の**追記のみ**。既存記述の書き換えなし (許容)

`proposal.md` / `design.md` / `specs/*/spec.md` は**いずれも無変更**。**逆流なし** ✅

### 8.3 未記録乖離の洗い出し

対応表に ❌ は 0 件。diff にあって Scenario に対応しない変更はすべて deviation.md の `[付随修正]` 4 項目 (samples の README 群 / kmp テストダミー / `scripts/scenario-id-coverage.py` の 3 箇所) で説明が付く。**未記録乖離 0 件** ✅

観察のみ (乖離ではない):

- `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の1引数登録は、spec が求める `(vm) → UIView` に加えて SwiftUI 系統・結果型指定版も対で追加している。既存の2引数登録が同じ4形を持つため、技術別オーバーロード (core/ADR-0011) の対称性を保った非破壊の上位集合であり、Requirement に反しない
- `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj` への3フラグ追加は MB-MA-02 の実施手段そのもの

### 8.4 UI 変更の検査

- `ui/brief.md` の「承認モック」節: mock 省略のオーナー判断 (2026-08-24) が記録済み ✅
- 「照合結果」節に視覚照合の結果 (5組・乖離ゼロ・合意済み妥協 0 件) が記録済み ✅
- 合意済み妥協の記録: MAUI iOS 面未撮影 (deviation 9)・メニュー行の `›` の食い違い (deviation 8。**オーナー判断待ちとして brief.md にも明記済み**) ⚠️

### 8.5 テスト実行 (本検証で再実行)

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | **158 passed / 0 failed / 0 skipped** (`TEST SUCCEEDED`) |
| android/ | `./gradlew test --rerun-tasks` | **66 tests / 0 failures** (`:ksdialogs:verifyNoDeclarativeUiDependency` と `:api-surface-check:compileDebugKotlin` を含む) |
| android/ (instrumented) | `ANDROID_SERIAL=<android-serial> ./gradlew connectedDebugAndroidTest` | **139 tests / 0 failures / 1 skipped** (`:ksdialogs` 105 + `:ksdialogs-compose` 34。skip 1 件は API レベル依存の `PB_SB_04` で本変更と無関係。MB-AN-02 の実行を結果 XML で確認済み) |
| kmp/ | `./gradlew allTests --rerun-tasks` | **57 tests / 0 failures** (iosSimulatorArm64 31 + androidHostTest 26) |
| maui/ | `dotnet test` | **86 tests / 0 failures** |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **15 tests / 0 failures** |

本変更が追加した負の compile 検査 4 本を個別に実行し、いずれも期待診断で失敗 (= 期待結果) することを確認:

| 検査 | フラグ | 出た診断 |
|---|---|---|
| MB-IO-02 | `KSDIALOGS_NEGATIVE_CHECK_VALUE_TYPE_VIEW_MODEL` | `non-class type 'ConsumerValueDialogViewModel' cannot conform to class protocol 'DialogViewModel'` |
| MB-MA-02 (契約外の型) | `KsDialogsNegativeCheckTypedShowContract` | CS0311 (`string` → `IDialogViewModel` の変換なし) |
| MB-MA-02 (結果型不一致の notifier) | `KsDialogsNegativeCheckNotifierResultType` | CS0029 (`DialogNotifier<bool>` → `DialogNotifier<string>`) |
| MB-MA-02 (値型 VM) | `KsDialogsNegativeCheckValueTypeViewModel` | CS0452 × 2 (登録・型指定 show の両方) |

### 8.6 Scenario ID 網羅検査

```
python3 scripts/scenario-id-coverage.py --show-locations
```

結果: **未網羅なし** (合計 91/97、除外 6 件 = PB-SM-01〜03 + MB-SM-01〜03)。本変更分は MB-AN 3/3・MB-IO 3/3・MB-KM 4/4・MB-MA 10/10・MB-NI 7/7・MB-TS 6/6・MB-SM 0/3 (除外 3 件)。

### 8.7 旧挙動テストの残骸

MB-NI-04 (同一 VM インスタンスの並行 show は失敗) と矛盾する既存テスト「同一 VM インスタンスの再 show は独立した重ね出しになる」は、3形態とも別インスタンス検証へ書き換え済み (deviation 5・16・21)。**旧挙動を固定するテストの残存なし** ✅

- `ios/Tests/KsDialogsTests/DialogRegistryTests.swift:65`
- `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogRegistryTests.kt:71`
- `maui/KsDialogs.Maui.Tests/DialogRegistryTests.cs:58`

---

## 9. 集計

Scenario 単位 (全 36 件):

| 区分 | 件数 |
|---|---|
| ✅ 一致 | 26 |
| ⚠️ deviation 記録済み | 10 |
| ❌ 欠落・乖離 | **0** |

⚠️ の内訳: MB-TS-05 (deviation 3)・MB-AN-03 (20)・MB-MA-02 (14)・MB-MA-07 (15)・MB-KM-01 (6)・MB-KM-04 (6)・MB-KM-02 (7)・MB-SM-01 (17)・MB-SM-02 (17・9)・MB-SM-03 (17・9)

Requirement 文面に対する記録済み差分 (Scenario 判定には影響しない): VM factory 登録 API の名前 (deviation 4・12)・`RegisterForDialog` の3型引数版の対追加 (13)。

未参照の deviation は 0 件 — deviation.md の 21 項目はすべて本検証で該当箇所に突き当たった。

---

## 追記 (2026-08-25・追加修正後の確認)

verify-001 の VALID 判定を出した後、オーナー裁定による追加修正が MAUI に入った (レビュー証跡: `kasane/changes/add-model-binding-di/review-003.md`、判定 APPROVED)。差分を再検査した。

### 差分の内容

| ファイル | 変更 |
|---|---|
| `maui/KsDialogs.Maui/Contract/DialogException.cs` | `DialogException.ValueTypeViewModel` の新設 (公開面追加) |
| `maui/KsDialogs.Maui/Internals/DialogPresenter.cs` | 共通提示入口 `PresentCoreAsync` を新設し、その先頭で値型 VM を実行時拒否 (factory 解決・提示委譲・紐付けのいずれよりも前)。レジストリ経由 / インライン / 型指定の全 show 経路が同じ入口を通る |
| `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:285,312`<br>`maui/KsDialogs.Maui.Tests/Support/ModelBindingTestTypes.cs:121` | 拒否テスト2件 (View fallback 構成下 / fallback なし構成) と値型テスト VM の追加 |
| `Hosting/KsDialogsServiceCollectionExtensions.cs`・`Contract/DialogViewModelExtensions.cs`・`Presentation/IKsDialogs.cs` | doc comment のみ |
| `kasane/changes/add-model-binding-di/deviation.md` | 「maui-binding spec (値型 VM の拒否点)」を追記。既存2件 (`›` 承認・notifier の class 制約) にも注記・更新 |
| `kasane/changes/add-model-binding-di/ui/brief.md` | 「未解決 (オーナー確認待ち)」節を「→ 解決済み」に更新 (`›` ありを承認) |

ios / android / kmp / samples / scripts に変更はない。

### Scenario 対応表への影響

**不変。** 対応表の全 36 行について、実装の置き場・テストの置き場・判定のいずれも変わらない。

- **MB-MA-01〜10**: 実装は `PresentCoreAsync` へ集約されたが、公開面のシグネチャ・解決順序 (`DialogResolution`) はそのまま。値型検査は `resolveFactory()` の**前**に入るため、参照型 VM を使う MB-MA-03〜10 の経路には一切かからない
- **MB-MA-07**: `UnresolvableFallbackTestViewModel` は参照型のため、失敗種別は `ViewFactoryNotRegistered` のまま (deviation 15 の記述どおり)
- **MB-NI-01〜07 / MB-TS-01〜06 (MAUI 分)**: 紐付け・除去 (`Bind` / `try…finally` の `Unbind`) の位置と順序は変わらない
- **MB-MA-02 (負の compile 検査)**: `class` 制約は削っていないため、コンパイル時の拒否はそのまま成立する

### 判定への影響

**VALID を維持。**

- 追加された実行時拒否は、maui-binding spec の Requirement 文言「型指定 show・登録・notifier 取得のジェネリック制約には `class` を含め、値型 VM をコンパイル時に拒否する」に対する**上乗せ** (spec 超過) である。spec が守ろうとした不変条件 (値型 VM を扱わない) は弱まるどころか強化されており、コンパイル時に拒否できない経路 (interface 受けのインスタンス渡し `ShowAsync`) を塞いでいる
- この上乗せは `deviation.md` の「maui-binding spec (値型 VM の拒否点)」に**合意済み差分として記録済み** (オーナー裁定 2026-08-25)。付随する失敗種別の変化 (`ViewFactoryNotRegistered` → `ValueTypeViewModel`) と公開例外型の追加も同エントリに明記されている
- したがって **deviation 記録済みの spec 超過として扱える**。未記録乖離ではない
- android-native の同型修正 (deviation 20、`ValueClassViewModel`) と対称になり、dialog-contract の「VM 契約は参照型に限定する」を4形態で揃った形で満たすようになった

新設の公開例外型 `DialogException.ValueTypeViewModel` は対応する Scenario を持たないが、同エントリの記録範囲に含まれる。`ViewModelFactoryNotRegistered` (MB-TS-03) / `ViewModelAlreadyShowing` (MB-NI-04) / `ServiceProviderUnavailable` (Decision 5 の配線) は verify-001 時点で確認済み。

### 逆流検査 (再実施)

`git diff 5a2c0c3 -- kasane/changes/add-model-binding-di/proposal.md design.md specs/` は**空** — 足場アーティファクトの書き換えなし ✅
`ui/brief.md` は差分に削除行がなく、追記と追記済み節の更新のみ ✅

### テスト再実行

| 検査 | 結果 |
|---|---|
| `dotnet test` (maui/) | **88 tests / 0 failures** (verify-001 時点の 86 + 値型拒否テスト2件) |
| MB-MA-02 `KsDialogsNegativeCheckTypedShowContract` | CS0311 で失敗 = 期待結果 |
| MB-MA-02 `KsDialogsNegativeCheckNotifierResultType` | CS0029 で失敗 = 期待結果 |
| MB-MA-02 `KsDialogsNegativeCheckValueTypeViewModel` | CS0452 (登録・型指定 show の2箇所) で失敗 = 期待結果 |
| `python3 scripts/scenario-id-coverage.py` | **未網羅なし** |

ios / android / android instrumented / kmp / maui-bridge は差分がないため verify-001 の実測 (158 / 66 / 139 / 57 / 15、いずれも 0 failures) をそのまま有効とする。

### 集計 (更新なし)

Scenario 単位: ✅ 26 / ⚠️ 10 / ❌ **0**。Requirement 文面に対する記録済み差分に「値型 VM の拒否点」(deviation 22) が1件加わる。

**最終判定: VALID**
