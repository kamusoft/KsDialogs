using System;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// ライブラリが外へ出す診断文言が英語で固定されていることを見る (cross/ADR-0015)。
/// </summary>
/// <remarks>
/// 読み手はライブラリを組み込む開発者であり、失敗型のメッセージと構成ミスの例外文言は英語で書く。
/// 文言そのものは互換契約ではない (契約は例外型・throw される条件) が、ここでは置き換えが
/// 意図どおりであることを完全一致で固定する。
/// </remarks>
[TestFixture]
public class DiagnosticMessageTests
{
    /// <summary>失敗型 6 種の <c>Message</c> が英語文言と完全一致する。</summary>
    [Test]
    [Description("DialogException の全入れ子型が英語文言を持つ")]
    public void DM_MA_01_EveryNestedDialogExceptionCarriesTheEnglishMessage()
    {
        Assert.Multiple(() =>
        {
            Assert.That(
                new DialogException.ViewFactoryNotRegistered("SampleViewModel").Message,
                Is.EqualTo("No View factory is registered for ViewModel type SampleViewModel."));
            Assert.That(
                new DialogException.ViewModelFactoryNotRegistered("SampleViewModel").Message,
                Is.EqualTo("No ViewModel factory is registered for ViewModel type SampleViewModel."));
            Assert.That(
                new DialogException.ViewModelAlreadyShowing("SampleViewModel").Message,
                Is.EqualTo("This ViewModel instance of type SampleViewModel is already being shown."));
            Assert.That(
                new DialogException.ValueTypeViewModel("SampleViewModel").Message,
                Is.EqualTo("ViewModel type SampleViewModel is a value type and cannot be used as a ViewModel."));
            Assert.That(
                new DialogException.ServiceProviderUnavailable().Message,
                Is.EqualTo("The app's IServiceProvider is not available yet."));
            Assert.That(
                new DialogException.PresentationHostUnavailable().Message,
                Is.EqualTo("No screen is available to present the Dialog."));
        });
    }

    /// <summary>既定の中身を facade で作ろうとしたときの例外文言が英語文言と完全一致する。</summary>
    /// <remarks>既定 View の中身は Native ライブラリが持つため、facade 側には生成手段がない。</remarks>
    [Test]
    [Description("既定の中身生成は英語文言の InvalidOperationException になる")]
    public void DM_MA_02_TheBuiltinContentCreationFailsWithTheEnglishMessage()
    {
        ToastPresentationRequest toast = new(
            createContentView: null,
            message: "saved",
            durationMilliseconds: null,
            showPlacement: null);
        LoadingPresentationRequest loading = new(
            createContentView: null,
            message: "loading",
            showPlacement: null,
            progressReceiver: null);

        InvalidOperationException? toastFailure =
            Assert.Throws<InvalidOperationException>(() => toast.CreateContent());
        InvalidOperationException? loadingFailure =
            Assert.Throws<InvalidOperationException>(() => loading.CreateContent());

        Assert.Multiple(() =>
        {
            Assert.That(
                toastFailure?.Message,
                Is.EqualTo("The Native library owns the content of the default Toast."));
            Assert.That(
                loadingFailure?.Message,
                Is.EqualTo("The Native library owns the content of the default Loading."));
        });
    }
}
