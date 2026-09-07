using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Native ライブラリへの委譲で、引数・結果・show 呼び出しの対応が保たれることの検証。
/// </summary>
/// <remarks>
/// ダイアログの表示そのものと、重ね表示・閉鎖の挙動は Native 実装の担当なので、
/// ここでは委譲面の契約 (show 1 回につき委譲 1 回・結果 1 個・中身と結果チャネルの結び付き) だけを見る。
/// MAUI 形態が Native の挙動を継承していると言えるのは、この対応が保たれている限りにおいてである。
/// </remarks>
[TestFixture]
public class DialogGatewayContractTests
{
    /// <summary>show 1 回につき委譲は 1 回だけ行われる。</summary>
    [Test]
    [Description("show 1 回につき委譲は 1 回")]
    public async Task EachShowDelegatesExactlyOnce()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(gateway.Requests, Has.Count.EqualTo(1));
        Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
    }

    /// <summary>委譲面へ渡る中身と結果チャネルは、その show の factory と報告口に結び付いている。</summary>
    [Test]
    [Description("委譲面へ渡る中身と結果チャネルはその show のもの")]
    public async Task DelegatedContentAndChannelBelongToTheSameShow()
    {
        Label registered = new();
        DialogNotifier<bool>? captured = null;
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            captured = notifier;
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        Task<DialogResult<bool>> showing = dialogs.ShowAsync(new BooleanTestDialogViewModel());
        captured!.Complete(true);
        DialogResult<bool> result = await showing;

        Assert.That(gateway.CreatedViews[0], Is.SameAs(registered));
        Assert.That(gateway.Requests[0].ResultChannel.IsResultSettled, Is.True);
        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
    }

    /// <summary>委譲面が返した completed / cancelled は、宣言結果型のまま呼び出し元へ返る。</summary>
    [Test]
    [Description("委譲面の結果が宣言結果型で返る")]
    public async Task GatewayOutcomeIsRestoredToTheDeclaredResultType()
    {
        DialogViewRegistry registry = new();
        registry.Register((StringTestDialogViewModel _, DialogNotifier<string> _) => new Label());
        TestDialogGateway completing =
            new(request => request.ResultChannel.Settle(new DialogOutcome.Completed("結果")));
        TestDialogGateway cancelling =
            new(request => request.ResultChannel.Settle(DialogOutcome.Cancelled.Instance));

        DialogResult<string> completed =
            await new Dialog(registry, completing).ShowAsync(new StringTestDialogViewModel());
        DialogResult<string> cancelled =
            await new Dialog(registry, cancelling).ShowAsync(new StringTestDialogViewModel());

        Assert.That(completed, Is.EqualTo(new DialogResult<string>.Completed("結果")));
        Assert.That(cancelled, Is.EqualTo(new DialogResult<string>.Cancelled()));
    }

    /// <summary>重ねて show した場合も、各呼び出しが自分のダイアログの結果を受け取る。</summary>
    [Test]
    [Description("重ねた show は各自の結果を受け取る")]
    public async Task StackedShowsReceiveTheirOwnResults()
    {
        List<DialogNotifier<bool>> notifiers = [];
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifiers.Add(notifier);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        Task<DialogResult<bool>> lower = dialogs.ShowAsync(new BooleanTestDialogViewModel("1枚目"));
        Task<DialogResult<bool>> upper = dialogs.ShowAsync(new BooleanTestDialogViewModel("2枚目"));
        Assert.That(notifiers, Has.Count.EqualTo(2), "2 枚目の委譲が行われていません。");

        // 手前 (2 枚目) を先に閉じ、続けて下の 1 枚を別の値で閉じる
        notifiers[1].Complete(true);
        notifiers[0].Complete(false);

        Assert.That(await upper, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(await lower, Is.EqualTo(new DialogResult<bool>.Completed(false)));
        Assert.That(gateway.Requests, Has.Count.EqualTo(2));
    }

    /// <summary>委譲面が構成エラーで失敗した場合、結果に化けず失敗のまま呼び出し元へ届く。</summary>
    [Test]
    [Description("委譲面の構成エラーは結果に化けない")]
    public void GatewayFailureIsNotTurnedIntoAResult()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> _) => new Label());
        FailingTestDialogGateway gateway = new(new DialogException.PresentationHostUnavailable());
        IKsDialog dialogs = new Dialog(registry, gateway);

        Assert.ThrowsAsync<DialogException.PresentationHostUnavailable>(
            async () => await dialogs.ShowAsync(new BooleanTestDialogViewModel()));

        Assert.That(gateway.PresentCount, Is.EqualTo(1));
    }
}
