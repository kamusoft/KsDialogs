package jp.kamusoft.ksdialogs

import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 出入りの演出 (core/ADR-0017) の順序・完了待ち・脱出口を、実ウィンドウの器で確かめる。
 *
 * 演出の見た目そのものは自動検査の対象にせず、契約が定める「順序と完了」だけを見る。
 */
@RunWith(AndroidJUnit4::class)
class DialogTransitionTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    // 添付

    @Test
    fun PB_TR_01_出現フックが表示時にちょうど1回_ホスト_View_を引数に_UI_スレッドで呼ばれる() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(presentation = probe.immediateHook(PRESENTATION)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))

            assertEquals(1, probe.callCount(PRESENTATION))
            val call = requireNotNull(probe.firstCall(PRESENTATION))
            assertSame("フックにはコンテンツのホスト View が渡る", container.contentView, call.hostView)
            assertTrue("フックは UI スレッドで開始される", call.isOnMainThread)
            assertTrue("フックが始まる時点でホスト View はウィンドウ上にある", call.isAttachedToWindow)
            assertNotEquals("フックが始まる時点でレイアウトは済んでいる", 0, call.width)

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_02_片側だけの添付では未指定側に既定が適用される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.immediateHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            assertEquals("未指定側の出現フックは呼ばれない", 0, probe.callCount(PRESENTATION))

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals(1, probe.callCount(DISMISSAL))
        }
    }

    @Test
    fun PB_TR_03_添付なしでは両フックとも呼ばれず既定のトランジションで表示_閉鎖される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, transition = null)
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)

            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertTrue("添付していないフックは観測されない", probe.events.isEmpty())
        }
    }

    @Test
    fun PB_TR_04_表示後の添付変更は退出に影響しない() = runBlocking<Unit> {
        coroutineScope {
            val adopted = DialogTransitionProbe()
            val replaced = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = adopted.immediateHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            onMainThread {
                container.contentView.ksDialogTransition =
                    DialogTransition(dismissal = replaced.immediateHook(DISMISSAL))
            }

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals("固定済みの演出が使われる", 1, adopted.callCount(DISMISSAL))
            assertEquals("書き換え後の演出は使われない", 0, replaced.callCount(DISMISSAL))
        }
    }

    // 退出の開始条件と直列化

    @Test
    fun PB_TR_05_出現中の閉鎖信号は出現の完走後に退出する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(
                this,
                DialogTransition(
                    presentation = probe.gatedHook(PRESENTATION, gate),
                    dismissal = probe.immediateHook(DISMISSAL),
                ),
            )

            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(PRESENTATION)))
            stage.notifier().complete(true)
            delay(NO_REACTION_WAIT_MILLIS)
            assertEquals("出現の完走前に退出は始まらない", 0, probe.callCount(DISMISSAL))

            gate.open()
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals(
                listOf(
                    DialogTransitionProbe.Event.Started(PRESENTATION),
                    DialogTransitionProbe.Event.Finished(PRESENTATION),
                    DialogTransitionProbe.Event.Started(DISMISSAL),
                    DialogTransitionProbe.Event.Finished(DISMISSAL),
                ),
                probe.events,
            )
        }
    }

    @Test
    fun PB_TR_06_複数の閉鎖信号でも退出フックは1回() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            onMainThread { container.reportOutsideTap() }

            gate.open()
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals(1, probe.callCount(DISMISSAL))
        }
    }

    @Test
    fun PB_TR_07_外側タップでも退出フックが実行される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.immediateHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            onMainThread { container.reportOutsideTap() }

            assertEquals(DialogResult.Cancelled, stage.awaitResult())
            assertEquals(1, probe.callCount(DISMISSAL))
        }
    }

    @Test
    fun PB_TR_08_呼び出し元キャンセルでも退出フックが実行され_CancellationException_が伝播する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.immediateHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.showTask.cancel()

            assertTrue("Kotlin ではキャンセルが例外として伝播する", stage.awaitCancellation())
            assertTrue(awaitState(container, DialogContainerState.REMOVED))
            assertEquals(1, probe.callCount(DISMISSAL))
        }
    }

    @Test
    fun PB_TR_09_器が画面を失うとフックを実行せず即_cancelled_になる() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(
                this,
                DialogTransition(
                    presentation = probe.immediateHook(PRESENTATION),
                    dismissal = probe.immediateHook(DISMISSAL),
                ),
            )
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            onMainThread { stage.surface.simulateHostLoss(container) }

            assertEquals(DialogResult.Cancelled, stage.awaitResult())
            assertEquals(0, probe.callCount(DISMISSAL))
            assertEquals(DialogContainerState.REMOVED, container.containerState)
            assertTrue(
                "ホスト View は解放される",
                awaitCondition { !container.contentView.isAttachedToWindow },
            )
        }
    }

    @Test
    fun PB_TR_19_提示開始前の報告は演出なしで配送される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(
                scope = this,
                transition = DialogTransition(
                    presentation = probe.immediateHook(PRESENTATION),
                    dismissal = probe.immediateHook(DISMISSAL),
                ),
                reportsInFactory = true,
                awaitsPresentation = false,
            )

            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertTrue("両フックとも呼ばれない", probe.events.isEmpty())
            assertTrue(awaitCondition { stage.surface.presentedContainers.isEmpty() })
        }
    }

    @Test
    fun PB_TR_20_提示開始前の呼び出し元キャンセルは演出なしで閉じる() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(
                scope = this,
                transition = DialogTransition(
                    presentation = probe.immediateHook(PRESENTATION),
                    dismissal = probe.immediateHook(DISMISSAL),
                ),
                holdsWindowAttachment = true,
            )
            val container = requireNotNull(stage.surface.topmostContainer)
            assertEquals("まだ提示は始まっていない", DialogContainerState.CREATED, container.containerState)

            // 取り消しは show が待ちに入った状態で届くため、この時点でキャンセルが確定する
            stage.showTask.cancel()
            delay(CANCELLATION_SETTLE_WAIT_MILLIS)
            onMainThread { stage.surface.attachHeldContainers() }

            assertTrue(stage.awaitCancellation())
            assertTrue(awaitState(container, DialogContainerState.REMOVED))
            assertTrue("両フックとも呼ばれない", probe.events.isEmpty())
        }
    }

    @Test
    fun PB_TR_21_none_直後の閉鎖は覆いの出現完了を待ってから退出する() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(this, DialogTransition.none())
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.PRESENTING))
            val stateAtReport = AtomicReference<DialogContainerState>()
            onMainThread {
                stage.notifier().complete(true)
                stateAtReport.set(container.containerState)
            }

            assertEquals("覆いの出現中はまだ退出しない", DialogContainerState.PRESENTING, stateAtReport.get())
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals(DialogContainerState.REMOVED, container.containerState)
        }
    }

    @Test
    fun PB_TR_28_出現中の呼び出し元キャンセルは出現をキャンセルして退出する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(
                this,
                DialogTransition(
                    presentation = probe.gatedHook(PRESENTATION, gate),
                    dismissal = probe.immediateHook(DISMISSAL),
                ),
            )

            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(PRESENTATION)))
            stage.showTask.cancel()

            assertTrue(stage.awaitCancellation())
            assertTrue(
                "出現の演出は完走を待たずに取り消される",
                awaitEvent(probe, DialogTransitionProbe.Event.Cancelled(PRESENTATION)),
            )
            assertFalse(probe.hasEvent(DialogTransitionProbe.Event.Finished(PRESENTATION)))
            assertTrue(awaitCondition { probe.callCount(DISMISSAL) == 1 })
        }
    }

    @Test
    fun PB_TR_29_退出中の呼び出し元キャンセルは実行中の退出フックをキャンセルして脱出する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            stage.showTask.cancel()

            assertTrue("ラッチ済みの completed は呼び出し元へ届かない", stage.awaitCancellation())
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Cancelled(DISMISSAL)))
            assertFalse(probe.hasEvent(DialogTransitionProbe.Event.Finished(DISMISSAL)))
            assertTrue(awaitState(container, DialogContainerState.REMOVED))
        }
    }

    @Test
    fun PB_TR_22_退出中の入力は無視される() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))

            val consumed = AtomicReference(false)
            onMainThread {
                consumed.set(container.dispatchTouchEvent(tapEvent()))
                container.reportOutsideTap()
                stage.notifier().complete(false)
            }
            assertTrue("覆いも中身もタップを受け取らない", consumed.get())

            gate.open()
            assertEquals("配送されるのは最初の報告の値", DialogResult.Completed(true), stage.awaitResult())
        }
    }

    // 結果のラッチと配送

    @Test
    fun PB_TR_10_show_は退出フック完了と器の撤去より先に返らない() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            delay(NO_REACTION_WAIT_MILLIS)
            assertTrue("フックが完了するまで show は返らない", stage.showTask.isActive)

            gate.open()
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals("配送は器の撤去のあと", DialogContainerState.REMOVED, container.containerState)
        }
    }

    @Test
    fun PB_TR_11_退出中の二重報告はラッチ済みの値を配送する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            stage.notifier().complete(false)

            gate.open()
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_12_退出中に器が画面を失うとフックをキャンセルして即配送する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(dismissal = probe.gatedHook(DISMISSAL, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            onMainThread { stage.surface.simulateHostLoss(container) }

            assertEquals("ラッチ済みの結果は cancelled に変わらない", DialogResult.Completed(true), stage.awaitResult())
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Cancelled(DISMISSAL)))
            assertEquals(DialogContainerState.REMOVED, container.containerState)
        }
    }

    @Test
    fun PB_TR_23_出現中に器が画面を失うとフックをキャンセルして_cancelled_を配送する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val gate = DialogTransitionGate()
            val stage = start(this, DialogTransition(presentation = probe.gatedHook(PRESENTATION, gate)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(PRESENTATION)))
            onMainThread { stage.surface.simulateHostLoss(container) }

            assertEquals(DialogResult.Cancelled, stage.awaitResult())
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Cancelled(PRESENTATION)))
            assertEquals(DialogContainerState.REMOVED, container.containerState)
        }
    }

    @Test
    fun PB_TR_13_添付なしの既定トランジションでも配送は撤去後() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(this, transition = null)
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)

            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertEquals("既定の退出処理が終わってから配送される", DialogContainerState.REMOVED, container.containerState)
        }
    }

    // フックの失敗

    @Test
    fun PB_TR_14_出現フックの失敗でも表示は継続する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(presentation = probe.failingHook(PRESENTATION)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue("状態機械は表示状態へ進む", awaitState(container, DialogContainerState.SHOWN))
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Failed(PRESENTATION)))

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_15_退出フックの失敗でも撤去と配送は完了する() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.failingHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)

            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Failed(DISMISSAL)))
            assertEquals(DialogContainerState.REMOVED, container.containerState)
        }
    }

    // フックの完了は利用者の責務 (前提条件と脱出口)

    @Test
    fun PB_TR_24_終了しないフックは呼び出し元キャンセルで脱出できる() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.neverEndingHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            stage.showTask.cancel()

            assertTrue(stage.awaitCancellation())
            assertTrue(awaitState(container, DialogContainerState.REMOVED))
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Cancelled(DISMISSAL)))
        }
    }

    @Test
    fun PB_TR_25_終了しないフックは器が画面を失うことで脱出できる() = runBlocking<Unit> {
        coroutineScope {
            val probe = DialogTransitionProbe()
            val stage = start(this, DialogTransition(dismissal = probe.neverEndingHook(DISMISSAL)))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertTrue(awaitEvent(probe, DialogTransitionProbe.Event.Started(DISMISSAL)))
            onMainThread { stage.surface.simulateHostLoss(container) }

            assertEquals(DialogResult.Completed(true), stage.awaitResult())
            assertTrue(probe.hasEvent(DialogTransitionProbe.Event.Cancelled(DISMISSAL)))
            assertEquals(DialogContainerState.REMOVED, container.containerState)
        }
    }

    // プリセット

    @Test
    fun PB_TR_16_プリセット指定で追加のフック記述なしに演出が差し替わり覆いの時間も揃う() = runBlocking<Unit> {
        coroutineScope {
            val duration = 120.milliseconds
            val stage = start(this, DialogTransition.slide(from = DialogTransitionEdge.BOTTOM, duration = duration))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            assertEquals("覆いの時間はプリセットの時間に揃う", duration, container.resolvedOverlayDuration)

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_17_none_プリセットでは中身側の待ちなしで表示_閉鎖される() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(this, DialogTransition.none())
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            assertEquals("覆いは器の既定の時間で扱われる", DEFAULT_OVERLAY_DURATION, container.resolvedOverlayDuration)

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_18_duration_0_のプリセットは即完了する() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(this, DialogTransition.fade(duration = Duration.ZERO))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue("覆いも即完了する", awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_26_負の_duration_のプリセットは即完了する() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(
                this,
                DialogTransition.slide(from = DialogTransitionEdge.START, duration = -100.milliseconds),
            )
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue(awaitState(container, DialogContainerState.SHOWN))
            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    @Test
    fun PB_TR_27_有効範囲外の_duration_のプリセットは即完了する() = runBlocking<Unit> {
        coroutineScope {
            val stage = start(this, DialogTransition.zoom(duration = Duration.INFINITE))
            val container = requireNotNull(stage.surface.topmostContainer)

            assertTrue("覆いも即完了する", awaitState(container, DialogContainerState.SHOWN))
            assertEquals(Duration.INFINITE, container.resolvedOverlayDuration)

            stage.notifier().complete(true)
            assertEquals(DialogResult.Completed(true), stage.awaitResult())
        }
    }

    // 組み立て

    /** 1回分の show とその観測に必要な道具。 */
    private class Stage(
        val surface: RecordingDialogPresentationSurface,
        val showTask: Deferred<DialogResult<Boolean>>,
        private val recordedNotifier: AtomicReference<DialogNotifier<Boolean>>,
    ) {
        /** この show の結果報告口。 */
        fun notifier(): DialogNotifier<Boolean> = requireNotNull(recordedNotifier.get())

        /** show の結果が配送されるまで待つ。 */
        suspend fun awaitResult(): DialogResult<Boolean> =
            withTimeout(RESULT_TIMEOUT_MILLIS) { showTask.await() }

        /** show がキャンセルとして終わるまで待つ。 */
        suspend fun awaitCancellation(): Boolean {
            val failure = runCatching { awaitResult() }.exceptionOrNull()
            return failure is CancellationException
        }
    }

    /** 演出を添付したダイアログを1枚表示し、器が画面に載るまで待つ。 */
    private suspend fun start(
        scope: CoroutineScope,
        transition: DialogTransition?,
        reportsInFactory: Boolean = false,
        holdsWindowAttachment: Boolean = false,
        awaitsPresentation: Boolean = true,
    ): Stage {
        val registry = DialogViewRegistry()
        val surface = RecordingDialogPresentationSurface()
        surface.holdsWindowAttachment = holdsWindowAttachment
        val dialogs = Dialog(registry, surface)
        val recordedNotifier = AtomicReference<DialogNotifier<Boolean>>()

        registry.register(PlainTestDialogViewModel::class) { _, notifier ->
            recordedNotifier.set(notifier)
            val density = resources.displayMetrics.density
            FixedContentSizeView(
                context = this,
                contentWidth = (CONTENT_SIZE_DP * density).toInt(),
                contentHeight = (CONTENT_SIZE_DP * density).toInt(),
            ).also {
                it.ksDialogTransition = transition
                if (reportsInFactory) {
                    notifier.complete(true)
                }
            }
        }

        val showTask = scope.async { dialogs.show(PlainTestDialogViewModel()) }
        if (awaitsPresentation) {
            check(awaitCondition { surface.presentedContainers.size == 1 }) { "ダイアログが提示されなかった" }
        }
        return Stage(surface, showTask, recordedNotifier)
    }

    /** 器が指定の段階になるまで待つ。 */
    private suspend fun awaitState(container: DialogContainer, state: DialogContainerState): Boolean =
        awaitCondition { container.containerState == state }

    /** その出来事が観測できるまで待つ。 */
    private suspend fun awaitEvent(probe: DialogTransitionProbe, event: DialogTransitionProbe.Event): Boolean =
        awaitCondition { probe.hasEvent(event) }

    /** 条件が満たされるまで待つ。 */
    private suspend fun awaitCondition(condition: () -> Boolean): Boolean =
        InstrumentedDialogWaiting.waitUntil(condition = condition)

    /** UI スレッドで実行し、終わるまで待つ。 */
    private fun onMainThread(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    /** 器の左上あたりを触る入力。退出中に届いても何も起こさないことの確認に使う。 */
    private fun tapEvent(): MotionEvent {
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, TAP_POINT, TAP_POINT, 0)
    }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 結果を待つ上限 (ミリ秒)。 */
        const val RESULT_TIMEOUT_MILLIS = 15_000L

        /** 「まだ何も起こらない」ことを確かめるための待ち (ミリ秒)。 */
        const val NO_REACTION_WAIT_MILLIS = 400L

        /** 呼び出し元の取り消しが器へ届くまでの待ち (ミリ秒)。 */
        const val CANCELLATION_SETTLE_WAIT_MILLIS = 100L

        /** 退出中の入力として送る点 (px)。 */
        const val TAP_POINT = 5f

        /** 器が既定で使う覆いのフェード時間。 */
        val DEFAULT_OVERLAY_DURATION = 250.milliseconds
    }
}
