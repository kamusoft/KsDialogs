using System;
using System.Threading.Tasks;
using KsDialogs.Bridge;
using Microsoft.Maui;
using Microsoft.Maui.ApplicationModel;

namespace KsDialogs;

/// <summary>
/// Android の互換面へ委譲する面。
/// </summary>
/// <remarks>
/// 提示先の解決・UI スレッドへのマーシャリング・重なりの扱いはすべて Native ライブラリが受け持ち、
/// この面は MAUI View の platform view 化と結果チャネルとの結び付けだけを行う。
/// 中身の写しそのものは器によらない共通部品 (<see cref="PlatformDialogContent"/>) が受け持つ。
/// </remarks>
internal sealed class PlatformDialogGateway : IDialogGateway
{
    /// <inheritdoc/>
    public async Task<DialogOutcome> PresentAsync(DialogPresentationRequest request)
    {
        DialogPresentationCompletion completion = new(request.ResultChannel);

        // 提示先の解決は画面の状態を読むため UI スレッドで行う。show 自体は任意のスレッドから呼べる
        MauiDialogPresentation presentation = await MainThread.InvokeOnMainThreadAsync(() =>
        {
            // platform view 化に要る文脈が取れない時点で提示先が無いため、View を作らずに失敗させる
            IMauiContext mauiContext = PlatformDialogContent.ResolveMauiContext()
                ?? throw new DialogException.PresentationHostUnavailable();

            return MauiDialogBridge.Shared!.Present(
                new ContentProvider(request, mauiContext),
                new ClosureListener(completion));
        }).ConfigureAwait(false);

        // 結果を確定した show が、自分の出した 1 枚だけを閉じる
        _ = request.ResultChannel.Result.ContinueWith(
            _ => presentation.Dismiss(),
            TaskContinuationOptions.ExecuteSynchronously);

        // 配送は退出の演出・覆いの消滅・器の撤去がすべて済んだあと。
        // 互換面の閉鎖の通知はその後に届くため、確定ではなくその通知を待って返る (core/ADR-0017)
        return await completion.Delivered.ConfigureAwait(false);
    }

    /// <summary>中身の MAUI View を platform view として供給し、メタ属性の実効値を添える。</summary>
    /// <param name="request">その show が提示する内容。</param>
    /// <param name="mauiContext">platform view 化に使う文脈。</param>
    private sealed class ContentProvider(DialogPresentationRequest request, IMauiContext mauiContext)
        : Java.Lang.Object, IMauiDialogContentProvider
    {
        public MauiDialogContent CreateContent() =>
            PlatformDialogContent.Create(request.CreateContent(), mauiContext);
    }

    /// <summary>互換面からの閉鎖の通知を、その show の結果の確定と配送へ結び付ける。</summary>
    /// <remarks>
    /// 互換面はダイアログが撤去されてからこの通知を出すため、通知の到着が配送の合図になる。
    /// </remarks>
    /// <param name="completion">その show の結果の確定と配送を扱う面。</param>
    private sealed class ClosureListener(DialogPresentationCompletion completion)
        : Java.Lang.Object, IMauiDialogClosureListener
    {
        public void OnCancelled()
        {
            completion.Settle(DialogOutcome.Cancelled.Instance);
            completion.Deliver();
        }

        // 呼び出し側からの閉鎖要求。結果はその時点で確定済みで、この通知が撤去済みの合図になる
        public void OnDismissed() => completion.Deliver();

        public void OnPresentationHostUnavailable(string? message) =>
            completion.Fail(new DialogException.PresentationHostUnavailable());

        public void OnFailed(string? message) =>
            completion.Fail(new InvalidOperationException(
                message ?? "Could not present the Dialog."));
    }
}
