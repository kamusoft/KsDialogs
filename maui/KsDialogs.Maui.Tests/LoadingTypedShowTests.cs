using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// ViewModel の型を渡す Loading の表示 (core/ADR-0035) の解決順序と実行順序の検証。
/// </summary>
/// <remarks>
/// 合流・世代・撤去の順序は Native 実装が唯一の正として持つため、ここでは検証しない
/// (core/ADR-0024)。MAUI 形態の責務は「生成し、configure を終え、解決した中身と供給値を
/// そのまま委譲面へ渡す」ところまでである。
/// </remarks>
[TestFixture]
public class LoadingTypedShowTests
{
    /// <summary>生成 → configure → 中身の生成 → 表示の一連が動く。</summary>
    [Test]
    [Description("[LD-TY-03] 型指定 show が生成 → configure → 表示の一連で動く")]
    public async Task LD_TY_03_TheTypedShowRunsCreationConfigureAndPresentation()
    {
        List<string> shownMessages = [];
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel viewModel) =>
        {
            // configure が設定した状態を中身の初期化から読めることを見る
            shownMessages.Add(viewModel.Message);
            return new Label();
        });
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        await loading.ShowAsync<TypedLoadingTestViewModel>(viewModel => viewModel.Message = "読み込み中");

        Assert.Multiple(() =>
        {
            Assert.That(shownMessages, Is.EqualTo(new[] { "読み込み中" }));
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "開始は 1 件だけ委譲される");
            Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        });
    }

    /// <summary>非同期 configure が完了するまで、中身の生成は始まらない。</summary>
    [Test]
    [Description("[LD-TY-04] 非同期 configure の完了まで View 生成が始まらない")]
    public async Task LD_TY_04_TheViewIsNotCreatedUntilTheAsynchronousConfigureCompletes()
    {
        TaskCompletionSource configureGate = new();
        int factoryCallCount = 0;
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel _) =>
        {
            factoryCallCount++;
            return new Label();
        });
        IKsLoading loading = NewLoading(registry, new TestLoadingGateway());

        Task show = loading.ShowAsync<TypedLoadingTestViewModel>(async viewModel =>
        {
            await configureGate.Task;
            viewModel.Message = "非同期で用意";
        });
        int factoryCallCountBeforeConfigureCompletes = factoryCallCount;
        configureGate.SetResult();
        await show;

        Assert.Multiple(() =>
        {
            Assert.That(
                factoryCallCountBeforeConfigureCompletes,
                Is.Zero,
                "configure の完了前に中身が生成されないこと");
            Assert.That(factoryCallCount, Is.EqualTo(1));
        });
    }

    /// <summary>ViewModel factory が未登録なら、構成ミスとして失敗し表示に進まない。</summary>
    [Test]
    [Description("[LD-TY-05] VM factory 未登録の型指定 show は構成ミスとして失敗する")]
    public void LD_TY_05_TheTypedShowFailsWhenTheViewModelFactoryIsNotRegistered()
    {
        LoadingViewRegistry registry = new();
        registry.Register((ViewModelFactoryMissingLoadingTestViewModel _) => new Label());
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        DialogException.ViewModelFactoryNotRegistered? failure =
            Assert.ThrowsAsync<DialogException.ViewModelFactoryNotRegistered>(
                () => loading.ShowAsync<ViewModelFactoryMissingLoadingTestViewModel>());

        Assert.Multiple(() =>
        {
            Assert.That(
                failure!.ViewModelTypeName,
                Does.Contain(nameof(ViewModelFactoryMissingLoadingTestViewModel)));
            Assert.That(gateway.Requests, Is.Empty, "表示は行われない");
        });
    }

    /// <summary>configure の失敗は提示に進まず、そのまま呼び出し元へ伝播する。</summary>
    [Test]
    [Description("[LD-TY-06] configure の失敗は提示に進まず伝播する")]
    public void LD_TY_06_TheFailureOfTheConfigurePropagatesWithoutPresenting()
    {
        InvalidOperationException thrown = new("configure に失敗しました");
        int factoryCallCount = 0;
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel _) =>
        {
            factoryCallCount++;
            return new Label();
        });
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);
        // 失敗しかしない lambda はどちらの configure の形にも当てはまるため、受ける形を明示する
        Action<TypedLoadingTestViewModel> configure = _ => throw thrown;

        InvalidOperationException? failure = Assert.ThrowsAsync<InvalidOperationException>(
            () => loading.ShowAsync(configure));

        Assert.Multiple(() =>
        {
            Assert.That(failure, Is.SameAs(thrown), "元の失敗がそのまま伝播する");
            Assert.That(factoryCallCount, Is.Zero, "View factory は呼ばれない");
            Assert.That(gateway.Requests, Is.Empty, "表示も合流もされない");
        });
    }

    /// <summary>configure を省略すると、ViewModel factory の生成物がそのまま表示される。</summary>
    [Test]
    [Description("[LD-TY-07] configure 省略の型指定 show は VM factory の生成物をそのまま表示する")]
    public async Task LD_TY_07_TheTypedShowWithoutConfigureUsesTheCreatedViewModelAsIs()
    {
        TypedLoadingTestViewModel created = new() { Message = "生成時の状態" };
        List<TypedLoadingTestViewModel> shownViewModels = [];
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => created);
        registry.Register((TypedLoadingTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            return new Label();
        });
        IKsLoading loading = NewLoading(registry, new TestLoadingGateway());

        await loading.ShowAsync<TypedLoadingTestViewModel>();

        Assert.That(shownViewModels, Is.EqualTo(new[] { created }));
    }

    /// <summary>生成した ViewModel がそのまま進捗の転送先になる。</summary>
    [Test]
    [Description("[LD-TY-08] 型指定 show で生成した VM にも進捗が転送される")]
    public async Task LD_TY_08_TheProgressReachesTheCreatedViewModel()
    {
        TypedLoadingTestViewModel created = new();
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => created);
        registry.Register((TypedLoadingTestViewModel _) => new Label());
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        await loading.StartAsync<TypedLoadingTestViewModel>(progress =>
        {
            progress.Report(0.25d);
            return Task.CompletedTask;
        });

        Assert.Multiple(() =>
        {
            Assert.That(
                gateway.Requests[0].ProgressReceiver,
                Is.SameAs(created),
                "転送先は生成した ViewModel そのものであること");
            Assert.That(created.ReceivedProgress, Is.EqualTo(new[] { 0.25d }));
        });
    }

    /// <summary>型指定 start は処理の戻り値をそのまま返す。</summary>
    [Test]
    [Description("[LD-TY-09] 型指定 start が処理の戻り値を返し合流 1 件を対で数える")]
    public async Task LD_TY_09_TheTypedStartReturnsTheValueOfTheAction()
    {
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel _) => new Label());
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        string value = await loading.StartAsync<TypedLoadingTestViewModel, string>(
            _ => Task.FromResult("結果"),
            viewModel => viewModel.Message = "読み込み中");

        Assert.Multiple(() =>
        {
            Assert.That(value, Is.EqualTo("結果"));
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "開始は 1 件だけ委譲される");
        });
    }

    /// <summary>ViewModel factory が未登録なら、処理ブロックは実行されない。</summary>
    [Test]
    [Description("[LD-TY-10] 型指定 start の VM factory 未登録は処理を実行しない")]
    public void LD_TY_10_TheTypedStartDoesNotRunTheActionWhenTheViewModelFactoryIsMissing()
    {
        bool actionRan = false;
        LoadingViewRegistry registry = new();
        registry.Register((ViewModelFactoryMissingLoadingTestViewModel _) => new Label());
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        Assert.ThrowsAsync<DialogException.ViewModelFactoryNotRegistered>(
            () => loading.StartAsync<ViewModelFactoryMissingLoadingTestViewModel>(_ =>
            {
                actionRan = true;
                return Task.CompletedTask;
            }));

        Assert.Multiple(() =>
        {
            Assert.That(actionRan, Is.False, "処理ブロックは実行されない");
            Assert.That(gateway.Requests, Is.Empty);
        });
    }

    /// <summary>置き場所の引数は、値のまま委譲面へ届く。</summary>
    [Test]
    [Description("[LD-TY-11] 型指定 show の置き場所引数が提示に渡る")]
    public async Task LD_TY_11_ThePlacementArgumentReachesThePresentation()
    {
        DialogPlacement placement = new() { VerticalAlignment = DialogAlignment.End, OffsetY = -24d };
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel _) => new Label());
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        await loading.ShowAsync<TypedLoadingTestViewModel>(placement: placement);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Requests[0].ShowPlacement, Is.SameAs(placement));
            Assert.That(gateway.Contents[0].Placement.VerticalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(gateway.Contents[0].Placement.OffsetY, Is.EqualTo(-24d));
        });
    }

    /// <summary>解決は呼び出し時点のエントリで行い、以後の再登録は使われない。</summary>
    [Test]
    [Description("[LD-TY-12] 型指定 show は呼び出し時点のエントリで View まで作る")]
    public async Task LD_TY_12_TheTypedShowUsesTheEntrySnapshotTakenAtTheCall()
    {
        TaskCompletionSource configureGate = new();
        TypedLoadingTestViewModel first = new();
        TypedLoadingTestViewModel second = new();
        List<TypedLoadingTestViewModel> shownViewModels = [];
        List<string> usedFactories = [];
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => first);
        registry.Register((TypedLoadingTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            usedFactories.Add("最初");
            return new Label();
        });
        IKsLoading loading = NewLoading(registry, new TestLoadingGateway());

        Task show = loading.ShowAsync<TypedLoadingTestViewModel>(async _ => await configureGate.Task);
        registry.RegisterViewModel(() => second);
        registry.Register((TypedLoadingTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            usedFactories.Add("再登録");
            return new Label();
        });
        configureGate.SetResult();
        await show;

        Assert.Multiple(() =>
        {
            Assert.That(shownViewModels, Is.EqualTo(new[] { first }), "再登録後の VM factory は使われない");
            Assert.That(usedFactories, Is.EqualTo(new[] { "最初" }), "再登録後の View factory も使われない");
        });
    }

    /// <summary>ViewModel factory の失敗は提示に進まず、そのまま呼び出し元へ伝播する。</summary>
    [Test]
    [Description("[LD-TY-13] VM factory の失敗は提示に進まず伝播する")]
    public void LD_TY_13_TheFailureOfTheViewModelFactoryPropagatesWithoutPresenting()
    {
        InvalidOperationException thrown = new("ViewModel を作れません");
        bool configureRan = false;
        int factoryCallCount = 0;
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel<TypedLoadingTestViewModel>(() => throw thrown);
        registry.Register((TypedLoadingTestViewModel _) =>
        {
            factoryCallCount++;
            return new Label();
        });
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(registry, gateway);

        InvalidOperationException? failure = Assert.ThrowsAsync<InvalidOperationException>(
            () => loading.ShowAsync<TypedLoadingTestViewModel>(_ => configureRan = true));

        Assert.Multiple(() =>
        {
            Assert.That(failure, Is.SameAs(thrown), "元の失敗がそのまま伝播する");
            Assert.That(configureRan, Is.False, "configure は呼ばれない");
            Assert.That(factoryCallCount, Is.Zero, "View factory は呼ばれない");
            Assert.That(gateway.Requests, Is.Empty, "表示も合流もされない");
        });
    }

    /// <summary>検証ごとに独立したレジストリで表示の入口を作る。</summary>
    /// <param name="registry">その検証だけが使うレジストリ。</param>
    /// <param name="gateway">値の受け取りを記録する委譲面。</param>
    /// <returns>表示の入口。</returns>
    private static IKsLoading NewLoading(LoadingViewRegistry registry, TestLoadingGateway gateway) =>
        new Loading(registry, new LoadingSettings(), gateway);
}
