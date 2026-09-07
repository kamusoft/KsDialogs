package jp.kamusoft.ksdialogs

import android.app.Activity
import android.graphics.Rect
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTouchInjection
import jp.kamusoft.ksdialogs.support.InteractiveToastContentView
import jp.kamusoft.ksdialogs.support.TapCountingView
import jp.kamusoft.ksdialogs.support.ToastLayoutObservation
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Toast の完全非対話と非モーダル (core/ADR-0031) を、実際のタップの注入で確かめる。
 * iOS Native の ToastNonModalTests のミラー。
 *
 * Toast のウィンドウはフォーカスもタッチも奪わないため、Toast の面へのタップは
 * 背後のページ要素へそのまま届き、カスタム View 内に置いた対話部品も反応しない。
 * タップで消えることもなく、消滅の契機は duration の経過だけである。
 */
@RunWith(AndroidJUnit4::class)
class ToastNonModalTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_NM_01_Toast_の真下の要素が操作できる() = runBlocking<Unit> {
        val background = installBackgroundTapTarget()
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("素通し", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val toastRect = ToastLayoutObservation.contentRectOnScreen(container)

        tapCenterOf(toastRect)

        assertEquals("背後の要素が反応していない", 1, background.tapCount)
        assertTrue("Toast はタップでは消えない", harness.isPresenting)
        assertTrue("duration まで表示され続ける", harness.waitUntilEmpty())
    }

    @Test
    fun TS_NM_02_カスタム_View_内の対話部品は反応しない() = runBlocking<Unit> {
        val background = installBackgroundTapTarget()
        val harness = ToastTestHarness(currentActivity())
        val content = AtomicReference<InteractiveToastContentView>()
        harness.registry.register(ToastTestViewModel::class) { _ ->
            InteractiveToastContentView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
                .also(content::set)
        }

        harness.toast.show(ToastTestViewModel(), durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val toastRect = ToastLayoutObservation.contentRectOnScreen(container)

        tapCenterOf(toastRect)

        assertEquals(
            "カスタム View 内の対話部品が反応している",
            0,
            requireNotNull(content.get()).tapCount,
        )
        assertEquals("タッチが背後へ素通しされていない", 1, background.tapCount)
        assertTrue("Toast はタップでは消えない", harness.isPresenting)
        assertTrue("duration まで表示され続ける", harness.waitUntilEmpty())
    }

    @Test
    fun Toast_のウィンドウはフォーカスもタッチも受け取らない() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("非モーダル", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val attributes = requireNotNull(harness.containers.single().window).attributes

        assertNotEquals(
            "フォーカスを奪わない指定がない",
            0,
            attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        )
        assertNotEquals(
            "タッチを受け取らない指定がない",
            0,
            attributes.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        )
        assertEquals(
            "背後を暗転させる指定が残っている",
            0,
            attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        )

        assertTrue(harness.waitUntilEmpty())
    }

    /** 画面いっぱいのタップ受けを提示先に敷き、Toast の真下の要素にする。 */
    private fun installBackgroundTapTarget(): TapCountingView {
        val target = AtomicReference<TapCountingView>()
        activityRule.scenario.onActivity { activity ->
            val view = TapCountingView(activity)
            activity.hostContainer.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            target.set(view)
        }
        return requireNotNull(target.get())
    }

    private fun tapCenterOf(rect: Rect) {
        DialogTouchInjection.tap(rect.exactCenterX(), rect.exactCenterY())
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 6_000

        /** カスタム Toast View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160
    }
}
