using KsDialogs.Maui.Tests.Support;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// DI チェーン上の Toast 版 1 行登録 (maui/ADR-0005 の Toast 版) の検証。
/// </summary>
/// <remarks>
/// 1 行登録は共有レジストリ (<see cref="ToastViewRegistry.Shared"/>) に載るため、
/// 検証は ViewModel の型を検証ごとに分ける。
/// </remarks>
[TestFixture]
public class ToastDependencyInjectionTests
{
    /// <summary>provider の保持を検証をまたいで持ち越さない。</summary>
    [TearDown]
    public void ClearSharedConfiguration() => DialogServiceProvider.Current = null;

    /// <summary>1 行登録だけで、ViewModel を渡した表示が BindingContext つきの View を出す。</summary>
    [Test]
    [Description("[TS-MA-02] DI 糖衣で登録したカスタム Toast が表示される")]
    public void TS_MA_02_TheRegisteredPairIsShownByInstance()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForToast<RegisteredToastTestView, RegisteredToastTestViewModel>());
        TestToastGateway gateway = new();
        IKsToast toast = new Toast(gateway);
        RegisteredToastTestViewModel viewModel = new();

        toast.Show(viewModel);

        View created = gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(created, Is.TypeOf<RegisteredToastTestView>());
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
        });
    }

    /// <summary>ViewModel をコンストラクタで受け取る View には、表示対象がそのまま届く。</summary>
    [Test]
    [Description("[TS-MA-02] コンストラクタで VM を受ける View に表示対象が注入される")]
    public void TS_MA_02_TheConstructorReceivesTheViewModelAndItsDependencies()
    {
        using TestMauiApp app = new(services => services
            .AddSingleton<ModelBindingTestDependency>()
            .RegisterForToast<DependentToastTestView, DependentToastTestViewModel>());
        TestToastGateway gateway = new();
        IKsToast toast = new Toast(gateway);
        DependentToastTestViewModel viewModel = app.Services
            .GetRequiredService<DependentToastTestViewModel>();

        toast.Show(viewModel);

        DependentToastTestView created = (DependentToastTestView)gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(viewModel.Dependency, Is.Not.Null, "VM のコンストラクタ依存が注入されること");
            Assert.That(created.InjectedViewModel, Is.SameAs(viewModel));
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
        });
    }

    /// <summary>1 行登録だけで、ViewModel の型を渡す表示まで使えるようになる。</summary>
    [Test]
    [Description("[TS-YM-02] 1 行登録だけで Toast の型指定 show が有効になる")]
    public void TS_YM_02_TheOneLineRegistrationEnablesTheTypedShow()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForToast<TypedRegisteredToastTestView, TypedRegisteredToastTestViewModel>());
        TestToastGateway gateway = new();
        IKsToast toast = new Toast(gateway);

        toast.Show<TypedRegisteredToastTestViewModel>();

        View created = gateway.CreatedViews[0];
        Assert.Multiple(() =>
        {
            Assert.That(created, Is.TypeOf<TypedRegisteredToastTestView>());
            Assert.That(
                created.BindingContext,
                Is.TypeOf<TypedRegisteredToastTestViewModel>(),
                "ViewModel はサービスから解決される");
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "表示の要求が委譲面へ渡る");
        });
    }
}
