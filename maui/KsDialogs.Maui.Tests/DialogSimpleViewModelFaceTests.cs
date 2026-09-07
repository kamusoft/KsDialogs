using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 結果型を書かない真偽値の顔 (<see cref="IDialogViewModel"/>) で宣言した ViewModel の登録と show の検証。
/// </summary>
/// <remarks>
/// 2 型引数の形と挙動が同じであること — 報告口が真偽値に型付き、show が真偽値の結果を返すこと — を見る。
/// </remarks>
[TestFixture]
public class DialogSimpleViewModelFaceTests
{
    /// <summary>ViewModel 型だけを型引数に取る登録の show は、完了操作で真偽値の completed を返す。</summary>
    [Test]
    [Description("型引数 1 つの登録の show は真偽値の completed を返す")]
    public async Task ViewModelOnlyRegistrationCompletesWithBooleanResult()
    {
        Label registered = new();
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            notifier.Complete(true);
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(new SimpleFacedTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(registered));
    }

    /// <summary>真偽値の顔で登録しても、キャンセル報告は cancelled として返る。</summary>
    [Test]
    [Description("型引数 1 つの登録でも cancelled が返る")]
    public async Task ViewModelOnlyRegistrationReturnsCancelled()
    {
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            notifier.Cancel();
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new SimpleFacedTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Cancelled()));
    }

    /// <summary>省略形で登録した紐付けは、2 型引数の形で登録したものと同じレジストリの枠に入る。</summary>
    [Test]
    [Description("省略形と 2 型引数の登録は同じキーを共有する")]
    public async Task ViewModelOnlyRegistrationSharesTheSameKeyAsTheTwoArgumentForm()
    {
        Label replacement = new();
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel, bool>((_, notifier) =>
        {
            notifier.Complete(false);
            return new Label();
        });
        // 同じ型への再登録は後勝ちで置き換わる
        registry.Register<SimpleFacedTestDialogViewModel>((_, notifier) =>
        {
            notifier.Complete(true);
            return replacement;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await dialogs.ShowAsync(new SimpleFacedTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(replacement));
    }

    /// <summary>省略形の登録でも、factory へ渡る ViewModel は show に渡したインスタンスそのもの。</summary>
    [Test]
    [Description("省略形の factory は show に渡した VM インスタンスを受け取る")]
    public async Task ViewModelOnlyRegistrationPassesTheShownInstanceToItsFactory()
    {
        SimpleFacedTestDialogViewModel shown = new("こんにちは");
        SimpleFacedTestDialogViewModel? received = null;
        DialogViewRegistry registry = new();
        registry.Register<SimpleFacedTestDialogViewModel>((viewModel, notifier) =>
        {
            received = viewModel;
            notifier.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        await dialogs.ShowAsync(shown);

        Assert.That(received, Is.SameAs(shown));
    }
}
