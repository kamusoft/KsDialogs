using System;
using System.Threading.Tasks;

namespace KsDialogs;

/// <summary>
/// 1 回の提示について、結果の確定 (ラッチ) と呼び出し元への配送を分けて扱う面。
/// </summary>
/// <remarks>
/// 結果は中身のコードが報告した最初の 1 つで不可逆に確定するが、呼び出し元へ渡すのは
/// 退出の演出と覆いの消滅が終わり、器が撤去された後になる (core/ADR-0017)。
/// 撤去まで進んだことは互換面の閉鎖の通知で分かるため、その通知を配送の合図として扱う。
/// <para>
/// 確定と配送を同じ時点にすると、結果を受けて次のダイアログを出す流れで前のダイアログが
/// まだ見えている状態が生まれる。この面はその 2 つの時点を分けるためだけに存在する。
/// </para>
/// <para>
/// 提示に入れなかった場合だけは確定した結果を持たないため、失敗をそのまま配送する。
/// </para>
/// </remarks>
/// <param name="resultChannel">その show の結果チャネル。</param>
internal sealed class DialogPresentationCompletion(DialogResultChannel resultChannel)
{
    private readonly TaskCompletionSource<DialogOutcome> _delivery =
        new(TaskCreationOptions.RunContinuationsAsynchronously);

    /// <summary>結果が配送されるまで待つ。</summary>
    public Task<DialogOutcome> Delivered => _delivery.Task;

    /// <summary>結果を確定させる。確定済みなら何もしない。配送はここでは行わない。</summary>
    /// <param name="outcome">確定させる結果。</param>
    public void Settle(DialogOutcome outcome) => resultChannel.Settle(outcome);

    /// <summary>器が撤去されたので、確定済みの結果を配送する。2 回目以降は何も起こさない。</summary>
    /// <remarks>
    /// 器が消えるまでに報告が 1 つも無ければ、その提示は報告のないまま閉じたことになるため、
    /// キャンセルとして確定させてから配送する。
    /// </remarks>
    public void Deliver()
    {
        resultChannel.Settle(DialogOutcome.Cancelled.Instance);
        if (resultChannel.SettledOutcome is DialogOutcome outcome)
        {
            _delivery.TrySetResult(outcome);
        }
    }

    /// <summary>提示に入れなかったことを失敗として配送する。2 回目以降は何も起こさない。</summary>
    /// <param name="error">提示できなかった理由。</param>
    public void Fail(Exception error) => _delivery.TrySetException(error);
}
