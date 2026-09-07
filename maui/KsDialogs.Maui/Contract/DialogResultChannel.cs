using System.Threading.Tasks;

namespace KsDialogs;

/// <summary>
/// 1 回の show に対応する結果チャネル。
/// </summary>
/// <remarks>
/// 任意のスレッドからの報告を受け付け、最初の報告だけを有効として下流へ流す (2 回目以降は捨てる)。
/// 待ち受け側の登録より先に報告が来ても、確定した結果はそのまま引き渡される。
/// </remarks>
internal sealed class DialogResultChannel
{
    private readonly TaskCompletionSource<DialogOutcome> _completionSource =
        new(TaskCreationOptions.RunContinuationsAsynchronously);

    /// <summary>結果が確定するまで待つ。確定済みなら即座に返る。</summary>
    public Task<DialogOutcome> Result => _completionSource.Task;

    /// <summary>結果が確定済みかどうか。</summary>
    public bool IsResultSettled => _completionSource.Task.IsCompleted;

    /// <summary>確定済みの結果。未確定なら <see langword="null"/>。</summary>
    public DialogOutcome? SettledOutcome =>
        _completionSource.Task.IsCompletedSuccessfully ? _completionSource.Task.Result : null;

    /// <summary>結果を確定させる。確定済みなら何もしない。</summary>
    /// <param name="outcome">確定させる結果。</param>
    public void Settle(DialogOutcome outcome) => _completionSource.TrySetResult(outcome);
}
