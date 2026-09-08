package jp.kamusoft.ksdialogs

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionGate
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe.Phase.DISMISSAL
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe.Phase.PRESENTATION
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.RecordingDialogPresentationSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 従来 View 系の添付面 (`View.ksDialogTransition`) と、呼び出し元キャンセル後の退出処理を確かめる。
 */
@RunWith(AndroidJUnit4::class)
class DialogTransitionAttachmentTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun PB_AA_01_View_拡張プロパティでの添付が器で採用される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val session = startShow(this, DialogTransition(presentation = probe.immediateHook(PRESENTATION)))
            val container = requireNotNull(session.surface.topmostContainer)

            assertTrue(
                InstrumentedDialogWaiting.waitUntil { container.containerState == DialogContainerState.SHOWN },
            )

            assertEquals(1, probe.callCount(PRESENTATION))
            val call = requireNotNull(probe.firstCall(PRESENTATION))
            assertSame("そのコンテンツのホスト View が渡る", container.contentView, call.hostView)
            assertTrue("Main スレッドで呼ばれる", call.isOnMainThread)

            session.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), session.awaitResult())
        }
    }

    @Test
    fun PB_AA_03_呼び出し元キャンセル後も退出処理が完遂される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val session = startShow(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(session.surface.topmostContainer)

            assertTrue(
                InstrumentedDialogWaiting.waitUntil { container.containerState == DialogContainerState.SHOWN },
            )
            session.showTask.cancel()

            val failure = runCatching { session.awaitResult() }.exceptionOrNull()
            assertTrue("呼び出し元には CancellationException が伝播する", failure is CancellationException)

            assertTrue(
                "退出フックは呼び出し元のキャンセルでは中断されない",
                InstrumentedDialogWaiting.waitUntil {
                    probe.hasEvent(DialogTransitionProbe.Event.Started(DISMISSAL))
                },
            )
            delay(NO_REACTION_WAIT_MILLIS)
            assertFalse(
                "門が開くまで退出フックは終わらない",
                probe.hasEvent(DialogTransitionProbe.Event.Finished(DISMISSAL)),
            )
            assertEquals(DialogContainerState.DISMISSING, container.containerState)

            gate.open()
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    probe.hasEvent(DialogTransitionProbe.Event.Finished(DISMISSAL))
                },
            )
            assertTrue(
                "器は撤去される",
                InstrumentedDialogWaiting.waitUntil {
                    container.containerState == DialogContainerState.REMOVED
                },
            )
        }
    }

    /** 提示中のダイアログ1枚と、その show。 */
    private class ShowSession(
        val surface: RecordingDialogPresentationSurface,
        val showTask: Deferred<DialogResult<Boolean>>,
        private val recordedNotifier: AtomicReference<DialogNotifier<Boolean>>,
    ) {
        fun notifier(): DialogNotifier<Boolean> = requireNotNull(recordedNotifier.get())

        suspend fun awaitResult(): DialogResult<Boolean> =
            withTimeout(RESULT_TIMEOUT_MILLIS) { showTask.await() }
    }

    /** 拡張プロパティで演出を添付した中身で show を始め、器が画面に載るまで待つ。 */
    private suspend fun startShow(scope: CoroutineScope, transition: DialogTransition): ShowSession {
        val registry = DialogViewRegistry()
        val surface = RecordingDialogPresentationSurface()
        val dialogs = Dialog(registry, surface)
        val recordedNotifier = AtomicReference<DialogNotifier<Boolean>>()

        registry.register(PlainTestDialogViewModel::class) { _, notifier ->
            recordedNotifier.set(notifier)
            val density = resources.displayMetrics.density
            FixedContentSizeView(
                context = this,
                contentWidth = (CONTENT_SIZE_DP * density).toInt(),
                contentHeight = (CONTENT_SIZE_DP * density).toInt(),
            ).also { it.ksDialogTransition = transition }
        }

        val showTask = scope.async { dialogs.show(PlainTestDialogViewModel()) }
        check(InstrumentedDialogWaiting.waitUntil { surface.presentedContainers.size == 1 }) {
            "ダイアログが提示されなかった"
        }
        return ShowSession(surface, showTask, recordedNotifier)
    }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 結果を待つ上限 (ミリ秒)。 */
        const val RESULT_TIMEOUT_MILLIS = 15_000L

        /** 「まだ何も起こらない」ことを確かめるための待ち (ミリ秒)。 */
        const val NO_REACTION_WAIT_MILLIS = 400L
    }
}
