namespace KsDialogs;

/// <summary>
/// ダイアログの結果。完了 (結果値つき) とキャンセル (結果値なし) を型で区別する (core/ADR-0003)。
/// </summary>
/// <typeparam name="TResult">結果値の型。</typeparam>
public abstract record DialogResult<TResult>
{
    // 派生を Completed / Cancelled の 2 つに閉じるため、基底のコンストラクタは入れ子の型からしか呼べない
    private DialogResult()
    {
    }

    /// <summary>ダイアログ側の完了操作で確定した結果。</summary>
    /// <param name="Value">完了操作が報告した結果値。</param>
    public sealed record Completed(TResult Value) : DialogResult<TResult>;

    /// <summary>キャンセル操作・外側タップ・戻るボタンで確定した結果。結果値は持たない。</summary>
    public sealed record Cancelled : DialogResult<TResult>;
}
