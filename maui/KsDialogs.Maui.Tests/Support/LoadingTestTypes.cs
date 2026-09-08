using System.Collections.Generic;
using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>1 行登録糖衣の検証に使うカスタム Loading の ViewModel。</summary>
internal sealed class RegisteredLoadingTestViewModel : ILoadingViewModel;

/// <summary>1 行登録糖衣の検証に使う、コンストラクタ引数を持たない中身の View。</summary>
internal sealed class RegisteredLoadingTestView : ContentView;

/// <summary>スコープ形での 1 行登録の検証に使うカスタム Loading の ViewModel。</summary>
internal sealed class ScopedRegisteredLoadingTestViewModel : ILoadingViewModel;

/// <summary>スコープ形での 1 行登録の検証に使う中身の View。</summary>
internal sealed class ScopedRegisteredLoadingTestView : ContentView;

/// <summary>ViewModel をコンストラクタで受け取るカスタム Loading の中身の View。</summary>
/// <param name="viewModel">表示対象の ViewModel。</param>
internal sealed class DependentLoadingTestView(DependentLoadingTestViewModel viewModel) : ContentView
{
    /// <summary>コンストラクタで受け取った ViewModel。</summary>
    public DependentLoadingTestViewModel InjectedViewModel { get; } = viewModel;
}

/// <summary>コンストラクタ依存が注入されることの検証に使うカスタム Loading の ViewModel。</summary>
/// <param name="dependency">サービスから注入される依存。</param>
internal sealed class DependentLoadingTestViewModel(ModelBindingTestDependency dependency) : ILoadingViewModel
{
    /// <summary>注入された依存。</summary>
    public ModelBindingTestDependency Dependency { get; } = dependency;
}

/// <summary>進捗の受け口を実装したカスタム Loading の ViewModel。</summary>
internal sealed class ProgressReceivingLoadingTestViewModel : ILoadingViewModel, ILoadingProgressReceiver
{
    /// <summary>受け取った進捗を受信順に記録したもの。</summary>
    public List<double> ReceivedProgress { get; } = [];

    /// <inheritdoc/>
    public void OnProgress(double progress) => ReceivedProgress.Add(progress);
}

/// <summary>進捗の受け口を実装しないカスタム Loading の ViewModel。</summary>
internal sealed class PlainLoadingTestViewModel : ILoadingViewModel;

/// <summary>レジストリの独立性の検証に使う、Dialog と Loading の両方の契約に準拠する ViewModel。</summary>
internal sealed class SharedContractLoadingTestViewModel : IDialogViewModel, ILoadingViewModel;

/// <summary>Loading レジストリに登録しないままにしておく検証用 ViewModel。</summary>
internal sealed class UnregisteredLoadingTestViewModel : ILoadingViewModel;

/// <summary>ViewModel の型を渡す表示の検証に使う、状態と進捗の受け口を持つ ViewModel。</summary>
internal sealed class TypedLoadingTestViewModel : ILoadingViewModel, ILoadingProgressReceiver
{
    /// <summary>configure が設定する状態。中身の生成から読めることを見るために使う。</summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>受け取った進捗を受信順に記録したもの。</summary>
    public List<double> ReceivedProgress { get; } = [];

    /// <inheritdoc/>
    public void OnProgress(double progress) => ReceivedProgress.Add(progress);
}

/// <summary>ViewModel factory を登録しないままにしておく、型を渡す表示の検証用 ViewModel。</summary>
internal sealed class ViewModelFactoryMissingLoadingTestViewModel : ILoadingViewModel;

/// <summary>1 行登録だけで型を渡す表示が使えることの検証に使う ViewModel。</summary>
internal sealed class TypedRegisteredLoadingTestViewModel : ILoadingViewModel;

/// <summary>1 行登録だけで型を渡す表示が使えることの検証に使う中身の View。</summary>
internal sealed class TypedRegisteredLoadingTestView : ContentView;

/// <summary>インライン factory とレジストリの独立の検証に使う ViewModel。</summary>
internal sealed class InlineLoadingTestViewModel : ILoadingViewModel;

/// <summary>
/// 値型のカスタム Loading の ViewModel。参照型限定が実行時にも守られることの検証にだけ使う。
/// </summary>
/// <remarks>
/// 登録と 1 行登録糖衣は class 制約でコンパイルできないため、この型は interface 引数の表示からのみ
/// 渡せる。
/// </remarks>
internal readonly struct ValueTypeLoadingTestViewModel : ILoadingViewModel;

/// <summary>解決できない依存を要求するカスタム Loading の中身の View。</summary>
/// <param name="dependency">サービスに登録されていない依存。</param>
internal sealed class UnconstructableLoadingTestView(UnregisteredTestDependency dependency) : ContentView
{
    /// <summary>注入されるはずだった依存。</summary>
    public UnregisteredTestDependency Dependency { get; } = dependency;
}

/// <summary>解決できない依存を要求する View を結び付けたカスタム Loading の ViewModel。</summary>
internal sealed class UnconstructableViewLoadingTestViewModel : ILoadingViewModel;
