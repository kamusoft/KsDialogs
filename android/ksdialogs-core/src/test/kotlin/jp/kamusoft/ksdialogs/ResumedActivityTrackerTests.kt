package jp.kamusoft.ksdialogs

import android.app.Activity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("提示先の自動追跡")
class ResumedActivityTrackerTests {

    @Test
    fun `resumed になった画面が提示先になる`() {
        val tracker = ResumedActivityTracker()
        val activity = Activity()

        tracker.onActivityResumed(activity)

        assertSame(activity, tracker.resumedActivity)
    }

    @Test
    fun `画面が paused になると提示先がなくなる`() {
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        tracker.onActivityResumed(activity)

        tracker.onActivityPaused(activity)

        assertNull(tracker.resumedActivity)
    }

    @Test
    fun `画面が破棄されると提示先がなくなる`() {
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        tracker.onActivityResumed(activity)

        tracker.onActivityDestroyed(activity)

        assertNull(tracker.resumedActivity)
    }

    @Test
    fun `画面遷移で提示先が次の画面へ入れ替わる`() {
        val tracker = ResumedActivityTracker()
        val previous = Activity()
        val next = Activity()

        tracker.onActivityResumed(previous)
        tracker.onActivityPaused(previous)
        tracker.onActivityResumed(next)

        assertSame(next, tracker.resumedActivity)
    }

    @Test
    fun `画面の破棄が購読者へ届く`() {
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        var destroyedCount = 0
        tracker.observeDestroy(activity) { destroyedCount++ }

        tracker.onActivityDestroyed(activity)

        assertEquals(1, destroyedCount)
    }

    @Test
    fun `購読を解除すると画面の破棄は届かない`() {
        val tracker = ResumedActivityTracker()
        val activity = Activity()
        var destroyedCount = 0
        val registration = tracker.observeDestroy(activity) { destroyedCount++ }

        registration.cancel()
        tracker.onActivityDestroyed(activity)

        assertEquals(0, destroyedCount)
    }

    @Test
    fun `別の画面の破棄は購読者へ届かない`() {
        val tracker = ResumedActivityTracker()
        var destroyedCount = 0
        tracker.observeDestroy(Activity()) { destroyedCount++ }

        tracker.onActivityDestroyed(Activity())

        assertEquals(0, destroyedCount)
    }

    @Test
    fun `前の画面の遅れた破棄通知では提示先を失わない`() {
        val tracker = ResumedActivityTracker()
        val previous = Activity()
        val next = Activity()
        tracker.onActivityResumed(previous)
        tracker.onActivityPaused(previous)
        tracker.onActivityResumed(next)

        // 前の画面の破棄は次の画面が前面に出た後から届く
        tracker.onActivityDestroyed(previous)

        assertSame(next, tracker.resumedActivity)
    }
}
