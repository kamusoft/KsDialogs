using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// スコープ形の処理の進捗と終了が、互換面へ渡る順序で検証されることを見る。
/// </summary>
/// <remarks>
/// Native 側の器は合流が終わった後の報告を捨てるため、報告が完了通知に追い越されると
/// 最後の進捗が表示にも ViewModel にも届かなくなる。報告と終了の順序は両 OS の委譲面が
/// 共有するこの手順が担保するので、検証もここに置く (core/ADR-0024)。
/// </remarks>
[TestFixture]
public class LoadingActionRunnerTests
{
    /// <summary>報告した直後にその場で終わる処理でも、最後の進捗が完了通知より先に届く。</summary>
    [Test]
    [Description("[LD-MA-03] 報告直後に同期で終わる処理でも最終の進捗が完了通知より先に届く")]
    public async Task LD_MA_03_TheFinalReportPrecedesTheCompletionNotice()
    {
        List<string> order = [];
        List<double> reported = [];

        await LoadingActionRunner.RunAsync(
            progress =>
            {
                progress.Report(0.5d);
                progress.Report(1d);
                return Task.CompletedTask;
            },
            value =>
            {
                order.Add("report");
                reported.Add(value);
            },
            () => order.Add("completion"),
            _ => order.Add("failure"));

        Assert.Multiple(() =>
        {
            Assert.That(order, Is.EqualTo(new[] { "report", "report", "completion" }));
            Assert.That(reported, Is.EqualTo(new[] { 0.5d, 1d }));
        });
    }

    /// <summary>報告は投げ直されず、報告したスレッドの上で戻る前に転送先へ届く。</summary>
    [Test]
    [Description("[LD-MA-03] 進捗は報告したスレッドのまま同期で転送される")]
    public async Task LD_MA_03_TheReportIsForwardedSynchronouslyOnTheReportingThread()
    {
        int reportingThread = 0;
        int forwardingThread = 0;
        bool arrivedBeforeReturn = false;

        await LoadingActionRunner.RunAsync(
            progress =>
            {
                reportingThread = Environment.CurrentManagedThreadId;
                progress.Report(1d);
                arrivedBeforeReturn = forwardingThread != 0;
                return Task.CompletedTask;
            },
            _ => forwardingThread = Environment.CurrentManagedThreadId,
            () => { },
            _ => { });

        Assert.Multiple(() =>
        {
            Assert.That(arrivedBeforeReturn, Is.True, "報告は戻る前に転送先へ届くこと");
            Assert.That(forwardingThread, Is.EqualTo(reportingThread));
        });
    }

    /// <summary>別スレッドから報告しても、その場で転送される。</summary>
    [Test]
    [Description("[LD-MA-03] 別スレッドからの報告もその場で転送される")]
    public async Task LD_MA_03_AReportFromAnotherThreadIsForwardedAsIs()
    {
        List<double> reported = [];
        int reportingThread = 0;
        int forwardingThread = 0;

        await LoadingActionRunner.RunAsync(
            async progress => await Task.Run(() =>
            {
                reportingThread = Environment.CurrentManagedThreadId;
                progress.Report(0.25d);
            }).ConfigureAwait(false),
            value =>
            {
                forwardingThread = Environment.CurrentManagedThreadId;
                reported.Add(value);
            },
            () => { },
            _ => { });

        Assert.Multiple(() =>
        {
            Assert.That(reported, Is.EqualTo(new[] { 0.25d }));
            Assert.That(forwardingThread, Is.EqualTo(reportingThread));
        });
    }

    /// <summary>処理が成功したとき、完了通知はちょうど1回だけ届く。</summary>
    [Test]
    [Description("[LD-MA-03] 成功した処理の完了通知はちょうど1回")]
    public async Task LD_MA_03_TheCompletionNoticeArrivesOnceOnSuccess()
    {
        int completions = 0;
        int failures = 0;

        await LoadingActionRunner.RunAsync(
            _ => Task.CompletedTask,
            _ => { },
            () => completions++,
            _ => failures++);

        Assert.Multiple(() =>
        {
            Assert.That(completions, Is.EqualTo(1));
            Assert.That(failures, Is.Zero);
        });
    }

    /// <summary>処理が失敗しても、理由が預けられたうえで完了通知はちょうど1回だけ届く。</summary>
    [Test]
    [Description("[LD-MA-03] 失敗した処理でも完了通知はちょうど1回で理由が預けられる")]
    public async Task LD_MA_03_TheFailureIsHandedOverAndTheCompletionNoticeStillArrivesOnce()
    {
        int completions = 0;
        List<Exception> failures = [];
        InvalidOperationException thrown = new("処理に失敗しました");

        await LoadingActionRunner.RunAsync(
            _ => Task.FromException(thrown),
            _ => { },
            () => completions++,
            failure => failures.Add(failure));

        Assert.Multiple(() =>
        {
            Assert.That(completions, Is.EqualTo(1));
            Assert.That(failures, Is.EqualTo(new[] { thrown }));
        });
    }
}
