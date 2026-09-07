package jp.kamusoft.ksdialogs

import android.graphics.Color
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseLoader
import jp.kamusoft.ksdialogs.support.DialogLayoutMeasurement
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.assertMatchesExpectedRect
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * 共通ケース表の全ケースを、実際にレイアウトした View の外形で検証する。
 *
 * ケース表の attributes は供給を合成した後の実効値なので、中身の View への添付だけで与えれば足りる。
 */
@RunWith(Parameterized::class)
class DialogLayoutCaseTableTests(private val layoutCase: DialogLayoutCase) {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun レイアウト完了後の実測外形が期待_rect_に一致する() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
            options = layoutCase.attributes.options(),
            attachedPlacement = layoutCase.attributes.placement(),
        )

        assertMatchesExpectedRect(layoutCase, actual)
    }

    @Test
    fun 覆いの色が透明でも同じ外形になる() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
            options = layoutCase.attributes.options(overlayColorOverride = Color.TRANSPARENT),
            attachedPlacement = layoutCase.attributes.placement(),
        )

        assertMatchesExpectedRect(layoutCase, actual, note = "覆いの色 = 透明")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<DialogLayoutCase> = DialogLayoutCaseLoader.table.cases
    }
}

/**
 * ケース表そのものが検証に足る状態で届いているかを確かめる。
 */
@RunWith(AndroidJUnit4::class)
class DialogLayoutCaseTableLoadingTests {

    @Test
    fun ケース表が読み込めている() {
        val table = DialogLayoutCaseLoader.table

        assertTrue(
            "ケース表が空では検証にならない: ${DialogLayoutCaseLoader.CASE_TABLE_ASSET}",
            table.cases.isNotEmpty(),
        )
        assertTrue("許容誤差が設定されていない", table.tolerance > 0)
    }
}
