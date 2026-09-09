package jp.kamusoft.ksdialogs

import android.accessibilityservice.AccessibilityService
import android.os.Build
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogScreenshotEvidence
import jp.kamusoft.ksdialogs.support.ImeSettleWaiting
import jp.kamusoft.ksdialogs.support.ImeWindowCleanup
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
        val activity = currentActivity()
        val harness = ToastTestHarness(activity)
        val ime = ImeSettleWaiting(activity, activity.inputField)
        ime.attach()
        try {
            // 画面の起動に伴うシステム側の要求が最初の表示要求と交差しないよう、先に落ち着かせる
            ime.assertIdleBeforeFirstRequest("画面の起動に伴う IME の出し入れが落ち着かない")

            // Toast を出す前の振る舞いを基準にする
            ime.requestShow()
            ime.assertSettled(true, "Toast を出す前から IME が出ない")
            ime.requestHide()
            ime.assertSettled(false, "Toast を出す前から IME が引っ込まない")

            harness.toast.show("IME と併存", durationMs = LONG_DURATION_MILLIS)
            assertTrue(harness.waitUntilPresenting())
            DialogScreenshotEvidence.capture("toast-ime-idle")

            ime.requestShow()
            ime.assertSettled(true, "Toast 表示中に IME が出ない")
            DialogScreenshotEvidence.capture("toast-ime-shown")
            assertTrue("IME の表示で Toast が消えている", harness.isPresenting)
            assertTrue("入力欄がフォーカスを失っている", currentActivity().inputField.hasFocus())

            ime.requestHide()
            ime.assertSettled(false, "Toast 表示中に IME が引っ込まない")
            DialogScreenshotEvidence.capture("toast-ime-hidden")
            assertTrue("IME の消滅で Toast が消えている", harness.isPresenting)

            detachToasts(harness)
            assertTrue("後始末で Toast の器が画面に残っている", awaitContainersDetached(harness))
        } finally {
            // 途中で落ちた回もウィンドウ・観測の口・IME の見えを残さない。
            // IME が出たまま抜けると、次のテストが最初に起こす操作をその IME が横取りする
            ime.restoreHidden()
            detachToasts(harness)
            ime.detach()
        }
    }

    @Test
    fun Toast_表示中でも戻るとホームが通る() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        // 前のテストが IME を出したまま抜けていると、最初の戻るを IME が食べる。
        // 順序への依存を受け側でも切る
        ImeWindowCleanup.ensureHidden(currentActivity(), currentActivity().inputField)
        val backPressesBefore = currentActivity().backPressCount

        try {
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

            detachToasts(harness)
            assertTrue("後始末で Toast の器が画面に残っている", awaitContainersDetached(harness))
        } finally {
            detachToasts(harness)
        }
    }

    /**
     * 検証を終えた Toast を画面から撤去する。
     *
     * 表示時間は IME の待ちの上限から独立させてあるので、期限切れを待つと試験時間が伸びる。
     * 提示先を外すと、表示は coordinator の中に残ったまま器のウィンドウだけが取り外される。
     */
    private suspend fun detachToasts(harness: ToastTestHarness) = withContext(Dispatchers.Main) {
        harness.changeHost(null)
    }

    /**
     * 器が画面から外れるまで待つ。
     *
     * 見るのは器の取り外しだけで、表示そのものが期限で終わることは見ない
     * (期限切れでの消滅は Toast の期限を扱う別のテストが担保する)。
     */
    private suspend fun awaitContainersDetached(harness: ToastTestHarness): Boolean =
        InstrumentedDialogWaiting.waitUntil { harness.containers.isEmpty() }

    /** 支援技術の全体操作を起こす。システムが受け取る戻る・ホームと同じ入口。 */
    private fun performGlobalAction(action: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue(
            "全体操作を起こせなかった ($action)",
            instrumentation.uiAutomation.performGlobalAction(action),
        )
        instrumentation.waitForIdleSync()
    }

    private fun currentActivity(): ToastInputTestActivity {
        val activity = AtomicReference<ToastInputTestActivity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 60_000
    }
}
