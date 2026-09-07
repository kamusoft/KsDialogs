using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Toast の C# 公開面が委譲面へ渡すもの (中身の解決・呼び出しの対応・失敗の扱い) の検証。
/// </summary>
/// <remarks>
/// 表示中のリスト・重なり順・計時・duration の丸めは Native 実装が唯一の正として持つため、
/// ここでは検証しない (core/ADR-0030・0031)。
/// MAUI 形態の責務は「解決してそのまま渡す」ところまでである。
/// </remarks>
[TestFixture]
public class ToastFacadeTests
{
    /// <summary>デフォルト View の表示では、中身の供給を伴わない要求が委譲される。</summary>
    [Test]
    [Description("[TS-MA-02] デフォルト View の要求は中身の供給を伴わない")]
    public void TS_MA_02_TheBuiltinRequestCarriesNoContentFactory()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);

        toast.Show("保存しました");

        ToastPresentationRequest delegated = gateway.Requests[0];
        Assert.Multiple(() =>
        {
            Assert.That(delegated.IsBuiltin, Is.True);
            Assert.That(delegated.Message, Is.EqualTo("保存しました"));
            Assert.That(delegated.DurationMilliseconds, Is.Null, "省略した duration は運ばれないこと");
            Assert.That(gateway.Contents, Is.Empty, "中身の生成は起きないこと");
        });
    }

    /// <summary>未登録の ViewModel 型は構成ミスとして呼び出し時点で失敗し、表示に進まない。</summary>
    [Test]
    [Description("[TS-MA-02] 未登録のカスタム Toast は fail-fast で表示に進まない")]
    public void TS_MA_02_AnUnregisteredViewModelFailsWithoutShowing()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);
        UnregisteredToastTestViewModel viewModel = new();

        Assert.Multiple(() =>
        {
            Assert.Throws<DialogException.ViewFactoryNotRegistered>(() => toast.Show(viewModel));
            Assert.That(gateway.Requests, Is.Empty);
        });
    }

    /// <summary>値型の ViewModel は、表示の入口で構成ミスとして弾かれる。</summary>
    [Test]
    [Description("[TS-MA-02] 値型の ViewModel は表示の入口で弾かれる")]
    public void TS_MA_02_AValueTypeViewModelIsRejectedAtTheEntry() =>
        Assert.Throws<DialogException.ValueTypeViewModel>(() =>
            NewToast(new TestToastGateway()).Show(new ValueTypeToastTestViewModel()));

    /// <summary>インライン factory の表示はレジストリを変えない (core/ADR-0013)。</summary>
    [Test]
    [Description("[TS-MA-02] インライン factory の表示はレジストリを変えない")]
    public void TS_MA_02_TheInlineFactoryDoesNotTouchTheRegistry()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);
        InlineToastTestViewModel viewModel = new();
        toast.Registry.Register<InlineToastTestViewModel>(_ => new Label { Text = "登録" });

        toast.Show(viewModel, _ => new Label { Text = "インライン" });
        toast.Show(viewModel);

        Assert.Multiple(() =>
        {
            Assert.That(((Label)gateway.CreatedViews[0]).Text, Is.EqualTo("インライン"));
            Assert.That(((Label)gateway.CreatedViews[1]).Text, Is.EqualTo("登録"));
        });
    }

    /// <summary>Loading と Toast のレジストリは独立しており、片方の登録が他方に影響しない。</summary>
    [Test]
    [Description("[TS-MA-02] Loading と Toast のレジストリは独立している")]
    public async System.Threading.Tasks.Task TS_MA_02_TheLoadingAndToastRegistriesAreIndependent()
    {
        TestToastGateway toastGateway = new();
        TestLoadingGateway loadingGateway = new();
        IKsToast toast = NewToast(toastGateway);
        IKsLoading loading = new Loading(LoadingViewRegistry.Shared, new LoadingSettings(), loadingGateway);
        SharedContractToastTestViewModel viewModel = new();

        loading.Registry.Register<SharedContractToastTestViewModel>(_ => new Label { Text = "ローディング" });
        toast.Registry.Register<SharedContractToastTestViewModel>(_ => new Label { Text = "トースト" });

        await loading.ShowAsync(viewModel);
        toast.Show(viewModel);

        Assert.Multiple(() =>
        {
            Assert.That(((Label)loadingGateway.CreatedViews[0]).Text, Is.EqualTo("ローディング"));
            Assert.That(((Label)toastGateway.CreatedViews[0]).Text, Is.EqualTo("トースト"));
        });
    }

    /// <summary>設定を持ち越さないよう、検証ごとに新しい設定の置き場を使う。</summary>
    /// <param name="gateway">値を受け取る委譲面。</param>
    /// <returns>その委譲面だけを見る表示の入口。</returns>
    private static IKsToast NewToast(TestToastGateway gateway) =>
        new Toast(ToastViewRegistry.Shared, new ToastSettings(), gateway);
}
