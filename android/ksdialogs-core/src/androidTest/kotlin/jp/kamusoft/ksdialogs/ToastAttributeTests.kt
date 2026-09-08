package jp.kamusoft.ksdialogs

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.view.ViewOutlineProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastLayoutObservation
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
