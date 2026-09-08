package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutMeasurement
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogPresentationProbe
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.LateAttachingContentView
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.assertMatchesRect
import jp.kamusoft.ksdialogs.support.attach
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * メタ属性の供給と優先順位 (show 引数 > コンテンツ添付 > 契約既定値) を確かめる。
 *
 * 期待 rect は軸別レイアウト規則から導いてある。供給経路そのものを見たいので、
 * 基準領域と余白は単純な条件 (ウィンドウ全体・余白なし) を土台にする。
 */
@RunWith(AndroidJUnit4::class)
class DialogAttributeSupplyTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 添付だけで_options_と_placement_が供給される() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            screen = SCREEN,
            insets = INSETS,
            contentSize = CONTENT_SIZE,
            options = plainOptions(),
            attachedPlacement = DialogPlacement(horizontalAlignment = DialogAlignment.END),
            label = "添付のみ",
        )

        // 水平は End (400 − 280)、垂直はウィンドウ基準の Center ((800 − 180) / 2)
        assertMatchesRect(DialogLayoutCase.Rect(x = 120.0, y = 310.0, w = 280.0, h = 180.0), actual, "添付のみ")
    }

    @Test
    fun 添付した覆いの色が器に反映される() {
        val container = presentContainer(
            options = DialogOptions(overlayColor = Color.TRANSPARENT),
            attachedPlacement = null,
            showPlacement = null,
        )
        val overlayColor = (requireNotNull(container.layoutHost).overlayView.background as ColorDrawable).color
        dismiss(container)

        assertEquals(Color.TRANSPARENT, overlayColor)
    }

    @Test
    fun show_引数の_placement_が添付の_placement_に勝つ() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            screen = SCREEN,
            insets = INSETS,
            contentSize = CONTENT_SIZE,
            options = plainOptions(),
            attachedPlacement = DialogPlacement(horizontalAlignment = DialogAlignment.END),
            showPlacement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
            label = "show 引数の優先",
        )

        // 水平は show 引数の Start。垂直の 310 は添付した options (ウィンドウ基準) が
        // 引き続き効いていることを示す (可視領域基準なら 320 になる)
        assertMatchesRect(DialogLayoutCase.Rect(x = 0.0, y = 310.0, w = 280.0, h = 180.0), actual, "show 引数の優先")
    }

    @Test
    fun show_引数の_placement_は添付をオブジェクト単位で置換する() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            screen = SCREEN,
            insets = INSETS,
            contentSize = CONTENT_SIZE,
            options = plainOptions(),
            attachedPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.END,
                offsetX = 30.0,
                offsetY = 40.0,
            ),
            showPlacement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
            label = "オブジェクト単位の置換",
        )

        // 実効 placement は show 引数のオブジェクト全体 (水平 Start・垂直 Center・移動量 0)。
        // フィールド単位で合成していれば垂直 End と移動量が残り、(30, 660) になる
        assertMatchesRect(
            DialogLayoutCase.Rect(x = 0.0, y = 310.0, w = 280.0, h = 180.0),
            actual,
            "オブジェクト単位の置換",
        )
    }

    @Test
    fun 無効値は正規化された実効値として扱われる() {
        val actual = DialogLayoutMeasurement.measureDialogRect(
            scenario = activityRule.scenario,
            screen = SCREEN,
            insets = INSETS,
            contentSize = CONTENT_SIZE,
            options = DialogOptions(
                layoutArea = DialogLayoutArea.WINDOW,
                dialogMargin = DialogEdgeInsets(top = -10.0, left = Double.NaN, bottom = 0.0, right = 0.0),
                proportionalWidth = Double.NaN,
                proportionalHeight = 1.5,
            ),
            attachedPlacement = DialogPlacement(offsetX = Double.POSITIVE_INFINITY),
            label = "無効値の正規化",
        )

        // 比率は 幅 = 非有限で未指定 / 高さ = 1 へ丸め、余白は 上 = 負で 0 / 左 = 非有限で既定 24、
        // 移動量は非有限で 0。水平は有効領域 (24〜400) の中央、垂直は基準 rect いっぱいになる
        assertMatchesRect(
            DialogLayoutCase.Rect(x = 72.0, y = 0.0, w = 280.0, h = 800.0),
            actual,
            "無効値の正規化",
        )
    }

    @Test
    fun 初回レイアウト完了前の添付変更は採用される() {
        // 静的に End / End を添付した器。遅れて届いた添付が採用されたかの答え合わせに使う
        val reference = presentContainer(
            options = plainOptions(),
            attachedPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.END,
            ),
            showPlacement = null,
        )
        awaitSnapshotFrozen(reference)
        val referenceRect = contentRectOf(reference)
        dismiss(reference)

        val outcomes = mutableListOf<DialogOutcome>()
        val resultChannel = DialogResultChannel()
        resultChannel.onSettle { outcomes.add(it) }
        // 中身が自分の測定の中で添付し直す = View の生成後・初回レイアウトパスの完了前の供給
        val container = presentLateAttachingContainer(resultChannel) { view ->
            view.ksDialogOptions = plainOptions().copy(isCanceledOnTouchOutside = false)
            view.ksDialogPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.END,
            )
        }
        awaitSnapshotFrozen(container)
        val actualRect = contentRectOf(container)

        container.reportOutsideTap()
        // 閉鎖そのものも結果を確定させるため、外側タップの効き目は閉じる前に読み取る
        val settledByOutsideTap = outcomes.size
        dismiss(container)

        // 器の構築時に固定していれば Start / Start のままになり、参照の器と一致しない
        assertEquals("パス完了時点の添付 (End / End) が採用されること", referenceRect, actualRect)
        assertEquals("操作挙動も完了前に届いた値 (false) に従う", 0, settledByOutsideTap)
    }

    @Test
    fun 初回レイアウト完了後の添付変更は表示にも操作にも反映されない() {
        val outcomes = mutableListOf<DialogOutcome>()
        val resultChannel = DialogResultChannel()
        resultChannel.onSettle { outcomes.add(it) }
        val container = presentContainer(
            options = plainOptions(),
            attachedPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
            ),
            showPlacement = null,
            resultChannel = resultChannel,
        )
        awaitSnapshotFrozen(container)
        val rectAtFirstLayoutPass = contentRectOf(container)

        // 提示済みのダイアログの中身に、別の配置と外側タップの扱いを添付し直す
        activityRule.scenario.onActivity {
            container.contentView.ksDialogOptions = plainOptions().copy(isCanceledOnTouchOutside = false)
            container.contentView.ksDialogPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.END,
                offsetX = 50.0,
            )
        }
        relayout(container)
        val rectAfterChange = contentRectOf(container)

        container.reportOutsideTap()
        dismiss(container)

        assertEquals("位置と見えはスナップショットのまま", rectAtFirstLayoutPass, rectAfterChange)
        assertEquals("外側タップの扱いもスナップショット時点の値 (既定 true) に従う", 1, outcomes.size)
        assertTrue(outcomes.first() is DialogOutcome.Cancelled)
    }

    @Test
    fun factory_の添付と_show_の引数が合成される() = runBlocking<Unit> {
        val attachedRect = captureRectAtFirstDraw(showPlacement = null)
        val showArgumentRect = captureRectAtFirstDraw(
            showPlacement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
            ),
        )

        // factory が添付したのは End / End。show 引数を渡した側だけが前端へ寄る
        assertTrue(
            "show 引数の水平配置が効いていない (添付 $attachedRect / show 引数 $showArgumentRect)",
            showArgumentRect.left < attachedRect.left,
        )
        assertTrue(
            "show 引数の垂直配置が効いていない (添付 $attachedRect / show 引数 $showArgumentRect)",
            showArgumentRect.top < attachedRect.top,
        )
    }

    /** factory が End / End を添付するダイアログを1枚出し、最初に描かれた時点の外形を返す。 */
    private suspend fun captureRectAtFirstDraw(showPlacement: DialogPlacement?): Rect =
        DialogPresentationProbe.captureRectAtFirstDraw(
            viewModelClass = PlainTestDialogViewModel::class,
            viewModel = PlainTestDialogViewModel(),
            placement = showPlacement,
            createContentView = { _, context ->
                TextView(context).apply { text = "供給の合成" }.attach(
                    options = DialogOptions(),
                    placement = DialogPlacement(
                        horizontalAlignment = DialogAlignment.END,
                        verticalAlignment = DialogAlignment.END,
                    ),
                )
            },
        )

    /** メタ属性を添付した中身でダイアログを1枚出す。 */
    private fun presentContainer(
        options: DialogOptions?,
        attachedPlacement: DialogPlacement?,
        showPlacement: DialogPlacement?,
        resultChannel: DialogResultChannel = DialogResultChannel(),
    ): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        activityRule.scenario.onActivity { activity ->
            val density = activity.resources.displayMetrics.density
            val contentView = FixedContentSizeView(
                context = activity,
                contentWidth = (CONTENT_SIZE.w * density).toInt(),
                contentHeight = (CONTENT_SIZE.h * density).toInt(),
            ).attach(options, attachedPlacement)
            DialogContainer(
                context = activity,
                contentView = contentView,
                resultChannel = resultChannel,
                placement = showPlacement,
            ).also {
                container.set(it)
                it.show()
            }
        }
        awaitLayout(container.get())
        return container.get()
    }

    /** 初回レイアウトパスの中で添付し直す中身でダイアログを1枚出す。 */
    private fun presentLateAttachingContainer(
        resultChannel: DialogResultChannel,
        attachDuringLayout: (View) -> Unit,
    ): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        activityRule.scenario.onActivity { activity ->
            val density = activity.resources.displayMetrics.density
            val contentView = LateAttachingContentView(
                context = activity,
                contentWidth = (CONTENT_SIZE.w * density).toInt(),
                contentHeight = (CONTENT_SIZE.h * density).toInt(),
                attachDuringLayout = attachDuringLayout,
            ).attach(
                plainOptions(),
                DialogPlacement(
                    horizontalAlignment = DialogAlignment.START,
                    verticalAlignment = DialogAlignment.START,
                ),
            )
            DialogContainer(
                context = activity,
                contentView = contentView,
                resultChannel = resultChannel,
                placement = null,
            ).also {
                container.set(it)
                it.show()
            }
        }
        awaitLayout(container.get())
        return container.get()
    }

    /** 実効値がスナップショットとして固定されるまで待つ。 */
    private fun awaitSnapshotFrozen(container: DialogContainer) {
        val host = requireNotNull(container.layoutHost) { "覆いの面が組み立てられていない" }
        val frozen = AtomicReference(false)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LAYOUT_TIMEOUT_SECONDS)
        while (System.nanoTime() < deadline) {
            activityRule.scenario.onActivity { frozen.set(host.isLayoutSnapshotFrozen) }
            if (frozen.get()) {
                return
            }
            Thread.sleep(SNAPSHOT_POLL_INTERVAL_MILLIS)
        }
        error("実効値がスナップショットとして固定されなかった")
    }

    /** 中身の外形 (画面座標)。 */
    private fun contentRectOf(container: DialogContainer): Rect {
        val rect = AtomicReference<Rect>()
        activityRule.scenario.onActivity {
            val location = IntArray(2)
            container.contentView.getLocationOnScreen(location)
            rect.set(
                Rect(
                    location[0],
                    location[1],
                    location[0] + container.contentView.width,
                    location[1] + container.contentView.height,
                ),
            )
        }
        return rect.get()
    }

    /** もう一度レイアウトパスを走らせる。 */
    private fun relayout(container: DialogContainer) {
        val host = requireNotNull(container.layoutHost)
        val laidOut = CountDownLatch(1)
        activityRule.scenario.onActivity {
            host.viewTreeObserver.addOnGlobalLayoutListener { laidOut.countDown() }
            host.requestLayout()
        }
        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "再レイアウトが完了しなかった" }
    }

    /** 覆いの面がレイアウトを終えるまで待つ。 */
    private fun awaitLayout(container: DialogContainer) {
        val host = requireNotNull(container.layoutHost) { "覆いの面が組み立てられていない" }
        val laidOut = CountDownLatch(1)
        activityRule.scenario.onActivity {
            if (host.isLaidOut) {
                laidOut.countDown()
                return@onActivity
            }
            host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> laidOut.countDown() }
        }
        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "レイアウトが完了しなかった" }
    }

    private fun dismiss(container: DialogContainer) {
        activityRule.scenario.onActivity { container.dismiss() }
    }

    private companion object {
        /** 期待値の導出を単純にする画面条件。 */
        val SCREEN = DialogLayoutCase.Size(w = 400.0, h = 800.0)
        val INSETS = DialogLayoutCase.Insets(top = 50.0, left = 0.0, bottom = 30.0, right = 0.0)
        val CONTENT_SIZE = DialogLayoutCase.Size(w = 280.0, h = 180.0)

        const val LAYOUT_TIMEOUT_SECONDS = 10L

        /** スナップショットの固定を待つときの見に行く間隔 (ミリ秒)。 */
        const val SNAPSHOT_POLL_INTERVAL_MILLIS = 16L

        /** ウィンドウ全体を基準にし余白を持たない静的メタ属性。 */
        fun plainOptions(): DialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets.ZERO,
        )
    }
}
