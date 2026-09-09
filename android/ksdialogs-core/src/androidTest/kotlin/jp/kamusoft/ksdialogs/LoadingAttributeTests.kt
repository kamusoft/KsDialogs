package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTouchInjection
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.InstrumentedStateSettling
import jp.kamusoft.ksdialogs.support.LoadingLayoutObservation
import jp.kamusoft.ksdialogs.support.LoadingTestGate
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.LoadingTestViewRecorder
import jp.kamusoft.ksdialogs.support.outsidePointOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Loading の器メタ属性の適用 (core/ADR-0022) を、実際に画面へ載せた器で確かめる。
 *
 * レイアウト系の属性は Dialog と同じ意味・同じ供給規則 (core/ADR-0015) で効き、
 * isCanceledOnTouchOutside だけは常に無効になる。
 *
 * 位置の期待値は、基準領域をウィンドウ全体・余白を 0 にしたうえで器の面の寸法から導いた値。
 * 端末ごとの画面サイズに依存しないよう、システム領域の余白が効かない条件で確かめる。
 */
@RunWith(AndroidJUnit4::class)
class LoadingAttributeTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_AT_01_placement_引数で既定ローディングの配置が変わる() = runBlocking<Unit> {
        val harness = newHarness()
        harness.loading.options = WINDOW_AREA_WITHOUT_MARGIN

        harness.loading.show()
        val centered = settledContentRect(harness)
        val hostSize = LoadingLayoutObservation.hostSize(requireNotNull(harness.container))
        harness.loading.hide()

        harness.loading.show(
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
                offsetX = OFFSET_X_DP,
                offsetY = OFFSET_Y_DP,
            ),
        )
        val placed = settledContentRect(harness)
        harness.loading.hide()

        assertNear("x が placement 引数どおりでない", toPixels(OFFSET_X_DP), placed.left)
        assertNear("y が placement 引数どおりでない", toPixels(OFFSET_Y_DP), placed.top)
        assertNear("既定は水平中央", (hostSize.first - centered.width()) / 2f, centered.left)
        assertNotEquals(centered.left, placed.left)
        assertNotEquals(centered.top, placed.top)
    }

    @Test
    fun LD_AT_02_カスタム_View_の添付属性が_Dialog_と同じ優先順位で効く() = runBlocking<Unit> {
        val harness = newHarness()
        registerCustomView(
            harness,
            options = WINDOW_AREA_WITHOUT_MARGIN,
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
                offsetX = ATTACHED_OFFSET_X_DP,
            ),
        )

        harness.loading.show(LoadingTestViewModel())
        val attached = settledContentRect(harness)
        harness.loading.hide()

        assertNear("添付の offsetX が効いていない", toPixels(ATTACHED_OFFSET_X_DP), attached.left)
        assertNear("添付の上寄せが効いていない", 0f, attached.top)

        // 引数を渡すと添付はオブジェクトまるごと置換される (offsetX も引き継がない)
        harness.loading.show(
            LoadingTestViewModel(),
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.END,
            ),
        )
        val byArgument = settledContentRect(harness)
        val hostSize = LoadingLayoutObservation.hostSize(requireNotNull(harness.container))
        harness.loading.hide()

        assertNear("引数の右寄せが効いていない", hostSize.first.toFloat(), byArgument.right)
        assertNear("引数の下寄せが効いていない", hostSize.second.toFloat(), byArgument.bottom)
    }

    @Test
    fun LD_AT_03_外側タップで閉じず_背後にも透過しない() = runBlocking<Unit> {
        val harness = newHarness()
        val backgroundTaps = addBackgroundTapRecorder()
        registerCustomView(
            harness,
            // Loading では常に無効なので、真を添付しても外側タップは効かない
            options = DialogOptions(isCanceledOnTouchOutside = true),
            placement = null,
        )

        harness.loading.show(LoadingTestViewModel())
        val contentRect = settledContentRectOnScreen(harness)
        // 器のウィンドウが入力の宛先になるまで待つ。切り替わる前に注入したタップは背後へ抜ける
        InstrumentedStateSettling.assertWindowFocused(
            requireNotNull(harness.container).layoutHost,
            "前提: Loading の器が入力の宛先になっている",
        )

        val (x, y) = outsidePointOf(contentRect)
        DialogTouchInjection.tap(x, y)

        val closed = InstrumentedDialogWaiting.waitUntil(NO_REACTION_WAIT_MILLIS) { !harness.isPresenting }
        assertFalse("表示は閉じない", closed)
        assertEquals("背後の画面の要素も反応しない", 0, backgroundTaps.get())

        harness.loading.hide()
    }

    @Test
    fun LD_AT_04_ダイアログ表示中の_Loading_は最前面で入力を遮る() = runBlocking<Unit> {
        val harness = newHarness()
        val dialogTaps = AtomicInteger(0)
        val dialogSession = presentDialog(dialogTaps)

        try {
            // ダイアログのウィンドウが入力の宛先になるまで待つ。宛先が切り替わる前に注入した
            // タップは背後の画面が受け取ってしまい、前提そのものが成立しない
            InstrumentedStateSettling.assertWindowFocused(
                dialogSession.contentView,
                "前提: ダイアログが入力の宛先になっている",
            )
            val dialogContentRect = LoadingLayoutObservation.readOnMain {
                LoadingLayoutObservation.rectOnScreen(dialogSession.contentView)
            }
            // ダイアログが先に画面へ載っていることを確かめてから Loading を出す
            DialogTouchInjection.tap(dialogContentRect.exactCenterX(), dialogContentRect.exactCenterY())
            assertEquals("前提: ダイアログの中身はタップを受け取る", 1, dialogTaps.get())

            harness.loading.show()
            settledContentRect(harness)
            InstrumentedStateSettling.assertWindowFocused(
                requireNotNull(harness.container).layoutHost,
                "前提: Loading の器が入力の宛先になっている",
            )

            DialogTouchInjection.tap(dialogContentRect.exactCenterX(), dialogContentRect.exactCenterY())
            assertEquals("ダイアログへのタップも遮られる", 1, dialogTaps.get())
            assertTrue("Loading は表示されたまま", harness.isPresenting)

            // Loading を閉じると、遮っていた入力はダイアログ側へ戻る
            harness.loading.hide()
            InstrumentedStateSettling.assertWindowFocused(
                dialogSession.contentView,
                "前提: 入力の宛先がダイアログへ戻っている",
            )
            DialogTouchInjection.tap(dialogContentRect.exactCenterX(), dialogContentRect.exactCenterY())
            assertEquals("閉じたあとはダイアログが受け取る", 2, dialogTaps.get())
        } finally {
            dialogSession.dismiss()
        }
    }

    @Test
    fun LD_AT_05_設定プロパティの_options_変更が次の表示から効く() = runBlocking<Unit> {
        val harness = newHarness()
        harness.loading.options = DialogOptions(
            overlayColor = OVERLAY_COLOR,
            isCanceledOnTouchOutside = true,
        )

        harness.loading.show()
        val contentRect = settledContentRectOnScreen(harness)
        val container = requireNotNull(harness.container)
        val overlayColor = LoadingLayoutObservation.readOnMain {
            (container.overlayView.background as? ColorDrawable)?.color
        }

        assertEquals("変更後の色で覆いが描かれる", OVERLAY_COLOR, overlayColor)

        // isCanceledOnTouchOutside を設定しても外側タップは無効のまま
        val (x, y) = outsidePointOf(contentRect)
        DialogTouchInjection.tap(x, y)
        val closed = InstrumentedDialogWaiting.waitUntil(NO_REACTION_WAIT_MILLIS) { !harness.isPresenting }
        assertFalse("外側タップでは閉じない", closed)

        harness.loading.hide()
    }

    @Test
    fun LD_WN_01_画面の変化をまたいで表示が継続し再配置される() = runBlocking<Unit> {
        // 画面の再生成を経た載せ直しを見るため、提示先の追跡は本番の面を使う
        val harness = LoadingTestHarness(ActivityLoadingPresentationSurface())
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_SIZE_PIXELS, CONTENT_SIZE_PIXELS).apply {
                // 比率と余白の両方が効く属性にして、寸法の変化が外形に現れるようにする
                ksDialogOptions = DialogOptions(
                    layoutArea = DialogLayoutArea.WINDOW,
                    dialogMargin = DialogEdgeInsets.ZERO,
                    proportionalWidth = PROPORTIONAL_WIDTH,
                    proportionalHeight = PROPORTIONAL_HEIGHT,
                )
                viewRecorder.record(this)
            }
        }
        val gate = LoadingTestGate()

        try {
            coroutineScope {
                val scope = async {
                    harness.loading.start(LoadingTestViewModel()) { _ -> gate.await() }
                }
                assertTrue(harness.waitUntilPresenting())
                val contentView = requireNotNull(harness.contentView)
                val containerBeforeRotation = requireNotNull(harness.container)
                val sizeBeforeRotation = LoadingLayoutObservation.hostSize(containerBeforeRotation)
                assertProportionalPlacement(harness, note = "回転前")

                // 固定後に添付を書き換えても、回転後の再配置は固定済みの値で行われる
                LoadingLayoutObservation.readOnMain {
                    contentView.ksDialogOptions = DialogOptions(
                        layoutArea = DialogLayoutArea.VISIBLE_AREA,
                        dialogMargin = DialogEdgeInsets(24.0),
                        proportionalWidth = 0.9,
                        proportionalHeight = 0.9,
                    )
                }

                rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

                // 向きの要求を出した時点ではまだ前の器が載っている。再生成の完了 (器の載せ直し)
                // を待たずに読むと、回転前の器をそのまま「回転後」として読んでしまう
                assertTrue(
                    "前提: 画面の再生成をまたいで器が載せ直されている",
                    InstrumentedDialogWaiting.waitUntil {
                        harness.container.let { it != null && it !== containerBeforeRotation }
                    },
                )
                assertTrue("画面の変化をまたいで表示は継続する", harness.waitUntilPresenting())
                assertSame("中身は作り直されない", contentView, harness.contentView)
                val containerAfterRotation = requireNotNull(harness.container)
                LoadingLayoutObservation.awaitSettled(containerAfterRotation)
                assertNotEquals(
                    "前提: 画面の寸法が実際に変わっている",
                    sizeBeforeRotation,
                    LoadingLayoutObservation.hostSize(containerAfterRotation),
                )
                assertTrue(
                    "実効値の固定は解けない",
                    LoadingLayoutObservation.readOnMain {
                        requireNotNull(harness.container).layoutHost.isLayoutSnapshotFrozen
                    },
                )
                assertProportionalPlacement(harness, note = "回転後")

                gate.open()
                scope.await()
                assertFalse("処理の完了で表示が消える", harness.isPresenting)
            }
        } finally {
            harness.tearDown()
            rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    // 組み立てと観察

    /** 提示先を実際の画面にした harness を組み立てる。 */
    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    /** 内容サイズ固定のカスタム View を、指定の属性を添付した形で登録する。 */
    private fun registerCustomView(
        harness: LoadingTestHarness,
        options: DialogOptions?,
        placement: DialogPlacement?,
    ) {
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_SIZE_PIXELS, CONTENT_SIZE_PIXELS).apply {
                if (options != null) {
                    ksDialogOptions = options
                }
                if (placement != null) {
                    ksDialogPlacement = placement
                }
            }
        }
    }

    /** 実効値の固定とレイアウトを待ってから、中身の外形 (器の面の座標) を読む。 */
    private fun settledContentRect(harness: LoadingTestHarness): Rect {
        val container = requireNotNull(harness.container)
        LoadingLayoutObservation.awaitSettled(container)
        return LoadingLayoutObservation.contentRect(container)
    }

    /** 実効値の固定とレイアウトを待ってから、中身の外形 (画面座標) を読む。 */
    private fun settledContentRectOnScreen(harness: LoadingTestHarness): Rect {
        val container = requireNotNull(harness.container)
        LoadingLayoutObservation.awaitSettled(container)
        return LoadingLayoutObservation.contentRectOnScreen(container)
    }

    /**
     * 比率サイズ・中央配置の実効値どおりに置かれていることを、器の面の寸法から確かめる。
     *
     * 基準領域はウィンドウ全体・余白 0 なので、期待値は面の寸法だけから導ける。
     */
    private fun assertProportionalPlacement(harness: LoadingTestHarness, note: String) {
        val container = requireNotNull(harness.container)
        LoadingLayoutObservation.awaitSettled(container)
        val (hostWidth, hostHeight) = LoadingLayoutObservation.hostSize(container)
        val rect = LoadingLayoutObservation.contentRect(container)
        val expectedWidth = hostWidth * PROPORTIONAL_WIDTH.toFloat()
        val expectedHeight = hostHeight * PROPORTIONAL_HEIGHT.toFloat()

        assertNear("$note: 幅が比率どおりでない", expectedWidth, rect.width())
        assertNear("$note: 高さが比率どおりでない", expectedHeight, rect.height())
        assertNear("$note: 水平中央でない", (hostWidth - expectedWidth) / 2f, rect.left)
        assertNear("$note: 垂直中央でない", (hostHeight - expectedHeight) / 2f, rect.top)
    }

    /** 画面の向きを変え、再生成が落ち着くまで待つ。 */
    private fun rotate(orientation: Int) {
        activityRule.scenario.onActivity { activity -> activity.requestedOrientation = orientation }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** 背後の画面のタップを数える面を敷く。 */
    private fun addBackgroundTapRecorder(): AtomicInteger {
        val taps = AtomicInteger(0)
        activityRule.scenario.onActivity { activity ->
            activity.hostContainer.addView(
                View(activity).apply {
                    isClickable = true
                    setOnClickListener { taps.incrementAndGet() }
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return taps
    }

    /** 提示中のダイアログ1枚。 */
    private class DialogSession(val contentView: View, val dismiss: () -> Unit) {
        fun dismiss() = dismiss.invoke()
    }

    /** タップを数える中身を持つダイアログを1枚出し、画面に載るまで待つ。 */
    private fun presentDialog(taps: AtomicInteger): DialogSession {
        val session = AtomicReference<DialogSession>()
        activityRule.scenario.onActivity { activity ->
            val contentView = FixedContentSizeView(
                activity,
                CONTENT_SIZE_PIXELS,
                CONTENT_SIZE_PIXELS,
            ).apply {
                isClickable = true
                setOnClickListener { taps.incrementAndGet() }
            }
            val resultChannel = DialogResultChannel()
            val container = DialogContainer(
                context = activity,
                contentView = contentView,
                resultChannel = resultChannel,
            )
            container.show()
            session.set(
                DialogSession(contentView) {
                    resultChannel.settle(DialogOutcome.Completed(true), DialogDismissalOrigin.REPORT)
                },
            )
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return requireNotNull(session.get())
    }

    /** 画素密度の丸めを吸収する許容差で一致を確かめる。 */
    private fun assertNear(message: String, expected: Float, actual: Int) {
        assertTrue(
            "$message (期待 ${expected.roundToInt()} 実測 $actual)",
            abs(expected - actual) <= TOLERANCE_PIXELS,
        )
    }

    private fun toPixels(dp: Double): Float {
        val density = LoadingLayoutObservation.readOnMain {
            InstrumentationRegistry.getInstrumentation()
                .targetContext.resources.displayMetrics.density
        }
        return (dp * density).toFloat()
    }

    private companion object {
        /** 基準領域をウィンドウ全体・余白 0 にした属性。端末の画面サイズに依らない期待値にするため。 */
        val WINDOW_AREA_WITHOUT_MARGIN = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets.ZERO,
        )

        /** 覆いの色の変更が観察できる、既定と明らかに違う色。 */
        const val OVERLAY_COLOR = 0x7FFF0000

        const val OFFSET_X_DP = 10.0
        const val OFFSET_Y_DP = 20.0
        const val ATTACHED_OFFSET_X_DP = 30.0

        /** カスタム View の内容サイズ (px)。 */
        const val CONTENT_SIZE_PIXELS = 200

        const val PROPORTIONAL_WIDTH = 0.5
        const val PROPORTIONAL_HEIGHT = 0.25

        /** 画素密度の丸めを吸収する許容差 (px)。 */
        const val TOLERANCE_PIXELS = 2f

        /** 反応が無いことを確かめるための待ち時間 (ミリ秒)。 */
        const val NO_REACTION_WAIT_MILLIS = 500L
    }
}
