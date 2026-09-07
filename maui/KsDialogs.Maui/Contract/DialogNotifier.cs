namespace KsDialogs;

/// <summary>
/// ダイアログ側から結果を報告する部品。
/// </summary>
/// <remarks>
/// 完了 (結果値つき) とキャンセルの 2 操作のみを持ち、結果値の型は ViewModel の宣言に固定される。
/// 報告はちょうど 1 回だけ有効で、確定後の報告は何も起こさない (core/ADR-0003)。
/// 任意のスレッドから呼び出してよい。
/// show 1 回につき新しいインスタンスが View factory へ渡されるため、
/// 同じ ViewModel を重ねて表示しても報告先が混ざることはない。
/// </remarks>
/// <typeparam name="TResult">報告できる結果値の型。</typeparam>
public sealed class DialogNotifier<TResult>
{
    private readonly DialogResultChannel _resultChannel;

    internal DialogNotifier(DialogResultChannel resultChannel) => _resultChannel = resultChannel;

    /// <summary>結果値つきで完了を報告する。</summary>
    /// <param name="value">報告する結果値。</param>
    public void Complete(TResult value) => _resultChannel.Settle(new DialogOutcome.Completed(value));

    /// <summary>キャンセルを報告する。</summary>
    public void Cancel() => _resultChannel.Settle(DialogOutcome.Cancelled.Instance);
}
