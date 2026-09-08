package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.view.WindowManager
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.StatusBarObservation
import jp.kamusoft.ksdialogs.support.attach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 */
@RunWith(AndroidJUnit4::class)
class DialogTransparentOverlayTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 覆いが透明ならステータスバーの明るさは表示前後で変わらない() {
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
        /** 見えが変わっていないとみなす明るさの差 (0〜255 のうち)。 */
        const val UNCHANGED_TOLERANCE = 6.0

        /** 暗転したと判定する明るさの差。 */
        const val DIMMED_DIFFERENCE = 30.0

        /** 契約が定める覆いの既定色 (黒の 40% 不透明)。 */
        const val DEFAULT_OVERLAY_COLOR = 0x66000000
    }
}
