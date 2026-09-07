package jp.kamusoft.ksdialogs.maui

import android.content.ContextWrapper
import android.view.View
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * MAUI 側の演出を実行する口の検証 (core/ADR-0017)。
 *
 * この面に課されるのは「供給された実行口を呼ぶこと」と「完了をちょうど1回だけ返すこと」だけで、
 * 演出そのものの駆動と完了待ちは Native ライブラリの器が受け持つ。
 */
@DisplayName("MAUI 側の演出の実行口")
class MauiDialogTransitionRunnerTests {

    private class CompletionRecorder : Runnable {
        var count: Int = 0
            private set

        override fun run() {
            count++
        }
    }

    private fun content(): MauiDialogContent = MauiDialogContent(
        view = View(ContextWrapper(null)),
        options = MauiDialogOptions(),
        placement = MauiDialogPlacement(),
    )

    @Test
    fun `供給された出現の実行口がホスト View を引数に呼ばれる`() {
        val content = content()
        val hostView = View(ContextWrapper(null))
        var receivedView: View? = null
        content.installTransition(
            presentation = { view, completion ->
                receivedView = view
                completion.run()
            },
            dismissal = null,
            overlayDurationMillis = null,
        )

        val recorder = CompletionRecorder()
        content.runPresentation(hostView, recorder)

        assertSame(hostView, receivedView)
        assertEquals(1, recorder.count)
    }

    @Test
    fun `完了通知が何度届いても完了はちょうど1回になる`() {
        val content = content()
        content.installTransition(
            presentation = null,
            dismissal = { _, completion ->
                completion.run()
                completion.run()
                completion.run()
            },
            overlayDurationMillis = null,
        )

        val recorder = CompletionRecorder()
        content.runDismissal(View(ContextWrapper(null)), recorder)

        assertEquals(1, recorder.count)
    }

    @Test
    fun `実行口が供給されていない側はその場で完了する`() {
        val content = content()
        content.installTransition(presentation = null, dismissal = null, overlayDurationMillis = null)

        val presentationRecorder = CompletionRecorder()
        val dismissalRecorder = CompletionRecorder()
        content.runPresentation(View(ContextWrapper(null)), presentationRecorder)
        content.runDismissal(View(ContextWrapper(null)), dismissalRecorder)

        assertEquals(1, presentationRecorder.count)
        assertEquals(1, dismissalRecorder.count)
    }

    @Test
    fun `指定された覆いの時間はミリ秒のまま演出の組へ渡る`() {
        val content = content()
        content.installTransition(
            presentation = { _, completion -> completion.run() },
            dismissal = null,
            overlayDurationMillis = 400L,
        )

        val transition = content.composeTransition()

        assertNotNull(transition)
        assertEquals(400.milliseconds, transition?.overlayDuration)
        assertNotNull(transition?.presentation)
        assertNull(transition?.dismissal)
    }

    @Test
    fun `覆いの時間が渡されなければ器の既定に委ねられる`() {
        val content = content()
        content.installTransition(
            presentation = { _, completion -> completion.run() },
            dismissal = null,
            overlayDurationMillis = null,
        )

        assertNull(content.composeTransition()?.overlayDuration)
    }
}
