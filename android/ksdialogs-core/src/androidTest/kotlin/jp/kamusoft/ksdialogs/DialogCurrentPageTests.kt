package jp.kamusoft.ksdialogs

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogCurrentPageStage
import jp.kamusoft.ksdialogs.support.DialogCurrentPageStage.END_END
import jp.kamusoft.ksdialogs.support.DialogCurrentPageStage.START_START
import jp.kamusoft.ksdialogs.support.DialogCurrentPageTestActivity
import jp.kamusoft.ksdialogs.support.OtherActivityWindow
import jp.kamusoft.ksdialogs.support.SameActivityModalWindow
import jp.kamusoft.ksdialogs.support.ShownCurrentPageDialog
import jp.kamusoft.ksdialogs.support.assertPixelsNear
import jp.kamusoft.ksdialogs.support.screenRect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 基準領域「表示中のページ」を、実際の show と Activity 側のページ領域の View で確かめる。
 *
 * ページの取得元は [DialogCurrentPage.provider] に登録した関数 (従来の View 向けの登録口)。
 * 器は Activity とは別のウィンドウに載るため、観察はすべて画面座標で行い、期待値は
 * 「ページの矩形と器のウィンドウの可視領域の共通部分」から dialogMargin (既定 0dp) を控除して導く。
 */
@RunWith(AndroidJUnit4::class)
class DialogCurrentPageTests {

    private var scenario: ActivityScenario<DialogCurrentPageTestActivity>? = null

    @After
    fun tearDown() {
        DialogCurrentPage.provider = null
        scenario?.let { opened ->
            opened.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            opened.close()
        }
        scenario = null
    }

    // 登録した View が基準になる

    @Test
    fun 登録した_View_で_End_配置が下部バーを避ける() = runBlocking<Unit> {
        val activity = launch()
        registerPage { activity.pageView }

        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val content = shown.contentRect()
        val page = rectOf { activity.pageView }
        val bar = rectOf { activity.bottomBar }
        val expected = shown.expectedRegion(page)

        assertPixelsNear("下端はページ領域の下端から dialogMargin 内側", expected.bottom - margin(), content.bottom)
        assertPixelsNear("右端はページ領域の右端から dialogMargin 内側", expected.right - margin(), content.right)
        assertTrue("下部バーと重ならない (中身 $content / 下部バー $bar)", content.bottom <= bar.top)
        assertTrue("ページが得られたので診断は出ない", shown.diagnostics().isEmpty())
        shown.close()
    }

    @Test
    fun 同じ画面で_VISIBLE_AREA_は下部バーに重なり_CURRENT_PAGE_より下に出る() = runBlocking<Unit> {
        val activity = launch()
        registerPage { activity.pageView }

        val onPage = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val pageBottom = onPage.contentRect().bottom
        onPage.close()

        val onVisible = DialogCurrentPageStage.show(this, DialogLayoutArea.VISIBLE_AREA, END_END)
        val content = onVisible.contentRect()
        val visible = onVisible.visibleAreaRect()
        val bar = rectOf { activity.bottomBar }

        assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
        assertTrue("下部バーに重なる (中身 $content / 下部バー $bar)", content.bottom > bar.top)
        assertPixelsNear("下部バーの分だけ CURRENT_PAGE より下に出る", visible.bottom - bar.top, content.bottom - pageBottom)
        onVisible.close()
    }

    @Test
    fun 器のウィンドウと_Activity_のウィンドウの原点が違っても同じ場所を指す() = runBlocking<Unit> {
        // ページ領域の上端を上部バーで下げ、器のウィンドウは上端を削って下寄せにする
        val activity = launch(topBarHeightDp = TOP_BAR_HEIGHT_DP)
        registerPage { activity.pageView }
        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, START_START)

        shrinkDialogWindowFromTop(shown, DialogCurrentPageStage.toPixels(WINDOW_TOP_CUT_DP.toDouble()))
        val decor = rectOf { activity.window.decorView }
        val page = rectOf { activity.pageView }
        shown.assertContentRectSettles("器の再配置が落ち着かなかった (ページ $page / Activity のウィンドウ $decor)") { rect ->
            val hostTop = shown.host.screenRect().top
            hostTop > decor.top && kotlin.math.abs(rect.top - (page.top + margin())) <= TOLERANCE
        }
        val host = shown.hostRect()
        val content = shown.contentRect()

        assertNotEquals("器のウィンドウの原点が Activity のウィンドウとずれている", decor.top, host.top)
        assertPixelsNear("画面上でページ領域の上端から dialogMargin 内側", page.top + margin(), content.top)
        assertPixelsNear("画面上でページ領域の左端から dialogMargin 内側", page.left + margin(), content.left)
        shown.close()
    }

    @Test
    fun edge_to_edge_でもシステムバーを含まない() = runBlocking<Unit> {
        val activity = launch(edgeToEdge = true)
        registerPage { activity.pageView }

        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, START_START)
        val page = rectOf { activity.pageView }
        val visible = shown.visibleAreaRect()
        val content = shown.contentRect()

        assertTrue("ページ領域がステータスバーの下まで伸びている (ページ $page / 可視領域 $visible)", page.top < visible.top)
        assertPixelsNear("ステータスバーの下端 (可視領域の上端) から dialogMargin 内側", visible.top + margin(), content.top)
        shown.close()
    }

    // 未解決は可視領域へ落ちる

    @Test
    fun 未登録なら可視領域と一致し未登録を示す診断ログが_1_件出る() = runBlocking<Unit> {
        launch()

        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val visible = shown.visibleAreaRect()
        val content = shown.contentRect()
        val diagnostics = shown.diagnostics()

        assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
        assertPixelsNear("可視領域の右端から dialogMargin 内側", visible.right - margin(), content.right)
        assertEquals("診断は 1 件: $diagnostics", 1, diagnostics.size)
        assertTrue(
            "iOS と同じ書き出しで未登録を示す: $diagnostics",
            diagnostics.single().startsWith(DialogCurrentPageResolution.DIAGNOSTIC_LEAD) &&
                diagnostics.single().contains("No current page provider is registered."),
        )
        shown.close()
    }

    @Test
    fun 登録した関数が_null_を返すと可視領域と一致する() = runBlocking<Unit> {
        launch()
        registerPage { null }

        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val visible = shown.visibleAreaRect()
        val content = shown.contentRect()

        assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
        assertTrue(shown.diagnostics().single().contains("returned null"))
        shown.close()
    }

    @Test
    fun 登録した関数が例外を投げても表示は失敗せず可視領域に出て結果は通常どおり返る() = runBlocking<Unit> {
        launch()
        registerPage { throw IllegalStateException("page is not ready") }

        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val visible = shown.visibleAreaRect()
        val content = shown.contentRect()

        assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
        assertTrue(shown.diagnostics().single().contains("threw an exception"))
        assertEquals(DialogResult.Completed(true), shown.close())
    }

    // 候補になるウィンドウ (提示先 Activity が持つウィンドウ。KsDialogs の器は除く)

    @Test
    fun 同じ_Activity_のモーダルのウィンドウに載った_View_は原点が違ってもその位置が基準になる() = runBlocking<Unit> {
        val activity = launch()
        val (modalWidthDp, modalHeightDp) = modalSizeDp(activity)
        val modal = SameActivityModalWindow.show(
            activity,
            leftDp = MODAL_LEFT_DP,
            topDp = MODAL_TOP_DP,
            widthDp = modalWidthDp,
            heightDp = modalHeightDp,
        )
        try {
            registerPage { modal.pageView }
            val decor = rectOf { activity.window.decorView }
            val modalRoot = rectOf { modal.windowRoot }
            val page = rectOf { modal.pageView }

            val endEnd = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
            val endContent = endEnd.contentRect()
            val endDiagnostics = endEnd.diagnostics()
            endEnd.close()
            val startStart = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, START_START)
            val startContent = startStart.contentRect()
            startStart.close()

            assertNotEquals("モーダルのウィンドウの原点が Activity のウィンドウとずれている (上端)", decor.top, modalRoot.top)
            assertNotEquals("モーダルのウィンドウの原点が Activity のウィンドウとずれている (左端)", decor.left, modalRoot.left)
            assertEquals("ページが得られたので診断は出ない", emptyList<String>(), endDiagnostics)
            assertPixelsNear("画面上でモーダルのページの下端から dialogMargin 内側", page.bottom - margin(), endContent.bottom)
            assertPixelsNear("画面上でモーダルのページの右端から dialogMargin 内側", page.right - margin(), endContent.right)
            assertPixelsNear("画面上でモーダルのページの上端から dialogMargin 内側", page.top + margin(), startContent.top)
            assertPixelsNear("画面上でモーダルのページの左端から dialogMargin 内側", page.left + margin(), startContent.left)
        } finally {
            modal.dismiss()
        }
    }

    @Test
    fun 別の_Activity_のウィンドウに載った_View_を返す関数は使われず可視領域になる() = runBlocking<Unit> {
        val activity = launch()
        registerPage { activity.pageView }
        // 後から別の Activity を前面に出す。ダイアログはそちら (再開中の画面) に出るため、
        // 登録した関数が返す View は提示先と別の Activity のウィンドウに載っていることになる
        val other = OtherActivityWindow.launch()
        try {
            val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
            val visible = shown.visibleAreaRect()
            val content = shown.contentRect()
            val diagnostics = shown.diagnostics()
            shown.close()

            assertTrue("元の画面のページ領域は画面に載ったまま", onMainSync { activity.pageView.isAttachedToWindow })
            assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
            assertTrue(
                "別の Activity の View であることが診断に出る: $diagnostics",
                diagnostics.single().contains("is not in a window of the activity presenting the dialog"),
            )
        } finally {
            other.close()
        }
    }

    @Test
    fun KsDialogs_の器に載った_View_を返す関数は使われず可視領域になる() = runBlocking<Unit> {
        launch()
        val underneath = DialogCurrentPageStage.show(this, DialogLayoutArea.VISIBLE_AREA, START_START)
        try {
            registerPage { underneath.contentView }

            val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
            val visible = shown.visibleAreaRect()
            val content = shown.contentRect()
            val diagnostics = shown.diagnostics()
            shown.close()

            assertPixelsNear("可視領域の下端から dialogMargin 内側", visible.bottom - margin(), content.bottom)
            assertPixelsNear("可視領域の右端から dialogMargin 内側", visible.right - margin(), content.right)
            assertTrue(
                "器の中の View であることが診断に出る: $diagnostics",
                diagnostics.single().contains("is not in a window of the activity presenting the dialog"),
            )
        } finally {
            underneath.close()
        }
    }

    // 登録の差し替えと再配置

    @Test
    fun 登録の差し替えは次の表示から効き表示中のダイアログは動かない() = runBlocking<Unit> {
        val activity = launch()
        registerPage { activity.pageView }
        val first = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val before = first.contentRect()

        // 下部バーを含む画面全体の根を返す関数へ差し替えたうえで、器のウィンドウの寸法を変えて
        // 表示中の器に問い合わせ直させる。問い合わせ直しても元の登録 (ページ領域) を使い続ける
        registerPage { activity.rootContainer }
        shrinkDialogWindowFromTop(first, DialogCurrentPageStage.toPixels(WINDOW_TOP_CUT_DP.toDouble()))
        val page = rectOf { activity.pageView }
        first.assertContentRectSettles("器のウィンドウの寸法が変わったあとも下端がページ基準のまま (変わる前の中身 $before)") { rect ->
            first.host.screenRect().top > 0 && kotlin.math.abs(rect.bottom - before.bottom) <= TOLERANCE
        }
        assertPixelsNear(
            "表示中のダイアログは元の登録 (ページ領域) を基準にしたまま",
            first.expectedRegion(page).bottom - margin(),
            first.contentRect().bottom,
        )
        assertPixelsNear("下端は動かない", before.bottom, first.contentRect().bottom)
        first.close()

        val second = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val root = rectOf { activity.rootContainer }
        val expected = second.expectedRegion(root)
        assertPixelsNear("次の表示は新しい登録の View が基準", expected.bottom - margin(), second.contentRect().bottom)
        assertTrue("新しい基準は元より下", second.contentRect().bottom > before.bottom)
        second.close()
    }

    @Test
    fun PB_WN_01_回転でページの矩形が変わると回転後のページを基準に再配置される() = runBlocking<Unit> {
        val activity = launch()
        registerPage { activity.pageView }
        val shown = DialogCurrentPageStage.show(this, DialogLayoutArea.CURRENT_PAGE, END_END)
        val portrait = shown.expectedRegion(rectOf { activity.pageView })
        assertPixelsNear("縦向き", portrait.bottom - margin(), shown.contentRect().bottom)

        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        shown.assertContentRectSettles("横向きに回らなかった") {
            val decor = activity.window.decorView
            decor.width > decor.height && !decor.isLayoutRequested && shown.host.width > shown.host.height
        }
        val landscapePage = rectOf { activity.pageView }
        val expected = shown.expectedRegion(landscapePage)
        shown.assertContentRectSettles("回転後のページ ($landscapePage) を基準に再配置される") { rect ->
            kotlin.math.abs(rect.bottom - (expected.bottom - margin())) <= TOLERANCE &&
                kotlin.math.abs(rect.right - (expected.right - margin())) <= TOLERANCE
        }
        shown.close()
    }

    // 組み立て

    private fun launch(edgeToEdge: Boolean = false, topBarHeightDp: Int = 0): DialogCurrentPageTestActivity {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DialogCurrentPageTestActivity::class.java)
            .putExtra(DialogCurrentPageTestActivity.EXTRA_EDGE_TO_EDGE, edgeToEdge)
            .putExtra(DialogCurrentPageTestActivity.EXTRA_TOP_BAR_HEIGHT_DP, topBarHeightDp)
        val opened = ActivityScenario.launch<DialogCurrentPageTestActivity>(intent)
        scenario = opened
        val activity = AtomicReference<DialogCurrentPageTestActivity>()
        opened.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    /**
     * 同じ Activity のモーダルのウィンドウの幅と高さ (dp) を、端末の画面の大きさから決める。
     *
     * 画面からはみ出したウィンドウはウィンドウ管理が画面内へ押し戻し、左端・上端のずらしが消える。
     * そこで画面の幅・高さから、ずらし分と反対側の余白 (ずらしと同じ量) を引いた大きさに収め、
     * 画面の狭い端末 (320x640dp) でもずらしが保たれるようにする。広い端末では上限の大きさで止める。
     */
    private fun modalSizeDp(activity: DialogCurrentPageTestActivity): Pair<Int, Int> {
        val configuration = onMainSync { activity.resources.configuration }
        val width = minOf(MODAL_MAX_WIDTH_DP, configuration.screenWidthDp - MODAL_LEFT_DP * 2)
        val height = minOf(MODAL_MAX_HEIGHT_DP, configuration.screenHeightDp - MODAL_TOP_DP * 2)
        check(width >= DialogCurrentPageStage.CONTENT_WIDTH_DP && height >= DialogCurrentPageStage.CONTENT_HEIGHT_DP) {
            "画面 (${configuration.screenWidthDp}x${configuration.screenHeightDp}dp) が狭く、" +
                "モーダルのページにダイアログの中身が収まらない (${width}x${height}dp)"
        }
        return width to height
    }

    private fun registerPage(provider: () -> View?) {
        DialogCurrentPage.provider = provider
    }

    private fun rectOf(view: () -> View): Rect = DialogCurrentPageStage.screenRectOf(scenario!!, view)

    private fun margin(): Int = DialogCurrentPageStage.toPixels(DEFAULT_MARGIN_DP)

    /** ページの矩形と器の可視領域の共通部分 (画面座標)。 */
    private fun ShownCurrentPageDialog.expectedRegion(page: Rect): Rect {
        val region = Rect(visibleAreaRect())
        check(region.intersect(page)) { "ページ $page が可視領域と重ならない" }
        return region
    }

    /** 器のウィンドウの上端を削り、下寄せにする。ウィンドウの原点が画面上で下へずれる。 */
    private fun shrinkDialogWindowFromTop(shown: ShownCurrentPageDialog, cutPixels: Int) {
        val activity = requireNotNull(currentActivity())
        activity.runOnUiThreadAndWait {
            val decor = shown.contentView.rootView
            val params = decor.layoutParams as WindowManager.LayoutParams
            params.gravity = Gravity.BOTTOM
            params.height = decor.height - cutPixels
            activity.windowManager.updateViewLayout(decor, params)
        }
    }

    private fun currentActivity(): DialogCurrentPageTestActivity? {
        val activity = AtomicReference<DialogCurrentPageTestActivity>()
        scenario?.onActivity { activity.set(it) }
        return activity.get()
    }

    private fun <T> onMainSync(block: () -> T): T {
        val result = AtomicReference<T>()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(block()) }
        return result.get()
    }

    private companion object {
        /** 契約既定の dialogMargin (dp)。全辺 0 (core/ADR-0039)。 */
        const val DEFAULT_MARGIN_DP = 0.0

        /** 原点差のケースでページ領域を下げる上部バーの高さ (dp)。 */
        const val TOP_BAR_HEIGHT_DP = 160

        /** 原点差のケースで器のウィンドウの上端から削る高さ (dp)。上部バーより小さくする。 */
        const val WINDOW_TOP_CUT_DP = 80

        /** 同じ Activity のモーダルのウィンドウの位置 (dp)。上端はステータスバーより下にする。 */
        const val MODAL_LEFT_DP = 32
        const val MODAL_TOP_DP = 120

        /** 同じ Activity のモーダルのウィンドウの大きさの上限 (dp)。実際の大きさは画面から決める (modalSizeDp)。 */
        const val MODAL_MAX_WIDTH_DP = 320
        const val MODAL_MAX_HEIGHT_DP = 400

        /** 画素の丸めの許容差 (px)。 */
        const val TOLERANCE = DialogCurrentPageStage.TOLERANCE_PIXELS
    }
}

/** UI スレッドで実行し、終わるまで待つ。 */
internal fun android.app.Activity.runOnUiThreadAndWait(block: () -> Unit) {
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
