using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Dialog の show が呼び出し元の打ち切りを受け付けることの検証 (maui/ADR-0006)。
/// </summary>
/// <remarks>
/// 打ち切りは委譲面から互換面のハンドルの打ち切りへ中継され、待っている間なら一度も表示せず、
/// 表示中なら閉じる。どちらも show は <see cref="OperationCanceledException"/> を投げる。
/// 互換面は <see cref="CancellableTestDialogGateway"/> で模す。互換面そのものの打ち切りの振る舞いは
/// 互換面のテスト (Android / iOS) が受け持つ。
/// </remarks>
[TestFixture]
public class DialogCallerCancellationTests
{
    /// <summary>中身を作ったときに報告口を控える登録を持つレジストリを作る。</summary>
    private static DialogViewRegistry RegistryRecordingNotifiers(List<DialogNotifier<bool>> notifiers)
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifiers.Add(notifier);
            return new Label();
        });
        return registry;
    }

    /// <summary>待っている間に打ち切ると、中身を作らずに打ち切りとして終わる。</summary>
    [Test]
    [Description("[PB-MC-02] 待っている間に打ち切ると、表示されずに OperationCanceledException になる")]
    public void PB_MC_02_CancellingWhileWaitingEndsWithoutPresenting()
    {
        CancellableTestDialogGateway gateway = new(hasPresentationHost: false);
        IKsDialog dialogs = new Dialog(RegistryRecordingNotifiers([]), gateway);
        using CancellationTokenSource cancellation = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(
            new BooleanTestDialogViewModel(), cancellationToken: cancellation.Token);
        Assert.That(gateway.Handles, Has.Count.EqualTo(1), "互換面を呼んで提示先を待っている");
        Assert.That(show.IsCompleted, Is.False, "提示先が無くても失敗しない");

        cancellation.Cancel();

        OperationCanceledException? failure =
            Assert.CatchAsync<OperationCanceledException>(async () => await show);
        Assert.Multiple(() =>
        {
            Assert.That(failure?.CancellationToken, Is.EqualTo(cancellation.Token));
            Assert.That(gateway.Handles[0].CancelCount, Is.EqualTo(1), "打ち切りが互換面へ中継される");
            Assert.That(gateway.CreatedViews, Is.Empty, "中身の View は作られない");
        });

        // 打ち切った後に提示先が現れても、表示されない
        gateway.MakeHostAppear();
        Assert.That(gateway.CreatedViews, Is.Empty);
    }

    /// <summary>表示中に打ち切ると、ダイアログが閉じて打ち切りとして終わる。</summary>
    [Test]
    [Description("[PB-MC-03] 表示中に打ち切ると、Dialog が閉じて OperationCanceledException になる")]
    public void PB_MC_03_CancellingWhilePresentingClosesTheDialog()
    {
        List<DialogNotifier<bool>> notifiers = [];
        CancellableTestDialogGateway gateway = new(hasPresentationHost: true);
        IKsDialog dialogs = new Dialog(RegistryRecordingNotifiers(notifiers), gateway);
        using CancellationTokenSource cancellation = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(
            new BooleanTestDialogViewModel(), null, cancellation.Token);
        Assert.That(gateway.Handles, Has.Count.EqualTo(1));
        Assert.That(gateway.Handles[0].IsPresented, Is.True, "表示中になっている");

        cancellation.Cancel();

        Assert.CatchAsync<OperationCanceledException>(async () => await show);
        Assert.Multiple(() =>
        {
            Assert.That(gateway.Handles[0].CancelCount, Is.EqualTo(1), "打ち切りが互換面へ中継される");
            Assert.That(gateway.Handles[0].IsClosed, Is.True, "ダイアログが閉じる");
            Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        });

        // 閉じた後の報告は結果を変えない
        notifiers[0].Complete(true);
        Assert.That(show.IsCanceled, Is.True);
    }

    /// <summary>呼び出しの時点で打ち切り済みなら、解決・生成・委譲のどれもせずに打ち切りとして終わる。</summary>
    [Test]
    [Description("[PB-MC-04] 呼び出しの時点で打ち切り済みなら、何もせずに OperationCanceledException になる")]
    public void PB_MC_04_AnAlreadyCancelledTokenDoesNothing()
    {
        int viewModelFactoryCallCount = 0;
        int configureCallCount = 0;
        int viewFactoryCallCount = 0;
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() =>
        {
            viewModelFactoryCallCount++;
            return new ModelBindingTestDialogViewModel();
        });
        registry.Register((ModelBindingTestDialogViewModel _) =>
        {
            viewFactoryCallCount++;
            return new Label();
        });
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> _) =>
        {
            viewFactoryCallCount++;
            return new Label();
        });
        CancellableTestDialogGateway gateway = new(hasPresentationHost: true);
        IKsDialog dialogs = new Dialog(registry, gateway);
        CancellationToken cancelled = new(canceled: true);

        Assert.Multiple(() =>
        {
            Assert.CatchAsync<OperationCanceledException>(
                async () => await dialogs.ShowAsync(new BooleanTestDialogViewModel(), cancellationToken: cancelled),
                "インスタンス渡しの show");
            Assert.CatchAsync<OperationCanceledException>(
                async () => await dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
                    _ => configureCallCount++,
                    cancellationToken: cancelled),
                "型指定の show");
            Assert.CatchAsync<OperationCanceledException>(
                async () => await dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
                    _ =>
                    {
                        configureCallCount++;
                        return Task.CompletedTask;
                    },
                    cancellationToken: cancelled),
                "非同期 configure の型指定の show");
            Assert.CatchAsync<OperationCanceledException>(
                async () => await dialogs.ShowAsync(
                    new InlineOnlyTestDialogViewModel(),
                    (_, _) =>
                    {
                        viewFactoryCallCount++;
                        return new Label();
                    },
                    cancellationToken: cancelled),
                "インライン表示の show");
        });

        Assert.Multiple(() =>
        {
            Assert.That(viewModelFactoryCallCount, Is.Zero, "VM factory は呼ばれない");
            Assert.That(configureCallCount, Is.Zero, "configure は呼ばれない");
            Assert.That(viewFactoryCallCount, Is.Zero, "View factory は呼ばれない");
            Assert.That(gateway.PresentCallCount, Is.Zero, "委譲面は呼ばれない");
        });
    }

    /// <summary>委譲面がハンドルを得る前に打ち切られたら、互換面を呼ばずに打ち切りとして終わる。</summary>
    [Test]
    [Description("[PB-MC-08] gateway がハンドルを得る前に打ち切られても、取りこぼさない")]
    public async Task PB_MC_08_CancellationBeforeTheHandleIsNotLost()
    {
        CancellableTestDialogGateway gateway = new(hasPresentationHost: true, holdsUiThreadHop: true);
        IKsDialog dialogs = new Dialog(RegistryRecordingNotifiers([]), gateway);
        using CancellationTokenSource cancellation = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(
            new BooleanTestDialogViewModel(), cancellationToken: cancellation.Token);
        await gateway.UiThreadHopReached;

        cancellation.Cancel();
        gateway.ReleaseUiThreadHop();

        Assert.CatchAsync<OperationCanceledException>(async () => await show);
        Assert.Multiple(() =>
        {
            Assert.That(gateway.PresentCallCount, Is.EqualTo(1), "委譲面までは進んでいた");
            Assert.That(gateway.BridgeCallCount, Is.Zero, "互換面は呼ばれない");
            Assert.That(gateway.CreatedViews, Is.Empty, "中身の View は作られない");
        });
    }

    /// <summary>ハンドルを得ている最中に打ち切られたら、得た直後にハンドルを打ち切る。</summary>
    [Test]
    [Description("互換面を呼んでいる最中の打ち切りは、ハンドルを得た直後に中継される")]
    public void CancellationDuringTheBridgeCallIsRelayedRightAfterTheHandle()
    {
        using CancellationTokenSource cancellation = new();
        using DialogCallerCancellation relay = new(cancellation.Token);
        List<string> events = [];

        string? handle = relay.Present(
            () =>
            {
                events.Add("互換面を呼んだ");
                cancellation.Cancel();
                events.Add("打ち切られた");
                return "handle";
            },
            received => events.Add($"{received} を打ち切った"));

        Assert.Multiple(() =>
        {
            Assert.That(handle, Is.EqualTo("handle"));
            Assert.That(events, Is.EqualTo(new[] { "互換面を呼んだ", "打ち切られた", "handle を打ち切った" }));
        });
    }

    /// <summary>待っている間に ViewModel が結果を報告すると、中身を作らずにその結果が返る。</summary>
    [Test]
    [Description("待っている間に結果が確定すると、中身を作らずにその結果が返る")]
    public async Task ReportingWhileWaitingReturnsTheResultWithoutPresenting()
    {
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new ModelBindingTestDialogViewModel());
        registry.Register((ModelBindingTestDialogViewModel _) => new Label());
        CancellableTestDialogGateway gateway = new(hasPresentationHost: false);
        IKsDialog dialogs = new Dialog(registry, gateway);
        ModelBindingTestDialogViewModel? shown = null;

        Task<DialogResult<bool>> show = dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
            viewModel => shown = viewModel);
        Assert.That(gateway.Handles, Has.Count.EqualTo(1));

        shown!.Notifier!.Complete(true);

        Assert.That(await show, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.Multiple(() =>
        {
            Assert.That(gateway.Handles[0].DismissCount, Is.EqualTo(1), "確定した show がハンドルを閉じる");
            Assert.That(gateway.Handles[0].CancelCount, Is.Zero);
            Assert.That(gateway.CreatedViews, Is.Empty, "中身の View は作られない");
        });
    }

    /// <summary>結果の確定の後でも、撤去が終わって配送される前に打ち切れば打ち切りとして終わる。</summary>
    [Test]
    [Description("配送の前の打ち切りは、確定済みの結果より優先する")]
    public void CancellationBeforeDeliveryWinsOverTheSettledResult()
    {
        List<DialogNotifier<bool>> notifiers = [];
        CancellableTestDialogGateway gateway = new(hasPresentationHost: true, holdsRemoval: true);
        IKsDialog dialogs = new Dialog(RegistryRecordingNotifiers(notifiers), gateway);
        using CancellationTokenSource cancellation = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(
            new BooleanTestDialogViewModel(), cancellationToken: cancellation.Token);
        notifiers[0].Complete(true);
        Assert.That(show.IsCompleted, Is.False, "撤去が終わるまでは配送されない");

        cancellation.Cancel();

        Assert.CatchAsync<OperationCanceledException>(async () => await show);
        Assert.That(gateway.Handles[0].CancelCount, Is.EqualTo(1), "撤去の途中でも打ち切りが中継される");
    }

    /// <summary>配送された後の打ち切りは、返った結果を変えない。</summary>
    [Test]
    [Description("配送の後の打ち切りは結果を変えない")]
    public async Task CancellationAfterDeliveryDoesNotChangeTheResult()
    {
        List<DialogNotifier<bool>> notifiers = [];
        CancellableTestDialogGateway gateway = new(hasPresentationHost: true);
        IKsDialog dialogs = new Dialog(RegistryRecordingNotifiers(notifiers), gateway);
        using CancellationTokenSource cancellation = new();

        Task<DialogResult<bool>> show = dialogs.ShowAsync(
            new BooleanTestDialogViewModel(), cancellationToken: cancellation.Token);
        notifiers[0].Complete(false);
        DialogResult<bool> result = await show;
        cancellation.Cancel();

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(false)));
        Assert.That(gateway.Handles[0].CancelCount, Is.Zero, "返った後の打ち切りは中継されない");
    }
}
