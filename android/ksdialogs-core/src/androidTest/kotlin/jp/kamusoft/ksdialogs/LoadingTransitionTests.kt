package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionGate
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Loading の出入りの演出 (core/ADR-0017・0022) を確かめる。
 * iOS Native の LoadingTransitionTests のミラー。
 *
 * カスタム Loading View は Dialog と同じ添付スロットで演出を差し替えられ、
 * 未添付のカスタム View と既定ローディングはライブラリ既定の演出で出入りする。
 * 覆いはフックの内容によらず、中身とは別のレイヤでフェードする。
 */
@RunWith(AndroidJUnit4::class)
class LoadingTransitionTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_TR_01_カスタム_View_の添付演出フックが実行される() = runBlocking<Unit> {
        val harness = newHarness()
        val probe = DialogTransitionProbe()
        val dismissalGate = DialogTransitionGate()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().apply {
                ksDialogTransition = DialogTransition(
                    presentation = probe.immediateHook(DialogTransitionProbe.Phase.PRESENTATION),
                    dismissal = probe.gatedHook(DialogTransitionProbe.Phase.DISMISSAL, dismissalGate),
                )
            }
        }

        harness.loading.show(LoadingTestViewModel())
        val contentView = requireNotNull(harness.contentView)

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
        assertEquals(0, probe.callCount(DialogTransitionProbe.Phase.DISMISSAL))

        coroutineScope {
            val hiding = async { harness.loading.hide() }
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    probe.callCount(DialogTransitionProbe.Phase.DISMISSAL) == 1
                },
            )
            val dismissalCall = requireNotNull(probe.firstCall(DialogTransitionProbe.Phase.DISMISSAL))
            assertSame(contentView, dismissalCall.hostView)
            assertNotNull("出のフックの完了前は撤去されない", contentView.parent)

            dismissalGate.open()
            hiding.await()
        }

        assertNull("出のフックの完了後に撤去される", contentView.parent)
        assertFalse(harness.isPresenting)
    }

    @Test
    fun LD_TR_02_未添付なら既定のトランジションで出入りする() = runBlocking<Unit> {
        val harness = newHarness()
        val probe = DialogTransitionProbe()
        harness.registry.register(LoadingTestViewModel::class) { _ -> newContentView() }

        // 演出を添付しないカスタム View
        harness.loading.show(LoadingTestViewModel())
        val customContainer = requireNotNull(harness.container)
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { customContainer.resolvedTransition != null },
        )
        val customTransition = requireNotNull(customContainer.resolvedTransition)
        assertNotNull("未添付側は器の既定で埋まる", customTransition.presentation)
        assertNotNull(customTransition.dismissal)
        val customContentView = requireNotNull(harness.contentView)
        assertTrue(InstrumentedDialogWaiting.waitUntil { customContentView.alpha == 1f })
        harness.loading.hide()

        // 既定ローディング
        harness.loading.show()
        val builtinContainer = requireNotNull(harness.container)
        assertNotSame(customContainer, builtinContainer)
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { builtinContainer.resolvedTransition != null },
        )
        val builtinTransition = requireNotNull(builtinContainer.resolvedTransition)
        assertNotNull(builtinTransition.presentation)
        assertNotNull(builtinTransition.dismissal)
        val builtinContentView = requireNotNull(harness.contentView)
        assertTrue(InstrumentedDialogWaiting.waitUntil { builtinContentView.alpha == 1f })
        harness.loading.hide()

        assertTrue(
            "フックを添付していないので利用者のフックは走らない",
            probe.events.isEmpty(),
        )
    }

    @Test
    fun LD_TR_03_覆いは別レイヤでフェードする() = runBlocking<Unit> {
        val harness = newHarness()
        val probe = DialogTransitionProbe()
        val presentationGate = DialogTransitionGate()
        val dismissalGate = DialogTransitionGate()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().apply {
                ksDialogTransition = DialogTransition(
                    presentation = probe.gatedHook(
                        DialogTransitionProbe.Phase.PRESENTATION,
                        presentationGate,
                    ),
                    dismissal = probe.gatedHook(DialogTransitionProbe.Phase.DISMISSAL, dismissalGate),
                )
            }
        }

        harness.loading.show(LoadingTestViewModel())
        val container = requireNotNull(harness.container)
        val contentView = requireNotNull(harness.contentView)
        val overlayView = container.overlayView

        // 入りのフックが終わらないうちに覆いは現れる (フックの内容に依存しない)
        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                probe.callCount(DialogTransitionProbe.Phase.PRESENTATION) == 1
            },
        )
        assertTrue(InstrumentedDialogWaiting.waitUntil { overlayView.alpha == 1f })
        assertFalse(
            "入りのフックはまだ完了していない",
            probe.hasEvent(
                DialogTransitionProbe.Event.Finished(DialogTransitionProbe.Phase.PRESENTATION),
            ),
        )
        assertNotSame("覆いは中身とは別のレイヤ", contentView, overlayView)
        assertSame(container.layoutHost, overlayView.parent)
        presentationGate.open()

        coroutineScope {
            val hiding = async { harness.loading.hide() }
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    probe.callCount(DialogTransitionProbe.Phase.DISMISSAL) == 1
                },
            )
            assertTrue(InstrumentedDialogWaiting.waitUntil { overlayView.alpha == 0f })
            assertFalse(
                "出のフックはまだ完了していない",
                probe.hasEvent(
                    DialogTransitionProbe.Event.Finished(DialogTransitionProbe.Phase.DISMISSAL),
                ),
            )

            dismissalGate.open()
            hiding.await()
        }
        assertFalse(harness.isPresenting)
    }

    /** 共通ケース表と同じ内容サイズを持つ、カスタム Loading の中身。 */
    private fun Context.newContentView(): FixedContentSizeView =
        FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)

    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160
    }
}
