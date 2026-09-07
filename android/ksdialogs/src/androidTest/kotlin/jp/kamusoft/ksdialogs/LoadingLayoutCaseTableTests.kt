package jp.kamusoft.ksdialogs

import android.graphics.Color
import androidx.test.ext.junit.rules.ActivityScenarioRule
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseLoader
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.LoadingLayoutMeasurement
import jp.kamusoft.ksdialogs.support.assertMatchesExpectedRect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * レイアウト共通ケース表の全ケースを、Loading の器で実際にレイアウトして検証する。
 *
 * レイアウト規則は Dialog と共有する部品を通るが、器が変われば制約・測定条件への反映漏れの形も
 * 変わるため、器ごとに実測する (core/ADR-0009・core/ADR-0022)。表は Dialog の器と同じ1つの正
 * (`core/layout-spec/cases.json`) を使う。
 *
 * isCanceledOnTouchOutside に関わる観察は Loading では「常に無効」に読み替えるが、
 * 表は同属性のケースを持たないため、読み替えは [LoadingAttributeTests] の LD-AT-03 が担う。
 *
 * ケース表の attributes は供給を合成した後の実効値なので、中身の View への添付だけで与えれば足りる。
 */
@RunWith(Parameterized::class)
class LoadingLayoutCaseTableTests(private val layoutCase: DialogLayoutCase) {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun レイアウト完了後の実測外形が期待_rect_に一致する() {
        val actual = LoadingLayoutMeasurement.measureLoadingRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
            options = layoutCase.attributes.options(),
            attachedPlacement = layoutCase.attributes.placement(),
        )

        assertMatchesExpectedRect(layoutCase, actual)
    }

    @Test
    fun 覆いの色が透明でも同じ外形になる() {
        val actual = LoadingLayoutMeasurement.measureLoadingRect(
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
