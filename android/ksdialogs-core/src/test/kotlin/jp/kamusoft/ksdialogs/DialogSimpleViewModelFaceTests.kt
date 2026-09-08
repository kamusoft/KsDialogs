package jp.kamusoft.ksdialogs

import android.view.View
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.SimpleFacedTestDialogViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("真偽値の結果を型引数なしで宣言する顔")
class DialogSimpleViewModelFaceTests : DialogUiThreadTest() {

    @Test
    fun `真偽値の顔で宣言した VM は結果報告口が真偽値の通知役になる`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()

        // 型引数を書かずに登録できることと、渡る notifier の型が真偽値であることを同時に見る
        harness.registry.register(SimpleFacedTestDialogViewModel::class) { _, notifier ->
            val boundNotifier: DialogNotifier<Boolean> = notifier
            View(this).also { recorder.record(it, boundNotifier) }
        }

        val showTask = async { harness.dialogs.show(SimpleFacedTestDialogViewModel("こんにちは")) }
        recorder.awaitNotifier(0).complete(true)

        val result: DialogResult<Boolean> = showTask.await()
        assertEquals(DialogResult.Completed(true), result)
    }
}
