# scout 調査: 登録 API の現状 (4形態) と KsSettingsView の View 技術両対応 (2026-08-17)

phase-5-2 論点「コンテンツ View 技術の両対応」の議論素材。ksn-scout による調査報告。

---

## 調査1: KsDialogs の登録 API・View factory の現状 (4形態)

| 形態 | 登録 API シグネチャ | キー | factory が返す型 | VM 契約 (結果型の宣言) | show |
|---|---|---|---|---|---|
| **iOS (Swift)** | `Dialog.shared.registry.register(_ viewModelType: ViewModel.Type, factory: @escaping @MainActor @Sendable (ViewModel, DialogNotifier<ViewModel.Result>) -> UIView)` | メタタイプ (`ViewModel.Type` → 内部 `DialogViewModelKey`) | **`UIView`** (非 Optional。型消去面 `DialogViewFactory.makeView` は `UIView?`) | `protocol DialogViewModel: Sendable { associatedtype Result: Sendable }` | `func show<ViewModel: DialogViewModel>(_ viewModel: ViewModel) async throws -> DialogResult<ViewModel.Result>` |
| **Android (Kotlin)** | `Dialog.instance.registry.register(viewModelClass: KClass<VM>, factory: Context.(VM, DialogNotifier<R>) -> View)` (`<R, VM : DialogViewModel<R>>`) | `KClass<VM>` | **`android.view.View`** (レシーバに提示先 `Context`) | `interface DialogViewModel<R>` | `suspend fun <R> show(viewModel: DialogViewModel<R>): DialogResult<R>` |
| **MAUI (C#)** | `Dialog.Instance.Registry.Register<TViewModel, TResult>(Func<TViewModel, DialogNotifier<TResult>, View> factory) where TViewModel : IDialogViewModel<TResult>` | `typeof(TViewModel)` | **`Microsoft.Maui.Controls.View`** | `interface IDialogViewModel<TResult>` | `Task<DialogResult<TResult>> ShowAsync<TResult>(IDialogViewModel<TResult> viewModel)` |
| **KMP** | **登録 API は commonMain に存在しない**。commonMain は `public interface DialogViewRegistry` (メンバなしのハンドル) のみ。実登録は各 OS の Native 側で行う (kmp/ADR-0002 の全委譲) | Android: 共有 VM の `KClass` がそのまま Native キー / iOS: 共有 VM の ObjC クラス | Android: `android.view.View` / iOS: `UIView` (ObjC 互換の機械面経由) | commonMain `expect interface DialogViewModel<R>` | `@Throws(...) suspend fun <R> show(viewModel: DialogViewModel<R>): DialogResult<R>` |

根拠パス:
- `ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:21-30` / `Registry/DialogViewFactory.swift` / `Contract/DialogViewModel.swift:7-10` / `Presentation/KsDialogs.swift` / `Presentation/Dialog.swift`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt:29-42` / `DialogViewFactory.kt` / `DialogViewModel.kt` / `KsDialogs.kt`
- `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:37-50` / `Presentation/IKsDialogs.cs` / `Contract/DialogViewModel.cs`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewRegistry.kt` (「登録は各 OS の Native API で行う」と明記)

**KMP iOS の現状の登録入口**は ObjC 互換の機械面 (`ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:37-51`):

```swift
@objc(registerViewFactoryForViewModelClass:resultType:factory:)
public func registerViewFactory(
    forViewModelClass viewModelClass: AnyClass,
    resultType: KsDialogsInteropResultType,
    factory: @escaping @MainActor (Any, KsDialogsInteropNotifier) -> UIView
)
```

VM は `Any` に型消去され、結果型は `KsDialogsInteropResultType` の自己申告。KMP Sample がこの機械面を直接使用中 (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleDialogRegistration.swift:14-18`) — kmp/ADR-0003 (proposed) が「利用者向け API は Swift パッケージ側に別途設計、差し替えは phase-5 の作業」とした未解消事項 (verify-001 ❌3)。

**MAUI「型引数2つ明示」の該当コード**: `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:37-39`。`TResult` は制約 `where TViewModel : IDialogViewModel<TResult>` からしか現れず、引数は型なしラムダのため C# の型推論が効かない → 利用側は必ず両方書く (`samples/maui/KsDialogs.Sample.Maui/SampleDialogRegistration.cs:12` → `Register<BasicDialogViewModel, bool>(...)`)。Swift は associatedtype、Kotlin は上限境界から推論されるため明示不要 — MAUI だけが非対称。(※因果の説明は scout の型システム上の解釈)

## 調査2: KsSettingsView の View 技術両対応

パス: `../KsSettingsView`

**両対応している。ただし「1つの登録 API が2技術を受ける」形ではなく、モジュール分割 + sealed な型消去ラッパ + 用途別ホスティングの3本立て。**

**(a) モジュール分割**: `core` (技術中立 model) / `ui` (従来 View 系 Native Host) / 宣言 UI 層 / `bridge` (MAUI 用) の4層。描画の実体は常に従来 View 系の Host (UIKit `KsSettingsViewController` / RecyclerView) で、SwiftUI・Compose は**その上の Bridge** (`UIViewControllerRepresentable` / `AndroidView`)。両方式は `SettingsRootStore → Native Host` の同一更新経路へ収束 (`kasane/concepts/core/architecture/declarative-ui-bridge.md`)。

**(b) 装飾領域 (Header/Footer) = sealed 型消去ラッパ `KsAnyView`**:
- iOS `KsSettingsViewCore/KsAnyView.swift` — `enum Backing { case swiftUI(() -> AnyView); case uiKit(() -> UIView) }` + 静的ファクトリ `KsAnyView.swiftUI { }` / `.uiKit { }`
- Android `core/KsAnyView.kt` — `sealed interface KsAnyView { class Compose(@Composable content); class AndroidView((Context) -> View) }`
- 但し書き: `KsAnyView` は意図的に `Equatable`/`equals` 非準拠 (クロージャは値等価を持てない)。差分検出に参加せず、更新は描画レイヤに委ねる
- ホスティング実体: Android は `ComposeView` を addView + `setContent` (`DisposeOnDetachedFromWindow` 強制)、iOS は `UIHostingConfiguration` を `contentConfiguration` に適用 (`KsSettingsViewController.swift:1931,1961`)

**(c) 行 (Cell) は別の解**:
- `CustomCell` — builder は **SwiftUI / Compose 専用**。ネイティブ View を使いたい場合は専用の口を設けず `UIViewRepresentable` / `AndroidView { }` の**公式 interop に丸投げ** (明文の設計判断)
- `KsCellRegistry` — 一級市民セル用の登録 API は**従来 View 系のみ**。宣言 UI で行を書きたい人は Registry を触らず CustomCell を使う「3層の使い分け」

**(d) 形態ごとの見え方**:
- MAUI: 両対応の分岐は消え `Microsoft.Maui.Controls.View` 一本。MAUI View を native platform view に実体化する専用機構 (`IKsViewMaterializer` + `IKsViewLease` + `KsAccessoryHostView`) + CustomCell content には世代トークン (`kasane/concepts/maui/architecture/view-materialization.md`)
- KMP: **KsSettingsView に KMP 形態は存在しない** — KMP 形態での両対応の先例は取れない

## KsDialogs 側への含意 (scout の分析、選択肢の整理)

- 現状の factory 戻り値は `UIView` / `android.view.View` の1系統固定で、宣言 UI の受け口は未存在
- 選択肢: (i) factory 戻り値を `KsAnyView` 相当の sealed 二択にする / (ii) 戻り値は従来 View のままで宣言 UI は利用者が公式 interop で包む / (iii) 登録 API を技術別に分ける
- KMP は KsSettingsView に前例がなく、kmp/ADR-0003 の Swift 向け登録 API 設計と両対応を同時に扱う必要がある

## 未確認事項

- KsSettingsView の `bridge` 層が `KsAnyView` の二択を interop 越しにどう運ぶかの詳細 (概念文書レベルの把握のみ)
