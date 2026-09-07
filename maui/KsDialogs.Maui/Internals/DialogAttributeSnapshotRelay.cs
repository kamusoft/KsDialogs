using System;

namespace KsDialogs;

/// <summary>
/// 初回のネイティブレイアウトパスの節目で、MAUI 側の添付を Native の添付面へ渡して固定する段取り。
/// </summary>
/// <remarks>
/// 契約が定める採用時点は初回のネイティブレイアウトパス完了時点であり、そこまでに届いた添付変更は
/// 採用される (core/ADR-0015)。器はレイアウトの計算で Native の添付面を読むため、
/// <see cref="TransferBeforeLayout"/> で計算前の値を渡し、そのパスが終わった時点で
/// <see cref="FreezeAndTransfer"/> により固定した値を渡し直す。
/// <para>
/// 渡し直しが要るのは、パスの中で走る MAUI の配置 (Arrange) の最中にも添付が変わり得るからである。
/// 固定するだけで渡し直さないと、その変更は MAUI 側の実効値にだけ残り、器の添付面へは届かない。
/// </para>
/// </remarks>
/// <param name="content">提示する中身と、その提示に効くメタ属性。</param>
/// <param name="transfer">実効値を Native の添付面へ写す操作。</param>
internal sealed class DialogAttributeSnapshotRelay(
    DialogPresentationContent content,
    Action<DialogAttributes> transfer)
{
    /// <summary>レイアウトの計算に入る前に、その時点の実効値を器が読む添付面へ渡す。</summary>
    public void TransferBeforeLayout()
    {
        if (content.AreAttributesFrozen)
        {
            return;
        }

        transfer(content.Attributes);
    }

    /// <summary>
    /// レイアウトパスの完了時点で実効値を固定し、固定した値を器が読む添付面へ渡し直す。
    /// </summary>
    /// <remarks>2 回目以降の呼び出しは何もしない (採用時点は 1 度きり)。</remarks>
    public void FreezeAndTransfer()
    {
        if (content.AreAttributesFrozen)
        {
            return;
        }

        content.FreezeAttributes();
        transfer(content.Attributes);
    }
}
