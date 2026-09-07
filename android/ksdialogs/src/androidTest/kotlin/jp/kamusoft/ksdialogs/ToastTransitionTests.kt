package jp.kamusoft.ksdialogs

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionGate
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Toast の出入りの演出 (core/ADR-0017) を確かめる。
 * iOS Native の ToastTransitionTests のミラー。
 *
 * カスタム Toast View は Dialog / Loading と同じ添付スロットで演出を差し替えられ、
 * 未添付のカスタム View とデフォルト View は器の既定の演出で出入りする。
 * Toast は結果を持たないため、結果のラッチと配送は関わらない。
 */
@RunWith(AndroidJUnit4::class)
class ToastTransitionTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_TR_01_カスタム_View_の演出フックが両局面で呼ばれる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        val probe = DialogTransitionProbe()
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS).apply {
                ksDialogTransition = DialogTransition(
                    presentation = probe.immediateHook(DialogTransitionProbe.Phase.PRESENTATION),
                    dismissal = probe.immediateHook(DialogTransitionProbe.Phase.DISMISSAL),
                )
            }
        }

        harness.toast.show(ToastTestViewModel(), durationMs = SHORT_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val contentView = harness.contentViews.single()

        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                probe.callCount(DialogTransitionProbe.Phase.PRESENTATION) == 1
            },
        )
        val presentationCall = requireNotNull(probe.firstCall(DialogTransitionProbe.Phase.PRESENTATION))
        assertSame("フックはホスト View を受け取る", contentView, presentationCall.hostView)
        assertTrue(
            "フックが始まる時点でホスト View は画面に載っている",
            presentationCall.isAttachedToWindow,
        )

        assertTrue(
            "duration の経過で出のフックが呼ばれる",
            InstrumentedDialogWaiting.waitUntil {
                probe.callCount(DialogTransitionProbe.Phase.DISMISSAL) == 1
            },
        )
        val dismissalCall = requireNotNull(probe.firstCall(DialogTransitionProbe.Phase.DISMISSAL))
        assertSame(contentView, dismissalCall.hostView)

        assertTrue("フックの完了後に撤去される", harness.waitUntilEmpty())
        assertTrue(
            probe.hasEvent(
                DialogTransitionProbe.Event.Finished(DialogTransitionProbe.Phase.DISMISSAL),
            ),
        )
    }

    @Test
    fun TS_TR_02_デフォルト_View_は器の既定演出で出入りする() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        val probe = DialogTransitionProbe()

        harness.toast.show("既定の演出", durationMs = SHORT_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { container.resolvedTransition != null },
        )
        val resolved = requireNotNull(container.resolvedTransition)
        assertNotNull("未添付側は器の既定で埋まる", resolved.presentation)
        assertNotNull(resolved.dismissal)

        val contentView = harness.contentViews.single()
        assertTrue(
            "入りの演出で中身が現れる",
            InstrumentedDialogWaiting.waitUntil { contentView.alpha == 1f },
        )
        assertTrue("演出起因で表示が残らない", harness.waitUntilEmpty())
        assertTrue(
            "フックを添付していないので利用者のフックは走らない",
            probe.events.isEmpty(),
        )
    }

    @Test
    fun 入りの演出の途中で提示先が入れ替わっても中身の見えが固まらない() = runBlocking<Unit> {
        val activity = currentActivity()
        val harness = ToastTestHarness(activity)
        val presentationGate = DialogTransitionGate()
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS).apply {
                // 途中まで進んだ入りの演出を再現する。門が開くまで中身は透明のまま止まる
                ksDialogTransition = DialogTransition(
                    presentation = { view ->
                        view.alpha = 0f
                        presentationGate.await()
                    },
                )
            }
        }

        harness.toast.show(ToastTestViewModel(), durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val contentView = harness.contentViews.single()
        assertTrue(
            "入りの演出の途中にならない",
            InstrumentedDialogWaiting.waitUntil {
                harness.containers.single().containerState == DialogContainerState.PRESENTING
            },
        )

        // 提示先がいったん失われてから戻る (画面の再生成と同じ経路) と、器だけが作り直される
        withContext(Dispatchers.Main) {
            harness.changeHost(null)
            harness.changeHost(activity)
        }
        assertTrue(harness.waitUntilPresenting())

        assertSame("中身は同じものが載せ替えられる", contentView, harness.contentViews.single())
        assertTrue(
            "中身が新しい器の画面に載らない",
            InstrumentedDialogWaiting.waitUntil { contentView.isAttachedToWindow },
        )
        assertEquals(
            "中身が入りの途中の見え (透明) のまま固まっている",
            1f,
            contentView.alpha,
            ALPHA_TOLERANCE,
        )

        presentationGate.open()
        assertTrue(harness.waitUntilEmpty())
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** カスタム Toast View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160

        /** 待ち時間を短く保つための表示時間 (ミリ秒)。 */
        const val SHORT_DURATION_MILLIS = 600

        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 6_000

        /** 透明度を同じとみなす許容差。 */
        const val ALPHA_TOLERANCE = 0.001f
    }
}
