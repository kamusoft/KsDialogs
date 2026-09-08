package jp.kamusoft.ksdialogs.compose

import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.compose.support.CaptureNotifier
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeTransitionTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.awaitPresented
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 宣言的 UI の属性宣言で添付した出入りの演出が、器で採用されることを確かめる (core/ADR-0017)。
 *
 * 演出の順序と完了待ちそのものは従来 View 系の検証が受け持つため、
 * ここで見るのは供給の届き方と、フックが受け取るホスト View である。
 */
@RunWith(AndroidJUnit4::class)
class ComposeDialogTransitionTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun PB_AA_02_Compose_属性宣言での添付が器で採用される() = runBlocking {
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        val hookedView = CompletableDeferred<View>()
        val startedOnMainThread = AtomicBoolean(false)

        DialogViewRegistry.shared.registerCompose(ComposeTransitionTestDialogViewModel::class) { _, supplied ->
            KsDialogAttributes(
                transition = DialogTransition(
                    presentation = { hostView ->
                        startedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper())
                        hookedView.complete(hostView)
                    },
                ),
            )
            ObserveHostView(observation)
            Box(Modifier.size(CONTENT_SIZE_DP.dp, CONTENT_SIZE_DP.dp))
            CaptureNotifier(supplied, notifier)
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeTransitionTestDialogViewModel()) }
            val hostView = hookedView.awaitPresented()

            assertTrue("フックには Compose ホスト View が渡る", hostView is DialogComposeContentView)
            assertSame(
                "ホスト View は中身の組み立てを載せている View を包んでいる",
                hostView,
                observation.contentView.get().parent,
            )
            assertTrue("フックは UI スレッドで開始される", startedOnMainThread.get())

            notifier.awaitPresented().complete(true)
            assertEquals(
                DialogResult.Completed(true),
                withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() },
            )
        }
    }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160
    }
}
