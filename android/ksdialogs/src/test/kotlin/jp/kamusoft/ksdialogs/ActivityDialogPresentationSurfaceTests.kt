package jp.kamusoft.ksdialogs

import android.app.Activity
import android.view.View
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("提示先の画面が破棄されたときの結果確定")
class ActivityDialogPresentationSurfaceTests {

    private class PresentationFixture {
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        val surface = ActivityDialogPresentationSurface(tracker, tracker)
        val resultChannel = DialogResultChannel()
        val outcomes = mutableListOf<DialogOutcome>()

        init {
            tracker.onActivityResumed(activity)
            resultChannel.onSettle { outcomes.add(it) }
        }

        fun present(): PresentedDialog = surface.present(
            DialogPresentationRequest(
                createContentView = { context -> View(context) },
                resultChannel = resultChannel,
            ),
        )
    }

    @Test
    fun `画面が破棄されると未確定の結果は cancelled になる`() {
        val fixture = PresentationFixture()
        fixture.present()

        // 画面の破棄では閉鎖の通知が届かず、ウィンドウが取り除かれるだけになる
        fixture.tracker.onActivityDestroyed(fixture.activity)

        assertEquals(1, fixture.outcomes.size)
        assertTrue(fixture.outcomes.first() is DialogOutcome.Cancelled)
    }

    @Test
    fun `画面が破棄されても確定済みの結果は変わらない`() {
        val fixture = PresentationFixture()
        fixture.present()
        DialogNotifier<Boolean>(fixture.resultChannel).complete(true)

        fixture.tracker.onActivityDestroyed(fixture.activity)

        assertEquals(1, fixture.outcomes.size)
        assertEquals(true, (fixture.outcomes.first() as DialogOutcome.Completed).value)
    }

    @Test
    fun `別の画面の破棄では結果が確定しない`() {
        val fixture = PresentationFixture()
        fixture.present()

        fixture.tracker.onActivityDestroyed(Activity())

        assertTrue(fixture.outcomes.isEmpty())
    }

    @Test
    fun `結果を確定して呼び出し元が待つのをやめた後の画面破棄は結果を変えない`() {
        val fixture = PresentationFixture()
        fixture.present()
        DialogNotifier<Boolean>(fixture.resultChannel).complete(true)
        fixture.resultChannel.cancelFromCaller()

        fixture.tracker.onActivityDestroyed(fixture.activity)

        assertEquals(1, fixture.outcomes.size)
        assertEquals(true, (fixture.outcomes.first() as DialogOutcome.Completed).value)
    }
}
