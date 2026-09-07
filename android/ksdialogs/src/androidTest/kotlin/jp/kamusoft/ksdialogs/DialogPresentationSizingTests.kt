package jp.kamusoft.ksdialogs

import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogPresentationProbe
import jp.kamusoft.ksdialogs.support.MessageTestDialogViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 提示された時点で、初期状態を反映した大きさになっていることを確かめる。
 */
@RunWith(AndroidJUnit4::class)
class DialogPresentationSizingTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 初期状態で伸びた内容が提示の時点で高さに反映されている() = runBlocking<Unit> {
        val shortRect = presentAndCaptureRect("短い本文")
        val longRect = presentAndCaptureRect(LONG_MESSAGE)

        assertTrue("提示の時点で高さが確定していること (実測 $shortRect)", shortRect.height() > 0)
        assertTrue(
            "初期状態を反映した内容の高さで提示されること (短い ${shortRect.height()} / 長い ${longRect.height()})",
            longRect.height() > shortRect.height(),
        )
    }

    @Test
    fun 同じ本文なら提示の時点の大きさは変わらない() = runBlocking<Unit> {
        val first = presentAndCaptureRect("同じ本文")
        val second = presentAndCaptureRect("同じ本文")

        assertEquals(first.width(), second.width())
        assertEquals(first.height(), second.height())
    }

    private suspend fun presentAndCaptureRect(message: String) =
        DialogPresentationProbe.captureRectAtFirstDraw(
            viewModelClass = MessageTestDialogViewModel::class,
            viewModel = MessageTestDialogViewModel(message),
            createContentView = { viewModel, context ->
                TextView(context).apply { text = viewModel.message }
            },
        )

    private companion object {
        /** 1行に収まらず、折り返して縦に伸びる本文。 */
        val LONG_MESSAGE: String = "折り返して縦に伸びる本文。".repeat(20)
    }
}
