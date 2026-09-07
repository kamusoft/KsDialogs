package jp.kamusoft.ksdialogs.compose

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.compose.support.CaptureNotifier
import jp.kamusoft.ksdialogs.compose.support.ComposeAttributeTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeLayoutTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.FixedSizeComposeContent
import jp.kamusoft.ksdialogs.compose.support.FixedSizeContentView
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.ViewLayoutTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.awaitPresented
import jp.kamusoft.ksdialogs.compose.support.observeFirstDrawnRect
import jp.kamusoft.ksdialogs.compose.support.rectOnScreen
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import jp.kamusoft.ksdialogs.support.DialogTouchInjection
import jp.kamusoft.ksdialogs.support.outsidePointOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * 宣言的 UI の添付 DSL が、初回表示の時点で効き、そのあとは動かないことを確かめる。
 *
 * 属性の意味そのもの (どこにどう置かれるか) は共通ケース表との突き合わせが受け持つため、
 * ここで見るのは供給の届き方と、スナップショットとして固定される時点である。
 */
@RunWith(AndroidJUnit4::class)
class ComposeDialogAttributeTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun 添付なしは従来_View_系の添付なしと同じ既定値で表示される() = runBlocking {
        val composeRect = presentComposeContent(options = null, placement = null).firstDrawnRect
        val viewRect = presentViewContentWithoutAttachment()

        assertEquals("添付なしどうしで外形が違う", viewRect, composeRect)
    }

    @Test
    fun 添付_DSL_で宣言した配置は初回に描かれた時点で効いている() = runBlocking {
        val attachedRect = presentComposeContent(
            options = DialogOptions(layoutArea = DialogLayoutArea.WINDOW, dialogMargin = DialogEdgeInsets.ZERO),
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
            ),
        ).firstDrawnRect
        val defaultRect = presentComposeContent(options = null, placement = null).firstDrawnRect

        // 既定は中央寄せなので、前端寄せを宣言した側は必ず左上へ寄る
        assertTrue(
            "宣言した配置が初回の描画に効いていない (宣言あり $attachedRect / 宣言なし $defaultRect)",
            attachedRect.left < defaultRect.left && attachedRect.top < defaultRect.top,
        )
    }

    @Test
    fun 非レイアウト属性も添付_DSL_で届く() = runBlocking {
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        DialogViewRegistry.shared.registerCompose(ComposeAttributeTestDialogViewModel::class) { _, supplied ->
            FixedSizeComposeContent(
                width = CONTENT_SIZE_DP.dp,
                height = CONTENT_SIZE_DP.dp,
                options = DialogOptions(
                    overlayColor = OVERLAY_COLOR,
                    isCanceledOnTouchOutside = false,
                ),
                observation = observation,
            )
            CaptureNotifier(supplied, notifier)
        }

        coroutineScope {
            val showTask = async { Dialog.instance.show(ComposeAttributeTestDialogViewModel()) }
            val contentRect = observation.firstDrawnRect.awaitPresented()

            assertEquals("宣言した覆いの色が使われていない", OVERLAY_COLOR, overlayColorOf(observation))

            val (x, y) = outsidePointOf(contentRect)
            DialogTouchInjection.tap(x, y)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue("外側タップで閉じない宣言が効いていない", showTask.isActive)

            notifier.awaitPresented().complete(true)
            assertEquals(
                DialogResult.Completed(true),
                withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() },
            )
        }
    }

    @Test
    fun 提示後に宣言値を変えても表示中のダイアログは追随しない() = runBlocking {
        val toggled = mutableStateOf(false)
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        DialogViewRegistry.shared.registerCompose(ComposeAttributeTestDialogViewModel::class) { _, supplied ->
            TogglingAttributeContent(toggled, observation)
            CaptureNotifier(supplied, notifier)
        }

        coroutineScope {
            val showTask: Deferred<DialogResult<Boolean>> =
                async { Dialog.instance.show(ComposeAttributeTestDialogViewModel()) }
            val rectAtFirstDraw = observation.firstDrawnRect.awaitPresented()
            val overlayAtFirstDraw = overlayColorOf(observation)

            // 中身の状態変化で、宣言する配置・覆いの色・外側タップの扱いをすべて変える
            InstrumentationRegistry.getInstrumentation().runOnMainSync { toggled.value = true }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals("配置がスナップショットのまま保たれていない", rectAtFirstDraw, contentRectOf(observation))
            assertEquals("覆いの色がスナップショットのまま保たれていない", overlayAtFirstDraw, overlayColorOf(observation))

            // 変更後の宣言は「外側タップで閉じない」。スナップショット時点の既定 (閉じる) に従うはず
            val (x, y) = outsidePointOf(rectAtFirstDraw)
            DialogTouchInjection.tap(x, y)

            assertEquals(
                DialogResult.Cancelled,
                withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() },
            )
        }
    }

    /**
     * 宣言的 UI の中身を1枚出し、観察してから完了で閉じる。
     *
     * @return 最初に描かれた時点の外形などの観察結果
     */
    private suspend fun presentComposeContent(
        options: DialogOptions?,
        placement: DialogPlacement?,
    ): PresentedContent {
        val observation = ComposeContentObservation()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        DialogViewRegistry.shared.registerCompose(ComposeLayoutTestDialogViewModel::class) { _, supplied ->
            FixedSizeComposeContent(
                width = CONTENT_SIZE_DP.dp,
                height = CONTENT_SIZE_DP.dp,
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
            PresentedContent(rect)
        }
    }

    /** 添付を持たない従来 View 系の中身を1枚出し、最初に描かれた時点の外形 (px) を返す。 */
    private suspend fun presentViewContentWithoutAttachment(): Rect {
        val firstDrawnRect = CompletableDeferred<Rect>()
        val notifier = CompletableDeferred<DialogNotifier<Boolean>>()
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        DialogViewRegistry.shared.register(ViewLayoutTestDialogViewModel::class) { _, supplied ->
            notifier.complete(supplied)
            FixedSizeContentView(
                context = this,
                contentWidth = (CONTENT_SIZE_DP * density).roundToInt(),
                contentHeight = (CONTENT_SIZE_DP * density).roundToInt(),
            ).apply { observeFirstDrawnRect(firstDrawnRect) }
        }

        return coroutineScope {
            val showTask = async { Dialog.instance.show(ViewLayoutTestDialogViewModel()) }
            val rect = firstDrawnRect.awaitPresented()
            notifier.awaitPresented().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
            rect
        }
    }

    /** 中身の現在の外形 (画面座標・px)。 */
    private fun contentRectOf(observation: ComposeContentObservation): Rect {
        var rect = Rect()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rect = observation.contentView.get().rectOnScreen()
        }
        return rect
    }

    /**
     * 覆いに塗られている色。
     *
     * 覆いは中身のホスト View の兄弟レイヤなので、中身から親をたどりながら
     * 単色の背景を持つ最初の子 (= 覆い) を見つけて読む。
     */
    private fun overlayColorOf(observation: ComposeContentObservation): Int {
        var color = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val overlay = generateSequence(observation.contentView.get().parent as? ViewGroup) {
                it.parent as? ViewGroup
            }
                .flatMap { group -> (0 until group.childCount).asSequence().map(group::getChildAt) }
                .first { it.background is ColorDrawable }
            color = (overlay.background as ColorDrawable).color
        }
        return color
    }

    /** 観察できた提示の結果。 */
    private class PresentedContent(val firstDrawnRect: Rect)

    private companion object {
        /** 中身が要求する大きさ (dp)。 */
        const val CONTENT_SIZE_DP = 160

        /** 既定の覆いの色とはっきり違う色。 */
        const val OVERLAY_COLOR: Int = Color.RED
    }
}

/**
 * 状態の切り替えで、宣言するメタ属性が丸ごと入れ替わる中身。
 *
 * 切り替え前は前端寄せ・赤い覆い・外側タップで閉じる。切り替え後は後端寄せ・緑の覆い・閉じない。
 */
@Composable
private fun TogglingAttributeContent(
    toggled: MutableState<Boolean>,
    observation: ComposeContentObservation,
) {
    val isToggled = toggled.value
    KsDialogAttributes(
        options = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets.ZERO,
            overlayColor = if (isToggled) Color.GREEN else Color.RED,
            isCanceledOnTouchOutside = !isToggled,
        ),
        placement = DialogPlacement(
            horizontalAlignment = if (isToggled) DialogAlignment.END else DialogAlignment.START,
            verticalAlignment = if (isToggled) DialogAlignment.END else DialogAlignment.START,
        ),
    )
    ObserveHostView(observation)
    Box(Modifier.size(TOGGLING_CONTENT_SIZE_DP.dp))
}

/** 切り替えつきの中身が要求する大きさ (dp)。 */
private const val TOGGLING_CONTENT_SIZE_DP = 160
