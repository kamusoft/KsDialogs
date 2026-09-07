package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("結果確定とダイアログの閉鎖")
class DialogContainerTests : DialogUiThreadTest() {

    private companion object {
        /** show の完了待ちの上限。 */
        const val SHOW_COMPLETION_TIMEOUT_MILLIS = 5_000L
    }

    private fun createContainer(resultChannel: DialogResultChannel): DialogContainer {
        val context = ContextWrapper(null)
        return DialogContainer(context, View(context), resultChannel)
    }

    @Test
    fun `器が画面から外れたら未確定の結果は cancelled になる`() {
        val resultChannel = DialogResultChannel()
        val outcomes = mutableListOf<DialogOutcome>()
        resultChannel.onSettle { outcomes.add(it) }
        val container = createContainer(resultChannel)

        container.reportDetached()

        assertEquals(1, outcomes.size)
        assertTrue(outcomes.first() is DialogOutcome.Cancelled)
    }

    @Test
    fun `確定済みの結果は器が画面から外れても変わらない`() {
        val resultChannel = DialogResultChannel()
        val outcomes = mutableListOf<DialogOutcome>()
        resultChannel.onSettle { outcomes.add(it) }
        val container = createContainer(resultChannel)
        DialogNotifier<Boolean>(resultChannel).complete(true)

        container.reportDetached()

        assertEquals(1, outcomes.size)
        assertEquals(true, (outcomes.first() as DialogOutcome.Completed).value)
    }

    @Test
    fun `提示先の画面が破棄された show は cancelled で完了する`() = runBlocking {
        // 器の出し入れは差し替えず、提示面・画面の追跡役・器の実装をつないだ状態で確かめる
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        tracker.onActivityResumed(activity)
        val registry = DialogViewRegistry()
        val recorder = DialogTestRecorder<Boolean>()
        registry.register(BasicTestDialogViewModel::class) { _, notifier ->
            View(this).also { recorder.record(it, notifier) }
        }
        val dialogs = Dialog(registry, ActivityDialogPresentationSurface(tracker, tracker))

        val showTask = async { dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        recorder.awaitNotifier(0)
        // 画面の破棄は UI スレッドで届く。提示が終わった後に届くよう UI スレッドへ載せる
        withContext(Dispatchers.Main) { tracker.onActivityDestroyed(activity) }

        // 結果が確定しない退行では await が返らないため、待ち時間を区切って失敗させる
        assertEquals(DialogResult.Cancelled, withTimeout(SHOW_COMPLETION_TIMEOUT_MILLIS) { showTask.await() })
    }

    @Test
    fun `外の要因で器が閉じた show は cancelled で完了する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        assertTrue(harness.waitForPresentedContainers(1))

        // 画面の破棄などで、結果報告を経ずに器だけが消える状況
        harness.topmostContainer?.let(harness.presentationSurface::detachExternally)

        assertEquals(DialogResult.Cancelled, showTask.await())
        assertTrue(harness.presentedContainers.isEmpty())
    }
}
