using System;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Loading の C# 公開面が委譲面へ渡すもの (中身の解決・呼び出しの対応・失敗の扱い) の検証。
/// </summary>
/// <remarks>
/// 合流・世代・撤去の順序は Native 実装が唯一の正として持つため、ここでは検証しない
/// (core/ADR-0024)。MAUI 形態の責務は「解決してそのまま渡す」ところまでである。
/// </remarks>
[TestFixture]
public class LoadingFacadeTests
{
    /// <summary>既定ローディングの表示では、中身の供給を伴わない要求が委譲される。</summary>
    [Test]
    [Description("既定ローディングの要求は中身の供給を伴わない")]
    public async Task TheBuiltinRequestCarriesNoContentFactory()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);

        await loading.ShowAsync();
        await loading.HideAsync();
        loading.SetMessage("更新");

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Requests[0].IsBuiltin, Is.True);
            Assert.That(gateway.Requests[0].Message, Is.Null);
            Assert.That(gateway.HideCount, Is.EqualTo(1));
            Assert.That(gateway.UpdatedMessages, Is.EqualTo(new[] { "更新" }));
        });
    }

    /// <summary>スコープ形は処理の戻り値をそのまま返す。</summary>
    [Test]
    [Description("スコープ形は処理の戻り値をそのまま返す")]
    public async Task TheScopedFormReturnsTheValueOfTheAction()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);

        string value = await loading.StartAsync(_ => Task.FromResult("結果"), "読み込み中");

        Assert.Multiple(() =>
        {
            Assert.That(value, Is.EqualTo("結果"));
            Assert.That(gateway.Requests[0].Message, Is.EqualTo("読み込み中"));
        });
    }

    /// <summary>スコープ形の処理の失敗は、握り潰されずに呼び出し元へ伝播する。</summary>
    [Test]
    [Description("スコープ形の処理の失敗が呼び出し元へ伝播する")]
    public void TheScopedFormPropagatesTheFailureOfTheAction()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);

        Assert.ThrowsAsync<InvalidOperationException>(() =>
            loading.StartAsync(_ => Task.FromException<int>(new InvalidOperationException("失敗"))));
    }

    /// <summary>未登録の ViewModel 型は構成ミスとして失敗し、処理も実行されない。</summary>
    [Test]
    [Description("未登録のカスタム Loading は fail-fast で処理を実行しない")]
    public void AnUnregisteredViewModelFailsWithoutRunningTheAction()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        UnregisteredLoadingTestViewModel viewModel = new();
        bool executed = false;

        Assert.Multiple(() =>
        {
            Assert.ThrowsAsync<DialogException.ViewFactoryNotRegistered>(() =>
                loading.StartAsync(viewModel, _ =>
                {
                    executed = true;
                    return Task.CompletedTask;
                }));
            Assert.That(executed, Is.False);
            Assert.That(gateway.Requests, Is.Empty);
        });
    }

    /// <summary>値型の ViewModel は、表示の入口で構成ミスとして弾かれる。</summary>
    [Test]
    [Description("値型の ViewModel は表示の入口で弾かれる")]
    public void AValueTypeViewModelIsRejectedAtTheEntry() =>
        Assert.ThrowsAsync<DialogException.ValueTypeViewModel>(() =>
            NewLoading(new TestLoadingGateway()).ShowAsync(new ValueTypeLoadingTestViewModel()));

    /// <summary>インライン factory の表示はレジストリを変えない (core/ADR-0013)。</summary>
    [Test]
    [Description("インライン factory の表示はレジストリを変えない")]
    public async Task TheInlineFactoryDoesNotTouchTheRegistry()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        InlineLoadingTestViewModel viewModel = new();
        loading.Registry.Register<InlineLoadingTestViewModel>(_ => new Label { Text = "登録" });

        await loading.ShowAsync(viewModel, _ => new Label { Text = "インライン" });
        await loading.ShowAsync(viewModel);

        Assert.Multiple(() =>
        {
            Assert.That(((Label)gateway.CreatedViews[0]).Text, Is.EqualTo("インライン"));
            Assert.That(((Label)gateway.CreatedViews[1]).Text, Is.EqualTo("登録"));
        });
    }

    /// <summary>Dialog と Loading のレジストリは独立しており、片方の登録が他方に影響しない。</summary>
    [Test]
    [Description("Dialog と Loading のレジストリは独立している")]
    public async Task TheDialogAndLoadingRegistriesAreIndependent()
    {
        TestLoadingGateway loadingGateway = new();
        TestDialogGateway dialogGateway = new(request =>
            request.ResultChannel.Settle(DialogOutcome.Cancelled.Instance));
        IKsLoading loading = NewLoading(loadingGateway);
        IKsDialog dialogs = new Dialog(dialogGateway);
        SharedContractLoadingTestViewModel viewModel = new();

        dialogs.Registry.Register<SharedContractLoadingTestViewModel>(
            (_, _) => new Label { Text = "ダイアログ" });
        loading.Registry.Register<SharedContractLoadingTestViewModel>(_ => new Label { Text = "ローディング" });

        await dialogs.ShowAsync(viewModel);
        await loading.ShowAsync(viewModel);

        Assert.Multiple(() =>
        {
            Assert.That(((Label)dialogGateway.CreatedViews[0]).Text, Is.EqualTo("ダイアログ"));
            Assert.That(((Label)loadingGateway.CreatedViews[0]).Text, Is.EqualTo("ローディング"));
        });
    }

    /// <summary>設定を持ち越さないよう、検証ごとに新しい設定の置き場を使う。</summary>
    /// <param name="gateway">値を受け取る委譲面。</param>
    /// <returns>その委譲面だけを見る表示の入口。</returns>
    private static IKsLoading NewLoading(TestLoadingGateway gateway) =>
        new Loading(LoadingViewRegistry.Shared, new LoadingSettings(), gateway);
}
