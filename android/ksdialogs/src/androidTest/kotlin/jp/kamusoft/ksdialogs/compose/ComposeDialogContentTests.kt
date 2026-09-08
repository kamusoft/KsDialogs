package jp.kamusoft.ksdialogs.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.compose.support.CaptureNotifier
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeContentTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeInlineTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.RecordOnce
import jp.kamusoft.ksdialogs.compose.support.TypedComposeTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.awaitPresented
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 宣言的 UI の中身を登録・表示したときに、結果の返り方と組み立ての破棄が
 * 従来 View 系と同じに成り立つことを確かめる。
 *
 * 提示は公開 API だけを使い、実際のダイアログのウィンドウを出す経路で行う。
 */
@RunWith(AndroidJUnit4::class)
class ComposeDialogContentTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun 登録した_Compose_コンテンツの完了操作で型付き結果が返る() = runBlocking {
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()

        DialogViewRegistry.shared.registerCompose(ComposeContentTestDialogViewModel::class) { _, supplied ->
            CaptureNotifier(supplied, notifier)
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeContentTestDialogViewModel()) }
            notifier.awaitPresented().complete(true)
            val result: DialogResult<Boolean> = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }

            assertEquals(DialogResult.Completed(true), result)
        }
    }

    @Test
    fun 登録した_Compose_コンテンツはカスタム結果型でも型付き結果を返す() = runBlocking {
        val notifier = CompletableDeferred<DialogNotifier<String>>()

        DialogViewRegistry.shared.registerCompose(TypedComposeTestDialogViewModel::class) { _, supplied ->
            CaptureNotifier(supplied, notifier)
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(TypedComposeTestDialogViewModel()) }
            notifier.awaitPresented().complete("入力された文字列")
            val result: DialogResult<String> = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }

            assertEquals(DialogResult.Completed("入力された文字列"), result)
        }
    }

    @Test
    fun 登録せずに渡した_Compose_コンテンツで表示と完了が成立する() = runBlocking {
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        val observation = ComposeContentObservation()

        coroutineScope {
            val showTask = async {
                Dialog.instance.showCompose(ComposeInlineTestDialogViewModel()) { _, supplied ->
                    ObserveHostView(observation)
                    CaptureNotifier(supplied, notifier)
                    Box(Modifier.size(CONTENT_SIZE_DP.dp))
                }
            }
            observation.firstDrawnRect.awaitPresented()
            notifier.awaitPresented().complete(true)
            val result = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }

            assertEquals(DialogResult.Completed(true), result)
        }
    }

    @Test
    fun 登録せずに渡した_Compose_コンテンツは既存の登録を使わず登録も変えない() = runBlocking {
        val builtContents = CopyOnWriteArrayList<String>()
        val registeredNotifier = CompletableDeferred<DialogNotifier<Boolean>>()
        DialogViewRegistry.shared.registerCompose(ComposeContentTestDialogViewModel::class) { _, supplied ->
            CaptureNotifier(supplied, registeredNotifier)
            RecordOnce(builtContents, "登録経由")
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        val inlineNotifier = CompletableDeferred<DialogNotifier<Boolean>>()
        coroutineScope {
            val showTask = async {
                Dialog.instance.showCompose(ComposeContentTestDialogViewModel()) { _, supplied ->
                    CaptureNotifier(supplied, inlineNotifier)
                    RecordOnce(builtContents, "インライン")
                    Box(Modifier.size(CONTENT_SIZE_DP.dp))
                }
            }
            inlineNotifier.awaitPresented().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
        }

        assertEquals("登録済みの中身が使われた", listOf("インライン"), builtContents.toList())

        // 登録内容はインライン表示の前後で変わらない
        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeContentTestDialogViewModel()) }
            registeredNotifier.awaitPresented().complete(false)
            assertEquals(
                DialogResult.Completed(false),
                withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() },
            )
        }
        assertEquals(listOf("インライン", "登録経由"), builtContents.toList())
    }

    @Test
    fun 登録せずに渡した_Compose_コンテンツは登録済みにしない() = runBlocking {
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()

        coroutineScope {
            val showTask = async {
                Dialog.instance.showCompose(ComposeInlineTestDialogViewModel()) { _, supplied ->
                    CaptureNotifier(supplied, notifier)
                    Box(Modifier.size(CONTENT_SIZE_DP.dp))
                }
            }
            notifier.awaitPresented().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
        }

        // 一時的にも登録しないため、あとからレジストリ経由で呼べば未登録の失敗になる
        val failure = runCatching { Dialog.instance.show(ComposeInlineTestDialogViewModel()) }.exceptionOrNull()
        assertTrue("未登録の失敗にならなかった: $failure", failure is DialogException.ViewFactoryNotRegistered)
    }

    @Test
    fun 完了で閉じたときに組み立てが破棄される() = runBlocking {
        val observation = presentAndClose { notifier -> notifier.complete(true) }

        assertTrue("完了で閉じても組み立てが破棄されない", awaitDisposal(observation))
    }

    @Test
    fun キャンセルで閉じたときに組み立てが破棄される() = runBlocking {
        val observation = presentAndClose { notifier -> notifier.cancel() }

        assertTrue("キャンセルで閉じても組み立てが破棄されない", awaitDisposal(observation))
    }

    @Test
    fun 呼び出し元キャンセルで閉じたときに組み立てが破棄される() = runBlocking {
        val observation = ComposeContentObservation()
        DialogViewRegistry.shared.registerCompose(ComposeContentTestDialogViewModel::class) { _, _ ->
            ObserveHostView(observation)
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = launch { Dialog.instance.show(ComposeContentTestDialogViewModel()) }
            observation.firstDrawnRect.awaitPresented()
            showTask.cancel()
            showTask.join()
        }

        assertTrue("呼び出し元キャンセルで閉じても組み立てが破棄されない", awaitDisposal(observation))
    }

    @Test
    fun 画面破棄で閉じたときに組み立てが破棄される() = runBlocking {
        val observation = ComposeContentObservation()
        DialogViewRegistry.shared.registerCompose(ComposeContentTestDialogViewModel::class) { _, _ ->
            ObserveHostView(observation)
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeContentTestDialogViewModel()) }
            observation.firstDrawnRect.awaitPresented()
            // 器を出したまま提示先の画面を畳む。ダイアログ側には閉鎖の通知が届かない経路になる
            activityRule.scenario.onActivity { activity -> activity.finish() }
            val result = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }

            assertEquals(DialogResult.Cancelled, result)
        }

        assertTrue("画面破棄で閉じても組み立てが破棄されない", awaitDisposal(observation))
    }

    /** Compose の中身でダイアログを1枚出し、渡された閉じ方で閉じてから観察結果を返す。 */
    private suspend fun presentAndClose(
        close: (DialogNotifier<Boolean>) -> Unit,
    ): ComposeContentObservation {
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        DialogViewRegistry.shared.registerCompose(ComposeContentTestDialogViewModel::class) { _, supplied ->
            ObserveHostView(observation)
            CaptureNotifier(supplied, notifier)
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeContentTestDialogViewModel()) }
            close(notifier.awaitPresented())
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
        }
        return observation
    }

    /** 組み立ての破棄が届くまで待つ。 */
    private fun awaitDisposal(observation: ComposeContentObservation): Boolean {
        val deadline = System.nanoTime() + PRESENTATION_TIMEOUT_MILLIS * 1_000_000
        while (System.nanoTime() < deadline) {
            if (observation.disposals.get() > 0) {
                return true
            }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return observation.disposals.get() > 0
    }

    private companion object {
        /** 中身が要求する大きさ (dp)。外形そのものは別の検証で測るため、ここでは値に意味を持たせない。 */
        const val CONTENT_SIZE_DP = 160

        const val POLL_INTERVAL_MILLIS = 16L
    }
}
