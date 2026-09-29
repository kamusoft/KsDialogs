package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 閉鎖の通知が、どの結末でもちょうど1回だけ届くことの検証。
 *
 * 通知が届かないと MAUI facade 側が結果を待ち続けるため、
 * 想定していない失敗まで含めて通知に変換されることを見る。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("閉鎖の通知はちょうど1回だけ届く")
class MauiDialogClosureReportTests {

    /** 通知先へ届いた呼び出しを順に記録するもの。 */
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

    @Test
    fun `完了は閉鎖要求による閉鎖として通知される`() = runTest {
        val listener = RecordingListener()

        reportClosure(listener) { DialogResult.Completed(true) }

        assertEquals(listOf("dismissed"), listener.closures)
    }

    @Test
    fun `閉鎖の通知は表示が結果を返すまで届かない`() = runTest {
        val listener = RecordingListener()
        // Native ライブラリの show は退出の演出・覆いの消滅・器の撤去がすべて済んでから返る。
        // その待ちをこの合図で模し、通知が配送の時点まで持ち越されることを見る (core/ADR-0017)
        val removed = CompletableDeferred<Unit>()

        val reporting = launch {
            reportClosure(listener) {
                removed.await()
                DialogResult.Completed(true)
            }
        }
        testScheduler.runCurrent()
        val beforeRemoval = listener.closures.toList()
        removed.complete(Unit)
        reporting.join()

        assertEquals(emptyList<String>(), beforeRemoval, "撤去が済むまでは通知を出さないこと")
        assertEquals(listOf("dismissed"), listener.closures, "撤去が済んだら通知が1回だけ届くこと")
    }

    @Test
    fun `キャンセルは利用者操作による閉鎖として通知される`() = runTest {
        val listener = RecordingListener()

        reportClosure(listener) { DialogResult.Cancelled }

        assertEquals(listOf("cancelled"), listener.closures)
    }

    @Test
    fun `中身の生成が例外を投げても失敗として通知される`() = runTest {
        val listener = RecordingListener()

        reportClosure(listener) { throw IllegalArgumentException("テーマが要件を満たしていません") }

        assertEquals(listOf("failed:テーマが要件を満たしていません"), listener.closures)
    }

    @Test
    fun `コルーチンのキャンセルは cancelled として通知してから伝播する`() = runTest {
        val listener = RecordingListener()

        val reporting = launch {
            reportClosure(listener) { awaitCancellation() }
        }
        testScheduler.runCurrent()
        reporting.cancel(CancellationException("呼び出し元が打ち切りました"))
        reporting.join()

        assertEquals(listOf("cancelled"), listener.closures, "cancelled がちょうど1回届くこと")
        assertTrue(reporting.isCancelled, "通知のあとキャンセルが伝播すること")
    }

    @Test
    fun `中身を作る前の閉鎖要求で止めた show は閉鎖要求として通知される`() = runTest {
        val listener = RecordingListener()

        val reporting = launch {
            reportClosure(listener, stoppedByDismissal = { true }) { awaitCancellation() }
        }
        testScheduler.runCurrent()
        reporting.cancel()
        reporting.join()

        assertEquals(listOf("dismissed"), listener.closures)
        assertTrue(reporting.isCancelled)
    }
}
