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
import org.junit.jupiter.api.assertThrows
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
    fun `提示 host 不在の show は即失敗する`() {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        assertThrows<DialogException.PresentationHostUnavailable> {
            runBlocking { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        }

        assertTrue(recorder.createdViews.isEmpty())
        assertTrue(harness.presentedContainers.isEmpty())
    }
}
