using System;
using System.Threading.Tasks;
using Foundation;
using KsDialogs.Bridge;
using Microsoft.Maui;

namespace KsDialogs;

/// <summary>
/// iOS の ObjC 互換面 (Loading) へ委譲する面。
/// </summary>
/// <remarks>
/// 合流カウント・表示世代・最新のメッセージと進捗は Native ライブラリの coordinator が持ち、
/// この面は MAUI View の platform view 化と値の写しだけを行う (core/ADR-0024)。
/// </remarks>
internal sealed class PlatformLoadingGateway : ILoadingGateway
{
    /// <inheritdoc/>
    public void ApplyStyle(LoadingStyle style) =>
        MauiLoadingBridge.Shared.ApplyStyle(ToBridgeStyle(style));

    /// <inheritdoc/>
    public void ApplyOptions(DialogOptions options) =>
        MauiLoadingBridge.Shared.ApplyOptions(PlatformDialogContent.ToBridgeOptions(options));

    /// <inheritdoc/>
    public Task ShowAsync(LoadingPresentationRequest request)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 中身を作れなかった失敗はこの呼び出し 1 回分の預かり口に残り、完了の通知で呼び出し元へ返る
        BridgeContentFailure contentFailure = new();
        MauiLoadingBridge.Shared.Show(
            ToBridgeContent(request, contentFailure),
            error => Settle(completion, contentFailure, error));
        return completion.Task;
    }

    /// <inheritdoc/>
    public async Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 処理の失敗は互換面へ渡さず、こちらで抱えたまま合流1件の終了だけを伝える。
        // 呼び出し元へは撤去の完了を待ってからそのまま投げ直す
        Exception? failure = null;
        // 中身を作れなかった失敗も同じく預かり、開始そのものの失敗として呼び出し元へ返す
        BridgeContentFailure contentFailure = new();

        MauiLoadingBridge.Shared.Start(
            ToBridgeContent(request, contentFailure),
            (report, actionCompletion) => _ = LoadingActionRunner.RunAsync(
                action,
                progress => report(progress),
                () => actionCompletion(),
                thrown => failure = thrown),
            error => Settle(completion, contentFailure, error));

        await completion.Task.ConfigureAwait(false);
        if (failure is not null)
        {
            throw failure;
        }
    }

    /// <inheritdoc/>
    public Task HideAsync()
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 撤去は中身を作らないため、預かる失敗も無い
        MauiLoadingBridge.Shared.Hide(error => Settle(completion, contentFailure: null, error));
        return completion.Task;
    }

    /// <inheritdoc/>
    public void SetMessage(string? message) => MauiLoadingBridge.Shared.SetMessage(message);

    /// <summary>互換面から届いた結末を、待っている呼び出しへ渡す。</summary>
    /// <remarks>
    /// 中身を作れなかったときに互換面が返す理由は「中身が無かった」ことだけなので、
    /// 元の失敗を預かっていればそれをそのまま渡す (型・メッセージ・スタックが保たれる)。
    /// 互換面そのものの失敗は預かりが無いため、これまでどおり理由を写して渡す。
    /// </remarks>
    /// <param name="completion">待っている呼び出しの完了源。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。中身を作らない操作では <see langword="null"/>。</param>
    /// <param name="error">失敗の理由。成功なら <see langword="null"/>。</param>
    private static void Settle(
        TaskCompletionSource completion,
        BridgeContentFailure? contentFailure,
        NSError? error)
    {
        if (error is null)
        {
            completion.TrySetResult();
            return;
        }

        if (contentFailure?.Cause is Exception cause)
        {
            completion.TrySetException(cause);
            return;
        }

        completion.TrySetException(new InvalidOperationException(
            error.LocalizedDescription ?? "Could not show the Loading."));
    }

    /// <summary>表示の要求を、互換面が受け取る中身の指定へ写す。</summary>
    /// <remarks>
    /// 既定ローディングでは中身の供給が無く、メッセージと置き場所だけを渡す。
    /// カスタム View では中身の生成を供給元として渡し、器が提示先を確保した後に呼ばれる。
    /// 供給元は互換面 (ObjC) から呼ばれるため、失敗を例外のまま境界へ返さず
    /// <see langword="null"/> として返す (<see cref="BridgeContentSupply"/>)。
    /// 中身なしとして返した開始は成立せず、その失敗が完了の通知として届く。
    /// 元の失敗は預かり口に残り、その通知を受けたときに呼び出し元へそのまま返る。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。</param>
    /// <returns>互換面へ渡す中身の指定。</returns>
    private static MauiLoadingContent ToBridgeContent(
        LoadingPresentationRequest request,
        BridgeContentFailure contentFailure)
    {
        MauiDialogPlacement? placement = request.ShowPlacement is null
            ? null
            : PlatformDialogContent.ToBridgePlacement(request.ShowPlacement);

        if (request.IsBuiltin)
        {
            return new MauiLoadingContent(request.Message, placement);
        }

        ILoadingProgressReceiver? receiver = request.ProgressReceiver;
        return new MauiLoadingContent(
            () => BridgeContentSupply.CreateOrFail(
                () => PlatformDialogContent.Create(
                    request.CreateContent(),
                    // 供給元が呼ばれるのは器が提示先を確保した後なので、この時点では文脈が取れる
                    PlatformDialogContent.ResolveMauiContext()
                        ?? throw new DialogException.PresentationHostUnavailable()),
                contentFailure),
            placement,
            receiver is null ? null : progress => receiver.OnProgress(progress));
    }

    /// <summary>既定ローディングの見た目を、互換面が受け取る形へ写す。</summary>
    /// <remarks>見えの実装は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="style">設定されたスタイル。</param>
    /// <returns>互換面へ渡すスタイル。</returns>
    private static MauiLoadingStyle ToBridgeStyle(LoadingStyle style)
    {
        Func<string?, double?, string> progressFormat = style.ProgressFormat;
        return new MauiLoadingStyle
        {
            IndicatorColorArgb = style.IndicatorColorArgb,
            MessageFontSize = style.MessageFontSize,
            MessageColorArgb = style.MessageColorArgb,
            DefaultMessage = style.DefaultMessage,
            ProgressFormat = (message, progress) => progressFormat(message, progress?.DoubleValue),
        };
    }
}
