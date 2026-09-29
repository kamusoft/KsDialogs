package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.view.WindowManager
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.InstrumentedStateSettling
import jp.kamusoft.ksdialogs.support.StateHistory
import jp.kamusoft.ksdialogs.support.StatusBarObservation
import jp.kamusoft.ksdialogs.support.SystemBarsObservation
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity
import jp.kamusoft.ksdialogs.support.attach
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * 覆いの色に透明を指定したときに、ステータスバーの見えが変わらないことを確かめる。
 *
 * 提示の構成を切り替える回避策ではなく、ウィンドウ自身がシステムバーの背景を描く構成で解消しているため、
 * 配置規則は覆いの色によらず同じになる (外形が変わらないことは共通ケース表の検証が受け持つ)。
 *
 * 画面の明るさを測る検証なので、比べる前に次の状態をそろえる。
 * - 提示先がステータスバーのアイコンを明るい地向けに指定している (白地に白いアイコン同士のように、
 *   差の出ない状態で比べない)
 * - 起動時の表示 (スプラッシュ) が画面から外れ、ステータスバーの見えが提示先自身のものになっている。
 *   これは明るさの安定では判定しない (止まって見えるスプラッシュを落ち着いた提示先と読み違えるため)。
 *   提示先のウィンドウがフォーカスを持ち、提示先自身の描画が送り出され、起動時の表示のウィンドウが
 *   無くなったことの合意で判定し、成り立たなければ測らずに失敗させる
 */
@RunWith(AndroidJUnit4::class)
class DialogTransparentOverlayTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<SystemBarsTestActivity> =
        ActivityScenarioRule(
            SystemBarsTestActivity.intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                SystemBarsTestActivity.Appearance.LIGHT_BARS,
            ),
        )

    /** 提示先の画面。UI スレッドの外からウィンドウを取り出せるよう、先に掴んでおく。 */
    private lateinit var hostActivity: SystemBarsTestActivity

    @Before
    fun captureHostActivity() {
        activityRule.scenario.onActivity { hostActivity = it }
    }

    @Test
    fun PB_SB_11_透明な覆いのダイアログを出してもステータスバーの帯の明るさは変わらない() {
        awaitHostOwnsStatusBar()
        val statusBarHeight = statusBarHeightPixels()
        val before = StatusBarObservation.awaitStableMeanBrightness(statusBarHeight)

        val container = presentContainer(Color.TRANSPARENT)
        val whilePresented = StatusBarObservation.awaitStableMeanBrightness(statusBarHeight)
        dismiss(container)

        assertTrue(
            "透明の覆いでステータスバーの明るさが変わっている (表示前 $before / 表示中 $whilePresented)",
            abs(whilePresented - before) <= UNCHANGED_TOLERANCE,
        )
    }

    @Test
    fun 既定の覆いではステータスバー領域も覆いの色で暗くなる() {
        // 上の検証が「そもそも明るさの差を検出できる」ことの裏取り。
        // 覆いはウィンドウ全体に効くため、既定の覆いなら帯は目に見えて暗くなる
        awaitHostOwnsStatusBar()
        val statusBarHeight = statusBarHeightPixels()
        val before = StatusBarObservation.awaitStableMeanBrightness(statusBarHeight)

        val container = presentContainer(DEFAULT_OVERLAY_COLOR)
        val whilePresented = StatusBarObservation.awaitStableMeanBrightness(statusBarHeight)
        dismiss(container)

        assertTrue(
            "既定の覆いでステータスバー領域が暗くならない (表示前 $before / 表示中 $whilePresented)",
            before - whilePresented > DIMMED_DIFFERENCE,
        )
    }

    @Test
    fun ダイアログのウィンドウはシステムの覆いを敷かせない() {
        val container = presentContainer(Color.TRANSPARENT)
        val window = requireNotNull(container.window)
        val attributes = window.attributes

        @Suppress("DEPRECATION")
        assertTrue(
            "システムバーの背景をウィンドウ自身が描く構成になっていない",
            attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS != 0,
        )
        assertEquals(
            "背後を暗転させる指定が残っている",
            0,
            attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        )
        @Suppress("DEPRECATION")
        assertEquals("ステータスバーの背景色が透明でない", Color.TRANSPARENT, window.statusBarColor)

        dismiss(container)
    }

    /**
     * 「表示前」を測ってよい状態 — ステータスバーの見えが提示先自身のものになった状態 — を待つ。
     *
     * 提示先が明るい地向けの明暗を指定していることを確かめたうえで、明るさとは独立の 3 つの観測
     * (提示先のウィンドウがフォーカスを持つ・提示先自身の描画が送り出された・起動時の表示のウィンドウが
     * 無い) の合意が続けて成り立つのを待つ。時間内に成り立たなければ測らずに失敗させる。
     */
    private fun awaitHostOwnsStatusBar() {
        assertEquals(
            "前提: 提示先が明るい地向けの明暗を指定している",
            SystemBarsTestActivity.LIGHT_BARS_APPEARANCE,
            SystemBarsObservation.readOnMain { SystemBarsObservation.requestedLightBars(hostActivity.window) },
        )
        val history = StateHistory()
        val settled = runBlocking {
            InstrumentedStateSettling.awaitSettled(HOST_SETTLE_TIMEOUT_MILLIS, HOST_STABLE_MILLIS) {
                val focused = SystemBarsObservation.readOnMain { hostActivity.hasWindowFocus() }
                val committed = hostActivity.hasCommittedOwnFrame
                val startingWindow = SystemBarsObservation.isStartingWindowPresent()
                history.recordChange(
                    "focused=$focused committedOwnFrame=$committed startingWindow=$startingWindow" +
                        " windows=[${SystemBarsObservation.describeOwnWindows()}]",
                )
                focused && committed && !startingWindow
            }
        }
        assertTrue(
            "前提: 起動時の表示が外れ、ステータスバーの見えが提示先自身のものにならなかったので測らない" +
                " (待ちの上限 $HOST_SETTLE_TIMEOUT_MILLIS ms)\n観測履歴:\n${history.format()}",
            settled,
        )
    }

    private fun statusBarHeightPixels(): Int {
        val height = AtomicReference(0)
        activityRule.scenario.onActivity { activity ->
            height.set(StatusBarObservation.heightPixels(activity))
        }
        return height.get()
    }

    /** 覆いの色だけを指定したダイアログを1枚出す。 */
    private fun presentContainer(overlayColor: Int): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        activityRule.scenario.onActivity { activity ->
            DialogContainer(
                context = activity,
                contentView = TextView(activity).apply { text = "覆いの色の確認" }
                    .attach(options = DialogOptions(overlayColor = overlayColor), placement = null),
                resultChannel = DialogResultChannel(),
            ).also {
                container.set(it)
                it.show()
            }
        }
        return container.get()
    }

    private fun dismiss(container: DialogContainer) {
        activityRule.scenario.onActivity { container.dismiss() }
    }

    private companion object {
        /** 提示先がステータスバーの見えを持つまでの待ちの上限 (ミリ秒)。 */
        const val HOST_SETTLE_TIMEOUT_MILLIS = 10_000L

        /** 提示先がステータスバーの見えを持ったとみなすまでに合意が続く時間 (ミリ秒)。 */
        const val HOST_STABLE_MILLIS = 300L

        /** 見えが変わっていないとみなす明るさの差 (0〜255 のうち)。 */
        const val UNCHANGED_TOLERANCE = 6.0

        /** 暗転したと判定する明るさの差。 */
        const val DIMMED_DIFFERENCE = 30.0

        /** 契約が定める覆いの既定色 (黒の 40% 不透明)。 */
        const val DEFAULT_OVERLAY_COLOR = 0x66000000
    }
}
