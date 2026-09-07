using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 添付された出入りの演出が、ブリッジの完了コールバック型の実行口へ渡り切ることの検証。
/// </summary>
/// <remarks>
/// 演出そのものの見え方を駆動するのは Native 実装で、MAUI 側の責務は
/// 「添付が実効値として運ばれる」「フックの <see cref="Task"/> の終わりが完了通知へ変わる」
/// 「完了通知が多重に届かない」の 3 点にある (core/ADR-0009 の層別)。
/// 実際の動きは実機での確認に委ねる。
/// </remarks>
[TestFixture]
public class DialogTransitionPassthroughTests
{
    /// <summary>添付した演出が実効値として運ばれ、フックが中身の View を引数に UI スレッドで始まる。</summary>
    [Test]
    [Description("[PB-MA-01] 添付プロパティでの添付がネイティブへパススルーされる")]
    public void PB_MA_01_TheAttachedTransitionIsPassedThroughToTheBridge()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        RecordingUiThread uiThread = new();
        List<VisualElement> hookArguments = [];
        bool startedOnUiThread = false;
        DialogTransition transition = new(presentation: view =>
        {
            hookArguments.Add(view);
            startedOnUiThread = uiThread.IsRunning;
            return Task.CompletedTask;
        });
        Dialog.SetTransition(contentView, transition);
        DialogPresentationContent content = new(contentView, null);
        List<DialogAttributes> transferred = [];
        DialogAttributeSnapshotRelay relay = new(content, transferred.Add);

        relay.TransferBeforeLayout();
        relay.FreezeAndTransfer();
        DialogTransition? adopted = transferred[^1].Transition;
        DialogTransitionRunner? runner = DialogTransitionRunner.Create(
            adopted?.Presentation,
            contentView,
            uiThread.Run);
        runner?.Run(() => { });

        Assert.Multiple(() =>
        {
            Assert.That(adopted, Is.SameAs(transition), "添付した組がそのまま実効値になること");
            Assert.That(runner, Is.Not.Null, "添付があれば実行口が作られること");
            Assert.That(hookArguments, Is.EqualTo(new[] { contentView }), "フックに中身の MAUI View が渡ること");
            Assert.That(startedOnUiThread, Is.True, "フックは UI スレッドで開始されること");
        });
    }

    /// <summary>演出が未添付なら実行口を作らず、器の既定に委ねる。</summary>
    [Test]
    [Description("[PB-MA-01] 未添付なら実行口を作らない")]
    public void PB_MA_01_NoRunnerIsCreatedWithoutAnAttachedTransition()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        DialogPresentationContent content = new(contentView, null);

        DialogTransitionRunner? runner = DialogTransitionRunner.Create(
            content.Transition?.Presentation,
            contentView,
            new RecordingUiThread().Run);

        Assert.Multiple(() =>
        {
            Assert.That(content.Transition, Is.Null, "未添付は実効値も未指定であること");
            Assert.That(runner, Is.Null, "実行口を作らないこと (器の既定が使われる)");
        });
    }

    /// <summary>完了通知はフックの Task が終わるまで返らない。</summary>
    [Test]
    [Description("[PB-MA-02] 退出フックの完了待ちが MAUI 経由でも成立する")]
    public void PB_MA_02_TheCompletionIsWithheldUntilTheHookTaskFinishes()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        TaskCompletionSource gate = new();
        DialogTransition transition = new(dismissal: _ => gate.Task);
        int completionCount = 0;
        DialogTransitionRunner runner = DialogTransitionRunner.Create(
            transition.Dismissal,
            contentView,
            new RecordingUiThread().Run)!;

        runner.Run(() => completionCount++);
        int countWhileRunning = completionCount;
        gate.SetResult();

        Assert.Multiple(() =>
        {
            Assert.That(countWhileRunning, Is.Zero, "フックが終わるまで完了は返らないこと");
            Assert.That(completionCount, Is.EqualTo(1), "フックが終わったら完了が返ること");
        });
    }

    /// <summary>フックが失敗しても完了は返り、失敗は呼び出し側へ伝播しない。</summary>
    [Test]
    [Description("[PB-MA-03] フックの Task が fault しても結果は配送される")]
    public void PB_MA_03_AFaultedHookStillReportsCompletion()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        int faultedCompletions = 0;
        int throwingCompletions = 0;
        DialogTransitionRunner faulted = DialogTransitionRunner.Create(
            _ => Task.FromException(new InvalidOperationException("演出の失敗")),
            contentView,
            new RecordingUiThread().Run)!;
        DialogTransitionRunner throwing = DialogTransitionRunner.Create(
            _ => throw new InvalidOperationException("演出の失敗"),
            contentView,
            new RecordingUiThread().Run)!;

        Assert.Multiple(() =>
        {
            Assert.DoesNotThrow(
                () => faulted.Run(() => faultedCompletions++),
                "フックの失敗を呼び出し側へ伝播させないこと");
            Assert.DoesNotThrow(
                () => throwing.Run(() => throwingCompletions++),
                "フックがその場で投げた失敗も伝播させないこと");
            Assert.That(faultedCompletions, Is.EqualTo(1), "fault でも完了が返ること");
            Assert.That(throwingCompletions, Is.EqualTo(1), "その場で投げても完了が返ること");
        });
    }

    /// <summary>キャンセルで終わったフックも失敗と同じく完了として扱う。</summary>
    [Test]
    [Description("[PB-MA-03] フックの Task がキャンセルされても完了は返る")]
    public void PB_MA_03_ACancelledHookStillReportsCompletion()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        int completionCount = 0;
        DialogTransitionRunner runner = DialogTransitionRunner.Create(
            _ => Task.FromCanceled(new CancellationToken(canceled: true)),
            contentView,
            new RecordingUiThread().Run)!;

        Assert.DoesNotThrow(() => runner.Run(() => completionCount++));

        Assert.That(completionCount, Is.EqualTo(1), "キャンセルでも完了が返ること");
    }

    /// <summary>完了通知は、何度届いてもネイティブの完了口へは 1 回しか通らない。</summary>
    [Test]
    [Description("[PB-MA-04] フックの完了通知は多重に届かない")]
    public void PB_MA_04_TheCompletionIsDeliveredExactlyOnce()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        TaskCompletionSource gate = new();
        int runnerCompletions = 0;
        int guardedCompletions = 0;
        DialogTransitionRunner runner = DialogTransitionRunner.Create(
            _ => gate.Task,
            contentView,
            new RecordingUiThread().Run)!;
        DialogSingleCompletion once = new(() => guardedCompletions++);

        runner.Run(() => runnerCompletions++);
        gate.SetResult();
        // 既に終わった Task へ重ねて完了を促しても増えないこと
        gate.TrySetResult();
        once.Complete();
        once.Complete();
        once.Complete();

        Assert.Multiple(() =>
        {
            Assert.That(runnerCompletions, Is.EqualTo(1), "実行口からの完了は 1 回だけ届くこと");
            Assert.That(guardedCompletions, Is.EqualTo(1), "完了口が多重に叩かれても 1 回に切り詰めること");
        });
    }

    /// <summary>プリセットの添付もカスタムフックと同じ実行口を通り、完了まで到達する。</summary>
    /// <remarks>
    /// MAUI のアニメーション API は View が実際のハンドラに載っていないと動かせないため、
    /// ここで見るのは「プリセットもカスタムフックと同じ実行口を通り、完了がちょうど 1 回返る」ところまでで、
    /// 動きそのものは実機での確認に委ねる。
    /// </remarks>
    [Test]
    [Description("[PB-MA-05] プリセット添付がネイティブ経路で機能する")]
    public void PB_MA_05_APresetTransitionRunsThroughTheSameBridgeRoute()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        DialogTransition preset = DialogTransition.Slide(DialogTransitionEdge.Bottom);
        Dialog.SetTransition(contentView, preset);
        DialogPresentationContent content = new(contentView, null);
        content.FreezeAttributes();
        RecordingUiThread uiThread = new();
        int presentationCompletions = 0;
        int dismissalCompletions = 0;

        DialogTransitionRunner presentation = DialogTransitionRunner.Create(
            content.Transition?.Presentation,
            contentView,
            uiThread.Run)!;
        DialogTransitionRunner dismissal = DialogTransitionRunner.Create(
            content.Transition?.Dismissal,
            contentView,
            uiThread.Run)!;
        presentation.Run(() => presentationCompletions++);
        dismissal.Run(() => dismissalCompletions++);

        Assert.Multiple(() =>
        {
            Assert.That(content.Transition, Is.SameAs(preset), "プリセットも添付の実効値として運ばれること");
            Assert.That(preset.OverlayDuration, Is.EqualTo(TimeSpan.FromMilliseconds(250d)));
            Assert.That(presentationCompletions, Is.EqualTo(1), "出現がプリセット経路で完了すること");
            Assert.That(dismissalCompletions, Is.EqualTo(1), "閉鎖がプリセット経路で完了すること");
        });
    }

    /// <summary>中身は MAUI の要素ツリーに載った状態で運ばれる。</summary>
    /// <remarks>
    /// MAUI の iOS 実装は親を持たない View に移動・拡大縮小を反映しないため、親がないと
    /// プリセットも利用者のフックも透明度しか動かない (実機で確認した症状)。親は弱参照で持たれるので、
    /// 提示 1 回分の入れ物が強く持ち続けていることまで見る。
    /// </remarks>
    [Test]
    [Description("[PB-MA-05] 中身は親を持った状態で運ばれる (移動と拡大縮小が platform view へ届く条件)")]
    public void PB_MA_05_TheContentIsCarriedInsideTheElementTree()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();

        DialogPresentationContent content = new(contentView, null);
        GC.Collect();
        GC.WaitForPendingFinalizers();
        GC.Collect();

        Assert.Multiple(() =>
        {
            Assert.That(content.ContentView, Is.SameAs(contentView), "運ばれるのは渡した中身そのものであること");
            Assert.That(
                contentView.Parent,
                Is.Not.Null,
                "中身が MAUI の要素ツリーに載っていること (回収されて親が消えていないこと)");
        });
    }

    /// <summary>4 種のプリセットはいずれも両フックと覆いの時間を備える。</summary>
    [Test]
    [Description("[PB-MA-05] プリセットは 4 種とも両フックと覆いの時間を備える")]
    public void PB_MA_05_EveryPresetSuppliesBothHooksAndAnOverlayDuration()
    {
        TimeSpan duration = TimeSpan.FromMilliseconds(400d);
        DialogTransition[] presets =
        [
            DialogTransition.Fade(duration),
            DialogTransition.Slide(DialogTransitionEdge.Start, duration),
            DialogTransition.Zoom(duration),
        ];

        Assert.Multiple(() =>
        {
            foreach (DialogTransition preset in presets)
            {
                Assert.That(preset.Presentation, Is.Not.Null);
                Assert.That(preset.Dismissal, Is.Not.Null);
                Assert.That(preset.OverlayDuration, Is.EqualTo(duration), "覆いの時間はプリセット自身の時間になること");
            }

            DialogTransition none = DialogTransition.None();
            Assert.That(none.Presentation, Is.Not.Null, "none も中身を動かさないフックを持つこと (器の既定に落ちない)");
            Assert.That(none.Dismissal, Is.Not.Null);
            Assert.That(
                none.OverlayDuration,
                Is.EqualTo(TimeSpan.FromMilliseconds(250d)),
                "none の覆いは既定の時間でフェードすること");
        });
    }

    /// <summary>プリセットが持つ覆いの時間が、ブリッジへ渡す値として運ばれる。</summary>
    /// <remarks>
    /// 覆いのフェードを駆動するのはネイティブ側の器なので、MAUI 側の責務は時間を値として渡すところまでになる。
    /// </remarks>
    [Test]
    [Description("[PB-MA-05] プリセットの覆いの時間がブリッジへ渡る")]
    public void PB_MA_05_ThePresetOverlayDurationIsCarriedToTheBridge()
    {
        TimeSpan duration = TimeSpan.FromMilliseconds(400d);

        Assert.Multiple(() =>
        {
            Assert.That(
                DialogTransition.ToBridgeOverlayMilliseconds(DialogTransition.Slide(DialogTransitionEdge.Bottom, duration).OverlayDuration),
                Is.EqualTo(400d),
                "プリセット自身の時間がミリ秒で渡ること");
            Assert.That(
                DialogTransition.ToBridgeOverlayMilliseconds(DialogTransition.None().OverlayDuration),
                Is.EqualTo(250d),
                "none も既定の時間を値として渡すこと");

            foreach (TimeSpan unusable in UnusableDurations())
            {
                Assert.That(
                    DialogTransition.ToBridgeOverlayMilliseconds(DialogTransition.Fade(unusable).OverlayDuration),
                    Is.EqualTo(0d),
                    $"{unusable} は器の既定へ落とさず、覆いも即完了する値で渡すこと");
            }
        });
    }

    /// <summary>覆いの時間を省いたカスタムの組は、未指定のままブリッジへ渡る。</summary>
    [Test]
    [Description("[PB-MA-01] 覆いの時間を省いた添付は未指定のまま渡る")]
    public void PB_MA_01_AnOmittedOverlayDurationIsCarriedAsUnset()
    {
        DialogTransition omitted = new(presentation: _ => Task.CompletedTask);
        DialogTransition specified = new(
            presentation: _ => Task.CompletedTask,
            overlayDuration: TimeSpan.FromMilliseconds(120d));

        Assert.Multiple(() =>
        {
            Assert.That(
                DialogTransition.ToBridgeOverlayMilliseconds(omitted.OverlayDuration),
                Is.Null,
                "省略は未指定のまま渡ること (器の既定が使われる)");
            Assert.That(
                DialogTransition.ToBridgeOverlayMilliseconds(specified.OverlayDuration),
                Is.EqualTo(120d),
                "明示した時間はそのミリ秒で渡ること");
        });
    }

    /// <summary>成立しない時間を渡したプリセットは、演出なしで即完了する。</summary>
    [Test]
    [Description("[PB-MA-05] 成立しない時間のプリセットは演出なしで即完了する")]
    public void PB_MA_05_APresetWithAnUnusableDurationCompletesWithoutAnimating()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        contentView.Opacity = 1d;

        foreach (TimeSpan duration in UnusableDurations())
        {
            int completionCount = 0;
            DialogTransition preset = DialogTransition.Fade(duration);
            DialogTransitionRunner runner = DialogTransitionRunner.Create(
                preset.Presentation,
                contentView,
                new RecordingUiThread().Run)!;

            runner.Run(() => completionCount++);

            Assert.Multiple(() =>
            {
                Assert.That(completionCount, Is.EqualTo(1), $"{duration} でも完了が返ること");
                Assert.That(contentView.Opacity, Is.EqualTo(1d), $"{duration} では見た目を動かさないこと");
                Assert.That(preset.OverlayDuration, Is.EqualTo(duration), "覆いの時間は渡された値のまま運ぶこと");
            });
        }
    }

    /// <summary>演出が成立しない片道の時間。</summary>
    /// <returns>0・負値・ミリ秒表現に収まらない大きさ。</returns>
    private static IEnumerable<TimeSpan> UnusableDurations() =>
    [
        TimeSpan.Zero,
        TimeSpan.FromMilliseconds(-250d),
        TimeSpan.FromMilliseconds((double)uint.MaxValue + 1d),
        TimeSpan.MaxValue,
    ];
}
