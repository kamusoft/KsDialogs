using System;
using System.Collections.Generic;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// ViewModel の型を渡す Toast の表示 (core/ADR-0035) の解決順序と失敗の分類の検証。
/// </summary>
/// <remarks>
/// 表示中のリスト・重なり順・計時は Native 実装が唯一の正として持つため、ここでは検証しない。
/// MAUI 形態の責務は「呼び出し時点で解決し、生成と configure を受理後の中身の生成に載せ、
/// 供給値をそのまま委譲面へ渡す」ところまでである。
/// </remarks>
[TestFixture]
public class ToastTypedShowTests
{
    /// <summary>生成 → configure → 中身の生成 → 表示の一連が動く。</summary>
    [Test]
    [Description("[TS-TY-02] 型指定 show が生成 → configure → 表示の一連で動く")]
    public void TS_TY_02_TheTypedShowRunsCreationConfigureAndPresentation()
    {
        List<string> shownMessages = [];
        ToastViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedToastTestViewModel());
        registry.Register((TypedToastTestViewModel viewModel) =>
        {
            // configure が設定した状態を中身の初期化から読めることを見る
            shownMessages.Add(viewModel.Message);
            return new Label();
        });
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(registry, gateway);

        toast.Show<TypedToastTestViewModel>(viewModel => viewModel.Message = "保存しました");

        Assert.Multiple(() =>
        {
            Assert.That(shownMessages, Is.EqualTo(new[] { "保存しました" }));
            Assert.That(gateway.Requests, Has.Count.EqualTo(1));
            Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1));
        });
    }

    /// <summary>ViewModel factory が未登録なら、呼び出し時点で同期に失敗する。</summary>
    [Test]
    [Description("[TS-TY-03] VM factory 未登録の型指定 show は呼び出し時点で失敗する")]
    public void TS_TY_03_TheTypedShowFailsSynchronouslyWhenTheViewModelFactoryIsNotRegistered()
    {
        ToastViewRegistry registry = new();
        registry.Register((ViewModelFactoryMissingToastTestViewModel _) => new Label());
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(registry, gateway);

        DialogException.ViewModelFactoryNotRegistered? failure =
            Assert.Throws<DialogException.ViewModelFactoryNotRegistered>(
                () => toast.Show<ViewModelFactoryMissingToastTestViewModel>());

        Assert.Multiple(() =>
        {
            Assert.That(
                failure!.ViewModelTypeName,
                Does.Contain(nameof(ViewModelFactoryMissingToastTestViewModel)));
            Assert.That(gateway.Requests, Is.Empty, "表示は行われない");
        });
    }

    /// <summary>configure の失敗は受理後の失敗として、その 1 枚だけを破棄する。</summary>
    [Test]
    [Description("[TS-TY-04] configure の失敗は受理後の失敗としてその 1 枚だけを破棄する")]
    public void TS_TY_04_TheFailureOfTheConfigureDiscardsOnlyThatOneToast()
    {
        InvalidOperationException thrown = new("configure に失敗しました");
        int factoryCallCount = 0;
        ToastViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedToastTestViewModel());
        registry.Register((TypedToastTestViewModel _) =>
        {
            factoryCallCount++;
            return new Label();
        });
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(registry, gateway);
        // 失敗しかしない lambda は configure の形を明示して渡す
        Action<TypedToastTestViewModel> configure = _ => throw thrown;

        toast.Show("先に出ている Toast");
        IReadOnlyList<string> warnings = RecordingTraceListener.Capture(
            () => Assert.DoesNotThrow(() => toast.Show(configure)));
        toast.Show<TypedToastTestViewModel>(viewModel => viewModel.Message = "後続");

        Assert.Multiple(() =>
        {
            Assert.That(warnings, Has.Count.EqualTo(1), "破棄の理由は警告に残る");
            Assert.That(warnings[0], Does.Contain("configure に失敗しました"));
            Assert.That(gateway.DiscardedCount, Is.EqualTo(1), "破棄されるのはその 1 枚だけ");
            Assert.That(factoryCallCount, Is.EqualTo(1), "失敗した 1 枚では View factory が呼ばれない");
            Assert.That(gateway.CreatedViews, Has.Count.EqualTo(1), "後続の表示は成立する");
        });
    }

    /// <summary>configure を省略すると、ViewModel factory の生成物がそのまま表示される。</summary>
    [Test]
    [Description("[TS-TY-05] configure 省略の型指定 show は VM factory の生成物をそのまま表示する")]
    public void TS_TY_05_TheTypedShowWithoutConfigureUsesTheCreatedViewModelAsIs()
    {
        TypedToastTestViewModel created = new() { Message = "生成時の状態" };
        List<TypedToastTestViewModel> shownViewModels = [];
        ToastViewRegistry registry = new();
        registry.RegisterViewModel(() => created);
        registry.Register((TypedToastTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            return new Label();
        });
        IKsToast toast = NewToast(registry, new TestToastGateway());

        toast.Show<TypedToastTestViewModel>();

        Assert.That(shownViewModels, Is.EqualTo(new[] { created }));
    }

    /// <summary>duration と配置の引数は、値のまま委譲面へ届く。</summary>
    [Test]
    [Description("[TS-TY-06] 型指定 show でも duration と置き場所の引数が効く")]
    public void TS_TY_06_TheDurationAndPlacementArgumentsReachThePresentation()
    {
        DialogPlacement placement = new() { VerticalAlignment = DialogAlignment.End, OffsetY = -24d };
        ToastViewRegistry registry = new();
        registry.RegisterViewModel(() => new TypedToastTestViewModel());
        registry.Register((TypedToastTestViewModel _) => new Label());
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(registry, gateway);

        toast.Show<TypedToastTestViewModel>(durationMs: 2500, placement: placement);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Requests[0].DurationMilliseconds, Is.EqualTo(2500));
            Assert.That(gateway.Requests[0].ShowPlacement, Is.SameAs(placement));
            Assert.That(gateway.Contents[0].Placement.VerticalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(gateway.Contents[0].Placement.OffsetY, Is.EqualTo(-24d));
        });
    }

    /// <summary>解決は呼び出し時点のエントリで行い、受理後の再登録は使われない。</summary>
    [Test]
    [Description("[TS-TY-07] 型指定 show は呼び出し時点のエントリで View まで作る")]
    public void TS_TY_07_TheTypedShowUsesTheEntrySnapshotTakenAtTheCall()
    {
        TypedToastTestViewModel first = new();
        TypedToastTestViewModel second = new();
        List<TypedToastTestViewModel> shownViewModels = [];
        List<string> usedFactories = [];
        ToastViewRegistry registry = new();
        registry.RegisterViewModel(() => first);
        registry.Register((TypedToastTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            usedFactories.Add("最初");
            return new Label();
        });
        TestToastGateway gateway = new() { DefersContentCreation = true };
        IKsToast toast = NewToast(registry, gateway);

        toast.Show<TypedToastTestViewModel>();
        registry.RegisterViewModel(() => second);
        registry.Register((TypedToastTestViewModel viewModel) =>
        {
            shownViewModels.Add(viewModel);
            usedFactories.Add("再登録");
            return new Label();
        });
        gateway.DrainDeferredContents();

        Assert.Multiple(() =>
        {
            Assert.That(shownViewModels, Is.EqualTo(new[] { first }), "再登録後の VM factory は使われない");
            Assert.That(usedFactories, Is.EqualTo(new[] { "最初" }), "再登録後の View factory も使われない");
        });
    }

    /// <summary>ViewModel factory の失敗は受理後の失敗として、その 1 枚だけを破棄する。</summary>
    [Test]
    [Description("[TS-TY-08] VM factory の失敗は受理後の失敗としてその 1 枚だけを破棄する")]
    public void TS_TY_08_TheFailureOfTheViewModelFactoryDiscardsOnlyThatOneToast()
    {
        InvalidOperationException thrown = new("ViewModel を作れません");
        bool configureRan = false;
        int factoryCallCount = 0;
        ToastViewRegistry registry = new();
        registry.RegisterViewModel<TypedToastTestViewModel>(() => throw thrown);
        registry.Register((TypedToastTestViewModel _) =>
        {
            factoryCallCount++;
            return new Label();
        });
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(registry, gateway);

        toast.Show("先に出ている Toast");
        IReadOnlyList<string> warnings = RecordingTraceListener.Capture(() =>
            Assert.DoesNotThrow(() => toast.Show<TypedToastTestViewModel>(_ => configureRan = true)));

        Assert.Multiple(() =>
        {
            Assert.That(warnings, Has.Count.EqualTo(1), "破棄の理由は警告に残る");
            Assert.That(warnings[0], Does.Contain("ViewModel を作れません"));
            Assert.That(configureRan, Is.False, "configure は呼ばれない");
            Assert.That(factoryCallCount, Is.Zero, "View factory は呼ばれない");
            Assert.That(gateway.DiscardedCount, Is.EqualTo(1), "破棄されるのはその 1 枚だけ");
            Assert.That(gateway.CreatedViews, Is.Empty);
        });
    }

    /// <summary>検証ごとに独立したレジストリで表示の入口を作る。</summary>
    /// <param name="registry">その検証だけが使うレジストリ。</param>
    /// <param name="gateway">値の受け取りを記録する委譲面。</param>
    /// <returns>表示の入口。</returns>
    private static IKsToast NewToast(ToastViewRegistry registry, TestToastGateway gateway) =>
        new Toast(registry, new ToastSettings(), gateway);
}
