using System;
using System.Threading.Tasks;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 結果の確定 (ラッチ) と呼び出し元への配送が別の時点であることの検証。
/// </summary>
/// <remarks>
/// 退出の演出と覆いの消滅を待って器を撤去するのは Native 実装で、MAUI 側の責務は
/// 「撤去済みを伝える閉鎖の通知が届くまで show を返さない」ところにある (core/ADR-0017)。
/// ここではその継ぎ目 — 確定だけでは配送されず、閉鎖の通知で初めて配送される — を固定する。
/// 実際の見え方 (中身が消えたコマで初めて結果が変わる) は実機での確認に委ねる。
/// </remarks>
[TestFixture]
public class DialogPresentationDeliveryTests
{
    /// <summary>結果を確定させただけでは配送されず、撤去の通知で初めて配送される。</summary>
    [Test]
    [Description("[PB-TR-10] show は dismissal フック完了と器の撤去より先に返らない")]
    public async Task PB_TR_10_TheOutcomeIsNotDeliveredBeforeTheContainerIsRemoved()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Settle(new DialogOutcome.Completed(true));
        bool deliveredOnSettle = completion.Delivered.IsCompleted;
        completion.Deliver();
        DialogOutcome delivered = await completion.Delivered;

        Assert.Multiple(() =>
        {
            Assert.That(resultChannel.IsResultSettled, Is.True, "報告した時点で結果は確定すること");
            Assert.That(deliveredOnSettle, Is.False, "確定しただけでは配送されないこと");
            Assert.That(delivered, Is.EqualTo(new DialogOutcome.Completed(true)), "撤去の後に確定済みの結果が配送されること");
        });
    }

    /// <summary>演出を添付していなくても、配送は撤去の通知を待つ。</summary>
    [Test]
    [Description("[PB-TR-13] 添付なしの既定トランジションでも配送は撤去後")]
    public async Task PB_TR_13_TheDeliveryWaitsForRemovalWithoutAnAttachedTransition()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Settle(new DialogOutcome.Completed("値"));
        bool deliveredOnSettle = completion.Delivered.IsCompleted;
        completion.Deliver();
        DialogOutcome delivered = await completion.Delivered;

        Assert.Multiple(() =>
        {
            Assert.That(deliveredOnSettle, Is.False, "添付の有無によらず確定だけでは配送されないこと");
            Assert.That(delivered, Is.EqualTo(new DialogOutcome.Completed("値")));
        });
    }

    /// <summary>退出の途中で二重に報告しても、配送されるのは最初に確定した値。</summary>
    [Test]
    [Description("[PB-TR-11] 退出中の二重報告はラッチ済みの値を配送する")]
    public async Task PB_TR_11_ADoubleReportDuringDismissalDeliversTheLatchedOutcome()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Settle(new DialogOutcome.Completed("A"));
        // 退出が終わる前に届いた 2 度目の報告
        completion.Settle(new DialogOutcome.Completed("B"));
        completion.Deliver();
        DialogOutcome delivered = await completion.Delivered;

        Assert.That(delivered, Is.EqualTo(new DialogOutcome.Completed("A")), "最初に確定した値が配送されること");
    }

    /// <summary>撤去の通知が何度届いても、配送は 1 回だけで値も変わらない。</summary>
    [Test]
    [Description("[PB-TR-10] 撤去の通知が重なっても配送は 1 回")]
    public async Task PB_TR_10_RepeatedRemovalNoticesDeliverOnlyOnce()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Settle(new DialogOutcome.Completed(true));
        completion.Deliver();
        completion.Deliver();
        completion.Deliver();
        DialogOutcome delivered = await completion.Delivered;

        Assert.That(delivered, Is.EqualTo(new DialogOutcome.Completed(true)));
    }

    /// <summary>報告のないまま器が消えたときは、キャンセルとして確定して配送する。</summary>
    [Test]
    [Description("[PB-TR-13] 報告のないまま撤去されたらキャンセルが配送される")]
    public async Task PB_TR_13_ARemovalWithoutAnyReportDeliversCancelled()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Deliver();
        DialogOutcome delivered = await completion.Delivered;

        Assert.That(delivered, Is.EqualTo(DialogOutcome.Cancelled.Instance));
    }

    /// <summary>提示に入れなかった場合は、確定した結果を持たないまま失敗が配送される。</summary>
    [Test]
    [Description("提示先不在は結果ではなく失敗として配送される")]
    public void AFailedPresentationDeliversTheFailure()
    {
        DialogResultChannel resultChannel = new();
        DialogPresentationCompletion completion = new(resultChannel);

        completion.Fail(new DialogException.PresentationHostUnavailable());

        Assert.Multiple(() =>
        {
            Assert.That(
                async () => await completion.Delivered,
                Throws.TypeOf<DialogException.PresentationHostUnavailable>());
            Assert.That(resultChannel.IsResultSettled, Is.False, "失敗では結果を確定させないこと");
        });
    }
}
