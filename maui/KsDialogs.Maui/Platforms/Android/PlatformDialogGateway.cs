using System;
using System.Threading.Tasks;
using KsDialogs.Bridge;
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
    /// <summary>基準領域「表示中のページ」のページを Native 実装へ教える口を、最初の表示より前に登録しておく。</summary>
    public PlatformDialogGateway() => PlatformCurrentPage.EnsureInstalled();

    /// <inheritdoc/>
    public async Task<DialogOutcome> PresentAsync(DialogPresentationRequest request)
    {
        DialogPresentationCompletion completion = new(request.ResultChannel);
        // 中身を作れなかった失敗はこの提示 1 回分の預かり口に残り、閉鎖の通知で呼び出し元へ返る
        BridgeContentFailure contentFailure = new();
        using DialogCallerCancellation cancellation = new(request.CancellationToken);

        // 互換面は UI スレッドから呼ぶ。show 自体は任意のスレッドから呼べる。
        // 提示先の有無はここでは判定しない。提示先が無ければ互換面の先 (Native) が出現を待ち、
        // MAUI の画面の文脈はその後に呼ばれる中身の供給の中で、その時点の提示先から解決する
        MauiDialogPresentation? presentation = await MainThread.InvokeOnMainThreadAsync(() =>
            cancellation.Present(
                () => MauiDialogBridge.Shared!.Present(
                    new ContentProvider(request, contentFailure),
                    new ClosureListener(completion, contentFailure)),
                handle => handle.Cancel())).ConfigureAwait(false);
        if (presentation is null)
        {
            // 互換面を呼ぶ前に打ち切られた。何も表示せずに cancelled で終える
            completion.Settle(DialogOutcome.Cancelled.Instance);
            completion.Deliver();
            return await completion.Delivered.ConfigureAwait(false);
        }

        // 結果を確定した show が、自分の出した 1 枚だけを閉じる。中身を作る前に確定した場合は、
        // 中身を作らずに提示そのものが止まる
        _ = request.ResultChannel.Result.ContinueWith(
            _ => presentation.Dismiss(),
            TaskContinuationOptions.ExecuteSynchronously);

        // 配送は退出の演出・覆いの消滅・器の撤去がすべて済んだあと。
        // 互換面の閉鎖の通知はその後に届くため、確定ではなくその通知を待って返る (core/ADR-0017)
        return await completion.Delivered.ConfigureAwait(false);
    }

    /// <summary>中身の MAUI View を platform view として供給し、メタ属性の実効値を添える。</summary>
    /// <remarks>
    /// 供給元は互換面 (Java) から呼ばれる。失敗を例外のまま境界へ返すと未処理の障害になるため、
    /// 中身なしとして返し、提示そのものの失敗として閉鎖の通知で受け取る。
    /// 元の失敗は預かり口に残り、その通知を受けたときに呼び出し元へそのまま返る (core/ADR-0033・core/ADR-0036)。
    /// </remarks>
    /// <param name="request">その show が提示する内容。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。</param>
    private sealed class ContentProvider(
        DialogPresentationRequest request,
        BridgeContentFailure contentFailure)
        : Java.Lang.Object, IMauiDialogContentProvider
    {
        public MauiDialogContent? CreateContent() =>
            BridgeContentSupply.CreateOrFail(
                () => PlatformDialogContent.CreateInPresentationContext(request.CreateContent),
                contentFailure);
    }

    /// <summary>互換面からの閉鎖の通知を、その show の結果の確定と配送へ結び付ける。</summary>
    /// <remarks>
    /// 互換面はダイアログが撤去されてからこの通知を出すため、通知の到着が配送の合図になる。
    /// 中身を作れなかったときに互換面が返す理由は「中身が無かった」ことだけなので、
    /// 元の失敗を預かっていればそれをそのまま渡す (型・メッセージ・スタックが保たれる)。
    /// </remarks>
    /// <param name="completion">その show の結果の確定と配送を扱う面。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。</param>
    private sealed class ClosureListener(
        DialogPresentationCompletion completion,
        BridgeContentFailure contentFailure)
        : Java.Lang.Object, IMauiDialogClosureListener
    {
        // 利用者の操作か、呼び出し元の打ち切りで閉じた
        public void OnCancelled()
        {
            completion.Settle(DialogOutcome.Cancelled.Instance);
            completion.Deliver();
        }

        // 呼び出し側からの閉鎖要求。結果はその時点で確定済みで、この通知が撤去済みの合図になる
        public void OnDismissed() => completion.Deliver();

        public void OnFailed(string? message) =>
            completion.Fail(contentFailure.Cause ?? new InvalidOperationException(
                message ?? "Could not present the Dialog."));
    }
}
