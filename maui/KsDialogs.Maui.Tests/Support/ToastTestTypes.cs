using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>1 行登録糖衣の検証に使うカスタム Toast の ViewModel。</summary>
internal sealed class RegisteredToastTestViewModel : IToastViewModel;

/// <summary>1 行登録糖衣の検証に使う、コンストラクタ引数を持たない中身の View。</summary>
internal sealed class RegisteredToastTestView : ContentView;

/// <summary>ViewModel をコンストラクタで受け取るカスタム Toast の中身の View。</summary>
/// <param name="viewModel">表示対象の ViewModel。</param>
internal sealed class DependentToastTestView(DependentToastTestViewModel viewModel) : ContentView
{
    /// <summary>コンストラクタで受け取った ViewModel。</summary>
    public DependentToastTestViewModel InjectedViewModel { get; } = viewModel;
}

/// <summary>コンストラクタ依存が注入されることの検証に使うカスタム Toast の ViewModel。</summary>
/// <param name="dependency">サービスから注入される依存。</param>
internal sealed class DependentToastTestViewModel(ModelBindingTestDependency dependency) : IToastViewModel
{
    /// <summary>注入された依存。</summary>
    public ModelBindingTestDependency Dependency { get; } = dependency;
}

/// <summary>配置の供給経路の検証に使う、素のカスタム Toast の ViewModel。</summary>
internal sealed class PlainToastTestViewModel : IToastViewModel;

/// <summary>Toast レジストリに登録しないままにしておく検証用 ViewModel。</summary>
internal sealed class UnregisteredToastTestViewModel : IToastViewModel;

/// <summary>ViewModel の型を渡す表示の検証に使う、状態を持つ ViewModel。</summary>
internal sealed class TypedToastTestViewModel : IToastViewModel
{
    /// <summary>configure が設定する状態。中身の生成から読めることを見るために使う。</summary>
    public string Message { get; set; } = string.Empty;
}

/// <summary>ViewModel factory を登録しないままにしておく、型を渡す表示の検証用 ViewModel。</summary>
internal sealed class ViewModelFactoryMissingToastTestViewModel : IToastViewModel;

/// <summary>1 行登録だけで型を渡す表示が使えることの検証に使う ViewModel。</summary>
internal sealed class TypedRegisteredToastTestViewModel : IToastViewModel;

/// <summary>1 行登録だけで型を渡す表示が使えることの検証に使う中身の View。</summary>
internal sealed class TypedRegisteredToastTestView : ContentView;

/// <summary>インライン factory とレジストリの独立の検証に使う ViewModel。</summary>
internal sealed class InlineToastTestViewModel : IToastViewModel;

/// <summary>レジストリの独立性の検証に使う、Loading と Toast の両方の契約に準拠する ViewModel。</summary>
internal sealed class SharedContractToastTestViewModel : ILoadingViewModel, IToastViewModel;

/// <summary>
/// 値型のカスタム Toast の ViewModel。参照型限定が実行時にも守られることの検証にだけ使う。
/// </summary>
/// <remarks>
/// 登録と 1 行登録糖衣は class 制約でコンパイルできないため、この型は interface 引数の表示からのみ
/// 渡せる。
/// </remarks>
internal readonly struct ValueTypeToastTestViewModel : IToastViewModel;

/// <summary>解決できない依存を要求するカスタム Toast の中身の View。</summary>
/// <param name="dependency">サービスに登録されていない依存。</param>
internal sealed class UnconstructableToastTestView(UnregisteredTestDependency dependency) : ContentView
{
    /// <summary>注入されるはずだった依存。</summary>
    public UnregisteredTestDependency Dependency { get; } = dependency;
}

/// <summary>解決できない依存を要求する View を結び付けたカスタム Toast の ViewModel。</summary>
internal sealed class UnconstructableViewToastTestViewModel : IToastViewModel;
