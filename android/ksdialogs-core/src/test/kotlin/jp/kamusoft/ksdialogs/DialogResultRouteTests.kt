package jp.kamusoft.ksdialogs

import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

@DisplayName("型付き結果の show")
class DialogResultRouteTests : DialogUiThreadTest() {

    @Test
    fun `完了操作で completed が返る`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `キャンセル操作で cancelled が返る`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        recorder.awaitNotifier(0).cancel()

        assertEquals(DialogResult.Cancelled, showTask.await())
    }

    @Test
    fun `外側タップで cancelled が返る (既定)`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        assertTrue(harness.waitForPresentedContainers(1))
        harness.topmostContainer?.reportOutsideTap()

        assertEquals(DialogResult.Cancelled, showTask.await())
    }

    @Test
    fun `Kotlin から show して結果を受け取る`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        // factory は UI スレッドで呼ばれ、観察は呼び出し元スレッドで行うためスレッド安全な入れ物を使う
        val receivedMessages = CopyOnWriteArrayList<String>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder) { viewModel ->
            receivedMessages.add(viewModel.message)
        }

        // 受け取り型を明示することで、結果型が ViewModel の宣言から導出されることを型検査で確かめる
        val showTask = async<DialogResult<Boolean>> {
            harness.dialogs.show(BasicTestDialogViewModel("こんにちは、KsDialogs!"))
        }
        recorder.awaitNotifier(0).complete(true)

        val result: DialogResult<Boolean> = showTask.await()
        assertEquals(listOf("こんにちは、KsDialogs!"), receivedMessages)
        assertTrue(result is DialogResult.Completed)
        assertEquals(true, (result as DialogResult.Completed).value)
    }
}
