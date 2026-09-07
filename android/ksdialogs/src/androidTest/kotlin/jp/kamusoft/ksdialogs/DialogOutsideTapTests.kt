package jp.kamusoft.ksdialogs

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTouchInjection
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.attach
import jp.kamusoft.ksdialogs.support.observeFirstDrawRect
import jp.kamusoft.ksdialogs.support.outsidePointOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * ダイアログ外形の外側へのタップの扱いを、実際の入力を注入して確かめる。
 *
 * 覆いのヒットテストとクリック検出を通した経路でしか確かめられないため、器の関数は直に呼ばない。
 */
@RunWith(AndroidJUnit4::class)
class DialogOutsideTapTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 既定では外側タップで_cancelled_が返る() = runBlocking<Unit> {
        coroutineScope {
            val session = startShow(this, options = null)

            tapOutside(session.contentRect)

            assertEquals(
                DialogResult.Cancelled,
                withTimeout(RESULT_TIMEOUT_MILLIS) { session.result.await() },
            )
        }
    }

    @Test
    fun 透明な覆いでも外側タップの扱いは変わらない() = runBlocking<Unit> {
        coroutineScope {
            val session = startShow(this, options = DialogOptions(overlayColor = Color.TRANSPARENT))

            tapOutside(session.contentRect)

            assertEquals(
                DialogResult.Cancelled,
                withTimeout(RESULT_TIMEOUT_MILLIS) { session.result.await() },
            )
        }
    }

    @Test
    fun 中身の上のタップでは_cancelled_にならない() = runBlocking<Unit> {
        coroutineScope {
            val session = startShow(this, options = null)

            // 外側と同じ既定 (isCanceledOnTouchOutside = true) のまま、中身の中央を触る
            DialogTouchInjection.tap(
                session.contentRect.exactCenterX(),
                session.contentRect.exactCenterY(),
            )

            val settled = withTimeoutOrNull(NO_REACTION_WAIT_MILLIS) { session.result.await() }
            assertNull("中身の上のタップでは閉じない", settled)

            // 待っている show を残さないよう、結果を報告して片付ける
            session.notifier.complete(true)
            withTimeout(RESULT_TIMEOUT_MILLIS) { session.result.await() }
        }
    }

    @Test
    fun false_なら外側タップは無反応でモーダル性を保つ() = runBlocking<Unit> {
        coroutineScope {
            val backgroundTaps = AtomicInteger(0)
            addBackgroundTapRecorder(backgroundTaps)
            val session = startShow(this, options = DialogOptions(isCanceledOnTouchOutside = false))

            tapOutside(session.contentRect)

            val settled = withTimeoutOrNull(NO_REACTION_WAIT_MILLIS) { session.result.await() }
            assertNull("ダイアログは表示されたまま", settled)
            assertEquals("背後の画面の要素も反応しない", 0, backgroundTaps.get())
            assertFalse("器は閉じていない", session.result.isCompleted)

            // 待っている show を残さないよう、結果を報告して片付ける
            session.notifier.complete(true)
            withTimeout(RESULT_TIMEOUT_MILLIS) { session.result.await() }
        }
    }

    /** 提示中のダイアログ1枚と、その show の結果。 */
    private class ShowSession(
        val contentRect: Rect,
        val result: Deferred<DialogResult<Boolean>>,
        val notifier: DialogNotifier<Boolean>,
    )

    /** メタ属性を添付した中身で show を始め、最初に描かれた時点まで進める。 */
    private suspend fun startShow(scope: CoroutineScope, options: DialogOptions?): ShowSession {
        val registry = DialogViewRegistry()
        val dialogs = Dialog(registry, ActivityDialogPresentationSurface())
        val rectAtFirstDraw = CompletableDeferred<Rect>()
        val notifier = AtomicReference<DialogNotifier<Boolean>>()
        val contentView = AtomicReference<View>()

        registry.register(PlainTestDialogViewModel::class) { _, dialogNotifier ->
            notifier.set(dialogNotifier)
            createContentView(this).attach(options, null).also {
                contentView.set(it)
                it.observeFirstDrawRect(rectAtFirstDraw)
            }
        }

        val showTask = scope.async { dialogs.show(PlainTestDialogViewModel()) }
        val rect = withTimeout(RESULT_TIMEOUT_MILLIS) { rectAtFirstDraw.await() }
        awaitWindowFocus(requireNotNull(contentView.get()))
        return ShowSession(rect, showTask, notifier.get())
    }

    /**
     * ダイアログのウィンドウが入力を受け取れる状態になるまで待つ。
     *
     * 最初の描画はウィンドウが入力の宛先として登録されるより前に起こりうる。
     * その時点で入力を注入すると、まだ背後の画面が宛先のままで、外側タップが器へ届かない。
     */
    private suspend fun awaitWindowFocus(contentView: View) {
        val focused = InstrumentedDialogWaiting.waitUntil {
            val hasFocus = AtomicBoolean(false)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                hasFocus.set(contentView.rootView.hasWindowFocus())
            }
            hasFocus.get()
        }
        check(focused) { "ダイアログのウィンドウが入力を受け取れる状態にならなかった" }
    }

    /** 画面いっぱいには広がらない中身。外側にあたる余地を確実に残す。 */
    private fun createContentView(context: Context): View {
        val density = context.resources.displayMetrics.density
        return FixedContentSizeView(
            context = context,
            contentWidth = (CONTENT_SIZE_DP * density).toInt(),
            contentHeight = (CONTENT_SIZE_DP * density).toInt(),
        )
    }

    /** 提示先の画面に、タップを数える面を敷く。 */
    private fun addBackgroundTapRecorder(taps: AtomicInteger) {
        activityRule.scenario.onActivity { activity ->
            val recorder = View(activity).apply {
                isClickable = true
                setOnClickListener { taps.incrementAndGet() }
            }
            activity.hostContainer.addView(
                recorder,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    private fun tapOutside(contentRect: Rect) {
        val (x, y) = outsidePointOf(contentRect)
        DialogTouchInjection.tap(x, y)
    }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 結果を待つ上限 (ミリ秒)。 */
        const val RESULT_TIMEOUT_MILLIS = 10_000L

        /** 「何も起こらない」ことを確かめるための待ち (ミリ秒)。 */
        const val NO_REACTION_WAIT_MILLIS = 1_000L
    }
}
