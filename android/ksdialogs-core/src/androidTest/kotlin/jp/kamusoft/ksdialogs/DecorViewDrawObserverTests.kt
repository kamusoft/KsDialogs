package jp.kamusoft.ksdialogs

import android.app.Activity
import android.os.SystemClock
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 既定の描画の観測 ([DecorViewDrawObserver]) が、実物の画面の描画を取りこぼさないことを確かめる。
 */
@RunWith(AndroidJUnit4::class)
class DecorViewDrawObserverTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun decorView_を次の周回で取り直して観測を張ったときは_最初の描画が済んでいても描画を促して知らせる() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = currentActivity()
        // 最初の描画が済み、画面が静止した状態にする (以後は何もしなければ描画が起きない)
        awaitSettledOnScreen(activity)
        val lookups = AtomicInteger(0)
        val drawn = CountDownLatch(1)
        val registration = AtomicReference<ActivityDrawRegistration>()

        instrumentation.runOnMainSync {
            // 観測を張った時点では decorView が無く、次の周回の取り直しで初めて得られる画面を再現する
            val observer = DecorViewDrawObserver(
                decorViewOf = { target: Activity ->
                    if (lookups.getAndIncrement() == 0) null else target.window?.peekDecorView()
                },
            )
            val observation = observer.observeNextDraw(activity) { drawn.countDown() }
            registration.set(observation)
            assertFalse("decorView が無い間は観測が始まらない", observation.isObserving)
        }

        try {
            assertTrue(
                "取り直して観測を張った後の描画が知らされない",
                drawn.await(DRAW_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
            assertTrue("取り直した後は観測が始まっている", registration.get().isObserving)
        } finally {
            instrumentation.runOnMainSync { registration.get()?.cancel() }
        }
    }

    @Test
    fun decorView_がある画面では_観測を張った後の描画を知らせる() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = currentActivity()
        awaitSettledOnScreen(activity)
        val drawn = CountDownLatch(1)
        val registration = AtomicReference<ActivityDrawRegistration>()

        instrumentation.runOnMainSync {
            val observer = DecorViewDrawObserver()
            val observation = observer.observeNextDraw(activity) { drawn.countDown() }
            registration.set(observation)
            assertTrue("decorView があれば観測が始まる", observation.isObserving)
            observer.requestDraw(activity)
        }

        try {
            assertTrue("促した描画が知らされない", drawn.await(DRAW_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync { registration.get()?.cancel() }
        }
    }

    /**
     * 画面が利用者に見え (ウィンドウがフォーカスを得て)、描画が静止するまで待つ。
     *
     * 開く演出の途中はウィンドウの表示面が整っておらず、促した描画が行われないことがある。
     */
    private fun awaitSettledOnScreen(activity: Activity) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + WINDOW_FOCUS_TIMEOUT_MILLIS
        var hasFocus = false
        while (!hasFocus && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync { hasFocus = activity.hasWindowFocus() }
            if (!hasFocus) SystemClock.sleep(POLLING_INTERVAL_MILLIS)
        }
        assertTrue("画面がフォーカスを得ない", hasFocus)
        instrumentation.waitForIdleSync()
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 描画の知らせを待つ上限 (秒)。静止した画面では、促さない限りこの間に描画は起きない。 */
        const val DRAW_TIMEOUT_SECONDS = 3L

        /** 画面がフォーカスを得るまで待つ上限 (ミリ秒)。 */
        const val WINDOW_FOCUS_TIMEOUT_MILLIS = 5_000L

        /** フォーカスを確かめ直す間隔 (ミリ秒)。 */
        const val POLLING_INTERVAL_MILLIS = 20L
    }
}
