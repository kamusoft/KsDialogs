package jp.kamusoft.ksdialogs

import androidx.test.ext.junit.rules.ActivityScenarioRule
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseLoader
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.ToastLayoutMeasurement
import jp.kamusoft.ksdialogs.support.assertMatchesExpectedRect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * レイアウト共通ケース表の全ケースを、Toast の器で実際にレイアウトして検証する。
 *
 * レイアウト規則は Dialog と共有する部品を通るが、器が変われば制約・測定条件への反映漏れの形も
 * 変わるため、器ごとに実測する (core/ADR-0009・core/ADR-0030)。表は Dialog の器と同じ1つの正
 * (`core/layout-spec/cases.json`) を使う。
 *
 * 覆いと外側タップに関わる観察は Toast では対象外に読み替える (どちらも Toast には存在しない)。
 * 契約既定配置のケース (C23) は、属性を何も添付しない Toast が実際にその位置へ置かれることで
 * 併せて確かめる。
 *
 * ケース表の attributes は供給を合成した後の実効値なので、中身の View への添付だけで与えれば足りる。
 */
@RunWith(Parameterized::class)
class ToastLayoutCaseTableTests(private val layoutCase: DialogLayoutCase) {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun レイアウト完了後の実測外形が期待_rect_に一致する() {
        val actual = ToastLayoutMeasurement.measureToastRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
            options = layoutCase.attributes.options(),
            attachedPlacement = layoutCase.attributes.placement(),
        )

        assertMatchesExpectedRect(layoutCase, actual)
    }

    @Test
    fun 契約既定配置のケースは添付なしでも同じ位置になる() {
        // 契約既定配置のケースだけは「何も指定しない Toast の実効値」そのものなので、
        // 添付を一切与えずに測っても同じ位置に落ちる
        if (layoutCase.id != CONTRACT_DEFAULT_CASE_ID) {
            return
        }
        val actual = ToastLayoutMeasurement.measureToastRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
        )

        assertMatchesExpectedRect(layoutCase, actual, note = "添付なし (契約既定配置)")
    }

    companion object {
        /** Toast の契約既定配置を定めるケースの ID。 */
        private const val CONTRACT_DEFAULT_CASE_ID = "C23"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<DialogLayoutCase> = DialogLayoutCaseLoader.table.cases
    }
}
