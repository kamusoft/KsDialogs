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

@DisplayName("戻るボタンによるキャンセル (通常時)")
class DialogBackPressTests : DialogUiThreadTest() {

    @Test
    fun `戻るボタンで cancelled が返る (キーボード非表示時)`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        assertTrue(harness.waitForPresentedContainers(1))
        harness.topmostContainer?.reportBackPress()

        assertEquals(DialogResult.Cancelled, showTask.await())
        assertTrue(harness.waitForPresentedContainers(0))
        Unit
    }
}
