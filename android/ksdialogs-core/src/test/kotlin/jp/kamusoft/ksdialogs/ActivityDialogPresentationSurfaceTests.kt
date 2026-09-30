package jp.kamusoft.ksdialogs

import android.app.Activity
import android.view.View
import jp.kamusoft.ksdialogs.support.TrackerTestDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("提示先の画面が破棄されたときの結果確定")
class ActivityDialogPresentationSurfaceTests {

    private class PresentationFixture {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val activity = Activity()
        val surface = ActivityDialogPresentationSurface(tracker, tracker)
        val resultChannel = DialogResultChannel()
        val outcomes = mutableListOf<DialogOutcome>()

        init {
            driver.launchAndDraw(activity)
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

    @Test
    fun `提示先が得られず器を載せられないと、中身を作らずに cancelled を届ける`() {
        val tracker = TrackerTestDriver().tracker
        val surface = ActivityDialogPresentationSurface(tracker, tracker, tracker, DialogHostWaitQueue())
        val resultChannel = DialogResultChannel()
        var contentCreations = 0
        val delivered = mutableListOf<DialogOutcome>()

        val presented = surface.present(
            DialogPresentationRequest(
                createContentView = { context ->
                    contentCreations += 1
                    View(context)
                },
                resultChannel = resultChannel,
            ),
        )
        presented.onDelivery { delivered.add(it) }

        assertEquals(0, contentCreations, "中身は作られない")
        assertEquals(listOf<DialogOutcome>(DialogOutcome.Cancelled), delivered, "器の消失と同じく cancelled が 1 回届く")
        assertEquals(DialogDismissalOrigin.HOST_LOST, resultChannel.resultOrigin)
    }
}
