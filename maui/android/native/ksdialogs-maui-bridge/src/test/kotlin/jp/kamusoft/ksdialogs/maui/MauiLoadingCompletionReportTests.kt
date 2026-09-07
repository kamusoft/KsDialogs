package jp.kamusoft.ksdialogs.maui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Loading の完了の通知が、どの結末でもちょうど1回だけ届くことの検証。
 *
 * 通知が届かないと MAUI facade 側が待ち続けるため、
 * 想定していない失敗まで含めて通知に変換されることを見る。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Loading の完了の通知はちょうど1回だけ届く")
class MauiLoadingCompletionReportTests {

    /** 通知先へ届いた呼び出しを順に記録するもの。 */
    private class RecordingListener : MauiLoadingCompletionListener {
        val completions: MutableList<String> = mutableListOf()

        override fun onCompleted() {
            completions += "completed"
        }

        override fun onFailure(message: String?) {
            completions += "failure:$message"
        }
    }

    @Test
    @DisplayName("[LD-MA-03] 完了した操作は完了として1回だけ通知される")
    fun `完了した操作は完了として1回だけ通知される`() = runTest {
        val listener = RecordingListener()

        reportLoadingCompletion(listener) { }

        assertEquals(listOf("completed"), listener.completions)
    }

    @Test
    @DisplayName("[LD-MA-03] 完了の通知は撤去が済むまで届かない")
    fun `完了の通知は撤去が済むまで届かない`() = runTest {
        val listener = RecordingListener()
        // 合流最後の 1 件は出の演出・覆いの消滅・器の撤去がすべて済んでから返る。
        // その待ちをこの合図で模し、通知が撤去の後まで持ち越されることを見る (core/ADR-0017)
        val removed = CompletableDeferred<Unit>()

        val reporting = launch {
            reportLoadingCompletion(listener) { removed.await() }
        }
        testScheduler.runCurrent()
        val beforeRemoval = listener.completions.toList()
        removed.complete(Unit)
        reporting.join()

        assertEquals(emptyList<String>(), beforeRemoval, "撤去が済むまでは通知を出さないこと")
        assertEquals(listOf("completed"), listener.completions, "撤去が済んだら通知が1回だけ届くこと")
    }

    @Test
    @DisplayName("[LD-MA-03] 中身の生成が例外を投げても失敗として1回だけ通知される")
    fun `中身の生成が例外を投げても失敗として1回だけ通知される`() = runTest {
        val listener = RecordingListener()

        reportLoadingCompletion(listener) {
            throw IllegalStateException("中身を組み立てられませんでした")
        }

        assertEquals(listOf("failure:中身を組み立てられませんでした"), listener.completions)
    }

    @Test
    @DisplayName("[LD-MA-03] コルーチンのキャンセルは通知に変換されずに伝播する")
    fun `コルーチンのキャンセルは通知に変換されずに伝播する`() = runTest {
        val listener = RecordingListener()

        assertThrows<CancellationException> {
            reportLoadingCompletion(listener) {
                throw CancellationException("呼び出し元がキャンセルされました")
            }
        }

        assertEquals(emptyList<String>(), listener.completions)
    }
}
