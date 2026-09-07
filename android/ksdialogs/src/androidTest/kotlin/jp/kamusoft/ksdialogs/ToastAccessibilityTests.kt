package jp.kamusoft.ksdialogs

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Toast の支援技術への通知を確かめる。
 * iOS Native の ToastAccessibilityTests のミラー。
 *
 * デフォルト View はメッセージを読み上げへ流すが、フォーカスは移動させない。
 * カスタム View の読み上げ内容は View を供給するアプリの責務であり、器は関与しない。
 */
@RunWith(AndroidJUnit4::class)
class ToastAccessibilityTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_AC_01_デフォルト_View_はメッセージを_announce_しフォーカスを奪わない() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("保存しました", durationMs = SHORT_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())

        // 中身がウィンドウへ載るのは器を出した次のレイアウトパスなので、通知はそこまで待つ
        assertTrue(
            "メッセージが読み上げへ流れていない",
            InstrumentedDialogWaiting.waitUntil {
                harness.announcer.announcedMessages == listOf("保存しました")
            },
        )
        assertFalse(
            "支援技術のフォーカスを動かす通知が発行されている",
            harness.announcer.didMoveFocus,
        )
        assertEquals(
            "デフォルト View が支援技術の走査対象に残っている",
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            harness.defaultContentViews.single().importantForAccessibility,
        )

        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_AC_01_カスタム_View_では器が通知もフォーカス移動もしない() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        harness.toast.show(ToastTestViewModel(), durationMs = SHORT_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                harness.contentViews.single().isAttachedToWindow
            },
        )

        assertTrue(
            "カスタム View で器が通知を発行している",
            harness.announcer.posts.isEmpty(),
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    @Suppress("DEPRECATION")
    fun TS_AC_01_既定の通知口は支援技術が無効でも例外を投げない() {
        // OS の AccessibilityManager は支援技術が動いていないときの発行を例外で弾く。
        // 器はそれを踏まないこと (差し替え可能な通知口のテストだけでは踏み抜けを検出できない)
        activityRule.scenario.onActivity { activity ->
            val host = activity.findViewById<ViewGroup>(android.R.id.content)
            val source = View(activity)
            host.addView(source)
            SystemToastAccessibilityAnnouncer()
                .announce(source, AccessibilityEvent.TYPE_ANNOUNCEMENT, "保存しました")
            host.removeView(source)
        }
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** カスタム Toast View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160

        /** 待ち時間を短く保つための表示時間 (ミリ秒)。 */
        const val SHORT_DURATION_MILLIS = 400
    }
}
