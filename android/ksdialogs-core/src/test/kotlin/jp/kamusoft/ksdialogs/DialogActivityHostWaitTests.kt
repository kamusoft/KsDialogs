package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.view.View
import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestWaiting
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.TrackerTestDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Activity を提示先にする面で、提示先の出現を待つ Dialog を確かめる。
 *
 * 提示先は resumed で、かつ描画された Activity で、入れ替わりは resume・描画・破棄で通知される。
 * 通知のたびに提示先を読み直すが、描画された resumed な Activity が無ければ待ち続ける。
 */
@DisplayName("Activity の resume と描画を待つ Dialog")
class DialogActivityHostWaitTests : DialogUiThreadTest() {

    /** 追跡役の入れ替わりの購読を数える。 */
    private class CountingChangeObserver(private val tracker: ResumedActivityTracker) : ResumedActivityChangeObserver {
        private val lock = Any()
        private var active = 0

        val activeCount: Int
            get() = synchronized(lock) { active }

        override fun observeResumedChange(onChanged: () -> Unit): ResumedActivityChangeRegistration {
            val registration = tracker.observeResumedChange(onChanged)
            synchronized(lock) { active += 1 }
            return ResumedActivityChangeRegistration {
                registration.cancel()
                synchronized(lock) { active -= 1 }
            }
        }
    }

    private class Fixture {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val changeObserver = CountingChangeObserver(tracker)
        val surface = ActivityDialogPresentationSurface(
            activityProvider = tracker,
            destroyObserver = tracker,
            changeObserver = changeObserver,
            hostWaitQueue = DialogHostWaitQueue(),
        )
        val registry = DialogViewRegistry()
        val dialogs = Dialog(registry, surface)
        val factoryContexts = CopyOnWriteArrayList<Context>()

        init {
            registry.register(BasicTestDialogViewModel::class) { _, _ ->
                factoryContexts.add(this)
                View(this)
            }
        }

        suspend fun waitForWaitingCount(count: Int): Boolean =
            DialogTestWaiting.waitUntil { surface.hostWaitQueue.waitingCount == count }

        /** 追跡役のライフサイクル通知は UI スレッドで届くので、同じ条件で送る。 */
        suspend fun onUiThread(block: () -> Unit) {
            withContext(Dispatchers.Main) { block() }
        }
    }

    @Test
    fun `PB-HA-01 Activity が resume して描画された時点で、待っていた Dialog が表示される`() = runBlocking {
        val fixture = Fixture()

        val showTask = async { fixture.dialogs.show(BasicTestDialogViewModel("resume を待つ")) }
        assertTrue(fixture.waitForWaitingCount(1), "resumed な Activity が無いので待つ")
        // 待ちの数は列に着いた時点で数えられ、購読はその直後に UI スレッドで張られる
        assertTrue(
            DialogTestWaiting.waitUntil { fixture.changeObserver.activeCount == 1 },
            "待っている間は入れ替わりを購読する",
        )
        assertTrue(fixture.factoryContexts.isEmpty())

        val activity = Activity()
        fixture.onUiThread { fixture.driver.launch(activity) }
        fixture.onUiThread { }

        assertTrue(fixture.factoryContexts.isEmpty(), "resume の時点では表示されない")
        assertEquals(1, fixture.surface.hostWaitQueue.waitingCount, "描画を待つ")

        fixture.onUiThread { fixture.driver.drawObserver.draw(activity) }

        assertTrue(DialogTestWaiting.waitUntil { fixture.factoryContexts.size == 1 }, "描画された時点で表示される")
        assertSame(activity, fixture.factoryContexts.single(), "描画された Activity に表示される")
        assertEquals(0, fixture.surface.hostWaitQueue.waitingCount)
        assertEquals(0, fixture.changeObserver.activeCount, "待ちの購読は解除されている")

        showTask.cancelAndJoin()
    }

    @Test
    fun `PB-HA-02 Activity が破棄されただけでは表示されず、次の Activity が resume して描画された時点で表示される`() = runBlocking {
        val fixture = Fixture()
        val previous = Activity()
        fixture.onUiThread {
            fixture.driver.launchAndDraw(previous)
            fixture.tracker.onActivityPaused(previous)
            fixture.tracker.onActivityStopped(previous)
        }

        val showTask = async { fixture.dialogs.show(BasicTestDialogViewModel("次の画面を待つ")) }
        assertTrue(fixture.waitForWaitingCount(1))

        // 破棄の通知は届くが、resumed な Activity は無いまま
        fixture.onUiThread { fixture.tracker.onActivityDestroyed(previous) }
        fixture.onUiThread { }

        assertTrue(fixture.factoryContexts.isEmpty(), "破棄の通知では表示されない")
        assertEquals(1, fixture.surface.hostWaitQueue.waitingCount, "待ちは続く")

        val next = Activity()
        fixture.onUiThread { fixture.driver.launch(next) }
        fixture.onUiThread { }
        assertTrue(fixture.factoryContexts.isEmpty(), "次の Activity の resume の時点では表示されない")

        fixture.onUiThread { fixture.driver.drawObserver.draw(next) }

        assertTrue(DialogTestWaiting.waitUntil { fixture.factoryContexts.size == 1 }, "次の Activity の描画で表示される")
        assertSame(next, fixture.factoryContexts.single())
        assertEquals(0, fixture.changeObserver.activeCount)

        showTask.cancelAndJoin()
    }

    /**
     * 提示の直前の読み直しで、決めた回数だけ提示先を取り逃がす供給。
     *
     * 提示先を確かめてから提示面が器を載せるまでの間に提示先が消えた状況を作る。
     */
    private class MissingOnceActivityProvider(private val tracker: ResumedActivityTracker) : ResumedActivityProvider {
        private val lock = Any()
        private var remainingMisses = 1

        override val resumedActivity: Activity?
            get() {
                val misses = synchronized(lock) { remainingMisses.also { if (it > 0) remainingMisses -= 1 } }
                return if (misses > 0) null else tracker.resumedActivity
            }
    }

    @Test
    fun `列から明けた show の器を載せられないと、その show は cancelled で終わり、次の show が明ける`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val placing = ActivityDialogPresentationSurface(
            activityProvider = MissingOnceActivityProvider(tracker),
            destroyObserver = tracker,
            changeObserver = tracker,
            hostWaitQueue = DialogHostWaitQueue(),
        )
        // 提示先の有無は追跡役から読み、器を載せるときだけ取り逃がしうる供給を使う
        val surface = object : DialogPresentationSurface by placing {
            override val canPresent: Boolean
                get() = tracker.resumedActivity != null
        }
        val registry = DialogViewRegistry()
        val createdMessages = CopyOnWriteArrayList<String>()
        registry.register(BasicTestDialogViewModel::class) { viewModel, _ ->
            createdMessages.add(viewModel.message)
            View(this)
        }
        val dialogs = Dialog(registry, surface)
        val viewModelA = BasicTestDialogViewModel("A")

        val showA = async { dialogs.show(viewModelA) }
        assertTrue(DialogTestWaiting.waitUntil { surface.hostWaitQueue.waitingCount == 1 })
        val showB = async { dialogs.show(BasicTestDialogViewModel("B")) }
        assertTrue(DialogTestWaiting.waitUntil { surface.hostWaitQueue.waitingCount == 2 })

        withContext(Dispatchers.Main) { driver.launchAndDraw() }

        assertEquals(DialogResult.Cancelled, showA.await(), "載せられなかった A は cancelled で終わる")
        assertEquals(null, viewModelA.notifier, "A の報告口の紐付けは解除されている")
        assertTrue(DialogTestWaiting.waitUntil { createdMessages.toList() == listOf("B") }, "A の番が返り、B が明けて表示される")
        assertEquals(0, surface.hostWaitQueue.waitingCount)

        showB.cancelAndJoin()
    }
}
