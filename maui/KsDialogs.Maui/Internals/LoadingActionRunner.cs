using System;
using System.Threading.Tasks;
#if IOS || ANDROID
using Microsoft.Maui.ApplicationModel;
#endif

namespace KsDialogs;

/// <summary>
/// スコープ形の処理を走らせ、進捗の報告と合流 1 件の終了を互換面へ渡す手順。
/// </summary>
/// <remarks>
/// 両 OS の委譲面が同じ順序の保証 (報告が完了通知を追い越されないこと・成否によらず終了を
/// ちょうど 1 回伝えること) を持つよう、手順そのものをここに 1 つだけ置く。
/// 互換面への受け渡し方 (関数か Java の面か) だけが OS ごとに異なるため、呼び出し側が
/// その差を吸収した委譲先を渡す。
/// <para>
/// 処理を始めるスレッドの振り分けもここで行う (core/ADR-0037)。互換面が C# の処理をどのスレッドで
/// 呼ぶかは OS ごとに異なるが、ここで振り分ければその差に左右されず両 OS で同じ保証になる。
/// </para>
/// </remarks>
internal static class LoadingActionRunner
{
    /// <summary>
    /// 指定されたスレッドで処理を始め、成否によらず合流 1 件の終了をちょうど 1 回伝える。
    /// </summary>
    /// <remarks>UI スレッドへは MAUI の UI スレッドへの移送 (本番の口) を通して渡す。</remarks>
    /// <param name="action">実行する処理。</param>
    /// <param name="actionThread">処理を始めるスレッド。</param>
    /// <param name="report">互換面へ進捗を渡す口。</param>
    /// <param name="completion">互換面へ処理の終了を伝える口。</param>
    /// <param name="onFailure">処理が失敗したときにその理由を預ける先。</param>
    /// <returns>処理の終了と完了通知の完了。</returns>
    public static Task RunAsync(
        Func<IProgress<double>, Task> action,
        LoadingActionThread actionThread,
        Action<double> report,
        Action completion,
        Action<Exception> onFailure) =>
        RunAsync(action, actionThread, InvokeOnUiThreadAsync, report, completion, onFailure);

    /// <summary>
    /// 指定されたスレッドで処理を始め、成否によらず合流 1 件の終了をちょうど 1 回伝える。
    /// </summary>
    /// <remarks>
    /// UI スレッドで呼ぶ口を差し替えられる形。UI スレッドを持たない素の .NET のテストでは、
    /// 専用スレッドで動く偽物に差し替えて振り分けを確かめる。
    /// <para>
    /// 失敗を握り潰さずに呼び出し元へ返すため、ここでは受け取るだけにして終了を伝える。
    /// 伝え忘れると表示が閉じ残るので、失敗経路でも必ず完了通知を呼ぶ。
    /// 振り分けは処理を始める場所だけを変え、報告の同期転送 (<see cref="DirectProgress"/>) と
    /// 処理を待ち終えた後の完了通知はどちらの経路でも同じにする。スレッドプール側でも報告を
    /// <see cref="Progress{T}"/> へ戻さないこと (完了通知が報告を追い越す)。
    /// </para>
    /// </remarks>
    /// <param name="action">実行する処理。</param>
    /// <param name="actionThread">処理を始めるスレッド。</param>
    /// <param name="invokeOnUiThread">渡された処理を UI スレッドで呼び、その完了を返す口。</param>
    /// <param name="report">互換面へ進捗を渡す口。</param>
    /// <param name="completion">互換面へ処理の終了を伝える口。</param>
    /// <param name="onFailure">処理が失敗したときにその理由を預ける先。</param>
    /// <returns>処理の終了と完了通知の完了。</returns>
    public static async Task RunAsync(
        Func<IProgress<double>, Task> action,
        LoadingActionThread actionThread,
        Func<Func<Task>, Task> invokeOnUiThread,
        Action<double> report,
        Action completion,
        Action<Exception> onFailure)
    {
        try
        {
            DirectProgress progress = new(report);
            Task running = actionThread switch
            {
                LoadingActionThread.Main => invokeOnUiThread(() => action(progress)),
                LoadingActionThread.Background => Task.Run(() => action(progress)),
                // 範囲外の値も処理の失敗として預け、終了を伝える経路に乗せる
                _ => throw new ArgumentOutOfRangeException(nameof(actionThread), actionThread, null),
            };
            await running.ConfigureAwait(false);
        }
        catch (Exception failure)
        {
            onFailure(failure);
        }
        finally
        {
            completion();
        }
    }

    /// <summary>渡された処理を UI スレッドで呼ぶ本番の口。</summary>
    /// <remarks>
    /// 型指定の configure と同じ移送を使う。UI スレッドから呼ばれたときはその場で呼ぶ。
    /// platform 実装を持たない実行環境 (素の .NET) には UI スレッドが無いため、その場で呼ぶ。
    /// </remarks>
    /// <param name="work">UI スレッドで呼ぶ処理。</param>
    /// <returns>処理の完了。</returns>
    private static Task InvokeOnUiThreadAsync(Func<Task> work) =>
#if IOS || ANDROID
        MainThread.InvokeOnMainThreadAsync(work);
#else
        work();
#endif

    /// <summary>報告をその場で転送する進捗の報告口。</summary>
    /// <remarks>
    /// <see cref="Progress{T}"/> は捕捉した同期文脈または ThreadPool へ報告を投げ直すため、
    /// 報告した直後に処理が終わると完了の通知が報告を追い越し、Native 側の器が終了後の報告として
    /// 最後の値を捨ててしまう。報告の受け口は任意のスレッドから呼べて自分で UI スレッドへ移すので、
    /// ここでは投げ直さずに呼ばれたスレッドのまま同期で渡し、報告と終了の順序をそのまま保つ。
    /// </remarks>
    /// <param name="report">報告の転送先。</param>
    private sealed class DirectProgress(Action<double> report) : IProgress<double>
    {
        /// <inheritdoc/>
        public void Report(double value) => report(value);
    }
}
