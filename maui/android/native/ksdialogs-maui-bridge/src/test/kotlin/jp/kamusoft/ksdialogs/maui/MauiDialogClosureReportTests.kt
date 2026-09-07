package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

        override fun onPresentationHostUnavailable(message: String?) {
            closures += "hostUnavailable:$message"
        }

        override fun onFailed(message: String?) {
            closures += "failed:$message"
        }
    }

    /** 提示先不在の検証にだけ使う ViewModel。他の検証と登録を取り合わないよう型を分ける。 */
    private class HostUnavailableTestViewModel : DialogViewModel<Boolean>

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
    fun `コルーチンのキャンセルは通知に変換されずに伝播する`() = runTest {
        val listener = RecordingListener()

        assertThrows<CancellationException> {
            reportClosure(listener) { throw CancellationException("呼び出し元がキャンセルされました") }
        }

        assertEquals(emptyList<String>(), listener.closures)
    }

    @Test
    fun `提示先不在はそれと分かる形で通知される`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val listener = RecordingListener()
            val dialogs = Dialog()
            // 提示先が無い状態では View factory は呼ばれずに失敗するため、中身は組み立てられない
            dialogs.registry.register(HostUnavailableTestViewModel::class) { _, _ ->
                error("提示先が無いので View factory は呼ばれない")
            }

            reportClosure(listener) { dialogs.show(HostUnavailableTestViewModel()) }

            assertEquals(1, listener.closures.size)
            assertTrue(
                listener.closures.single().startsWith("hostUnavailable:"),
                "提示先不在が失敗の通知に紛れました: ${listener.closures.single()}",
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}
