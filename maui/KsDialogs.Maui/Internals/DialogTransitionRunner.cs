using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// MAUI 側の演出フックを、ブリッジの完了コールバック型の実行口として動かす面。
/// </summary>
/// <remarks>
/// 器は演出の完了を待って次へ進むため、フックの <see cref="Task"/> がどう終わっても
/// 完了通知をちょうど 1 回返す必要がある。失敗 (例外・キャンセル) は演出の失敗として吸収し、
/// ログに残したうえで完了として扱う (core/ADR-0017)。
/// <para>
/// フックの開始と完了通知はどちらも UI スレッドで行う。器はどちらの側からも同じスレッドで
/// 続きを進められる。
/// </para>
/// </remarks>
/// <param name="contentView">フックへ渡す中身の MAUI View。</param>
/// <param name="hook">利用者が添付した演出。</param>
/// <param name="startOnUiThread">渡された処理を UI スレッドで実行する操作。</param>
internal sealed class DialogTransitionRunner(
    VisualElement contentView,
    Func<VisualElement, Task> hook,
    Action<Action> startOnUiThread)
{
    /// <summary>添付された演出があれば、それを動かす実行口を作る。</summary>
    /// <param name="hook">利用者が添付した演出。未添付なら <see langword="null"/>。</param>
    /// <param name="contentView">フックへ渡す中身の MAUI View。</param>
    /// <param name="startOnUiThread">渡された処理を UI スレッドで実行する操作。</param>
    /// <returns>実行口。未添付なら <see langword="null"/> (器の既定が使われる)。</returns>
    public static DialogTransitionRunner? Create(
        Func<VisualElement, Task>? hook,
        VisualElement contentView,
        Action<Action> startOnUiThread) =>
        hook is null ? null : new DialogTransitionRunner(contentView, hook, startOnUiThread);

    /// <summary>演出を開始し、終わったら完了をちょうど 1 回返す。</summary>
    /// <param name="completion">演出が終わったときに呼ぶ完了通知。</param>
    public void Run(Action completion)
    {
        DialogSingleCompletion once = new(completion);
        startOnUiThread(() => Start(once));
    }

    /// <summary>UI スレッドでフックを呼び、その終わりを完了通知へつなぐ。</summary>
    /// <param name="once">1 回だけ通す完了通知。</param>
    private void Start(DialogSingleCompletion once)
    {
        Task running;
        try
        {
            running = hook(contentView) ?? Task.CompletedTask;
        }
        catch (Exception error)
        {
            // フックがその場で投げた失敗も、Task が fault した場合と同じく演出の失敗として扱う
            Report(error);
            once.Complete();
            return;
        }

        running.ContinueWith(
            finished =>
            {
                if (!finished.IsCompletedSuccessfully)
                {
                    Report(finished.Exception?.GetBaseException());
                }

                // フックが別スレッドで終わることもあるため、完了通知は UI スレッドへ戻して返す
                startOnUiThread(once.Complete);
            },
            CancellationToken.None,
            TaskContinuationOptions.ExecuteSynchronously,
            TaskScheduler.Default);
    }

    /// <summary>演出の失敗を開発時の手掛かりとして残す。</summary>
    /// <param name="error">フックが投げた失敗。キャンセルなど理由が取れない場合は <see langword="null"/>。</param>
    private static void Report(Exception? error) =>
        Trace.TraceWarning(
            "The Dialog transition failed. The Dialog result is not affected: {0}",
            error?.Message ?? "cancelled");
}

/// <summary>
/// 完了通知が何度届いても 1 回だけ通す入れ物。
/// </summary>
/// <remarks>完了が 2 回以上届くと器の進行が二重に走るため、この面で 1 回に切り詰める。</remarks>
/// <param name="completion">1 回だけ通す先の完了通知。</param>
internal sealed class DialogSingleCompletion(Action completion)
{
    private int _isCompleted;

    /// <summary>完了を通す。2 回目以降は何も起こさない。</summary>
    public void Complete()
    {
        if (Interlocked.Exchange(ref _isCompleted, 1) == 0)
        {
            completion();
        }
    }
}
