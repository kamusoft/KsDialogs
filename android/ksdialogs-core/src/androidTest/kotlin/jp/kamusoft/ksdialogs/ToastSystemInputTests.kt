package jp.kamusoft.ksdialogs

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogScreenshotEvidence
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastInputTestActivity
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * タッチもフォーカスも奪わない全画面透過ウィンドウが、ソフトキーボードとシステムの
 * 戻る・ホームに干渉しないことを実機・エミュレータで確かめる。
 *
 * この組み合わせはこのライブラリでは Toast の器だけが使うため、判定は
 * 「Toast を出す前と後で、IME の出し入れと戻る・ホームの届き方が変わらないこと」で行う。
 * 戻る・ホームは実機のジェスチャと同じ経路を計測から起こせないので、支援技術の全体操作
 * (システムが受け取る戻る・ホームと同じ入口) を使う。
 */
@RunWith(AndroidJUnit4::class)
class ToastSystemInputTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ToastInputTestActivity> =
        ActivityScenarioRule(ToastInputTestActivity::class.java)

    @Test
    fun Toast_表示中でも_IME_を出し入れできる() = runBlocking<Unit> {
        // IME が出ているかどうかを直接読めるのは API 30 以降。それ以前は判定材料が無い
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val harness = ToastTestHarness(currentActivity())

        // Toast を出す前の振る舞いを基準にする
        showSoftInput()
        assertTrue("Toast を出す前から IME が出ない", awaitImeVisible(true))
        hideSoftInput()
        assertTrue(awaitImeVisible(false))

        harness.toast.show("IME と併存", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        DialogScreenshotEvidence.capture("toast-ime-idle")

        showSoftInput()
        assertTrue("Toast 表示中に IME が出ない", awaitImeVisible(true))
        DialogScreenshotEvidence.capture("toast-ime-shown")
        assertTrue("IME の表示で Toast が消えている", harness.isPresenting)
        assertTrue("入力欄がフォーカスを失っている", currentActivity().inputField.hasFocus())

        hideSoftInput()
        assertTrue("Toast 表示中に IME が引っ込まない", awaitImeVisible(false))
        DialogScreenshotEvidence.capture("toast-ime-hidden")
        assertTrue("IME の消滅で Toast が消えている", harness.isPresenting)

        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun Toast_表示中でも戻るとホームが通る() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        val backPressesBefore = currentActivity().backPressCount

        harness.toast.show("ジェスチャと併存", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                harness.contentViews.single().let { it.isAttachedToWindow && it.alpha == 1f }
            },
        )
        DialogScreenshotEvidence.capture("toast-gesture-before")

        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        assertTrue(
            "Toast 表示中に戻るが画面へ届かない",
            InstrumentedDialogWaiting.waitUntil {
                currentActivity().backPressCount == backPressesBefore + 1
            },
        )
        assertTrue("戻るで Toast が消えている", harness.isPresenting)
        DialogScreenshotEvidence.capture("toast-back-delivered")

        val stopsBefore = currentActivity().stopCount
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        assertTrue(
            "Toast 表示中にホームが通らない",
            InstrumentedDialogWaiting.waitUntil {
                currentActivity().stopCount == stopsBefore + 1
            },
        )

        assertEquals(
            "ホームで戻るが余分に届いている",
            backPressesBefore + 1,
            currentActivity().backPressCount,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    /** ソフトキーボードを出す。 */
    private suspend fun showSoftInput() = withContext(Dispatchers.Main) {
        val activity = currentActivity()
        activity.inputField.requestFocus()
        val manager = activity.getSystemService(InputMethodManager::class.java)
        manager.showSoftInput(activity.inputField, 0)
    }

    /** ソフトキーボードを引っ込める。 */
    private suspend fun hideSoftInput() = withContext(Dispatchers.Main) {
        val activity = currentActivity()
        val manager = activity.getSystemService(InputMethodManager::class.java)
        manager.hideSoftInputFromWindow(activity.inputField.windowToken, 0)
    }

    /** IME の見えが目的の状態になるまで待つ。 */
    private suspend fun awaitImeVisible(visible: Boolean): Boolean =
        InstrumentedDialogWaiting.waitUntil(IME_TIMEOUT_MILLIS) {
            val insets = readOnMain {
                currentActivity().window.decorView.rootWindowInsets
            }
            insets != null && insets.isVisible(WindowInsets.Type.ime()) == visible
        }

    /** 支援技術の全体操作を起こす。システムが受け取る戻る・ホームと同じ入口。 */
    private fun performGlobalAction(action: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue(
            "全体操作を起こせなかった ($action)",
            instrumentation.uiAutomation.performGlobalAction(action),
        )
        instrumentation.waitForIdleSync()
    }

    private fun <T> readOnMain(read: () -> T): T {
        val value = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value.set(read()) }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    private fun currentActivity(): ToastInputTestActivity {
        val activity = AtomicReference<ToastInputTestActivity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 8_000

        /** IME の出し入れを待つ上限 (ミリ秒)。 */
        const val IME_TIMEOUT_MILLIS = 8_000L
    }
}
