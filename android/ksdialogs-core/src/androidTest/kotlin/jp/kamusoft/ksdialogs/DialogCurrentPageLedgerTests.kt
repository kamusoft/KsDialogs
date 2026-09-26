package jp.kamusoft.ksdialogs

import android.graphics.RectF
import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogCurrentPageStage
import jp.kamusoft.ksdialogs.support.DialogCurrentPageTestActivity
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.OtherActivityWindow
import jp.kamusoft.ksdialogs.support.SameActivityModalWindow
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.screenRect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * modifier の台帳が、実際のウィンドウに載った枠をどう候補にするかを確かめる。
 *
 * 枠は台帳の入口が受け取る形 ([DialogCurrentPageMarker]) を直に実装した代役で与える。
 * Compose の modifier が実際に台帳へ出入りすることは、Compose モジュールの検証が受け持つ。
 */
@OptIn(KsDialogsInternalApi::class)
@RunWith(AndroidJUnit4::class)
class DialogCurrentPageLedgerTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogCurrentPageTestActivity> =
        ActivityScenarioRule(DialogCurrentPageTestActivity::class.java)

    @Test
    fun 配置中の枠だけが候補になり外れると見つからない() {
        activityRule.scenario.onActivity { activity ->
            val ledger = DialogCurrentPageMarkerLedger()
            val root = activity.window.decorView
            val marker = FixedMarker(activity.pageView, RectF(0f, 100f, 300f, 500f))

            ledger.attach(marker)
            val found = ledger.lookUpPageRect(root)
            ledger.detach(marker)
            val afterDetach = ledger.lookUpPageRect(root)

            val origin = root.screenRect()
            assertEquals(
                DialogCurrentPageLookup.Found(
                    DialogScreenRect(0f, 100f, 300f, 500f).offset(origin.left.toFloat(), origin.top.toFloat()),
                ),
                found,
            )
            assertTrue("外れた枠は候補にならない: $afterDetach", afterDetach is DialogCurrentPageLookup.NotFound)
        }
    }

    @Test
    fun 同じ_Activity_のモーダルのウィンドウに載った枠は候補になり_そのウィンドウの原点で画面座標になる() {
        val activity = AtomicReference<DialogCurrentPageTestActivity>()
        activityRule.scenario.onActivity(activity::set)
        val modal = SameActivityModalWindow.show(
            activity.get(),
            leftDp = MODAL_LEFT_DP,
            topDp = MODAL_TOP_DP,
            widthDp = MODAL_SIZE_DP,
            heightDp = MODAL_SIZE_DP,
        )
        try {
            activityRule.scenario.onActivity { current ->
                val ledger = DialogCurrentPageMarkerLedger()
                val root = current.window.decorView
                val inMainWindow = FixedMarker(current.pageView, RectF(0f, 100f, 300f, 500f))
                val inModal = FixedMarker(modal.pageView, RectF(10f, 20f, 110f, 220f))

                ledger.attach(inMainWindow)
                ledger.attach(inModal)
                val found = ledger.lookUpPageRect(root) as DialogCurrentPageLookup.Found

                val modalOrigin = modal.windowRoot.screenRect()
                assertNotEquals("モーダルのウィンドウの原点が Activity のウィンドウとずれている", root.screenRect().top, modalOrigin.top)
                assertEquals(
                    "後から配置されたモーダルの枠が、モーダルのウィンドウの原点を足した画面座標で選ばれる",
                    DialogScreenRect(10f, 20f, 110f, 220f).offset(modalOrigin.left.toFloat(), modalOrigin.top.toFloat()),
                    found.rect,
                )
            }
        } finally {
            modal.dismiss()
        }
    }

    @Test
    fun 別の_Activity_のウィンドウに載った枠は後から配置されても候補にならない() {
        val other = OtherActivityWindow.launch()
        try {
            val otherView = AtomicReference<View>()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { otherView.set(other.activity.hostContainer) }
            activityRule.scenario.onActivity { activity ->
                val ledger = DialogCurrentPageMarkerLedger()
                val root = activity.window.decorView
                val inWindow = FixedMarker(activity.pageView, RectF(0f, 100f, 300f, 500f))
                val inOtherActivity = FixedMarker(otherView.get(), RectF(0f, 0f, 200f, 200f))
                assertTrue("別の Activity の枠は画面に載っている", otherView.get().isAttachedToWindow)

                ledger.attach(inWindow)
                ledger.attach(inOtherActivity)
                val found = ledger.lookUpPageRect(root) as DialogCurrentPageLookup.Found
                ledger.detach(inWindow)
                val onlyOther = ledger.lookUpPageRect(root)

                val origin = root.screenRect()
                assertEquals(
                    "ダイアログを出す画面のウィンドウの枠が選ばれる",
                    DialogScreenRect(0f, 100f, 300f, 500f).offset(origin.left.toFloat(), origin.top.toFloat()),
                    found.rect,
                )
                assertTrue("別の Activity の枠だけでは見つからない: $onlyOther", onlyOther is DialogCurrentPageLookup.NotFound)
            }
        } finally {
            other.close()
        }
    }

    @Test
    fun KsDialogs_の器のウィンドウに載った枠は候補にならない() = runBlocking<Unit> {
        val activity = AtomicReference<DialogCurrentPageTestActivity>()
        activityRule.scenario.onActivity(activity::set)
        val loadingHarness = LoadingTestHarness(activity.get())
        val toastHarness = ToastTestHarness(activity.get())
        val dialog = DialogCurrentPageStage.show(this, DialogLayoutArea.VISIBLE_AREA, DialogCurrentPageStage.START_START)
        try {
            loadingHarness.loading.show("loading", placement = null)
            assertTrue("Loading が取り付く", loadingHarness.waitUntilPresenting())
            activityRule.scenario.onActivity { toastHarness.toast.show("toast", durationMs = TOAST_DURATION_MS, placement = null) }
            assertTrue("Toast が取り付く", toastHarness.waitUntilPresenting())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            activityRule.scenario.onActivity { current ->
                val root = current.window.decorView
                val containerContents = mapOf(
                    "Dialog" to dialog.contentView,
                    "Loading" to requireNotNull(loadingHarness.contentView),
                    "Toast" to toastHarness.contentViews.single(),
                )
                for ((name, content) in containerContents) {
                    val ledger = DialogCurrentPageMarkerLedger()
                    ledger.attach(FixedMarker(content, RectF(0f, 100f, 300f, 500f)))
                    val lookup = ledger.lookUpPageRect(root)

                    assertTrue("$name の器は画面に載っている", content.isAttachedToWindow)
                    assertTrue("$name の器のウィンドウは器の印を持つ", content.rootView.isKsDialogsContainerWindowRoot)
                    assertTrue("$name の器の枠は候補にならない: $lookup", lookup is DialogCurrentPageLookup.NotFound)
                }
            }
        } finally {
            loadingHarness.tearDown()
            toastHarness.waitUntilEmpty()
            dialog.close()
        }
    }

    /** ウィンドウの座標で決まった矩形を返す枠の代役。 */
    private class FixedMarker(
        override val hostView: View,
        private val bounds: RectF,
    ) : DialogCurrentPageMarker {
        override fun boundsInWindow(): RectF = RectF(bounds)
    }

    private companion object {
        /** モーダルのウィンドウの左端・上端の位置 (dp)。上端はステータスバーより下にする。 */
        const val MODAL_LEFT_DP = 40
        const val MODAL_TOP_DP = 160

        /** モーダルのウィンドウの一辺 (dp)。 */
        const val MODAL_SIZE_DP = 240

        /** 観察の間は消えず、後片付けで待てる長さの Toast の表示時間 (ミリ秒)。 */
        const val TOAST_DURATION_MS = 3_000
    }
}
