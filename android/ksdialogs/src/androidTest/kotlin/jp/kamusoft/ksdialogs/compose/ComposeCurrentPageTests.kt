package jp.kamusoft.ksdialogs.compose

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogCurrentPage
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.compose.support.CurrentPageComposeTestActivity
import jp.kamusoft.ksdialogs.compose.support.CurrentPageTestScreen
import jp.kamusoft.ksdialogs.compose.support.FixedSizeContentView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.ViewLayoutTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.rectOnScreen
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import jp.kamusoft.ksdialogs.support.InstrumentedStateSettling
import jp.kamusoft.ksdialogs.support.StateHistory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compose の `Modifier.markAsDialogCurrentPage()` が、実際の配置と離脱に追随して表示中のページを名乗ることを確かめる。
 *
 * 画面は `Scaffold` の content 枠と下部バーを模した縦並び ([CurrentPageComposeTestActivity])。
 * ダイアログは従来 View の中身で出し、基準領域だけを表示中のページにする。観察はすべて画面座標で行い、
 * 期待値は「名乗った枠の矩形と器のウィンドウの可視領域の共通部分」から dialogMargin (既定 0dp) を控除して導く。
 */
@RunWith(AndroidJUnit4::class)
class ComposeCurrentPageTests {

    private lateinit var scenario: ActivityScenario<CurrentPageComposeTestActivity>
    private lateinit var activity: CurrentPageComposeTestActivity

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(CurrentPageComposeTestActivity::class.java)
        val launched = AtomicReference<CurrentPageComposeTestActivity>()
        scenario.onActivity { launched.set(it) }
        activity = requireNotNull(launched.get())
        runBlocking { awaitRendered(CurrentPageTestScreen.MARKED) }
    }

    @After
    fun tearDown() {
        DialogCurrentPage.provider = null
        scenario.close()
    }

    @Test
    fun Scaffold_の_content_枠に付けると下部バーを避ける() = runBlocking<Unit> {
        val shown = showDialog()
        val frame = contentFrameRect()
        val bar = bottomBarRect()
        val expected = shown.region(frame)
        val content = shown.contentRect()

        assertNear("下端は content 枠の下端から dialogMargin 内側", expected.bottom - margin(), content.bottom)
        assertNear("右端は content 枠の右端から dialogMargin 内側", expected.right - margin(), content.right)
        assertTrue("下部バーと重ならない (中身 $content / 下部バー $bar)", content.bottom <= bar.top)
        shown.close()
    }

    @Test
    fun 遷移で枠が離脱すると台帳から外れ_VISIBLE_AREA_と一致する() = runBlocking<Unit> {
        showScreen(CurrentPageTestScreen.UNMARKED)

        val shown = showDialog()
        val visible = shown.visibleAreaRect()
        val content = shown.contentRect()

        assertNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
        assertNear("可視領域の右端から dialogMargin 内側", visible.right - margin(), content.right)
        shown.close()
    }

    @Test
    fun 戻る遷移で再び付けた画面が基準になる() = runBlocking<Unit> {
        showScreen(CurrentPageTestScreen.UNMARKED)
        showScreen(CurrentPageTestScreen.MARKED)

        val shown = showDialog()
        val expected = shown.region(contentFrameRect())

        assertNear("content 枠の下端から dialogMargin 内側", expected.bottom - margin(), shown.contentRect().bottom)
        shown.close()
    }

    @Test
    fun 入れ子は後から外側が配置されても内側が勝つ() = runBlocking<Unit> {
        showScreen(CurrentPageTestScreen.NESTED)
        showScreen(CurrentPageTestScreen.NESTED, marksOuterFrame = true)

        val shown = showDialog(placement = BOTH_START)
        val frame = contentFrameRect()
        val inset = toPixels(CurrentPageComposeTestActivity.NESTED_INSET_DP)
        val inner = Rect(frame.left, frame.top + inset, frame.right, frame.bottom - inset)
        val expected = shown.region(inner)

        assertNear("上端は内側の枠の上端から dialogMargin 内側", expected.top + margin(), shown.contentRect().top)
        shown.close()
    }

    @Test
    fun 窓の外に配置された枠は後から配置されても候補にならない() = runBlocking<Unit> {
        showScreen(CurrentPageTestScreen.WITH_OUTSIDE_FRAME)

        val shown = showDialog()
        val expected = shown.region(contentFrameRect())

        assertNear("窓内の枠の下端から dialogMargin 内側", expected.bottom - margin(), shown.contentRect().bottom)
        assertNear("窓内の枠の右端から dialogMargin 内側", expected.right - margin(), shown.contentRect().right)
        shown.close()
    }

    @Test
    fun 同じ_Activity_で出したモーダルの中の枠は後から配置されると候補になり画面上の位置が基準になる() = runBlocking<Unit> {
        showScreen(CurrentPageTestScreen.WITH_MODAL)
        awaitSettledOrFail(
            reason = "モーダルの枠が配置されなかった",
            observe = {
                activity.modalFrameView?.let {
                    "modalFrame=${it.rectOnScreen().toShortString()} laidOut=${it.isLaidOut}" +
                        " rootLayoutRequested=${it.rootView.isLayoutRequested}"
                } ?: "modalFrame=null"
            },
        ) {
            activity.modalFrameView?.let { it.isLaidOut && it.width > 0 && !it.rootView.isLayoutRequested } == true
        }
        val modalFrame = onMain { requireNotNull(activity.modalFrameView).rectOnScreen() }
        val modalRoot = onMain { requireNotNull(activity.modalFrameView).rootView.rectOnScreen() }
        val activityRoot = onMain { activity.window.decorView.rectOnScreen() }

        val endShown = showDialog()
        val endExpected = endShown.region(modalFrame)
        val endContent = endShown.contentRect()
        endShown.close()
        val startShown = showDialog(placement = BOTH_START)
        val startExpected = startShown.region(modalFrame)
        val startContent = startShown.contentRect()
        startShown.close()

        assertTrue(
            "モーダルのウィンドウの原点が Activity のウィンドウとずれている (モーダル $modalRoot / Activity $activityRoot)",
            modalRoot.left != activityRoot.left || modalRoot.top != activityRoot.top,
        )
        assertNear("下端はモーダルの枠の下端から dialogMargin 内側", endExpected.bottom - margin(), endContent.bottom)
        assertNear("右端はモーダルの枠の右端から dialogMargin 内側", endExpected.right - margin(), endContent.right)
        assertNear("上端はモーダルの枠の上端から dialogMargin 内側", startExpected.top + margin(), startContent.top)
        assertNear("左端はモーダルの枠の左端から dialogMargin 内側", startExpected.left + margin(), startContent.left)
    }

    @Test
    fun 台帳が登録した関数に勝ち_台帳が空になると登録した関数へ進む() = runBlocking<Unit> {
        // 登録した関数は下部バーを含む画面全体を返す
        DialogCurrentPage.provider = { activity.composeView }

        val onLedger = showDialog()
        val ledgerExpected = onLedger.region(contentFrameRect())
        assertNear("modifier を付けた枠が基準", ledgerExpected.bottom - margin(), onLedger.contentRect().bottom)
        onLedger.close()

        showScreen(CurrentPageTestScreen.UNMARKED)
        val onProvider = showDialog()
        val providerExpected = onProvider.region(onMain { activity.composeView.rectOnScreen() })
        assertNear(
            "台帳が空なので登録した関数の View が基準",
            providerExpected.bottom - margin(),
            onProvider.contentRect().bottom,
        )
        assertTrue(
            "登録した関数の View は下部バーを含むので、台帳の結果より下に出る",
            onProvider.contentRect().bottom > ledgerExpected.bottom - margin(),
        )
        onProvider.close()
    }

    // 組み立て

    /** 画面を切り替え、組み立てが確定するまで待つ。 */
    private suspend fun showScreen(screen: CurrentPageTestScreen, marksOuterFrame: Boolean = false) {
        scenario.onActivity {
            it.screen = screen
            it.marksOuterFrame = marksOuterFrame
        }
        awaitRendered(screen, marksOuterFrame)
    }

    private suspend fun awaitRendered(screen: CurrentPageTestScreen, marksOuterFrame: Boolean = false) {
        awaitSettledOrFail(
            reason = "画面 $screen が組み立てられなかった",
            observe = {
                "renderedState=${activity.renderedState}" +
                    " composeViewLayoutRequested=${activity.composeView.isLayoutRequested}"
            },
        ) {
            activity.renderedState == (screen to marksOuterFrame) && !activity.composeView.isLayoutRequested
        }
    }

    /** content 枠の矩形 (画面座標)。下部バーの上までの領域。 */
    private fun contentFrameRect(): Rect = onMain {
        val whole = activity.composeView.rectOnScreen()
        Rect(whole.left, whole.top, whole.right, whole.bottom - toPixels(CurrentPageComposeTestActivity.BOTTOM_BAR_HEIGHT_DP))
    }

    /** 下部バーの矩形 (画面座標)。 */
    private fun bottomBarRect(): Rect = onMain {
        val whole = activity.composeView.rectOnScreen()
        Rect(whole.left, whole.bottom - toPixels(CurrentPageComposeTestActivity.BOTTOM_BAR_HEIGHT_DP), whole.right, whole.bottom)
    }

    /** 基準領域を表示中のページにしたダイアログを 1 枚出し、配置が落ち着くまで待つ。 */
    private suspend fun CoroutineScope.showDialog(placement: DialogPlacement = BOTH_END): ShownDialog {
        val presented = CompletableDeferred<Pair<View, DialogNotifier<Boolean>>>()
        val showTask = async {
            Dialog.instance.show(ViewLayoutTestDialogViewModel()) { _, notifier ->
                FixedSizeContentView(
                    context = this,
                    contentWidth = toPixels(CONTENT_WIDTH_DP),
                    contentHeight = toPixels(CONTENT_HEIGHT_DP),
                ).apply {
                    ksDialogOptions = DialogOptions(layoutArea = DialogLayoutArea.CURRENT_PAGE)
                    ksDialogPlacement = placement
                    presented.complete(this to notifier)
                }
            }
        }
        val (contentView, notifier) = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { presented.await() }
        val shown = ShownDialog(contentView, notifier, showTask)
        shown.awaitSettled()
        return shown
    }

    /** 表示中のダイアログ 1 枚。 */
    private inner class ShownDialog(
        private val contentView: View,
        private val notifier: DialogNotifier<Boolean>,
        private val showTask: Deferred<DialogResult<Boolean>>,
    ) {
        fun contentRect(): Rect = onMain { contentView.rectOnScreen() }

        /** 器のウィンドウの可視領域 (画面座標)。システムバーを除いた領域。 */
        fun visibleAreaRect(): Rect = onMain {
            val root = contentView.rootView
            val rect = root.rectOnScreen()
            val insets = root.systemBarInsets()
            Rect(rect.left + insets.left, rect.top + insets.top, rect.right - insets.right, rect.bottom - insets.bottom)
        }

        /** 枠の矩形と器の可視領域の共通部分 (画面座標)。 */
        fun region(frame: Rect): Rect {
            val region = visibleAreaRect()
            check(region.intersect(frame)) { "枠 $frame が可視領域と重ならない" }
            return region
        }

        /** レイアウトが済み、外形が変わらなくなるまで待つ。落ち着かなければ観測履歴を添えて落とす。 */
        suspend fun awaitSettled() {
            var previous: Rect? = null
            awaitSettledOrFail(
                reason = "ダイアログの配置が落ち着かなかった",
                observe = {
                    "content=${contentView.rectOnScreen().toShortString()} laidOut=${contentView.isLaidOut}" +
                        " rootLayoutRequested=${contentView.rootView.isLayoutRequested}"
                },
            ) {
                // 前回の読みから外形が動いていれば偽になり、共通の待ちが落ち着きの計時を数え直す
                val laidOut = contentView.isLaidOut && !contentView.rootView.isLayoutRequested
                val current = contentView.rectOnScreen()
                val stable = laidOut && current == previous
                previous = current
                stable
            }
        }

        suspend fun close() {
            onMain { notifier.complete(true) }
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
        }
    }

    /**
     * [settled] が成り立ち続けるまで共通の待ち ([InstrumentedStateSettling.awaitSettled]) で待ち、
     * 落ち着かなければ観測履歴を添えて落とす。
     *
     * 読むたびに [observe] の結果を履歴へ積む (変化したときだけ)。時間切れの回に何が起きていたかは
     * 事後には読めないため。[observe] と [settled] は UI スレッドで評価する。
     */
    private suspend fun awaitSettledOrFail(reason: String, observe: () -> String, settled: () -> Boolean) {
        val history = StateHistory()
        val result = InstrumentedStateSettling.awaitSettled(PRESENTATION_TIMEOUT_MILLIS) {
            onMain {
                val satisfied = settled()
                history.recordChange("${observe()} satisfied=$satisfied")
                satisfied
            }
        }
        check(result) {
            "$reason (待ちの上限 $PRESENTATION_TIMEOUT_MILLIS ms)\n" +
                "観測履歴 (数値は観測開始からの経過ミリ秒):\n${history.format()}"
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Any?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(block()) }
        @Suppress("UNCHECKED_CAST")
        return result.get() as T
    }

    private fun toPixels(dp: Int): Int =
        (dp * InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density).roundToInt()

    private fun margin(): Int = toPixels(DEFAULT_MARGIN_DP)

    private fun assertNear(message: String, expected: Int, actual: Int) {
        assertTrue("$message: 期待 $expected px / 実測 $actual px", abs(expected - actual) <= TOLERANCE_PIXELS)
    }

    private companion object {
        const val CONTENT_WIDTH_DP = 200
        const val CONTENT_HEIGHT_DP = 120
        /** 契約既定の dialogMargin (dp)。全辺 0 (core/ADR-0039)。 */
        const val DEFAULT_MARGIN_DP = 0
        const val TOLERANCE_PIXELS = 2

        val BOTH_END = DialogPlacement(horizontalAlignment = DialogAlignment.END, verticalAlignment = DialogAlignment.END)
        val BOTH_START = DialogPlacement(horizontalAlignment = DialogAlignment.START, verticalAlignment = DialogAlignment.START)
    }
}

/** ウィンドウが報告するシステムバーの幅 (px)。 */
private fun View.systemBarInsets(): Rect {
    val insets = rootWindowInsets ?: return Rect()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        Rect(bars.left, bars.top, bars.right, bars.bottom)
    } else {
        @Suppress("DEPRECATION")
        Rect(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
    }
}
