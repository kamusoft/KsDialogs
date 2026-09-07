using System;
using System.Threading;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 呼び出しコンテキストの契約の検証。
/// </summary>
/// <remarks>
/// facade は提示 host の引数を持たず、呼び出しスレッドも選ばない。
/// 提示処理そのものの UI スレッドへのマーシャリングは委譲面の実装が受け持つため、
/// ここでは facade 側が呼び出しスレッドに縛られないことと、提示先不在の失敗経路を見る。
/// </remarks>
[TestFixture]
public class DialogCallContextTests
{
    /// <summary>UI スレッド以外のスレッドから呼び出しても show は成立する。</summary>
    [Test]
    [Description("UI スレッド外からの show が成立する")]
    public async Task ShowFromWorkerThreadSucceeds()
    {
        int startingThreadId = Thread.CurrentThread.ManagedThreadId;
        int callerThreadId = 0;
        int gatewayThreadId = 0;
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
        TestDialogGateway gateway = new(_ => gatewayThreadId = Thread.CurrentThread.ManagedThreadId);
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await Task.Run(async () =>
        {
            callerThreadId = Thread.CurrentThread.ManagedThreadId;
            return await dialogs.ShowAsync(new BooleanTestDialogViewModel());
        });

        Assert.That(callerThreadId, Is.Not.EqualTo(startingThreadId));
        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        // facade は呼び出しスレッドを変えずに委譲する。UI スレッドへ移すのは委譲面の実装の責務
        Assert.That(gatewayThreadId, Is.EqualTo(callerThreadId));
    }

    /// <summary>中身の生成が失敗しても、show は結果を待ち続けずに失敗として完了する。</summary>
    [Test]
    [Description("中身の生成が失敗した show は結果を待たずに失敗する")]
    public void ShowFailsWhenContentViewCreationThrows()
    {
        DialogViewRegistry registry = new();
        registry.Register<BooleanTestDialogViewModel, bool>(
            (_, _) => throw new InvalidOperationException("中身を組み立てられません"));
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        InvalidOperationException failure = Assert.ThrowsAsync<InvalidOperationException>(
            async () => await dialogs.ShowAsync(new BooleanTestDialogViewModel()))!;

        Assert.That(failure.Message, Is.EqualTo("中身を組み立てられません"));
    }

    /// <summary>提示先が存在しない場合、結果を返さずに失敗し、View も生成されない。</summary>
    [Test]
    [Description("提示 host 不在の show は即失敗する")]
    public void ShowWithoutPresentationHostFailsImmediately()
    {
        bool viewCreated = false;
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> _) =>
        {
            viewCreated = true;
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new HostlessDialogGateway());

        Assert.ThrowsAsync<DialogException.PresentationHostUnavailable>(
            async () => await dialogs.ShowAsync(new BooleanTestDialogViewModel()));

        Assert.That(viewCreated, Is.False, "提示先が無いのに View が生成されました。");
    }
}
