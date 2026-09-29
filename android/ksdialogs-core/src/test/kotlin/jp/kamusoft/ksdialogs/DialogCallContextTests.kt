package jp.kamusoft.ksdialogs

import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

@DisplayName("呼び出しコンテキストの契約")
class DialogCallContextTests : DialogUiThreadTest() {

    @Test
    fun `UI スレッド外からの show が成立する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val factoryThreads = CopyOnWriteArrayList<Thread>()
        harness.registry.register(BasicTestDialogViewModel::class) { _, notifier ->
            factoryThreads.add(Thread.currentThread())
            android.view.View(this).also { recorder.record(it, notifier) }
        }

        // UI スレッド以外のスレッドから show を呼び出す
        val showTask = async(Dispatchers.Default) {
            val callerThread = Thread.currentThread()
            callerThread to harness.dialogs.show(BasicTestDialogViewModel("こんにちは"))
        }
        recorder.awaitNotifier(0).complete(true)

        val (callerThread, result) = showTask.await()
        assertFalse(callerThread === uiThread, "show は UI スレッド以外から呼び出される")
        assertEquals(listOf(uiThread), factoryThreads.toList(), "View の生成は UI スレッドで行われる")
        assertEquals(DialogResult.Completed(true), result)
    }

    @Test
    fun `PB-HW-01 提示先が無い間は待ち、現れたら表示する`() = runBlocking {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        assertTrue(harness.waitForWaitingCount(1), "show は失敗も完了もせず、提示先を待つ列に並ぶ")
        assertFalse(showTask.isCompleted)
        assertTrue(recorder.createdViews.isEmpty(), "提示先が無い間は View factory が呼ばれない")
        assertTrue(harness.presentedContainers.isEmpty())

        harness.makeHostAppear()

        val notifier = recorder.awaitNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1), "提示先が現れた時点で表示される")
        assertEquals(1, recorder.createdViews.size)
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
        assertEquals(0, harness.presentationSurface.activeHostChangeRegistrationCount, "表示したら購読を解除する")
    }
}
