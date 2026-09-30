package jp.kamusoft.ksdialogs

import android.app.Activity
import jp.kamusoft.ksdialogs.support.TrackerTestDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 提示先 (resumed で、かつ描画された Activity) と、アプリが前面にいるかの追跡を確かめる。
 *
 * 描画とメッセージ周回は、テストが偽の実装から手で送る。
 */
@DisplayName("提示先の自動追跡")
class ResumedActivityTrackerTests {

    /** 入れ替わりの通知の回数を数える。 */
    private class ChangeCounter(tracker: ResumedActivityTracker) {
        var count = 0
            private set

        init {
            tracker.observeResumedChange { count += 1 }
        }
    }

    @Test
    fun `resumed になって描画された画面が提示先になる`() {
        val driver = TrackerTestDriver()
        val activity = Activity()

        driver.launch(activity)
        assertNull(driver.tracker.resumedActivity, "resumed でも描画の前は提示先にならない")

        driver.drawObserver.draw(activity)

        assertSame(activity, driver.tracker.resumedActivity)
    }

    @Test
    fun `描画の知らせが届いた時点で入れ替わりの購読者へ通知が届く`() {
        val driver = TrackerTestDriver()
        val activity = driver.launch()
        val counter = ChangeCounter(driver.tracker)

        driver.drawObserver.draw(activity)

        assertEquals(1, counter.count)
    }

    @Test
    fun `resume の時点で未描画なら描画を促す`() {
        val driver = TrackerTestDriver()
        val activity = driver.launch()

        assertEquals(listOf(activity), driver.drawObserver.requestedDraws)
    }

    @Test
    fun `start の通知を受けずに resume した画面も、描画を待って提示先になる`() {
        val driver = TrackerTestDriver()
        val activity = Activity()

        driver.tracker.onActivityResumed(activity)
        assertNull(driver.tracker.resumedActivity)

        driver.drawObserver.draw(activity)

        assertSame(activity, driver.tracker.resumedActivity)
    }

    @Test
    fun `start の時点で decorView が無かった画面は、未描画のまま resume した時点で観測を張り直し、描画で提示先になる`() {
        val driver = TrackerTestDriver()
        val activity = Activity()
        // 中身を置かずに開き、最初の resume の時点でもウィンドウが作られていない画面
        driver.drawObserver.withholdDecorView(activity)
        driver.launch(activity)
        driver.tracker.onActivityPaused(activity)
        assertEquals(0, driver.drawObserver.startedObservationCount, "decorView が無い間は観測が始まらない")

        // 上に開いた半透明の画面が閉じ、stop を経ずに resume する。この時点で decorView が作られている
        driver.drawObserver.provideDecorView(activity)
        driver.tracker.onActivityResumed(activity)
        assertEquals(1, driver.drawObserver.startedObservationCount, "resume の時点で観測を張り直す")
        assertSame(activity, driver.drawObserver.requestedDraws.last(), "張り直した後に描画を促す")

        driver.drawObserver.draw(activity)

        assertSame(activity, driver.tracker.resumedActivity, "描画された時点で提示先になる")
    }

    @Test
    fun `観測が始まっている未描画の画面は、resume で観測を張り直さない`() {
        val driver = TrackerTestDriver()
        val activity = driver.launch()
        driver.tracker.onActivityPaused(activity)

        driver.tracker.onActivityResumed(activity)

        assertEquals(1, driver.drawObserver.startedObservationCount, "start で張った観測をそのまま使う")
        assertEquals(1, driver.drawObserver.activeObservationCount)
    }

    @Test
    fun `画面が paused になると提示先がなくなる`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()

        driver.tracker.onActivityPaused(activity)

        assertNull(driver.tracker.resumedActivity)
    }

    @Test
    fun `画面が破棄されると提示先がなくなる`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()

        driver.tracker.onActivityDestroyed(activity)

        assertNull(driver.tracker.resumedActivity)
    }

    @Test
    fun `画面遷移で提示先が次の画面へ入れ替わるのは、次の画面が描画された時点`() {
        val driver = TrackerTestDriver()
        val previous = driver.launchAndDraw()
        val next = Activity()

        driver.tracker.onActivityPaused(previous)
        driver.launch(next)
        assertNull(driver.tracker.resumedActivity, "次の画面の描画の前は提示先が無い")

        driver.drawObserver.draw(next)

        assertSame(next, driver.tracker.resumedActivity)
    }

    @Test
    fun `画面の破棄が購読者へ届く`() {
        val tracker = TrackerTestDriver().tracker
        val activity = Activity()
        var destroyedCount = 0
        tracker.observeDestroy(activity) { destroyedCount++ }

        tracker.onActivityDestroyed(activity)

        assertEquals(1, destroyedCount)
    }

    @Test
    fun `購読を解除すると画面の破棄は届かない`() {
        val tracker = TrackerTestDriver().tracker
        val activity = Activity()
        var destroyedCount = 0
        val registration = tracker.observeDestroy(activity) { destroyedCount++ }

        registration.cancel()
        tracker.onActivityDestroyed(activity)

        assertEquals(0, destroyedCount)
    }

    @Test
    fun `別の画面の破棄は購読者へ届かない`() {
        val tracker = TrackerTestDriver().tracker
        var destroyedCount = 0
        tracker.observeDestroy(Activity()) { destroyedCount++ }

        tracker.onActivityDestroyed(Activity())

        assertEquals(0, destroyedCount)
    }

    @Test
    fun `前の画面の遅れた破棄通知では提示先を失わない`() {
        val driver = TrackerTestDriver()
        val previous = driver.launchAndDraw()
        driver.tracker.onActivityPaused(previous)
        val next = driver.launchAndDraw()
        driver.tracker.onActivityStopped(previous)

        // 前の画面の破棄は次の画面が前面に出た後から届く
        driver.tracker.onActivityDestroyed(previous)

        assertSame(next, driver.tracker.resumedActivity)
    }

    @Test
    fun `PB-HA-05 pause だけから復帰した Activity は、再描画を待たずに提示先に戻る`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()
        driver.tracker.onActivityPaused(activity)
        val counter = ChangeCounter(driver.tracker)

        // stop しないので描画は起きない
        driver.tracker.onActivityResumed(activity)

        assertSame(activity, driver.tracker.resumedActivity, "resume の時点で提示先に戻る")
        assertEquals(1, counter.count, "入れ替わりの通知が届く")
    }

    @Test
    fun `PB-HA-06 stop した Activity は、start の後に描画されるまで提示先にならない`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()
        driver.tracker.onActivityPaused(activity)
        driver.tracker.onActivityStopped(activity)

        driver.tracker.onActivityStarted(activity)
        driver.tracker.onActivityResumed(activity)
        assertNull(driver.tracker.resumedActivity, "描画されるまでは提示先が無い")
        val counter = ChangeCounter(driver.tracker)

        driver.drawObserver.draw(activity)

        assertSame(activity, driver.tracker.resumedActivity, "描画された時点で提示先になる")
        assertEquals(1, counter.count, "入れ替わりの通知が届く")
    }

    @Test
    fun `PB-HA-07 作成の通知だけが届いた Activity があれば、前面の待ちと判定される`() {
        val driver = TrackerTestDriver()
        assertFalse(driver.tracker.isInForeground, "Activity が 1 つも無い間は背面")

        driver.tracker.onActivityCreated(Activity(), null)

        assertTrue(driver.tracker.isInForeground, "前面と判定される")
        assertNull(driver.tracker.resumedActivity, "提示先は無い")
    }

    @Test
    fun `PB-HA-08 描画済みの Activity が pause しただけなら、前面のまま提示先を失う`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()

        driver.tracker.onActivityPaused(activity)
        driver.turnPoster.runNextTurn()

        assertNull(driver.tracker.resumedActivity, "提示先は無くなる")
        assertTrue(driver.tracker.isInForeground, "前面と判定される")
    }

    @Test
    fun `PB-HA-09 前面の Activity がすべて stop すると背面になり、通知が届く`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()
        driver.tracker.onActivityPaused(activity)
        val counter = ChangeCounter(driver.tracker)

        driver.tracker.onActivityStopped(activity)
        assertTrue(driver.tracker.isInForeground, "stop の通知の直後はまだ背面と確定しない")
        assertEquals(0, counter.count)

        driver.turnPoster.runNextTurn()

        assertFalse(driver.tracker.isInForeground, "次の周回で背面と判定される")
        assertEquals(1, counter.count, "入れ替わりの購読者へ通知が届く")
    }

    @Test
    fun `背面から start した Activity があれば前面に戻る`() {
        val driver = TrackerTestDriver()
        val activity = driver.launchAndDraw()
        driver.tracker.onActivityPaused(activity)
        driver.tracker.onActivityStopped(activity)
        driver.turnPoster.runNextTurn()
        assertFalse(driver.tracker.isInForeground)

        driver.tracker.onActivityStarted(activity)

        assertTrue(driver.tracker.isInForeground)
    }

    @Test
    fun `PB-HA-10 描画が印になる前に stop した Activity は、その描画では提示先にならない`() {
        val driver = TrackerTestDriver()
        val activity = driver.launch()
        // 描画は受けたが、印を立てる次の周回はまだ来ていない
        driver.drawObserver.receiveDraw(activity)

        driver.tracker.onActivityPaused(activity)
        driver.tracker.onActivityStopped(activity)
        driver.tracker.onActivityStarted(activity)
        driver.tracker.onActivityResumed(activity)
        driver.drawObserver.deliverReceivedDraws()

        assertNull(driver.tracker.resumedActivity, "stop の前の描画は印にならず、提示先は無いまま")

        driver.drawObserver.draw(activity)

        assertSame(activity, driver.tracker.resumedActivity, "次の描画の後に提示先になる")
    }

    @Test
    fun `PB-HA-11 作り直しの間は背面と判定されず、新しい Activity が描画された時点で提示先になる`() {
        val driver = TrackerTestDriver()
        val previous = driver.launchAndDraw()
        val counter = ChangeCounter(driver.tracker)
        var sawBackground = false
        driver.tracker.observeResumedChange { if (!driver.tracker.isInForeground) sawBackground = true }

        // 構成の変更による作り直し: 旧 Activity の pause・stop・破棄と新 Activity の作成が同じメッセージの中で続く
        driver.tracker.onActivityPaused(previous)
        driver.tracker.onActivityStopped(previous)
        driver.tracker.onActivityDestroyed(previous)
        val next = driver.launch()
        driver.turnPoster.runNextTurn()

        assertTrue(driver.tracker.isInForeground, "背面と判定されない")
        assertFalse(sawBackground, "前面を離れた通知は届かない")
        assertNull(driver.tracker.resumedActivity, "新 Activity の描画の前は提示先が無い (前面の待ち)")
        val countBeforeDraw = counter.count

        driver.drawObserver.draw(next)

        assertSame(next, driver.tracker.resumedActivity, "新 Activity が描画された時点で提示先になる")
        assertEquals(countBeforeDraw + 1, counter.count, "描画の時点で入れ替わりの通知が届く")
    }
}
