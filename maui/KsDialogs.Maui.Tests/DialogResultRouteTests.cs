using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// MAUI facade から見た結果経路の検証。
/// </summary>
/// <remarks>
/// 委譲面を差し替えて、platform 実装なしに completed / cancelled / 構成エラーの 3 経路を通す。
/// </remarks>
[TestFixture]
public class DialogResultRouteTests
{
    /// <summary>完了操作の報告が、宣言結果型の completed として呼び出し元へ返る。</summary>
    [Test]
    [Description("完了操作で completed が返る")]
    public async Task CompleteOperationReturnsCompleted()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
    }

    /// <summary>結果値は ViewModel の宣言結果型そのままで受け取れる。</summary>
    [Test]
    [Description("完了の結果値は宣言結果型で受け取れる")]
    public async Task CompletedValueKeepsDeclaredResultType()
    {
        DialogViewRegistry registry = new();
        registry.Register((StringTestDialogViewModel _, DialogNotifier<string> notifier) =>
        {
            notifier.Complete("結果");
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<string> result = await dialogs.ShowAsync(new StringTestDialogViewModel());

        // 受け取り側で型を書ける = 宣言結果型が結ばれている
        Assert.That(result, Is.InstanceOf<DialogResult<string>.Completed>());
        string value = ((DialogResult<string>.Completed)result).Value;
        Assert.That(value, Is.EqualTo("結果"));
    }

    /// <summary>キャンセル操作の報告が cancelled として返る。</summary>
    [Test]
    [Description("キャンセル操作で cancelled が返る")]
    public async Task CancelOperationReturnsCancelled()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Cancel();
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Cancelled()));
    }

    /// <summary>器が受けた外側タップは、既定でキャンセルとして返る。</summary>
    [Test]
    [Description("外側タップで cancelled が返る (既定)")]
    public async Task OutsideTapReturnsCancelledByDefault()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> _) => new Label());
        // 外側タップは器が結果チャネルへ直接キャンセルを報告する
        TestDialogGateway gateway = new(request => request.ResultChannel.Settle(DialogOutcome.Cancelled.Instance));
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Cancelled()));
    }

    /// <summary>結果が確定した後の報告は、完了・キャンセルのどちらも結果を書き換えない。</summary>
    [Test]
    [Description("確定後の再報告は無効")]
    public async Task ReportsAfterSettlementAreNoOp()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            notifier.Complete(false);
            notifier.Cancel();
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
    }

    /// <summary>未登録の ViewModel 型は結果を返さず、失敗した Task で届く。View の生成・提示も行われない。</summary>
    [Test]
    [Description("未登録 VM 型の show は即エラー")]
    public void ShowWithUnregisteredViewModelTypeFailsImmediately()
    {
        DialogViewRegistry registry = new();
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogException.ViewFactoryNotRegistered failure =
            Assert.ThrowsAsync<DialogException.ViewFactoryNotRegistered>(
                async () => await dialogs.ShowAsync(new UnregisteredTestDialogViewModel()))!;

        Assert.That(
            failure.ViewModelTypeName,
            Is.EqualTo(typeof(UnregisteredTestDialogViewModel).FullName));
        Assert.That(gateway.Requests, Is.Empty, "View の生成・提示が行われました。");
    }
}
