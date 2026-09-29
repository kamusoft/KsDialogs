package jp.kamusoft.ksdialogs

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.RecordingDialogPresentationSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 提示先を待っていた Dialog を、器を実際にウィンドウへ載せて観察する。
 *
 * 契約ロジックだけを見る JVM の `DialogHostWaitTests` との違いは、待ちが明けたあとの器が実際の
 * ウィンドウとして呼んだ順に重なることまで見る点にある。
 */
@RunWith(AndroidJUnit4::class)
class DialogHostWaitPresentationTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun PB_HW_06_待っていた2枚は呼んだ順にウィンドウへ載り_後から呼んだものが手前になる() = runBlocking<Unit> {
        val registry = DialogViewRegistry()
        val surface = RecordingDialogPresentationSurface().apply { hidesPresentationHost = true }
        val dialogs = Dialog(registry, surface)
        val notifiers = CopyOnWriteArrayList<DialogNotifier<Boolean>>()
        val createdMessages = CopyOnWriteArrayList<String>()
        registry.register(PlainTestDialogViewModel::class) { viewModel, notifier ->
            createdMessages.add(viewModel.message)
            notifiers.add(notifier)
            val size = (CONTENT_SIZE_DP * resources.displayMetrics.density).toInt()
            FixedContentSizeView(context = this, contentWidth = size, contentHeight = size)
        }

        val callerScope = CoroutineScope(Dispatchers.Main)
        try {
            // UI スレッドから、間に待ち合わせを挟まずに A、B の順で呼ぶ。
            // UNDISPATCHED で起動すると、show が最初に中断するまでを呼び出しの中で観察できる
            val (showA, showB, waitingCountAfterCalls) = withContext(Dispatchers.Main) {
                val a = callerScope.async(start = CoroutineStart.UNDISPATCHED) {
                    dialogs.show(PlainTestDialogViewModel("A"))
                }
                val b = callerScope.async(start = CoroutineStart.UNDISPATCHED) {
                    dialogs.show(PlainTestDialogViewModel("B"))
                }
                Triple(a, b, surface.hostWaitQueue.waitingCount)
            }
            assertEquals(
                "UI スレッドを離れずに列まで進み、呼び出しから戻る前に 2 件とも並んでいる",
                2,
                waitingCountAfterCalls,
            )
            assertTrue("提示先が無い間は中身を作らない", createdMessages.isEmpty())

            withContext(Dispatchers.Main) {
                surface.hidesPresentationHost = false
                surface.fireHostChange()
            }

            assertTrue(
                "2 枚とも表示される",
                InstrumentedDialogWaiting.waitUntil { surface.presentedContainers.size == 2 },
            )
            assertEquals("A、B の順に中身が作られる", listOf("A", "B"), createdMessages.toList())
            val bottom = surface.presentedContainers[0]
            val top = surface.presentedContainers[1]
            assertTrue(
                "両方の器がウィンドウに載る",
                InstrumentedDialogWaiting.waitUntil {
                    bottom.contentView.isAttachedToWindow && top.contentView.isAttachedToWindow
                },
            )
            assertTrue(
                "後から呼んだ B が手前 (入力の焦点を持つ) になる",
                InstrumentedDialogWaiting.waitUntil { top.window?.decorView?.hasWindowFocus() == true },
            )
            assertEquals("待つ表示が無くなれば購読を解除する", 0, surface.activeHostChangeRegistrationCount)

            notifiers[1].complete(true)
            assertEquals(DialogResult.Completed(true), withTimeout(RESULT_TIMEOUT_MILLIS) { showB.await() })
            notifiers[0].complete(false)
            assertEquals(DialogResult.Completed(false), withTimeout(RESULT_TIMEOUT_MILLIS) { showA.await() })
        } finally {
            callerScope.cancel()
        }
    }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 結果を待つ上限 (ミリ秒)。 */
        const val RESULT_TIMEOUT_MILLIS = 15_000L
    }
}
