using System;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 表示中の ViewModel から結果報告口を引ける紐付け (core/ADR-0018) の検証。
/// </summary>
[TestFixture]
public class DialogNotifierSupplyTests
{
    /// <summary>ViewModel 引数だけの factory の中身が、ViewModel 経由の報告口で結果を返せる。</summary>
    [Test]
    [Description("[MB-NI-01] VM 引数のみの factory の中身が VM 経由の報告口で結果を返す")]
    public async Task MB_NI_01_TheViewModelOnlyFactoryReportsThroughTheSuppliedNotifier()
    {
        DialogNotifier<bool>? notifierInFactory = null;
        DialogViewRegistry registry = new();
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            // factory 実行中に引けることを見る (factory 完了後の紐付けではこの検証は通らない)
            notifierInFactory = viewModel.Notifier;
            notifierInFactory?.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new ModelBindingTestDialogViewModel());

        Assert.Multiple(() =>
        {
            Assert.That(notifierInFactory, Is.Not.Null, "factory 実行中に報告口を引けること");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }

    /// <summary>show していない ViewModel からは報告口を引けない。</summary>
    [Test]
    [Description("[MB-NI-02] show 前の VM からは報告口を引けない")]
    public void MB_NI_02_TheNotifierIsAbsentBeforeTheShow()
    {
        ModelBindingTestDialogViewModel viewModel = new();

        Assert.That(viewModel.Notifier, Is.Null);
    }

    /// <summary>結果が配送し終わった ViewModel からは報告口を引けない。</summary>
    [Test]
    [Description("[MB-NI-03] 結果配送後の VM からは報告口を引けない")]
    public async Task MB_NI_03_TheNotifierIsAbsentAfterTheOutcomeIsDelivered()
    {
        DialogViewRegistry registry = new();
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            viewModel.Notifier?.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());
        ModelBindingTestDialogViewModel shown = new();

        await dialogs.ShowAsync(shown);

        Assert.That(shown.Notifier, Is.Null);
    }

    /// <summary>同じインスタンスの並行 show は構成ミスとして失敗し、先行の表示には影響しない。</summary>
    [Test]
    [Description("[MB-NI-04] 同一 VM インスタンスの並行 show は構成ミスとして失敗する")]
    public async Task MB_NI_04_ShowingTheSameViewModelInstanceConcurrentlyFails()
    {
        DialogNotifier<bool>? notifierOfFirstShow = null;
        DialogViewRegistry registry = new();
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            notifierOfFirstShow ??= viewModel.Notifier;
            return new Label();
        });
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);
        ModelBindingTestDialogViewModel viewModel = new();

        Task<DialogResult<bool>> first = dialogs.ShowAsync(viewModel);
        Assert.ThrowsAsync<DialogException.ViewModelAlreadyShowing>(
            async () => await dialogs.ShowAsync(viewModel));

        // 表示中の 1 枚目はそのまま操作でき、報告した結果はそちらへ配送される
        notifierOfFirstShow!.Complete(true);
        DialogResult<bool> result = await first;

        Assert.Multiple(() =>
        {
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
            Assert.That(gateway.Requests, Has.Count.EqualTo(1), "失敗した 2 度目は提示に進まないこと");
        });
    }

    /// <summary>2 引数 factory で登録しても、ViewModel 経由の報告口は同じ配送先を指す。</summary>
    [Test]
    [Description("[MB-NI-05] 2引数 factory でも VM 経由の報告口は同じ配送先を指す")]
    public async Task MB_NI_05_TheSuppliedNotifierTargetsTheSameDeliveryAsTheFactoryArgument()
    {
        DialogViewRegistry registry = new();
        registry.Register((ModelBindingTestDialogViewModel viewModel, DialogNotifier<bool> _) =>
        {
            // factory 引数ではなく ViewModel から引いた報告口で報告する
            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());

        DialogResult<bool> result = await dialogs.ShowAsync(new ModelBindingTestDialogViewModel());

        Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
    }

    /// <summary>等価比較が一致する別インスタンスは、別の紐付けとして扱われる。</summary>
    [Test]
    [Description("[MB-NI-06] 等価な別インスタンスの VM は互いに干渉しない")]
    public async Task MB_NI_06_EqualButDistinctViewModelInstancesDoNotInterfere()
    {
        DialogViewRegistry registry = new();
        registry.Register((EquatableTestDialogViewModel _) => new Label());
        TestDialogGateway gateway = new();
        IKsDialog dialogs = new Dialog(registry, gateway);
        EquatableTestDialogViewModel first = new();
        EquatableTestDialogViewModel second = new();

        Task<DialogResult<bool>> firstShow = dialogs.ShowAsync(first);
        Task<DialogResult<bool>> secondShow = dialogs.ShowAsync(second);
        second.Notifier!.Complete(true);
        DialogResult<bool> secondResult = await secondShow;

        Assert.Multiple(() =>
        {
            Assert.That(first, Is.EqualTo(second), "等価比較は一致すること");
            Assert.That(first, Is.Not.SameAs(second));
            Assert.That(secondResult, Is.EqualTo(new DialogResult<bool>.Completed(true)));
            Assert.That(firstShow.IsCompleted, Is.False, "報告していない側は表示されたままであること");
            Assert.That(gateway.Requests, Has.Count.EqualTo(2));
        });

        first.Notifier!.Cancel();
        await firstShow;
    }

    /// <summary>中身の生成が失敗しても紐付けは外れ、同じインスタンスを再び show できる。</summary>
    [Test]
    [Description("[MB-NI-07] 異常終了でも紐付けが外れ、同じ VM を再 show できる")]
    public async Task MB_NI_07_TheBindingIsRemovedOnFailureSoTheViewModelCanBeShownAgain()
    {
        int factoryCallCount = 0;
        DialogViewRegistry registry = new();
        registry.Register((ModelBindingTestDialogViewModel viewModel) =>
        {
            if (factoryCallCount++ == 0)
            {
                throw new InvalidOperationException("中身の生成に失敗しました。");
            }

            viewModel.Notifier!.Complete(true);
            return new Label();
        });
        IKsDialog dialogs = new Dialog(registry, new TestDialogGateway());
        ModelBindingTestDialogViewModel viewModel = new();

        Assert.ThrowsAsync<InvalidOperationException>(async () => await dialogs.ShowAsync(viewModel));
        DialogNotifier<bool>? notifierAfterFailure = viewModel.Notifier;
        DialogResult<bool> result = await dialogs.ShowAsync(viewModel);

        Assert.Multiple(() =>
        {
            Assert.That(notifierAfterFailure, Is.Null, "失敗直後に紐付けが外れていること");
            Assert.That(result, Is.EqualTo(new DialogResult<bool>.Completed(true)));
        });
    }
}
