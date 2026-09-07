using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// DI チェーン上の Loading 版 1 行登録 (maui/ADR-0005 の Loading 版) の検証。
/// </summary>
/// <remarks>
/// 1 行登録は共有レジストリ (<see cref="LoadingViewRegistry.Shared"/>) に載るため、
/// 検証は ViewModel の型を検証ごとに分ける。
/// </remarks>
[TestFixture]
public class LoadingDependencyInjectionTests
{
    /// <summary>provider の保持を検証をまたいで持ち越さない。</summary>
    [TearDown]
    public void ClearSharedConfiguration() => DialogServiceProvider.Current = null;

    /// <summary>1 行登録だけで、ViewModel を渡した表示が BindingContext つきの View を出す。</summary>
    [Test]
    [Description("[LD-MA-02] DI 糖衣で登録したカスタム Loading が表示される")]
    public async Task LD_MA_02_TheRegisteredPairIsShownByInstance()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForLoading<RegisteredLoadingTestView, RegisteredLoadingTestViewModel>());
        TestLoadingGateway gateway = new();
        IKsLoading loading = new Loading(gateway);
        RegisteredLoadingTestViewModel viewModel = new();

        await loading.ShowAsync(viewModel);

        View created = gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(created, Is.TypeOf<RegisteredLoadingTestView>());
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
        });
    }

    /// <summary>ViewModel をコンストラクタで受け取る View には、表示対象がそのまま届く。</summary>
    [Test]
    [Description("[LD-MA-02] コンストラクタで VM を受ける View に表示対象が注入される")]
    public async Task LD_MA_02_TheConstructorReceivesTheViewModelAndItsDependencies()
    {
        using TestMauiApp app = new(services => services
            .AddSingleton<ModelBindingTestDependency>()
            .RegisterForLoading<DependentLoadingTestView, DependentLoadingTestViewModel>());
        TestLoadingGateway gateway = new();
        IKsLoading loading = new Loading(gateway);
        DependentLoadingTestViewModel viewModel = app.Services
            .GetRequiredService<DependentLoadingTestViewModel>();

        await loading.ShowAsync(viewModel);

        DependentLoadingTestView created = (DependentLoadingTestView)gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(viewModel.Dependency, Is.Not.Null, "VM のコンストラクタ依存が注入されること");
            Assert.That(created.InjectedViewModel, Is.SameAs(viewModel));
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
        });
    }

    /// <summary>スコープ形でも同じ 1 行登録で解決され、処理はそのまま実行される。</summary>
    [Test]
    [Description("[LD-MA-02] DI 糖衣で登録したカスタム Loading をスコープ形でも表示できる")]
    public async Task LD_MA_02_TheRegisteredPairIsUsedByTheScopedForm()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForLoading<ScopedRegisteredLoadingTestView, ScopedRegisteredLoadingTestViewModel>());
        TestLoadingGateway gateway = new();
        IKsLoading loading = new Loading(gateway);
        ScopedRegisteredLoadingTestViewModel viewModel = new();

        int value = await loading.StartAsync(viewModel, _ => Task.FromResult(42));

        Assert.Multiple(() =>
        {
            Assert.That(gateway.CreatedViews[0], Is.TypeOf<ScopedRegisteredLoadingTestView>());
            Assert.That(value, Is.EqualTo(42));
        });
    }

    /// <summary>1 行登録だけで、ViewModel の型を渡す表示まで使えるようになる。</summary>
    [Test]
    [Description("[LD-YM-02] 1 行登録だけで Loading の型指定 show が有効になる")]
    public async Task LD_YM_02_TheOneLineRegistrationEnablesTheTypedShow()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForLoading<TypedRegisteredLoadingTestView, TypedRegisteredLoadingTestViewModel>());
        TestLoadingGateway gateway = new();
        IKsLoading loading = new Loading(gateway);

        await loading.ShowAsync<TypedRegisteredLoadingTestViewModel>();

        View created = gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(created, Is.TypeOf<TypedRegisteredLoadingTestView>());
            Assert.That(
                created.BindingContext,
                Is.TypeOf<TypedRegisteredLoadingTestViewModel>(),
                "ViewModel はサービスから解決される");
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "表示の要求が委譲面へ渡る");
        });
    }
}
