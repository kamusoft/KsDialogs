package jp.kamusoft.ksdialogs

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 画面が作り直されたときに、多重表示の Toast が起動順のまま新しい画面へ載せ直されることを確かめる。
 *
 * Android の回転では Activity が作り直され、器のウィンドウも失われる。表示のリストと中身は
 * coordinator が持ち、器だけを使い捨てにして表示を継続させる (core/ADR-0027 と同じ機構)。
 * 期限は受理時点から数えているので、載せ直しでも残り時間は巻き戻らない。
 */
@RunWith(AndroidJUnit4::class)
class ToastActivityRecreationTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_AN_03_画面の再生成で多重_Toast_が起動順のまま再取り付けされる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("1枚目", durationMs = DURATION_MILLIS)
        harness.toast.show("2枚目", durationMs = DURATION_MILLIS)
        harness.toast.show("3枚目", durationMs = DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting(count = 3))
        val containersBefore = harness.containers.toList()
        val contentViewsBefore = harness.contentViews.toList()
        assertEquals(
            listOf("1枚目", "2枚目", "3枚目"),
            harness.defaultContentViews.map { it.displayedText },
        )

        delay(HALFWAY_MILLIS)
        activityRule.scenario.recreate()
        withContext(Dispatchers.Main) { harness.changeHost(currentActivity()) }

        assertTrue(
            "再生成後に3枚とも載せ直されていない",
            InstrumentedDialogWaiting.waitUntil { harness.containers.size == 3 },
        )
        assertEquals(
            "重なり順が起動順から変わっている",
            listOf("1枚目", "2枚目", "3枚目"),
            harness.defaultContentViews.map { it.displayedText },
        )
        assertEquals("中身はそのまま載せ替えられる", contentViewsBefore, harness.contentViews)
        harness.containers.forEachIndexed { index, container ->
            assertNotSame("器は作り直される", containersBefore[index], container)
        }

        assertTrue(
            "残り duration が巻き戻っている",
            InstrumentedDialogWaiting.waitUntil(REMAINING_ALLOWANCE_MILLIS) {
                harness.displayCount == 0
            },
        )
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 再生成をまたげる長さの表示時間 (ミリ秒)。 */
        const val DURATION_MILLIS = 4_000

        /** 画面を作り直す前に消費させる時間 (ミリ秒)。 */
        const val HALFWAY_MILLIS = 2_000L

        /** 残り時間が維持されていれば足りる待ちの上限 (ミリ秒)。 */
        const val REMAINING_ALLOWANCE_MILLIS = 3_000L
    }
}
