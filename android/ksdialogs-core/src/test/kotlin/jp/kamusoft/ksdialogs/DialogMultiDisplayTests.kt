package jp.kamusoft.ksdialogs

import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogTestWaiting
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("多段表示の基本保証")
class DialogMultiDisplayTests : DialogUiThreadTest() {

    /** ダイアログを2枚重ねて表示し、下・上それぞれの show を返す。 */
    private suspend fun CoroutineScope.presentTwoDialogs(
        harness: DialogTestHarness,
        recorder: DialogTestRecorder<Boolean>,
    ): Pair<Deferred<DialogResult<Boolean>>, Deferred<DialogResult<Boolean>>> {
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val bottomTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("下")).also(recorder::record)
        }
        assertTrue(harness.waitForPresentedContainers(1))
        val topTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("上")).also(recorder::record)
        }
        assertTrue(harness.waitForPresentedContainers(2))
        return bottomTask to topTask
    }

    @Test
    fun PB_MD_01_結果確定で自分のダイアログだけが閉じる() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val (bottomTask, topTask) = presentTwoDialogs(harness, recorder)

        recorder.notifiers[1].complete(true)
        assertEquals(DialogResult.Completed(true), topTask.await())

        // 下の1枚は表示されたまま結果未確定であることを、待ち合わせずに観察する
        assertTrue(harness.waitForPresentedContainers(1))
        assertEquals(1, recorder.results.size)

        recorder.notifiers[0].complete(false)
        assertEquals(DialogResult.Completed(false), bottomTask.await())
    }

    @Test
    fun PB_MD_02_2枚重ねて上から順に閉じる() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val (bottomTask, topTask) = presentTwoDialogs(harness, recorder)

        // 2枚を異なる結果値で閉じ、結果チャネルの取り違えがないことまで観察する
        recorder.notifiers[1].complete(true)
        assertEquals(DialogResult.Completed(true), topTask.await())

        assertTrue(harness.waitForPresentedContainers(1))
        recorder.notifiers[0].complete(false)
        assertEquals(DialogResult.Completed(false), bottomTask.await())

        assertTrue(harness.waitForPresentedContainers(0))
        Unit
    }

    @Test
    fun PB_MD_03_重ね出し中の外側タップは手前のみに届く() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val (bottomTask, topTask) = presentTwoDialogs(harness, recorder)

        harness.topmostContainer?.reportOutsideTap()
        assertEquals(DialogResult.Cancelled, topTask.await())

        // 下の1枚は表示されたまま結果未確定であることを、待ち合わせずに観察する
        assertTrue(harness.waitForPresentedContainers(1))
        assertEquals(1, recorder.results.size)

        recorder.notifiers[0].complete(false)
        assertEquals(DialogResult.Completed(false), bottomTask.await())
    }

    @Test
    fun PB_MD_04_下の段を先に閉じたときの挙動() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val (bottomTask, topTask) = presentTwoDialogs(harness, recorder)

        // 下 (先に出した) の結果報告口へ先に完了報告する
        recorder.notifiers[0].complete(false)

        // 保証されるのは「各 show が自分の結果値で独立に確定すること」だけ
        assertEquals(DialogResult.Completed(false), bottomTask.await())

        // 保証範囲外の挙動 (どちらの器が残るか・上の show がどうなるか) は観察して記録する
        val topFinishedWithoutReport = DialogTestWaiting.waitUntil(timeoutMillis = 500) {
            recorder.results.size == 2
        }
        println(
            "下先閉じの実挙動: 上の show は報告なしで完了したか=$topFinishedWithoutReport / " +
                "残っている器の枚数=${harness.presentedContainers.size}",
        )

        // 上の show は結果報告を受ければ確定する
        recorder.notifiers[1].complete(true)
        assertEquals(DialogResult.Completed(true), topTask.await())
    }
}
