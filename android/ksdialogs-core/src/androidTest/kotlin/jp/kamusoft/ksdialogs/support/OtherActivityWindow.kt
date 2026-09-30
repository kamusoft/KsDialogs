package jp.kamusoft.ksdialogs.support

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/**
 * 提示先とは別の Activity を前面に出し、元の Activity を残したまま観察できるようにする。
 *
 * `ActivityScenario.launch` は起動のたびに既存のタスクを片付けて元の Activity を破棄するため使わず、
 * 計測器から直接起動する。元の Activity は背面に回るだけで、ウィンドウは画面に載ったまま残る。
 */
internal class OtherActivityWindow private constructor(
    /** 前面に出した別の Activity。 */
    val activity: DialogLayoutTestActivity,
) {

    /** 別の Activity を閉じ、破棄されるまで待つ。 */
    fun close() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { activity.finish() }
        val deadline = System.nanoTime() + CLOSE_TIMEOUT_MILLIS * 1_000_000
        while (System.nanoTime() < deadline && !isDestroyed()) {
            instrumentation.waitForIdleSync()
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
    }

    private fun isDestroyed(): Boolean {
        var destroyed = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            destroyed = ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity) == Stage.DESTROYED
        }
        return destroyed
    }

    companion object {
        /** 別の Activity を前面に出し、再開するまで待つ。 */
        fun launch(): OtherActivityWindow {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val intent = Intent(instrumentation.targetContext, DialogLayoutTestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val activity = instrumentation.startActivitySync(intent) as DialogLayoutTestActivity
            instrumentation.waitForIdleSync()
            return OtherActivityWindow(activity)
        }

        private const val CLOSE_TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 16L
    }
}
