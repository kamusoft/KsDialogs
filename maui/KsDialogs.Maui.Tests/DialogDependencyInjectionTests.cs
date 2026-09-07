using System;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// DI チェーン上の 1 行登録と一括解決 (maui/ADR-0005・core/ADR-0021) の検証。
/// </summary>
/// <remarks>
/// 1 行登録と一括解決の設定はどちらも共有レジストリ (<see cref="DialogViewRegistry.Shared"/>) に載るため、
/// 検証は ViewModel の型を検証ごとに分け、一括解決の設定は各検証のあとに解除する。
/// </remarks>
[TestFixture]
public class DialogDependencyInjectionTests
{
    /// <summary>一括解決の設定と provider の保持を、検証をまたいで持ち越さない。</summary>
    [TearDown]
    public void ClearSharedConfiguration()
    {
        DialogViewRegistry.Shared.UseFallbacks(null);
        DialogServiceProvider.Current = null;
    }

    /// <summary>1 行登録だけで、インスタンス渡し show が BindingContext つきの View を出す。</summary>
    [Test]
    [Description("[MB-MA-03] 1行登録したペアがインスタンス渡し show で表示される")]
    public async Task MB_MA_03_TheRegisteredPairIsShownByInstance()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForDialog<SimpleRegisteredTestView, SimpleRegisteredTestViewModel>());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);
        SimpleRegisteredTestViewModel viewModel = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(viewModel);
        View created = gateway.CreatedViews[0];
        ((ReportingTestView)created).ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(created, Is.TypeOf<SimpleRegisteredTestView>());
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>1 行登録だけで、型指定 show が DI 解決の ViewModel で動く。</summary>
    [Test]
    [Description("[MB-MA-04] 1行登録だけで型指定 show が DI 解決の VM で動く")]
    public async Task MB_MA_04_TheTypedShowUsesTheViewModelResolvedFromServices()
    {
        using TestMauiApp app = new(services => services
            .AddSingleton<ModelBindingTestDependency>()
            .RegisterForDialog<DependentTestView, DependentTestViewModel>());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        Task<DialogResult<bool>> show = dialogs.ShowAsync<DependentTestViewModel>(
            viewModel => viewModel.Message = "確認");
        View created = gateway.CreatedViews[0];
        DependentTestViewModel shown = (DependentTestViewModel)created.BindingContext;
        ((ReportingTestView)created).ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(shown.Dependency, Is.Not.Null, "コンストラクタ依存が注入されること");
            Assert.That(shown.Message, Is.EqualTo("確認"), "configure が表示前に適用されること");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>ViewModel をコンストラクタで受け取る View には、show 対象がそのまま 1 回だけ届く。</summary>
    [Test]
    [Description("[MB-MA-09] VM をコンストラクタで受ける TView に同一インスタンスが1回だけ届く")]
    public async Task MB_MA_09_TheConstructorReceivesTheSameViewModelInstanceOnce()
    {
        using TestMauiApp app = new(services =>
            services.RegisterForDialog<ConstructorInjectedTestView, ConstructorInjectedTestViewModel>());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);
        ConstructorInjectedTestViewModel viewModel = new();
        ConstructorInjectedTestViewModel.ResetCreatedCount();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(viewModel);
        ConstructorInjectedTestView created = (ConstructorInjectedTestView)gateway.CreatedViews[0];
        created.ReportCompleted(true);
        await show;

        Assert.Multiple(() =>
        {
            Assert.That(created.InjectedViewModel, Is.SameAs(viewModel));
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
            Assert.That(
                ConstructorInjectedTestViewModel.CreatedCount,
                Is.Zero,
                "show に渡した 1 つ以外に ViewModel が生成されないこと");
        });
    }

    /// <summary>明示登録がある型では一括解決は呼ばれない。</summary>
    [Test]
    [Description("[MB-MA-05] 明示登録が fallback より優先される")]
    public async Task MB_MA_05_TheExplicitRegistrationWinsOverTheFallback()
    {
        int fallbackCallCount = 0;
        Label registered = new();
        using TestMauiApp app = new(services => services.AddKsDialogs(options =>
            options.UseViewFallback((_, _) =>
            {
                fallbackCallCount++;
                return new Label();
            })));
        DialogViewRegistry.Shared.Register((ExplicitOverFallbackTestViewModel viewModel) =>
        {
            viewModel.Notifier!.Complete(true);
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(new ExplicitOverFallbackTestViewModel());

        Assert.Multiple(() =>
        {
            Assert.That(gateway.CreatedViews[0], Is.SameAs(registered));
            Assert.That(fallbackCallCount, Is.Zero, "fallback は呼ばれないこと");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>未登録の型は View fallback で解決され、BindingContext もライブラリが設定する。</summary>
    [Test]
    [Description("[MB-MA-06] 未登録 VM 型が View fallback で解決され表示される")]
    public async Task MB_MA_06_TheUnregisteredViewModelTypeIsResolvedByTheViewFallback()
    {
        using TestMauiApp app = new(services => services.AddKsDialogs(options =>
            options.UseViewFallback((viewModelType, _) =>
                viewModelType == typeof(ViewFallbackTestViewModel) ? new ReportingTestView() : null)));
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);
        ViewFallbackTestViewModel viewModel = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(viewModel);
        ReportingTestView created = (ReportingTestView)gateway.CreatedViews[0];
        created.ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(created.BindingContext, Is.SameAs(viewModel));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>fallback が解決できない型は、結果に化けさせず構成ミスとして失敗する。</summary>
    [Test]
    [Description("[MB-MA-07] fallback が解決できない場合は構成ミスとして失敗する")]
    public void MB_MA_07_TheUnresolvableFallbackFailsAsAConfigurationError()
    {
        using TestMauiApp app = new(services =>
            services.AddKsDialogs(options => options.UseViewFallback((_, _) => null)));
        IKsDialog dialogs = new Dialog(new TestDialogGateway());

        Assert.ThrowsAsync<DialogException.ViewFactoryNotRegistered>(
            async () => await dialogs.ShowAsync(new UnresolvableFallbackTestViewModel()));
    }

    /// <summary>ViewModel fallback の既定実装で、サービス登録された ViewModel が型指定 show に載る。</summary>
    [Test]
    [Description("[MB-MA-08] VM fallback の既定実装で型指定 show が動く")]
    public async Task MB_MA_08_TheDefaultViewModelFallbackResolvesFromServices()
    {
        using TestMauiApp app = new(services => services
            .AddTransient<ViewModelFallbackTestViewModel>()
            .AddKsDialogs(options => options.UseViewModelFallback()));
        DialogViewRegistry.Shared.Register((ViewModelFallbackTestViewModel viewModel) =>
        {
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        DialogResult<bool> result = await dialogs.ShowAsync<ViewModelFallbackTestViewModel>();

        Assert.Multiple(() =>
        {
            Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>明示 ViewModel factory と View fallback は、スロット単位に独立して組み合わさる。</summary>
    [Test]
    [Description("[MB-MA-10] 明示 VM factory と View fallback の組み合わせが成立する")]
    public async Task MB_MA_10_TheExplicitViewModelFactoryCombinesWithTheViewFallback()
    {
        using TestMauiApp app = new(services => services.AddKsDialogs(options =>
            options.UseViewFallback((viewModelType, _) =>
                viewModelType == typeof(MixedSlotTestViewModel) ? new ReportingTestView() : null)));
        DialogViewRegistry.Shared.RegisterViewModel(
            () => new MixedSlotTestViewModel { Origin = "明示 factory" });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        Task<DialogResult<bool>> show = dialogs.ShowAsync<MixedSlotTestViewModel>();
        ReportingTestView created = (ReportingTestView)gateway.CreatedViews[0];
        MixedSlotTestViewModel shown = (MixedSlotTestViewModel)created.BindingContext;
        created.ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(shown.Origin, Is.EqualTo("明示 factory"));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>設定済みの一括解決は、引数なしの再呼び出しで消えない。</summary>
    [Test]
    [Description("設定付き AddKsDialogs のあとの引数なし AddKsDialogs が fallback を消さない")]
    public async Task TheConfiguredFallbackSurvivesALaterParameterlessCall()
    {
        using TestMauiApp app = new(services => services
            .AddKsDialogs(options => options.UseViewFallback((viewModelType, _) =>
                viewModelType == typeof(FallbackRetentionTestViewModel) ? new ReportingTestView() : null))
            .AddKsDialogs());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);
        FallbackRetentionTestViewModel viewModel = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(viewModel);
        ReportingTestView created = (ReportingTestView)gateway.CreatedViews[0];
        created.ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(created.BindingContext, Is.SameAs(viewModel), "先の呼び出しの View fallback が生きていること");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>View と ViewModel の一括解決は、別々の呼び出しで設定しても両方とも残る。</summary>
    [Test]
    [Description("View / ViewModel fallback を別々の AddKsDialogs で設定しても両方が残る")]
    public async Task TheFallbackSlotsSetBySeparateCallsAreBothKept()
    {
        using TestMauiApp app = new(services => services
            .AddTransient<SeparateFallbackTestViewModel>()
            .AddKsDialogs(options => options.UseViewFallback((viewModelType, _) =>
                viewModelType == typeof(SeparateFallbackTestViewModel) ? new ReportingTestView() : null))
            .AddKsDialogs(options => options.UseViewModelFallback()));
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        Task<DialogResult<bool>> show = dialogs.ShowAsync<SeparateFallbackTestViewModel>();
        ReportingTestView created = (ReportingTestView)gateway.CreatedViews[0];
        created.ReportCompleted(true);
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(
                created.BindingContext,
                Is.TypeOf<SeparateFallbackTestViewModel>(),
                "View fallback と ViewModel fallback の両方が働くこと");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>値型の ViewModel は、View fallback が解決できる構成でも提示まで到達しない。</summary>
    /// <remarks>
    /// fallback が働く構成では未登録失敗が起きないため、参照型限定を実行時に守らないと
    /// 値型の ViewModel が提示まで通ってしまう (紐付けが boxing 済みの箱に付き、結果を報告できなくなる)。
    /// </remarks>
    [Test]
    [Description("View fallback 構成でも値型 VM は提示に至らず構成ミスとして失敗する")]
    public void TheValueTypeViewModelIsRejectedEvenWhenTheViewFallbackCanResolveIt()
    {
        int fallbackCallCount = 0;
        using TestMauiApp app = new(services => services.AddKsDialogs(options =>
            options.UseViewFallback((_, _) =>
            {
                fallbackCallCount++;
                return new ReportingTestView();
            })));
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        DialogException.ValueTypeViewModel failure =
            Assert.ThrowsAsync<DialogException.ValueTypeViewModel>(
                async () => await dialogs.ShowAsync(new ValueTypeTestViewModel()))!;

        Assert.Multiple(() =>
        {
            Assert.That(failure.ViewModelTypeName, Is.EqualTo(typeof(ValueTypeTestViewModel).FullName));
            Assert.That(fallbackCallCount, Is.Zero, "中身の解決へ進まないこと");
            Assert.That(gateway.CreatedViews, Is.Empty, "提示に至らないこと");
        });
    }

    /// <summary>値型の ViewModel は、一括解決を設定しない通常の構成でも同じ失敗になる。</summary>
    [Test]
    [Description("fallback なしの構成でも値型 VM は同じ構成ミスとして失敗する")]
    public void TheValueTypeViewModelFailsTheSameWayWithoutAnyFallback()
    {
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(gateway);

        DialogException.ValueTypeViewModel failure =
            Assert.ThrowsAsync<DialogException.ValueTypeViewModel>(
                async () => await dialogs.ShowAsync(new ValueTypeTestViewModel()))!;

        Assert.Multiple(() =>
        {
            Assert.That(failure.ViewModelTypeName, Is.EqualTo(typeof(ValueTypeTestViewModel).FullName));
            Assert.That(gateway.CreatedViews, Is.Empty, "提示に至らないこと");
        });
    }

    /// <summary>初期化サービスの登録は、複数の入口から重ねて呼んでも 1 つだけになる。</summary>
    [Test]
    [Description("初期化サービスの登録は冪等")]
    public void TheInitializerRegistrationIsIdempotent()
    {
        using TestMauiApp app = new(services => services
            .AddKsDialogs()
            .RegisterForDialog<SimpleRegisteredTestView, SimpleRegisteredTestViewModel>()
            .AddKsDialogs());

        Assert.Multiple(() =>
        {
            Assert.That(app.Initializers, Has.Count.EqualTo(1));
            Assert.That(DialogServiceProvider.Current, Is.SameAs(app.Services));
        });
    }
}
