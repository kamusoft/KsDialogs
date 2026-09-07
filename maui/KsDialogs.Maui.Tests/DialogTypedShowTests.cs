using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// ViewModel の型を渡す show (core/ADR-0019・0021) の解決順序と実行順序の検証。
/// </summary>
[TestFixture]
public class DialogTypedShowTests
{
    /// <summary>生成 → configure → 表示 → 結果の一連が動く。</summary>
    [Test]
    [Description("[MB-TS-01] 型指定 show が生成 → configure → 表示 → 結果の一連で動く")]
    public async Task MB_TS_01_TheTypedShowRunsCreationConfigurePresentationAndOutcome()
    {
        List<string> shownMessages = [];
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new ModelBindingTestDialogViewModel());
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            // configure が設定した状態を中身の初期化から読めることを見る
            shownMessages.Add(viewModel.Message);
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
            viewModel => viewModel.Message = "確認");

        Assert.Multiple(() =>
        {
            Assert.That(shownMessages, Is.EqualTo(new[] { "確認" }));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>非同期 configure が完了するまで、中身の生成は始まらない。</summary>
    [Test]
    [Description("[MB-TS-02] 非同期 configure の完了まで中身の生成が始まらない")]
    public async Task MB_TS_02_TheViewIsNotCreatedUntilTheAsynchronousConfigureCompletes()
    {
        TaskCompletionSource configureGate = new();
        int factoryCallCount = 0;
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new ModelBindingTestDialogViewModel());
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            factoryCallCount++;
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        Task<DialogResult<bool>> show = dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
            async viewModel =>
            {
                await configureGate.Task;
                viewModel.Message = "非同期で用意";
            });
        int factoryCallCountBeforeConfigureCompletes = factoryCallCount;
        configureGate.SetResult();
        DialogResult<bool> result = await show;

        Assert.Multiple(() =>
        {
            Assert.That(
                factoryCallCountBeforeConfigureCompletes,
                Is.Zero,
                "configure の完了前に中身が生成されないこと");
            Assert.That(factoryCallCount, Is.EqualTo(1));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>ViewModel factory が未登録なら、結果に化けさせず構成ミスとして失敗する。</summary>
    [Test]
    [Description("[MB-TS-03] ViewModel factory 未登録の型指定 show は構成ミスとして失敗する")]
    public void MB_TS_03_TheTypedShowFailsWhenTheViewModelFactoryIsNotRegistered()
    {
        DialogViewRegistry registry = new();
        registry.Register((ViewModelFactoryMissingTestViewModel _) => new Label());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        Assert.ThrowsAsync<DialogException.ViewModelFactoryNotRegistered>(
            async () => await dialogs.ShowAsync<ViewModelFactoryMissingTestViewModel>());
        Assert.That(gateway.Requests, Is.Empty, "提示に進まないこと");
    }

    /// <summary>configure を省略すると、ViewModel factory の生成物がそのまま表示される。</summary>
    [Test]
    [Description("[MB-TS-04] configure 省略の型指定 show は ViewModel factory の生成物をそのまま表示する")]
    public async Task MB_TS_04_TheTypedShowWithoutConfigureUsesTheFactoryOutcomeAsIs()
    {
        List<string> shownMessages = [];
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new ModelBindingTestDialogViewModel { Message = "factory の値" });
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            shownMessages.Add(viewModel.Message);
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync<ModelBindingTestDialogViewModel>();

        Assert.Multiple(() =>
        {
            Assert.That(shownMessages, Is.EqualTo(new[] { "factory の値" }));
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>configure の失敗は提示に進まず伝播し、そのあとの型指定 show は正常に動く。</summary>
    [Test]
    [Description("[MB-TS-05] configure の失敗は提示に進まず伝播する")]
    public async Task MB_TS_05_TheConfigureFailurePropagatesWithoutPresenting()
    {
        int factoryCallCount = 0;
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new ModelBindingTestDialogViewModel());
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            factoryCallCount++;
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        Assert.ThrowsAsync<InvalidOperationException>(async () =>
            await dialogs.ShowAsync<ModelBindingTestDialogViewModel>(
                _ =>
                {
                    throw new InvalidOperationException("configure に失敗しました。");
                }));
        int factoryCallCountAfterFailure = factoryCallCount;
        DialogResult<bool> result = await dialogs.ShowAsync<ModelBindingTestDialogViewModel>();

        Assert.Multiple(() =>
        {
            Assert.That(factoryCallCountAfterFailure, Is.Zero, "中身が生成されないこと");
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "失敗した呼び出しは提示に進まないこと");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>スロット単位の後勝ちで、片方の再登録はもう片方を保持する。</summary>
    [Test]
    [Description("[MB-TS-06] 再登録はスロット単位の後勝ちで他方を保持する")]
    public async Task MB_TS_06_ReregisteringOneSlotKeepsTheOther()
    {
        Label replaced = new();
        DialogViewRegistry registry = new();
        registry.RegisterViewModel(() => new SlotIndependenceTestViewModel());
        registry.Register((SlotIndependenceTestViewModel _) => new Label());
        registry.Register((SlotIndependenceTestViewModel viewModel) =>
        {
            viewModel.Notifier!.Complete(true);
            return replaced;
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);

        DialogResult<bool> result = await dialogs.ShowAsync<SlotIndependenceTestViewModel>();

        Assert.Multiple(() =>
        {
            Assert.That(gateway.CreatedViews[0], Is.SameAs(replaced), "新しい View factory が使われること");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }
}
