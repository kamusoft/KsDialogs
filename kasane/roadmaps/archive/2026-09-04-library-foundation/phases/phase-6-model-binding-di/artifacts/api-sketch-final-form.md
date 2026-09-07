# API スケッチ: phase-6 完了時点の最終形イメージ (2026-08-17)

phase-5-2 の議論 (既定結果型 Bool = core/ADR-0012、notifier の VM 注入織り込み、原典水準1行登録の必須要件化) から生まれた到達イメージ。**名前・シグネチャは未確定のスケッチ** — phase-6 の spec 化で確定する。

前提となる決定: core/ADR-0011 (技術別オーバーロード)・core/ADR-0012 (既定結果型 Bool)・kmp/ADR-0004 (Swift 向け KMP 面)・phase-6 必須要件 (1行登録)・VM 注入 (phase-6 ライフサイクル論点)。

## MAUI (C#)

```csharp
// 1行登録 (DI チェーン) — 原典水準
builder.Services
    .RegisterForDialog<OKDialog, OKDialogViewModel>()
    .RegisterForDialog<TextInputDialog, TextInputViewModel>();

// VM 定義: bool 既定なら結果型の宣言なし
public sealed class OKDialogViewModel : IDialogViewModel { }
public sealed class TextInputViewModel : IDialogViewModel<string> { }   // カスタム型だけ宣言

// 呼び出し
var result = await Dialog.Instance.ShowAsync(new OKDialogViewModel());   // DialogResult<bool>
var input  = await Dialog.Instance.ShowAsync(new TextInputViewModel());  // DialogResult<string>
```

## iOS (Swift)

```swift
// 登録 — VM 注入後は notifier 引数なし
Dialog.shared.registry.register(OKViewModel.self) { vm in
    OKDialogView(viewModel: vm)            // UIKit 版
}
Dialog.shared.registry.register(OKViewModel.self) { vm in
    OKDialogContent(viewModel: vm)         // SwiftUI 版 (some View)
}
// 型のみ1行登録 (View 側が init(viewModel:) 規約に従う場合の糖衣。要否は phase-6 で判断)
Dialog.shared.registry.register(OKDialogView.self, for: OKViewModel.self)

// VM 定義: bool 既定ならResult 宣言なし (デフォルト associatedtype)
final class OKViewModel: DialogViewModel { }
final class TextInputViewModel: DialogViewModel { typealias Result = String }

// View からの結果報告 (VM 注入): vm 経由で叩く。細部 (スロットの形・基底クラス糖衣) は phase-6
// 例: viewModel.notifier?.complete(true)

// 呼び出し
let result = try await Dialog.shared.show(OKViewModel())        // DialogResult<Bool>
let input  = try await Dialog.shared.show(TextInputViewModel()) // DialogResult<String>
```

## Android (Kotlin)

```kotlin
// 登録 — VM 注入後は notifier 引数なし (レシーバ this = Context)
Dialog.instance.registry.register(OKViewModel::class) { vm ->
    OKDialogView(this, vm)                 // View 版
}
Dialog.instance.registry.registerCompose(OKViewModel::class) { vm ->
    OKDialogContent(vm)                    // Compose 版 (@Composable)
}
// 型のみ1行登録 (コンストラクタ参照が (Context, VM) -> View に一致する場合の糖衣。要否は phase-6)
Dialog.instance.registry.register(OKViewModel::class, ::OKDialogView)
// Koin 連携の書き味は phase-6 の DI 論点で検討

// VM 定義: bool 既定は typealias の顔 (名前は spec 化で確定)
class OKViewModel : SimpleDialogViewModel          // = DialogViewModel<Boolean>
class TextInputViewModel : DialogViewModel<String>

// 呼び出し
val result = Dialog.instance.show(OKViewModel())        // DialogResult<Boolean>
val input  = Dialog.instance.show(TextInputViewModel()) // DialogResult<String>
```

## KMP (共有コード + Swift 側)

```kotlin
// 共有コード (commonMain) からの呼び出し — 変更なし
val result = KsDialogs.instance.show(SharedConfirmViewModel(...))   // DialogResult<Boolean>
```

```swift
// Swift 側の登録 (kmp/ADR-0004 + result: 省略 = Bool)
KsDialogs.kmpRegistry.register(SharedConfirmViewModel.self) { vm in   // result: Bool.self 省略
    ConfirmView(viewModel: vm)
}
// カスタム型は明示
KsDialogs.kmpRegistry.register(SharedInputViewModel.self, result: String.self) { vm in ... }
```

## 注記

- notifier 引数つきの factory 形 `(vm, notifier) -> View` は低水準 API として残る (phase-5 帯の基本形。VM 注入は非破壊追加)
- 型のみ1行登録 (Swift / Kotlin の糖衣) は原典水準パリティの Native 版としてのスケッチであり、採否自体が phase-6 の論点
