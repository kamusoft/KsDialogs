using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// VM 型キーによる View 解決・毎回生成・入口をまたいだレジストリ共有の検証。
/// </summary>
[TestFixture]
public class DialogRegistryTests
{
    /// <summary>登録済みの ViewModel 型では、factory が生成した View がそのまま中身になる。</summary>
    [Test]
    [Description("登録済み VM 型の show で View が表示される")]
    public async Task RegisteredViewModelTypeResolvesItsView()
    {
        Label registered = new();
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return registered;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        await dialogs.ShowAsync(new BooleanTestDialogViewModel());

        Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(registered));
    }

    /// <summary>show のたびに factory が呼ばれ、View は使い回されない。</summary>
    [Test]
    [Description("show ごとに View は新規生成される")]
    public async Task EachShowCreatesItsOwnView()
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        await dialogs.ShowAsync(new BooleanTestDialogViewModel("1枚目"));
        await dialogs.ShowAsync(new BooleanTestDialogViewModel("2枚目"));

        Assert.That(gateway.CreatedViews, Has.Count.EqualTo(2));
        Assert.That(gateway.CreatedViews[0], Is.Not.SameAs(gateway.CreatedViews[1]));
    }

    /// <summary>同じ ViewModel 型の別インスタンスで重ねて show すると、View と結果は show ごとに独立する。</summary>
    /// <remarks>
    /// 同一インスタンスの並行 show は結果報告口の紐付けが 1 つに限られるため成立しない
    /// (core/ADR-0018)。その挙動は notifier の供給の検証が受け持つ。
    /// </remarks>
    [Test]
    [Description("同一 VM 型の別インスタンスの show は独立した重ね出しになる")]
    public async Task DistinctInstancesOfSameViewModelTypeAreShownIndependently()
    {
        int shownCount = 0;
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            // 1 枚目は true、2 枚目は false で完了させ、結果の取り違えを見分ける
            notifier.Complete(shownCount++ == 0);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> first = await dialogs.ShowAsync(new BooleanTestDialogViewModel("1枚目"));
        DialogResult<bool> second = await dialogs.ShowAsync(new BooleanTestDialogViewModel("2枚目"));

        Assert.That(first, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(second, Is.EqualTo(new DialogResult<bool>.Completed(false)));
        Assert.That(gateway.Requests, Has.Count.EqualTo(2));
        Assert.That(
            gateway.Requests[0].ResultChannel,
            Is.Not.SameAs(gateway.Requests[1].ResultChannel),
            "2 回の show が同じ結果チャネルを共有しました。");
    }

    /// <summary>既定 singleton エントリのレジストリは、共有レジストリそのものを指す。</summary>
    [Test]
    [Description("既定エントリのレジストリは共有レジストリを指す")]
    public void DefaultEntryPointUsesSharedRegistry()
    {
        Assert.That(Dialog.Instance.Registry, Is.SameAs(DialogViewRegistry.Shared));
    }

    /// <summary>既定 singleton エントリ経由の登録が、契約 interface 経由の参照から解決できる。</summary>
    [Test]
    [Description("片方の入口の登録がもう片方から見える")]
    public async Task RegistrationFromOneEntryPointIsVisibleFromTheOther()
    {
        Label registered = new();
        Dialog.Instance.Registry.Register((SharedRegistryTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return registered;
        });
        TestDialogGateway gateway = new();
        // DI 注入で使うインスタンスは既定のレジストリを共有する
        IKsDialog injected = new Dialog(gateway);

        DialogResult<bool> result = await injected.ShowAsync(new SharedRegistryTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        Assert.That(gateway.CreatedViews[0], Is.SameAs(registered));
    }
}
