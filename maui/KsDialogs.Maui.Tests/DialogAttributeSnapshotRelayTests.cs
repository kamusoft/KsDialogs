using System.Collections.Generic;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 初回のネイティブレイアウトパスで、MAUI 側の添付が Native の添付面へ渡り切ることの検証。
/// </summary>
/// <remarks>
/// 採用時点そのもの (パスの完了をどう捉えるか) は platform ごとの通知に依るが、
/// 「計算前に暫定の値を渡す → パス完了で固定して渡し直す」という段取りは共通で、
/// ここではその段取りだけを見る。パスの最中に届いた変更が渡し直されなければ、
/// MAUI 側の実効値と器が読む値が食い違う。
/// </remarks>
[TestFixture]
public class DialogAttributeSnapshotRelayTests
{
    /// <summary>レイアウトの計算に入る前に、その時点の実効値が添付面へ渡る。</summary>
    [Test]
    [Description("計算前に暫定の実効値が渡る")]
    public void TheEffectiveAttributesAreTransferredBeforeTheLayoutIsCalculated()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
        DialogPresentationContent content = new(contentView, null);
        List<DialogAttributes> transferred = [];
        DialogAttributeSnapshotRelay relay = new(content, transferred.Add);

        relay.TransferBeforeLayout();

        Assert.Multiple(() =>
        {
            Assert.That(transferred, Has.Count.EqualTo(1));
            Assert.That(transferred[0].Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.Start));
            Assert.That(content.AreAttributesFrozen, Is.False, "計算前の受け渡しでは固定しない");
        });
    }

    /// <summary>パスの最中に届いた添付変更は、固定されたうえで添付面へ渡し直される。</summary>
    [Test]
    [Description("パスの最中に届いた変更が固定されて渡し直される")]
    public void AnAttributeChangeDuringTheLayoutPassIsFrozenAndTransferredAgain()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
        DialogPresentationContent content = new(contentView, null);
        List<DialogAttributes> transferred = [];
        DialogAttributeSnapshotRelay relay = new(content, transferred.Add);

        relay.TransferBeforeLayout();
        // MAUI の配置の最中に添付が変わる状況 (バインディングの反映など)
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.End);
        Dialog.SetOffsetX(contentView, 24d);
        Dialog.SetIsCanceledOnTouchOutside(contentView, false);
        relay.FreezeAndTransfer();

        Assert.Multiple(() =>
        {
            Assert.That(content.AreAttributesFrozen, Is.True, "パスの完了が採用時点");
            Assert.That(transferred, Has.Count.EqualTo(2), "固定した値を渡し直すこと");
            Assert.That(transferred[1].Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(transferred[1].Placement.OffsetX, Is.EqualTo(24d));
            Assert.That(transferred[1].Options.IsCanceledOnTouchOutside, Is.False);
            Assert.That(
                transferred[1],
                Is.EqualTo(content.Attributes),
                "渡し直した値と MAUI 側の実効値が一致すること");
        });
    }

    /// <summary>採用時点を過ぎたあとの添付変更は、固定値も添付面も動かさない。</summary>
    [Test]
    [Description("採用時点を過ぎた変更は渡らない")]
    public void AnAttributeChangeAfterTheAdoptionPointIsNotTransferred()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
        DialogPresentationContent content = new(contentView, null);
        List<DialogAttributes> transferred = [];
        DialogAttributeSnapshotRelay relay = new(content, transferred.Add);

        relay.TransferBeforeLayout();
        relay.FreezeAndTransfer();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.End);
        // 2 パス目以降の節目
        relay.TransferBeforeLayout();
        relay.FreezeAndTransfer();

        Assert.Multiple(() =>
        {
            Assert.That(transferred, Has.Count.EqualTo(2), "採用時点より後は渡さない");
            Assert.That(transferred[1].Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.Start));
            Assert.That(content.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.Start));
        });
    }
}
