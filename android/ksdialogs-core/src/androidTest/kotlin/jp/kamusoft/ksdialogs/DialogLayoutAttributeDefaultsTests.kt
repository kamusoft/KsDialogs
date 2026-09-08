package jp.kamusoft.ksdialogs

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseLoader
import jp.kamusoft.ksdialogs.support.DialogLayoutMeasurement
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.assertMatchesExpectedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * メタ属性を何も供給しないダイアログが、契約の既定値どおりに出ることを確かめる。
 */
@RunWith(AndroidJUnit4::class)
class DialogLayoutAttributeDefaultsTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 添付のない_View_は供給なしとして扱われる() {
        val contentView = View(InstrumentationRegistry.getInstrumentation().targetContext)

        assertNull(contentView.ksDialogOptions)
        assertNull(contentView.ksDialogPlacement)
    }

    @Test
    fun 添付した値がそのまま読み出せる() {
        val contentView = View(InstrumentationRegistry.getInstrumentation().targetContext)
        val options = DialogOptions(layoutArea = DialogLayoutArea.WINDOW, isCanceledOnTouchOutside = false)
        val placement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 12.0)

        contentView.ksDialogOptions = options
        contentView.ksDialogPlacement = placement

        assertEquals(options, contentView.ksDialogOptions)
        assertEquals(placement, contentView.ksDialogPlacement)
    }

    @Test
    fun 添付も_show_引数もないダイアログの表示が既定値ケースの_rect_と一致する() {
        // 全属性が既定値のケースが、メタ属性を供給しない既存の呼び出しの見え方を固定している
        val layoutCase = requireNotNull(DialogLayoutCaseLoader.table.cases.firstOrNull { it.attributes.isEmpty() }) {
            "全属性が既定値のケースがケース表に見当たらない"
        }

        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            layoutCase = layoutCase,
        )

        assertMatchesExpectedRect(layoutCase, actual, note = "供給なし")
    }
}
