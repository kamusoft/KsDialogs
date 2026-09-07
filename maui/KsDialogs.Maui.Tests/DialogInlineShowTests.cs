using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 登録せずに factory を直接渡すインライン show の検証。
/// </summary>
/// <remarks>
/// 表示・結果の一連が成立することに加えて、レジストリに一切干渉しないこと
/// (既存登録との共存・並行インライン表示の独立) を見る (core/ADR-0013)。
/// </remarks>
[TestFixture]
public class DialogInlineShowTests
{
    /// <summary>未登録の ViewModel 型でも、渡した factory の View が中身になり型付き結果が返る。</summary>
    [Test]
    [Description("未登録 VM のインライン表示で型付き結果が返る")]
    public async Task InlineShowWithUnregisteredViewModelPresentsAndReturnsTypedResult()
    {
        Label inline = new();
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(new DialogViewRegistry(), gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel(),
            (_, notifier) =>
            {
                notifier.Complete(true);
                return inline;
            });

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(inline));
    }

    /// <summary>結果型を宣言した ViewModel のインライン show は、その型の結果を返す。</summary>
    [Test]
    [Description("インライン show の結果は宣言結果型で返る")]
    public async Task InlineShowKeepsDeclaredResultType()
    {
        IKsDialog dialogs = new Dialog(new DialogViewRegistry(), new TestDialogGateway());

        DialogResult<string> result = await dialogs.ShowAsync(
            new StringTestDialogViewModel(),
            (StringTestDialogViewModel _, DialogNotifier<string> notifier) =>
            {
                notifier.Complete("結果");
                return new Label();
            });

        Assert.That(result, Is.EqualTo(new DialogResult<string>.Completed("結果")));
    }

    /// <summary>インライン show でもキャンセル報告は cancelled として返る。</summary>
    [Test]
    [Description("インライン show でも cancelled が返る")]
    public async Task InlineShowReturnsCancelled()
    {
        IKsDialog dialogs = new Dialog(new DialogViewRegistry(), new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel(),
            (_, notifier) =>
            {
                notifier.Cancel();
                return new Label();
            });

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Cancelled()));
    }

    /// <summary>show の引数で渡した置き場所は、インライン経路でもそのまま器へ届く。</summary>
    [Test]
    [Description("インライン show の placement は器へそのまま渡る")]
    public async Task InlineShowPassesItsPlacementToTheContainer()
    {
        DialogPlacement placement = new() { HorizontalAlignment = DialogAlignment.Start, OffsetX = 12d };
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(new DialogViewRegistry(), gateway);

        await dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel(),
            (_, notifier) =>
            {
                notifier.Complete(true);
                return new Label();
            },
            placement);

        Assert.That(gateway.Requests, Has.Count.EqualTo(1));
        Assert.That(gateway.Requests[0].ShowPlacement, Is.SameAs(placement));
        Assert.That(gateway.Contents[0].Placement, Is.SameAs(placement));
    }

    /// <summary>登録済みの ViewModel 型でも、インライン show は渡された factory だけを使う。</summary>
    [Test]
    [Description("インライン show は既存の登録を使わない")]
    public async Task InlineShowIgnoresTheExistingRegistration()
    {
        Label registered = new();
        Label inline = new();
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            notifier.Complete(false);
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(
            new SimpleFacedTestDialogViewModel("インライン"),
            (_, notifier) =>
            {
                notifier.Complete(true);
                return inline;
            });

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(inline));
    }

    /// <summary>インライン show の前後で、同じ型の登録内容は変わらない。</summary>
    [Test]
    [Description("インライン show の前後で登録内容は変わらない")]
    public async Task InlineShowLeavesTheExistingRegistrationUntouched()
    {
        Label registered = new();
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            notifier.Complete(false);
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        await dialogs.ShowAsync(
            new SimpleFacedTestDialogViewModel("インライン"),
            (_, notifier) =>
            {
                notifier.Complete(true);
                return new Label();
            });

        // レジストリ経由の show は、インライン表示の影響を受けず元の factory を使う
        DialogResult<bool> result = await dialogs.ShowAsync(new SimpleFacedTestDialogViewModel("登録経由"));

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(false)));
        Assert.That(gateway.CreatedViews[1], Is.SameAs(registered));
    }

    /// <summary>インライン表示しても、その ViewModel 型が登録済みになるわけではない。</summary>
    [Test]
    [Description("インライン show をしても登録済みにはならない")]
    public async Task InlineShowDoesNotRegisterTheViewModelType()
    {
        DialogViewRegistry registry = new();
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        await dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel(),
            (_, notifier) =>
            {
                notifier.Complete(true);
                return new Label();
            });

        // 一時的にも登録していないため、あとからレジストリ経由で呼べば未登録の失敗になる
        Assert.ThrowsAsync<DialogException.ViewFactoryNotRegistered>(
            async () => await dialogs.ShowAsync(new InlineOnlyTestDialogViewModel()));
        Assert.That(gateway.Requests, Has.Count.EqualTo(1), "未登録の show が提示に進みました。");
    }

    /// <summary>同じ ViewModel 型のインライン show を並行させても、factory と結果は独立する。</summary>
    [Test]
    [Description("同じ VM 型の並行インライン show は factory も結果も独立する")]
    public async Task ConcurrentInlineShowsOfTheSameViewModelTypeStayIndependent()
    {
        List<DialogNotifier<bool>> notifiers = [];
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(new DialogViewRegistry(), gateway);

        // 委譲面は結果が確定するまで待つため、報告するまでどちらの show も未確定のまま並ぶ
        Task<DialogResult<bool>> first = dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel("1枚目"),
            (_, notifier) =>
            {
                notifiers.Add(notifier);
                return new Label();
            });
        Task<DialogResult<bool>> second = dialogs.ShowAsync(
            new InlineOnlyTestDialogViewModel("2枚目"),
            (_, notifier) =>
            {
                notifiers.Add(notifier);
                return new Label();
            });

        Assert.That(notifiers, Has.Count.EqualTo(2));
        Assert.That(gateway.CreatedViews[0], Is.Not.SameAs(gateway.CreatedViews[1]));

        // 片方を閉じても、もう片方は未確定のまま
        notifiers[1].Complete(true);
        Assert.That(await second, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(first.IsCompleted, Is.False, "片方の結果がもう片方を確定させました。");

        notifiers[0].Complete(false);
        Assert.That(await first, Is.EqualTo(new DialogResult<bool>.Completed(false)));
    }

    /// <summary>インライン show と登録経由の show を並行させても、結果の宛先は混ざらない。</summary>
    [Test]
    [Description("インライン show と登録経由の show は結果が混ざらない")]
    public async Task InlineAndRegisteredShowsOfTheSameViewModelTypeDoNotMixResults()
    {
        DialogNotifier<bool>? registeredNotifier = null;
        DialogNotifier<bool>? inlineNotifier = null;
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            registeredNotifier = notifier;
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        Task<DialogResult<bool>> registered = dialogs.ShowAsync(new SimpleFacedTestDialogViewModel("登録経由"));
        Task<DialogResult<bool>> inline = dialogs.ShowAsync(
            new SimpleFacedTestDialogViewModel("インライン"),
            (_, notifier) =>
            {
                inlineNotifier = notifier;
                return new Label();
            });

        inlineNotifier!.Complete(true);
        registeredNotifier!.Complete(false);

        Assert.That(await inline, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(await registered, Is.EqualTo(new DialogResult<bool>.Completed(false)));
    }
}
