package jp.kamusoft.ksdialogs.maui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 提示先を待っている Dialog を、互換面の handle で打ち切る・閉じることの検証。
 *
 * 素の JVM 上には resumed な Activity が無いため、互換面から呼んだ Native の show は
 * 提示先の出現を待ち続ける。その待ちを handle で止めたときの閉鎖の通知と、中身の供給が
 * 呼ばれないことを見る。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("提示先を待っている Dialog は handle で止められる")
class MauiDialogCancellationTests {

    /** 閉鎖の通知先へ届いた呼び出しを順に記録するもの。 */
    private class RecordingListener : MauiDialogClosureListener {
        val closures: MutableList<String> = mutableListOf()

        override fun onCancelled() {
            closures += "cancelled"
        }

        override fun onDismissed() {
            closures += "dismissed"
        }

        override fun onFailed(message: String?) {
            closures += "failed:$message"
        }
    }

    /** 中身の供給が呼ばれた回数を数える供給元。中身は作らない (呼ばれたら失敗として見える)。 */
    private class CountingContentProvider : MauiDialogContentProvider {
        var callCount: Int = 0

        override fun createContent(): MauiDialogContent? {
            callCount++
            return null
        }
    }

    /** 互換面と Native の show が使う UI スレッドを、テストのスケジューラで動かす。 */
    private fun runOnTestMain(body: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            body()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @DisplayName("[PB-MC-05] Android のブリッジは、打ち切りを cancelled の閉鎖通知にしてから投げ直す")
    fun `PB-MC-05 Android のブリッジは、打ち切りを cancelled の閉鎖通知にしてから投げ直す`() = runOnTestMain {
        val listener = RecordingListener()
        val provider = CountingContentProvider()

        val presentation = MauiDialogBridge.shared.present(provider, listener)
        testScheduler.runCurrent()
        assertEquals(emptyList<String>(), listener.closures, "提示先が無くても失敗せずに待っていること")

        presentation.cancel()
        testScheduler.runCurrent()

        assertEquals(listOf("cancelled"), listener.closures, "cancelled がちょうど1回届くこと")
        assertEquals(0, provider.callCount, "中身の供給は呼ばれないこと")

        // 打ち切りのあとに閉鎖を求めても、通知は増えない
        presentation.dismiss()
        testScheduler.runCurrent()
        assertEquals(listOf("cancelled"), listener.closures)
    }

    @Test
    @DisplayName("[PB-MC-07] 中身を作る前にブリッジを閉じると、一度も表示されない")
    fun `PB-MC-07 中身を作る前にブリッジを閉じると、一度も表示されない`() = runOnTestMain {
        val listener = RecordingListener()
        val provider = CountingContentProvider()

        val presentation = MauiDialogBridge.shared.present(provider, listener)
        testScheduler.runCurrent()
        assertEquals(emptyList<String>(), listener.closures, "提示先を待っていること")

        // 結果が確定した show が閉じる経路と同じ操作
        presentation.dismiss()
        testScheduler.runCurrent()

        assertEquals(listOf("dismissed"), listener.closures, "閉鎖要求で閉じたことがちょうど1回届くこと")
        assertEquals(0, provider.callCount, "中身の供給は呼ばれないこと")
    }

    @Test
    @DisplayName("互換面を呼んだ直後に打ち切っても取りこぼさない")
    fun `互換面を呼んだ直後に打ち切っても取りこぼさない`() = runOnTestMain {
        val listener = RecordingListener()
        val provider = CountingContentProvider()

        // Native の show が走り出す前に打ち切る
        val presentation = MauiDialogBridge.shared.present(provider, listener)
        presentation.cancel()
        testScheduler.runCurrent()

        assertEquals(listOf("cancelled"), listener.closures)
        assertEquals(0, provider.callCount)
    }
}
