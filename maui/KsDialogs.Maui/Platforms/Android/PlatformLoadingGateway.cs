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
        MauiLoadingBridge.Shared!.Show(ToBridgeContent(request), new CompletionListener(completion));
        return completion.Task;
    }

    /// <inheritdoc/>
    public async Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        // 処理の失敗は互換面へ渡さず、こちらで抱えたまま合流1件の終了だけを伝える。
        // 呼び出し元へは撤去の完了を待ってからそのまま投げ直す
        LoadingActionFailure failure = new();

        MauiLoadingBridge.Shared!.Start(
            ToBridgeContent(request),
            new ActionRunner(action, failure),
            new CompletionListener(completion));

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
        MauiLoadingBridge.Shared!.Hide(new CompletionListener(completion));
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
    /// <returns>互換面へ渡す中身の指定。</returns>
    private static MauiLoadingContent ToBridgeContent(LoadingPresentationRequest request)
    {
        MauiDialogPlacement? placement = request.ShowPlacement is null
            ? null
            : PlatformDialogContent.ToBridgePlacement(request.ShowPlacement);

        return request.IsBuiltin
            ? new MauiLoadingContent(request.Message, placement)
            : new MauiLoadingContent(
                new ContentProvider(request),
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
    /// <param name="request">その表示の中身と供給値。</param>
    private sealed class ContentProvider(LoadingPresentationRequest request)
        : Java.Lang.Object, IMauiLoadingContentProvider
    {
        public MauiDialogContent CreateContent() =>
            PlatformDialogContent.Create(
                request.CreateContent(),
                // 供給元が呼ばれるのは器が提示先を確保した後なので、この時点では文脈が取れる
                PlatformDialogContent.ResolveMauiContext()
                    ?? throw new DialogException.PresentationHostUnavailable());
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
    /// <param name="completion">待っている呼び出しの完了源。</param>
    private sealed class CompletionListener(TaskCompletionSource completion)
        : Java.Lang.Object, IMauiLoadingCompletionListener
    {
        public void OnCompleted() => completion.TrySetResult();

        public void OnFailure(string? message) =>
            completion.TrySetException(new InvalidOperationException(
                message ?? "Could not show the Loading."));
    }
}
