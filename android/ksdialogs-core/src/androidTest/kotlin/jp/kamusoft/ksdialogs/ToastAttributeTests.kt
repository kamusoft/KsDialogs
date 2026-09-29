package jp.kamusoft.ksdialogs

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.view.ViewOutlineProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutCaseAttributes
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastLayoutMeasurement
import jp.kamusoft.ksdialogs.support.ToastLayoutObservation
import jp.kamusoft.ksdialogs.support.ToastTestAnnouncer
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Toast の配置属性と ToastStyle (core/ADR-0015・0032) を確かめる。
 * iOS Native の ToastAttributeTests のミラー。
 *
 * 配置の実効値は「show の placement 引数 > 中身への添付 > ToastStyle のアプリ既定配置 >
 * Toast の契約既定値」の優先順で決まり、まるごと置換で採用される。
 * 視覚項目はデフォルト View にだけ効き、設定の変更は次の表示から効く。
 */
@RunWith(AndroidJUnit4::class)
class ToastAttributeTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_AT_01_placement_引数で配置が変わる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        val defaultRect = showDefaultAndMeasure(harness, placement = null)
        val topRect = showDefaultAndMeasure(
            harness,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
        )

        assertTrue(
            "契約既定 (下部) より上へ移っていない (既定 $defaultRect / 引数 $topRect)",
            topRect.top < defaultRect.top,
        )
    }

    @Test
    fun TS_AT_02_優先順は_show_引数_添付_style_既定_契約既定() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS).apply {
                ksDialogPlacement = ATTACHED_PLACEMENT
            }
        }

        // 中身への添付 (契約既定値と同じ位置を指す)
        val attachedPosition = showCustomAndMeasure(harness, showPlacement = null)

        // style のアプリ既定配置 — デフォルト View にも効く既定値項目
        harness.toast.style = ToastStyle(defaultPlacement = STYLE_PLACEMENT)
        val styleDefault = showDefaultAndMeasure(harness, placement = null)
        val styleDefaultOnCustom = showCustomAndMeasure(harness, showPlacement = null)

        // 添付は style の既定より強い
        assertEquals(
            "カスタム View の添付が style の既定に負けている",
            attachedPosition.top,
            styleDefaultOnCustom.top,
        )
        assertNotEquals(
            "style のアプリ既定配置がデフォルト View に効いていない",
            attachedPosition.top,
            styleDefault.top,
        )

        // show 引数はまるごと置換で最優先
        val argument = showCustomAndMeasure(harness, showPlacement = SHOW_PLACEMENT)
        assertTrue(
            "show 引数が添付を置換していない (添付 $attachedPosition / 引数 $argument)",
            argument.top < attachedPosition.top,
        )
    }

    @Test
    fun TS_AT_03_style_の変更は次の表示から効く() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        harness.toast.style = ToastStyle(backgroundColor = Color.RED, textColor = Color.RED)

        harness.toast.show("先に出た", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val presented = harness.defaultContentViews.single()

        harness.toast.style = ToastStyle(backgroundColor = Color.BLUE, textColor = Color.BLUE)
        harness.toast.show("後から出た", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting(count = 2))

        assertEquals(
            "表示中の Toast にも変更が及んでいる",
            Color.RED,
            presented.displayedBackgroundColor,
        )
        assertEquals(
            "新しい Toast に変更後の style が反映されていない",
            Color.BLUE,
            harness.defaultContentViews.last().displayedBackgroundColor,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_AT_04_デフォルト_View_は契約既定配置で余白_24_の内側に置かれる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("既定の位置", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        val visible = ToastLayoutObservation.visibleArea(container)

        val expectedBottom = visible.bottom -
            toPixels(DEFAULT_VIEW_MARGIN_DP + ToastPlacementDefault.BOTTOM_BAR_CLEARANCE)
        assertPixelsNear(
            "下端が可視領域の下端から「余白 24 + 上方向オフセット」だけ上にない (可視領域 $visible / 実測 $rect)",
            expectedBottom,
            rect.bottom,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_AT_05_長いメッセージでもデフォルト_View_の左右に余白_24_が残る() {
        // 左右のシステム領域で可視領域を幅 200 まで狭める。余白を引いた幅 (152) がデフォルト View 自身の
        // 最大幅 (取り付け先の画面の幅の 80%) より狭くなり、余白の制約が先に効く。
        // 余白の添付が無ければ、ピルは可視領域の左右の端 (100 / 300) まで広がる
        val stage = DialogLayoutCase(
            id = "TS-AT-05",
            screen = DialogLayoutCase.Size(w = 400.0, h = 800.0),
            insets = DialogLayoutCase.Insets(top = 50.0, left = 100.0, bottom = 30.0, right = 100.0),
            contentSize = DialogLayoutCase.Size(w = 0.0, h = 0.0),
            attributes = DialogLayoutCaseAttributes(),
            expected = DialogLayoutCase.Rect(x = 0.0, y = 0.0, w = 0.0, h = 0.0),
        )
        val actual = ToastLayoutMeasurement.measureToastRect(
            scenario = activityRule.scenario,
            layoutCase = stage,
            createContentView = { context ->
                ToastDefaultContentView(context, LONG_MESSAGE, ToastStyle(), ToastTestAnnouncer())
            },
        )

        val expectedLeft = stage.insets.left + DEFAULT_VIEW_MARGIN_DP
        val expectedRight = stage.screen.w - stage.insets.right - DEFAULT_VIEW_MARGIN_DP
        assertTrue(
            "左端が可視領域の左端からちょうど 24 内側にない (期待 $expectedLeft / 実測 $actual)",
            abs(actual.x - expectedLeft) <= DP_TOLERANCE,
        )
        assertTrue(
            "右端が可視領域の右端からちょうど 24 内側にない (期待 $expectedRight / 実測 $actual)",
            abs(actual.x + actual.w - expectedRight) <= DP_TOLERANCE,
        )
    }

    @Test
    fun TS_AT_06_配置を渡してもデフォルト_View_の余白は保たれる() = runBlocking<Unit> {
        val topPlacement = DialogPlacement(verticalAlignment = DialogAlignment.START)

        // show の placement 引数で渡す
        val withArgument = ToastTestHarness(currentActivity())
        withArgument.toast.show("引数の配置", durationMs = LONG_DURATION_MILLIS, placement = topPlacement)
        assertTrue(withArgument.waitUntilPresenting())
        val argumentContainer = withArgument.containers.single()
        ToastLayoutObservation.awaitSettled(argumentContainer)
        val argumentRect = ToastLayoutObservation.contentRect(argumentContainer)
        val argumentVisible = ToastLayoutObservation.visibleArea(argumentContainer)
        assertPixelsNear(
            "show 引数の配置で上端が可視領域の上端から 24 内側にない (可視領域 $argumentVisible / 実測 $argumentRect)",
            argumentVisible.top + toPixels(DEFAULT_VIEW_MARGIN_DP),
            argumentRect.top,
        )
        assertTrue(withArgument.waitUntilEmpty())

        // ToastStyle のアプリ既定配置に設定して、配置なしで出す
        val withStyle = ToastTestHarness(currentActivity())
        withStyle.toast.style = ToastStyle(defaultPlacement = topPlacement)
        withStyle.toast.show("style の配置", durationMs = LONG_DURATION_MILLIS)
        assertTrue(withStyle.waitUntilPresenting())
        val styleContainer = withStyle.containers.single()
        ToastLayoutObservation.awaitSettled(styleContainer)
        val styleRect = ToastLayoutObservation.contentRect(styleContainer)
        val styleVisible = ToastLayoutObservation.visibleArea(styleContainer)
        assertPixelsNear(
            "アプリ既定配置で上端が可視領域の上端から 24 内側にない (可視領域 $styleVisible / 実測 $styleRect)",
            styleVisible.top + toPixels(DEFAULT_VIEW_MARGIN_DP),
            styleRect.top,
        )
        assertTrue(withStyle.waitUntilEmpty())
    }

    @Test
    fun TS_AT_07_何も添付しないカスタム_View_は余白_0_で契約既定配置に置かれる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        harness.toast.show(ToastTestViewModel(), durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        val visible = ToastLayoutObservation.visibleArea(container)

        assertPixelsNear(
            "下端が可視領域の下端から上方向オフセットだけ上にない (可視領域 $visible / 実測 $rect)",
            visible.bottom - toPixels(ToastPlacementDefault.BOTTOM_BAR_CLEARANCE),
            rect.bottom,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun 余白を添付しないカスタム_Toast_は末尾寄せの配置引数で可視領域の下端に接する() = runBlocking<Unit> {
        // 余白の契約既定値は全辺 0 (core/ADR-0039)
        val harness = ToastTestHarness(currentActivity())
        harness.registry.register(ToastTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        harness.toast.show(
            ToastTestViewModel(),
            durationMs = LONG_DURATION_MILLIS,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 0.0),
        )
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        val visible = ToastLayoutObservation.visibleArea(container)

        assertPixelsNear(
            "下端が可視領域の下端に接していない (可視領域 $visible / 実測 $rect)",
            visible.bottom,
            rect.bottom,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun デフォルト_View_に_ToastStyle_の視覚項目が反映される() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())
        harness.toast.style = ToastStyle(
            backgroundColor = Color.DKGRAY,
            textColor = Color.YELLOW,
            fontSize = 20.0,
            cornerRadius = 8.0,
        )

        harness.toast.show("見た目", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val content = harness.defaultContentViews.single()
        val density = currentActivity().resources.displayMetrics.density

        assertEquals(Color.DKGRAY, content.displayedBackgroundColor)
        assertEquals(Color.YELLOW, content.displayedTextColor)
        assertTrue(
            "文字の大きさが反映されていない (実際 ${content.displayedFontSize})",
            abs(content.displayedFontSize - 20.0) < FONT_SIZE_TOLERANCE,
        )
        assertEquals(
            "角丸半径が反映されていない",
            (8.0 * density).roundToInt(),
            content.displayedCornerRadius.roundToInt(),
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun デフォルト_View_は承認済みモックの落ち影を持つ() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("落ち影", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val content = harness.defaultContentViews.single()

        assertTrue(
            "落ち影が出ない (影の高さ ${content.elevation})",
            content.elevation > 0f,
        )
        assertSame(
            "影の形が地色の輪郭 (角丸長方形) をなぞらない",
            ViewOutlineProvider.BACKGROUND,
            content.outlineProvider,
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun 空文字はそのまま表示され長文は折り返して高さが伸びる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val emptyContainer = harness.containers.single()
        ToastLayoutObservation.awaitSettled(emptyContainer)
        val emptyRect = ToastLayoutObservation.contentRect(emptyContainer)
        assertEquals("空文字はそのまま表示される", "", harness.defaultContentViews.single().displayedText)

        harness.toast.show(LONG_MESSAGE, durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting(count = 2))
        val longContainer = harness.containers.last()
        ToastLayoutObservation.awaitSettled(longContainer)
        val longRect = ToastLayoutObservation.contentRect(longContainer)

        assertTrue(
            "長文が複数行に折り返されず高さが伸びていない (空 $emptyRect / 長文 $longRect)",
            longRect.height() > emptyRect.height(),
        )
        assertTrue(harness.waitUntilEmpty())
    }

    /** デフォルト View を表示して外形を測り、消えるまで待つ。 */
    private suspend fun showDefaultAndMeasure(
        harness: ToastTestHarness,
        placement: DialogPlacement?,
    ): Rect {
        harness.toast.show("配置", durationMs = SHORT_DURATION_MILLIS, placement = placement)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        assertTrue(harness.waitUntilEmpty())
        return rect
    }

    /** 添付を持つカスタム Toast を表示して外形を測り、消えるまで待つ。 */
    private suspend fun showCustomAndMeasure(
        harness: ToastTestHarness,
        showPlacement: DialogPlacement?,
    ): Rect {
        harness.toast.show(
            ToastTestViewModel(),
            durationMs = SHORT_DURATION_MILLIS,
            placement = showPlacement,
        )
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        assertTrue(harness.waitUntilEmpty())
        return rect
    }

    /** 論理単位 (dp) をこの端末の px へ直す。 */
    private fun toPixels(dp: Double): Int =
        (dp * currentActivity().resources.displayMetrics.density).roundToInt()

    /** px の位置が、論理単位 1 の丸めの範囲で一致することを確かめる。 */
    private fun assertPixelsNear(message: String, expected: Int, actual: Int) {
        val tolerance = currentActivity().resources.displayMetrics.density * DP_TOLERANCE
        assertTrue("$message (期待 $expected px / 実測 $actual px)", abs(expected - actual) <= tolerance)
    }

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** カスタム Toast View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160

        /** 待ち時間を短く保つための表示時間 (ミリ秒)。 */
        const val SHORT_DURATION_MILLIS = 300

        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 6_000

        /** デフォルト View が自分に添付している余白 (dp、全辺)。 */
        const val DEFAULT_VIEW_MARGIN_DP = 24.0

        /** 位置の比較で許す差 (dp)。ケース表の許容誤差と同じく、密度による丸めの分を見込む。 */
        const val DP_TOLERANCE = 1.0

        /** 文字の大きさの許容差 (sp)。 */
        const val FONT_SIZE_TOLERANCE = 0.5

        /** 折り返しが起きる長さの文言。 */
        const val LONG_MESSAGE =
            "保存には成功しましたが、同期は次にネットワークへつながったときにまとめて行われます。"

        /** カスタム View へ添付する配置 (契約既定値と同じ位置に置く)。 */
        val ATTACHED_PLACEMENT = DialogPlacement(
            verticalAlignment = DialogAlignment.END,
            offsetY = -80.0,
        )

        /** style のアプリ既定配置。契約既定値とは別の位置に置く。 */
        val STYLE_PLACEMENT = DialogPlacement(verticalAlignment = DialogAlignment.CENTER)

        /** show 引数で渡す配置。添付・style のどちらとも別の位置に置く。 */
        val SHOW_PLACEMENT = DialogPlacement(verticalAlignment = DialogAlignment.START)
    }
}
