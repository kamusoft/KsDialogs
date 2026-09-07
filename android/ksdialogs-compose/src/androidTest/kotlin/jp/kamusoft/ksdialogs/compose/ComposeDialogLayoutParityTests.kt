package jp.kamusoft.ksdialogs.compose

import android.graphics.Rect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.compose.support.CaptureNotifier
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeLayoutTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.FixedSizeComposeContent
import jp.kamusoft.ksdialogs.compose.support.FixedSizeContentView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.ViewLayoutTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.awaitPresented
import jp.kamusoft.ksdialogs.compose.support.observeFirstDrawnRect
import jp.kamusoft.ksdialogs.compose.support.toLogicalRect
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseLoader
import jp.kamusoft.ksdialogs.support.assertMatchesRect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

/**
 * 添付 DSL で属性を宣言した宣言的 UI の中身が、同じ属性を添付した従来 View 系の中身と
 * まったく同じ外形になることを、共通ケース表 (core/ADR-0009) の全ケースの属性で確かめる。
 *
 * 従来 View 系の外形が共通ケース表の期待 rect に適合することは、Android Native 本体の
 * レイアウト検証が受け持っている。ここで両者の一致を示すことで、宣言的 UI 経由の中身も
 * 同じ規則・同じケース表に適合することが従う。
 *
 * 画面条件 (ウィンドウの大きさとシステム領域の余白) は実機のものをそのまま使い、
 * 2つの経路で同一にする。差が出るとすれば属性の供給経路の違いだけになる。
 */
@RunWith(Parameterized::class)
class ComposeDialogLayoutParityTests(private val layoutCase: DialogLayoutCase) {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun 添付_DSL_で宣言した属性は従来_View_系の添付と同じ外形になる() = runBlocking {
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val options = layoutCase.attributes.options()
        val placement = layoutCase.attributes.placement()

        val viewRect = presentViewContent(options, placement, density).toLogicalRect(density)
        val composeRect = presentComposeContent(options, placement, density).toLogicalRect(density)

        assertMatchesRect(
            expected = viewRect,
            actual = composeRect,
            note = "${layoutCase.id} [宣言的 UI と従来 View 系の一致]",
        )
    }

    /** 従来 View 系の中身を添付つきで1枚出し、最初に描かれた時点の外形 (px) を返す。 */
    private suspend fun presentViewContent(
        options: DialogOptions,
        placement: DialogPlacement,
        density: Float,
    ): Rect {
        val firstDrawnRect = CompletableDeferred<Rect>()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()

        DialogViewRegistry.shared.register(ViewLayoutTestDialogViewModel::class) { _, supplied ->
            notifier.complete(supplied)
            FixedSizeContentView(
                context = this,
                contentWidth = (layoutCase.contentSize.w * density).roundToInt(),
                contentHeight = (layoutCase.contentSize.h * density).roundToInt(),
            ).apply {
                ksDialogOptions = options
                ksDialogPlacement = placement
                observeFirstDrawnRect(firstDrawnRect)
            }
        }

        return coroutineScope {
            val showTask = async { Dialog.instance.show(ViewLayoutTestDialogViewModel()) }
            val rect = firstDrawnRect.awaitPresented()
            notifier.awaitPresented().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
            rect
        }
    }

    /** 宣言的 UI の中身を添付 DSL つきで1枚出し、最初に描かれた時点の外形 (px) を返す。 */
    private suspend fun presentComposeContent(
        options: DialogOptions,
        placement: DialogPlacement,
        density: Float,
    ): Rect {
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()

        DialogViewRegistry.shared.registerCompose(ComposeLayoutTestDialogViewModel::class) { _, supplied ->
            FixedSizeComposeContent(
                width = layoutCase.contentSize.w.dp,
                height = layoutCase.contentSize.h.dp,
                options = options,
                placement = placement,
                observation = observation,
            )
            CaptureNotifier(supplied, notifier)
        }

        return coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeLayoutTestDialogViewModel()) }
            val rect = observation.firstDrawnRect.awaitPresented()
            notifier.awaitPresented().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
            rect
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<DialogLayoutCase> = DialogLayoutCaseLoader.table.cases
    }
}
