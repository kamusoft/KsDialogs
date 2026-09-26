using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// スコープ形の処理を始めるスレッドの指定 (<see cref="LoadingActionThread"/>) の振り分けと引き回しの検証。
/// </summary>
/// <remarks>
/// 素の .NET には UI スレッドが無いため、手順 (<see cref="LoadingActionRunner"/>) の「UI スレッドで呼ぶ口」を
/// 専用スレッドで動く偽物 (<see cref="DedicatedUiThread"/>) に差し替え、どちらの経路を通ってどのスレッドで
/// 始まったかを確かめる。本物の UI スレッドへの移送は MAUI の配管で、ここでは検証しない。
/// </remarks>
[TestFixture]
public class LoadingActionThreadTests
{
    /// <summary>指定なし (既定) の処理は、UI スレッドで呼ぶ口を通り、その口のスレッドで始まる。</summary>
    [Test]
    [Description("[LD-HM-02] 既定では UI スレッドで呼ぶ口を通って action が始まる")]
    public async Task LD_HM_02_TheDefaultActionStartsThroughTheUiThreadInvoker()
    {
        using DedicatedUiThread uiThread = new();
        int startedThread = 0;

        await LoadingActionRunner.RunAsync(
            _ =>
            {
                startedThread = Environment.CurrentManagedThreadId;
                return Task.CompletedTask;
            },
            default,
            uiThread.InvokeAsync,
            _ => { },
            () => { },
            _ => { });

        Assert.Multiple(() =>
        {
            Assert.That(default(LoadingActionThread), Is.EqualTo(LoadingActionThread.Main), "既定値は Main");
            Assert.That(uiThread.DispatchCount, Is.EqualTo(1), "UI スレッドで呼ぶ口を 1 回通る");
            Assert.That(startedThread, Is.EqualTo(uiThread.ManagedThreadId), "口のスレッドで始まる");
        });
    }

    /// <summary>
    /// <see cref="LoadingActionThread.Background"/> の処理は、UI スレッドで呼ぶ口を通らず、
    /// 呼び出し元 (偽物の UI スレッド) とは別のスレッドで始まる。
    /// </summary>
    [Test]
    [Description("[LD-HM-03] Background では UI スレッドで呼ぶ口を通らずに action が始まる")]
    public async Task LD_HM_03_TheBackgroundActionStartsOffTheUiThreadWithoutTheInvoker()
    {
        using DedicatedUiThread uiThread = new();
        int startedThread = 0;
        int callingThread = 0;

        // 呼び出し元を偽物の UI スレッドにそろえる (口を通った回数には数えない)
        await uiThread.PostAsync(() =>
        {
            callingThread = Environment.CurrentManagedThreadId;
            return LoadingActionRunner.RunAsync(
                _ =>
                {
                    startedThread = Environment.CurrentManagedThreadId;
                    return Task.CompletedTask;
                },
                LoadingActionThread.Background,
                uiThread.InvokeAsync,
                _ => { },
                () => { },
                _ => { });
        });

        Assert.Multiple(() =>
        {
            Assert.That(callingThread, Is.EqualTo(uiThread.ManagedThreadId), "偽物の UI スレッドから呼んでいる");
            Assert.That(uiThread.DispatchCount, Is.Zero, "UI スレッドで呼ぶ口を通らない");
            Assert.That(startedThread, Is.Not.Zero);
            Assert.That(startedThread, Is.Not.EqualTo(uiThread.ManagedThreadId), "偽物の UI スレッドとは別のスレッドで始まる");
        });
    }

    /// <summary>
    /// どちらの経路でも、報告は完了通知より先に同期で転送され、終了はちょうど 1 回伝わり、
    /// 失敗は預けられる。
    /// </summary>
    /// <param name="actionThread">処理を始めるスレッド。</param>
    /// <param name="fails">処理が報告の後に例外を投げるか。</param>
    [TestCase(LoadingActionThread.Main, false)]
    [TestCase(LoadingActionThread.Main, true)]
    [TestCase(LoadingActionThread.Background, false)]
    [TestCase(LoadingActionThread.Background, true)]
    [Description("[LD-HM-04] どちらの経路でも Runner の順序の保証が保たれる")]
    public async Task LD_HM_04_TheRunnerKeepsItsOrderingGuaranteesOnEitherPath(
        LoadingActionThread actionThread,
        bool fails)
    {
        using DedicatedUiThread uiThread = new();
        object gate = new();
        List<string> order = [];
        List<Exception> failures = [];
        int reportingThread = 0;
        int forwardingThread = 0;
        bool arrivedBeforeReturn = false;
        InvalidOperationException thrown = new("処理に失敗しました");

        await LoadingActionRunner.RunAsync(
            progress =>
            {
                reportingThread = Environment.CurrentManagedThreadId;
                progress.Report(1d);
                lock (gate)
                {
                    arrivedBeforeReturn = order.Contains("report");
                }

                // 報告の直後にその場で終わる (完了通知が報告を追い越しやすい形)
                return fails ? Task.FromException(thrown) : Task.CompletedTask;
            },
            actionThread,
            uiThread.InvokeAsync,
            _ =>
            {
                forwardingThread = Environment.CurrentManagedThreadId;
                lock (gate)
                {
                    order.Add("report");
                }
            },
            () =>
            {
                lock (gate)
                {
                    order.Add("completion");
                }
            },
            failure =>
            {
                lock (gate)
                {
                    failures.Add(failure);
                }
            });

        Assert.Multiple(() =>
        {
            Assert.That(order, Is.EqualTo(new[] { "report", "completion" }), "報告が先で、終了はちょうど 1 回");
            Assert.That(arrivedBeforeReturn, Is.True, "報告は戻る前に転送先へ届く");
            Assert.That(forwardingThread, Is.EqualTo(reportingThread), "報告は報告したスレッドのまま転送される");
            Assert.That(failures, Is.EqualTo(fails ? new Exception[] { thrown } : Array.Empty<Exception>()));
        });
    }

    /// <summary>10 本の入口は、指定なしなら <see cref="LoadingActionThread.Main"/> を委譲面へ渡す。</summary>
    [Test]
    [Description("[LD-HM-05] 10 本の入口から、指定なしでは Main が gateway に届く")]
    public async Task LD_HM_05_EveryEntryHandsMainToTheGatewayByDefault()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        TypedLoadingTestViewModel viewModel = new();

        await loading.StartAsync(_ => Task.CompletedTask);
        await loading.StartAsync(_ => Task.FromResult(1));
        await loading.StartAsync(viewModel, _ => Task.CompletedTask);
        await loading.StartAsync(viewModel, _ => Task.FromResult(1));
        await loading.StartAsync(viewModel, _ => new Label(), _ => Task.CompletedTask);
        await loading.StartAsync(viewModel, _ => new Label(), _ => Task.FromResult(1));
        await loading.StartAsync<TypedLoadingTestViewModel>(_ => Task.CompletedTask, model => model.Message = "同期");
        await loading.StartAsync<TypedLoadingTestViewModel>(_ => Task.CompletedTask, model => Task.CompletedTask);
        await loading.StartAsync<TypedLoadingTestViewModel, int>(_ => Task.FromResult(1), model => model.Message = "同期");
        await loading.StartAsync<TypedLoadingTestViewModel, int>(_ => Task.FromResult(1), model => Task.CompletedTask);

        Assert.That(gateway.ActionThreads, Is.EqualTo(Enumerable.Repeat(LoadingActionThread.Main, 10)));
    }

    /// <summary>10 本の入口は、渡された <see cref="LoadingActionThread.Background"/> をそのまま委譲面へ渡す。</summary>
    [Test]
    [Description("[LD-HM-05] 10 本の入口から、Background を渡すと Background が gateway に届く")]
    public async Task LD_HM_05_EveryEntryHandsBackgroundToTheGatewayWhenGiven()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        TypedLoadingTestViewModel viewModel = new();
        const LoadingActionThread Background = LoadingActionThread.Background;

        await loading.StartAsync(_ => Task.CompletedTask, actionThread: Background);
        await loading.StartAsync(_ => Task.FromResult(1), actionThread: Background);
        await loading.StartAsync(viewModel, _ => Task.CompletedTask, actionThread: Background);
        await loading.StartAsync(viewModel, _ => Task.FromResult(1), actionThread: Background);
        await loading.StartAsync(viewModel, _ => new Label(), _ => Task.CompletedTask, actionThread: Background);
        await loading.StartAsync(viewModel, _ => new Label(), _ => Task.FromResult(1), actionThread: Background);
        await loading.StartAsync<TypedLoadingTestViewModel>(
            _ => Task.CompletedTask,
            model => model.Message = "同期",
            actionThread: Background);
        await loading.StartAsync<TypedLoadingTestViewModel>(
            _ => Task.CompletedTask,
            model => Task.CompletedTask,
            actionThread: Background);
        await loading.StartAsync<TypedLoadingTestViewModel, int>(
            _ => Task.FromResult(1),
            model => model.Message = "同期",
            actionThread: Background);
        await loading.StartAsync<TypedLoadingTestViewModel, int>(
            _ => Task.FromResult(1),
            model => Task.CompletedTask,
            actionThread: Background);

        Assert.That(gateway.ActionThreads, Is.EqualTo(Enumerable.Repeat(LoadingActionThread.Background, 10)));
    }

    /// <summary>
    /// 表示先の無い委譲面では、指定に関係なく呼び出し元のスレッドでそのまま始まり、戻り値を返す。
    /// </summary>
    /// <param name="actionThread">処理を始めるスレッドの指定。</param>
    [TestCase(LoadingActionThread.Main)]
    [TestCase(LoadingActionThread.Background)]
    [Description("[LD-HM-06] UI スレッドを持たない環境では、指定に関係なくその場で実行し戻り値を返す")]
    public async Task LD_HM_06_TheHostlessGatewayRunsInPlaceRegardlessOfTheThreadChoice(
        LoadingActionThread actionThread)
    {
        IKsLoading loading = NewLoading(new HostlessLoadingGateway());
        int callingThread = Environment.CurrentManagedThreadId;
        int startedThread = 0;

        Task<string> running = loading.StartAsync(
            _ =>
            {
                startedThread = Environment.CurrentManagedThreadId;
                return Task.FromResult("結果");
            },
            actionThread: actionThread);
        // 呼び出しから戻った時点で、処理はもう呼び出し元のスレッドで始まっている
        int startedThreadWhenReturned = startedThread;
        string value = await running;

        Assert.Multiple(() =>
        {
            Assert.That(startedThreadWhenReturned, Is.EqualTo(callingThread), "呼び出し元のスレッドでそのまま始まる");
            Assert.That(value, Is.EqualTo("結果"));
        });
    }

    /// <summary>表示先の無い委譲面では、指定に関係なく処理の失敗がそのまま呼び出し元へ返る。</summary>
    /// <param name="actionThread">処理を始めるスレッドの指定。</param>
    [TestCase(LoadingActionThread.Main)]
    [TestCase(LoadingActionThread.Background)]
    [Description("[LD-HM-06] UI スレッドを持たない環境では、指定に関係なく失敗がそのまま返る")]
    public void LD_HM_06_TheHostlessGatewayReturnsTheFailureRegardlessOfTheThreadChoice(
        LoadingActionThread actionThread)
    {
        IKsLoading loading = NewLoading(new HostlessLoadingGateway());
        int callingThread = Environment.CurrentManagedThreadId;
        int startedThread = 0;
        InvalidOperationException thrown = new("処理に失敗しました");

        InvalidOperationException? failure = Assert.ThrowsAsync<InvalidOperationException>(
            () => loading.StartAsync(
                _ =>
                {
                    startedThread = Environment.CurrentManagedThreadId;
                    return Task.FromException(thrown);
                },
                actionThread: actionThread));

        Assert.Multiple(() =>
        {
            Assert.That(failure, Is.SameAs(thrown), "元の失敗がそのまま返る");
            Assert.That(startedThread, Is.EqualTo(callingThread), "呼び出し元のスレッドでそのまま始まる");
        });
    }

    /// <summary>
    /// インスタンス渡し・その場の factory・型指定のどの入口でも使えるよう登録を済ませた Loading を作る。
    /// </summary>
    /// <param name="gateway">委譲面。</param>
    /// <returns>検証に使う Loading。</returns>
    private static IKsLoading NewLoading(ILoadingGateway gateway)
    {
        LoadingViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedLoadingTestViewModel());
        registry.Register((TypedLoadingTestViewModel _) => new Label());
        return new Loading(registry, new LoadingSettings(), gateway);
    }
}
