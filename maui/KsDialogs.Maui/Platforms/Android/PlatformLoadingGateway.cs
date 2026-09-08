using System;
using System.Threading.Tasks;
using KsDialogs.Bridge;
using Microsoft.Maui;

namespace KsDialogs;

/// <summary>
/// Android の互換面 (Loading) へ委譲する面。
/// </summary>
/// <remarks>
/// 合流カウント・表示世代・最新のメッセージと進捗は Native ライブラリの coordinator が持ち、
/// この面は MAUI View の platform view 化と値の写しだけを行う (core/ADR-0024)。
/// </remarks>
internal sealed class PlatformLoadingGateway : ILoadingGateway
{
    /// <inheritdoc/>
    public void ApplyStyle(LoadingStyle style) =>
        MauiLoadingBridge.Shared!.ApplyStyle(ToBridgeStyle(style));

    /// <inheritdoc/>
    public void ApplyOptions(DialogOptions options) =>
        MauiLoadingBridge.Shared!.ApplyOptions(PlatformDialogContent.ToBridgeOptions(options));

    /// <inheritdoc/>
    public Task ShowAsync(LoadingPresentationRequest request)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 中身を作れなかった失敗はこの表示 1 回分の預かり口に残り、完了の通知で呼び出し元へ返る
        BridgeContentFailure contentFailure = new();

        MauiLoadingBridge.Shared!.Show(
            ToBridgeContent(request, contentFailure),
            new CompletionListener(completion, contentFailure));
        return completion.Task;
    }

    /// <inheritdoc/>
    public async Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 処理の失敗は互換面へ渡さず、こちらで抱えたまま合流1件の終了だけを伝える。
        // 呼び出し元へは撤去の完了を待ってからそのまま投げ直す
        LoadingActionFailure failure = new();
        // 中身を作れなかった失敗はこの表示 1 回分の預かり口に残り、完了の通知で呼び出し元へ返る
        BridgeContentFailure contentFailure = new();

        MauiLoadingBridge.Shared!.Start(
            ToBridgeContent(request, contentFailure),
            new ActionRunner(action, failure),
            new CompletionListener(completion, contentFailure));

        await completion.Task.ConfigureAwait(false);
        if (failure.Value is not null)
        {
            throw failure.Value;
        }
    }

    /// <inheritdoc/>
    public Task HideAsync()
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 閉じる操作には中身の供給が無いため、預かり口も持たない
        MauiLoadingBridge.Shared!.Hide(new CompletionListener(completion, contentFailure: null));
        return completion.Task;
    }

    /// <inheritdoc/>
    public void SetMessage(string? message) => MauiLoadingBridge.Shared!.SetMessage(message);

    /// <summary>表示の要求を、互換面が受け取る中身の指定へ写す。</summary>
    /// <remarks>
    /// 既定ローディングでは中身の供給が無く、メッセージと置き場所だけを渡す。
    /// カスタム View では中身の生成を供給元として渡し、器が提示先を確保した後に呼ばれる。
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

        return request.IsBuiltin
            ? new MauiLoadingContent(request.Message, placement)
            : new MauiLoadingContent(
                new ContentProvider(request, contentFailure),
                placement,
                request.ProgressReceiver is null ? null : new ProgressReceiver(request.ProgressReceiver));
    }

    /// <summary>既定ローディングの見た目を、互換面が受け取る形へ写す。</summary>
    /// <remarks>見えの実装は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="style">設定されたスタイル。</param>
    /// <returns>互換面へ渡すスタイル。</returns>
    private static MauiLoadingStyle ToBridgeStyle(LoadingStyle style) => new()
    {
        IndicatorColorArgb = style.IndicatorColorArgb,
        MessageFontSize = style.MessageFontSize,
        MessageColorArgb = style.MessageColorArgb,
        DefaultMessage = style.DefaultMessage,
        ProgressFormat = new ProgressFormat(style.ProgressFormat),
    };

    /// <summary>スコープ形の処理が失敗したときの理由を、撤去の完了まで抱えておく入れ物。</summary>
    private sealed class LoadingActionFailure
    {
        /// <summary>抱えている失敗。成功なら <see langword="null"/>。</summary>
        public Exception? Value { get; set; }
    }

    /// <summary>カスタム Loading の中身を、器が提示先を確保した後に供給する。</summary>
    /// <remarks>
    /// 供給元は互換面 (Java) から呼ばれる。失敗を例外のまま境界へ返すと未処理の障害になるため、
    /// 中身なしとして返し、表示そのものの失敗として完了の通知で受け取る。
    /// 元の失敗は預かり口に残り、その通知を受けたときに呼び出し元へそのまま返る (core/ADR-0033・core/ADR-0036)。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。</param>
    private sealed class ContentProvider(
        LoadingPresentationRequest request,
        BridgeContentFailure contentFailure)
        : Java.Lang.Object, IMauiLoadingContentProvider
    {
        public MauiDialogContent? CreateContent() =>
            BridgeContentSupply.CreateOrFail(
                () => PlatformDialogContent.Create(
                    request.CreateContent(),
                    // 供給元が呼ばれるのは器が提示先を確保した後なので、この時点では文脈が取れる
                    PlatformDialogContent.ResolveMauiContext()
                        ?? throw new DialogException.PresentationHostUnavailable()),
                contentFailure);
    }

    /// <summary>互換面から届いた進捗を、MAUI 側の ViewModel の受け口へ渡す。</summary>
    /// <param name="receiver">進捗の転送先。</param>
    private sealed class ProgressReceiver(ILoadingProgressReceiver receiver)
        : Java.Lang.Object, IMauiLoadingProgressReceiver
    {
        public void OnProgress(double progress) => receiver.OnProgress(progress);
    }

    /// <summary>MAUI 側の組み立て方を、互換面が呼ぶ口として差し出す。</summary>
    /// <param name="format">表示テキストを組み立てる関数。</param>
    private sealed class ProgressFormat(Func<string?, double?, string> format)
        : Java.Lang.Object, IMauiLoadingProgressFormat
    {
        public string Format(string? message, Java.Lang.Double? progress) =>
            format(message, progress?.DoubleValue());
    }

    /// <summary>MAUI 側の処理を、互換面が呼ぶ口として差し出す。</summary>
    /// <param name="action">実行する処理。</param>
    /// <param name="failure">処理が失敗したときにその理由を預ける先。</param>
    private sealed class ActionRunner(Func<IProgress<double>, Task> action, LoadingActionFailure failure)
        : Java.Lang.Object, IMauiLoadingAction
    {
        public void Run(IMauiLoadingProgressReport report, Java.Lang.IRunnable completion) =>
            // 完了通知は処理が終わるまで持ち越すため、その間 Java 側の実体を掴んだままにする
            _ = LoadingActionRunner.RunAsync(
                action,
                progress => report.Report(progress),
                completion.Run,
                thrown => failure.Value = thrown);
    }

    /// <summary>互換面から届いた結末を、待っている呼び出しへ渡す。</summary>
    /// <remarks>
    /// 中身を作れなかったときに互換面が返す理由は「中身が無かった」ことだけなので、
    /// 元の失敗を預かっていればそれをそのまま渡す (型・メッセージ・スタックが保たれる)。
    /// </remarks>
    /// <param name="completion">待っている呼び出しの完了源。</param>
    /// <param name="contentFailure">中身を作れなかった失敗の預かり口。供給が無い操作では null。</param>
    private sealed class CompletionListener(
        TaskCompletionSource completion,
        BridgeContentFailure? contentFailure)
        : Java.Lang.Object, IMauiLoadingCompletionListener
    {
        public void OnCompleted() => completion.TrySetResult();

        public void OnFailure(string? message) =>
            completion.TrySetException(contentFailure?.Cause ?? new InvalidOperationException(
                message ?? "Could not show the Loading."));
    }
}
