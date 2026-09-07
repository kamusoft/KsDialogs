using System;
using System.Collections.Generic;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Hosting;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>ViewModel 主導の呼び出し面の検証に使う、真偽値の顔の ViewModel。</summary>
internal sealed class ModelBindingTestDialogViewModel : IDialogViewModel
{
    /// <summary>configure が設定する状態。表示された中身から観察できることを見るために使う。</summary>
    public string Message { get; set; } = "既定";
}

/// <summary>結果型を宣言する側の、ViewModel 主導の呼び出し面の検証用 ViewModel。</summary>
internal sealed class ModelBindingTextTestDialogViewModel : IDialogViewModel<string>
{
    /// <summary>configure が設定する状態。</summary>
    public string Text { get; set; } = "既定";
}

/// <summary>等価比較が一致する別インスタンスを作れる検証用 ViewModel。</summary>
/// <param name="Message">ダイアログに出す文言。等価比較の対象になる。</param>
internal sealed record EquatableTestDialogViewModel(string Message = "同じ") : IDialogViewModel;

/// <summary>ViewModel factory を登録しないままにしておく検証用 ViewModel。</summary>
internal sealed class ViewModelFactoryMissingTestViewModel : IDialogViewModel;

/// <summary>型指定 show のスロット独立性の検証にだけ使う ViewModel。</summary>
internal sealed class SlotIndependenceTestViewModel : IDialogViewModel;

/// <summary>
/// 中身の View の代わりに使う、ViewModel 経由の報告口で結果を返せる View。
/// </summary>
/// <remarks>
/// BindingContext に入った ViewModel から報告口を引くため、ライブラリが BindingContext を
/// 設定する経路 (1 行登録・fallback resolver) の検証にそのまま使える。
/// </remarks>
internal class ReportingTestView : ContentView
{
    /// <summary>BindingContext の ViewModel が持つ報告口で完了を報告する。</summary>
    /// <param name="value">報告する結果値。</param>
    public void ReportCompleted(bool value) =>
        ((IDialogViewModel<bool>)BindingContext).Notifier!.Complete(value);
}

/// <summary>ViewModel をコンストラクタで受け取る中身の View。</summary>
/// <param name="viewModel">表示対象の ViewModel。</param>
internal sealed class ConstructorInjectedTestView(ConstructorInjectedTestViewModel viewModel) : ReportingTestView
{
    /// <summary>コンストラクタで受け取った ViewModel。</summary>
    public ConstructorInjectedTestViewModel InjectedViewModel { get; } = viewModel;
}

/// <summary>コンストラクタ注入の検証に使う ViewModel。生成回数を型ごとに数える。</summary>
internal sealed class ConstructorInjectedTestViewModel : IDialogViewModel
{
    /// <summary>この型がこれまでに生成された回数。</summary>
    public static int CreatedCount { get; private set; }

    /// <summary>生成回数を数えながら作る。</summary>
    public ConstructorInjectedTestViewModel() => CreatedCount++;

    /// <summary>生成回数の数え直し。</summary>
    public static void ResetCreatedCount() => CreatedCount = 0;
}

/// <summary>コンストラクタ依存が注入されることの検証に使う ViewModel。</summary>
/// <param name="dependency">サービスから注入される依存。</param>
internal sealed class DependentTestViewModel(ModelBindingTestDependency dependency) : IDialogViewModel
{
    /// <summary>注入された依存。</summary>
    public ModelBindingTestDependency Dependency { get; } = dependency;

    /// <summary>configure が設定する状態。</summary>
    public string Message { get; set; } = "既定";
}

/// <summary>ViewModel のコンストラクタへ注入される依存。</summary>
internal sealed class ModelBindingTestDependency
{
    /// <summary>注入されたことを見分けるための印。</summary>
    public string Name => "注入済み";
}

/// <summary>1 行登録の検証に使う、コンストラクタ引数を持たない中身の View。</summary>
internal sealed class SimpleRegisteredTestView : ReportingTestView;

/// <summary>1 行登録の検証に使う ViewModel。</summary>
internal sealed class SimpleRegisteredTestViewModel : IDialogViewModel;

/// <summary>依存つき ViewModel と組む中身の View。</summary>
internal sealed class DependentTestView : ReportingTestView;

/// <summary>明示登録と fallback の優先順位の検証に使う ViewModel。</summary>
internal sealed class ExplicitOverFallbackTestViewModel : IDialogViewModel;

/// <summary>View fallback だけで解決される ViewModel。</summary>
internal sealed class ViewFallbackTestViewModel : IDialogViewModel;

/// <summary>View fallback が解決できない ViewModel。</summary>
internal sealed class UnresolvableFallbackTestViewModel : IDialogViewModel;

/// <summary>ViewModel fallback (サービス解決) で生成される ViewModel。</summary>
internal sealed class ViewModelFallbackTestViewModel : IDialogViewModel;

/// <summary>設定を重ねても View fallback が保持されることの検証に使う ViewModel。</summary>
internal sealed class FallbackRetentionTestViewModel : IDialogViewModel;

/// <summary>View fallback と ViewModel fallback を別々の呼び出しで設定する検証に使う ViewModel。</summary>
internal sealed class SeparateFallbackTestViewModel : IDialogViewModel;

/// <summary>
/// 値型の ViewModel。参照型限定が実行時にも守られることの検証にだけ使う。
/// </summary>
/// <remarks>
/// 登録と型指定 show は class 制約でコンパイルできないため、この型はインスタンス渡し show
/// (interface 引数) からのみ渡せる。
/// </remarks>
internal readonly struct ValueTypeTestViewModel : IDialogViewModel;

/// <summary>明示 ViewModel factory と View fallback を組み合わせる ViewModel。</summary>
internal sealed class MixedSlotTestViewModel : IDialogViewModel
{
    /// <summary>明示 factory が設定する印。</summary>
    public string Origin { get; set; } = "既定";
}

/// <summary>
/// アプリ起動時の配線を、テストから再現する入れ物。
/// </summary>
/// <remarks>
/// サービス集合を組み立てて provider を作り、登録された初期化サービスを実行することで、
/// ライブラリが show の時点で provider を引ける状態にする。
/// </remarks>
internal sealed class TestMauiApp : IDisposable
{
    private readonly ServiceProvider _provider;

    /// <summary>サービス集合を組み立て、初期化サービスまで実行する。</summary>
    /// <param name="configure">サービス集合への登録。</param>
    public TestMauiApp(Action<IServiceCollection> configure)
    {
        ServiceCollection services = [];
        configure(services);
        _provider = services.BuildServiceProvider();
        foreach (IMauiInitializeService initializer in _provider.GetServices<IMauiInitializeService>())
        {
            initializer.Initialize(_provider);
        }
    }

    /// <summary>組み立てた provider。</summary>
    public IServiceProvider Services => _provider;

    /// <summary>登録された初期化サービスの一覧。冪等な登録の検証に使う。</summary>
    public IReadOnlyList<IMauiInitializeService> Initializers =>
        [.. _provider.GetServices<IMauiInitializeService>()];

    /// <inheritdoc/>
    public void Dispose()
    {
        DialogServiceProvider.Current = null;
        _provider.Dispose();
    }
}
