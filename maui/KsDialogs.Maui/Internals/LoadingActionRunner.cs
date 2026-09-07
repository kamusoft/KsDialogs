using System;
using System.Threading.Tasks;

namespace KsDialogs;

/// <summary>
/// スコープ形の処理を走らせ、進捗の報告と合流 1 件の終了を互換面へ渡す手順。
/// </summary>
/// <remarks>
/// 両 OS の委譲面が同じ順序の保証 (報告が完了通知を追い越されないこと・成否によらず終了を
/// ちょうど 1 回伝えること) を持つよう、手順そのものをここに 1 つだけ置く。
/// 互換面への受け渡し方 (関数か Java の面か) だけが OS ごとに異なるため、呼び出し側が
/// その差を吸収した委譲先を渡す。
/// </remarks>
internal static class LoadingActionRunner
{
    /// <summary>
    /// 処理を走らせ、成否によらず合流 1 件の終了をちょうど 1 回伝える。
    /// </summary>
    /// <remarks>
    /// 失敗を握り潰さずに呼び出し元へ返すため、ここでは受け取るだけにして終了を伝える。
    /// 伝え忘れると表示が閉じ残るので、失敗経路でも必ず完了通知を呼ぶ。
    /// </remarks>
    /// <param name="action">実行する処理。</param>
    /// <param name="report">互換面へ進捗を渡す口。</param>
    /// <param name="completion">互換面へ処理の終了を伝える口。</param>
    /// <param name="onFailure">処理が失敗したときにその理由を預ける先。</param>
    public static async Task RunAsync(
        Func<IProgress<double>, Task> action,
        Action<double> report,
        Action completion,
        Action<Exception> onFailure)
    {
        try
        {
            await action(new DirectProgress(report)).ConfigureAwait(false);
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
