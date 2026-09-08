package jp.kamusoft.ksdialogs.support

import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.DialogLayoutHost
import jp.kamusoft.ksdialogs.DialogLayoutSnapshot
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPixelInsets
import jp.kamusoft.ksdialogs.ksDialogOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * ダイアログを実際の画面に載せたまま、ウィンドウの寸法と可視領域の余白を後から変えられる舞台。
 *
 * 表示中の変化への追随は「載せて測る」だけでは確かめられないため、実効値を固定したあとで
 * 寸法・余白を変え、同じ器がどこへ置き直されるかを実測する。
 * 寸法と余白はケースが与えた値を使い、環境差を期待値の分岐ではなく入力の差で表す。
 */
internal class DialogWindowGeometryStage private constructor(
    private val scenario: ActivityScenario<DialogLayoutTestActivity>,
    private val host: DialogLayoutHost,
    /** 器が置いている中身の View。固定後の添付の書き換えにも使う。 */
    val contentView: View,
    private val density: Float,
    private val insets: AtomicReference<DialogLayoutCase.Insets>,
) {

    /** 実効値を固定済みか。 */
    val isLayoutSnapshotFrozen: Boolean
        get() = host.isLayoutSnapshotFrozen

    /** ウィンドウの寸法と可視領域の余白を変え、レイアウトが終わるまで待つ。 */
    fun changeGeometry(screen: DialogLayoutCase.Size, insets: DialogLayoutCase.Insets) {
        this.insets.set(insets)
        awaitNextLayout {
            host.layoutParams = FrameLayout.LayoutParams(
                toPixels(screen.w).roundToInt(),
                toPixels(screen.h).roundToInt(),
            )
        }
    }

    /** 中身に添付する静的メタ属性を書き換える。固定後は表示に反映されない。 */
    fun rewriteAttachedOptions(options: DialogOptions) {
        scenario.onActivity { contentView.ksDialogOptions = options }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** 今の中身の外形を論理単位 (dp) で読む。原点はウィンドウの左上。 */
    fun contentRect(): DialogLayoutCase.Rect {
        val rect = AtomicReference<DialogLayoutCase.Rect>()
        scenario.onActivity {
            val holder = host.contentHolder
            rect.set(
                DialogLayoutCase.Rect(
                    x = holder.left / density.toDouble(),
                    y = holder.top / density.toDouble(),
                    w = holder.width / density.toDouble(),
                    h = holder.height / density.toDouble(),
                ),
            )
        }
        return requireNotNull(rect.get())
    }

    /** 舞台を片付ける。 */
    fun dispose() {
        scenario.onActivity { activity -> activity.hostContainer.removeAllViews() }
    }

    private fun toPixels(value: Double): Float = (value * density).toFloat()

    /**
     * レイアウトを変える操作を UI スレッドで行い、次のレイアウトパスが終わるまで待つ。
     *
     * 余白だけを変えた場合は器自身の外形が変わらないため、器ではなくウィンドウ全体の
     * レイアウト完了を待つ。
     */
    private fun awaitNextLayout(change: () -> Unit) {
        val laidOut = CountDownLatch(1)
        scenario.onActivity {
            val observer = host.viewTreeObserver
            observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this)
                    laidOut.countDown()
                }
            })
            change()
            host.requestLayout()
        }
        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "レイアウトが完了しなかった" }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    companion object {
        /** レイアウト完了・実効値の固定を待つ上限。 */
        private const val LAYOUT_TIMEOUT_SECONDS = 10L

        /** 実効値の固定を待つ間隔 (ミリ秒)。 */
        private const val FREEZE_POLLING_INTERVAL_MILLIS = 16L

        /**
         * 指定の寸法・余白のウィンドウへ器を載せ、実効値が固定されるまで待つ。
         *
         * @param options 中身の View に添付する静的メタ属性
         * @param contentSize 中身が要求する内容サイズ (dp)
         */
        fun present(
            scenario: ActivityScenario<DialogLayoutTestActivity>,
            screen: DialogLayoutCase.Size,
            insets: DialogLayoutCase.Insets,
            contentSize: DialogLayoutCase.Size,
            options: DialogOptions,
        ): DialogWindowGeometryStage {
            val stage = AtomicReference<DialogWindowGeometryStage>()
            val currentInsets = AtomicReference(insets)

            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                fun toPixels(value: Double): Float = (value * density).toFloat()

                val contentView = FixedContentSizeView(
                    context = activity,
                    contentWidth = toPixels(contentSize.w).roundToInt(),
                    contentHeight = toPixels(contentSize.h).roundToInt(),
                ).apply { ksDialogOptions = options }

                val host = DialogLayoutHost(
                    context = activity,
                    snapshot = DialogLayoutSnapshot(contentView, null),
                    contentView = contentView,
                    visibleAreaInsets = {
                        val effective = currentInsets.get()
                        DialogPixelInsets(
                            top = toPixels(effective.top),
                            left = toPixels(effective.left),
                            bottom = toPixels(effective.bottom),
                            right = toPixels(effective.right),
                        )
                    },
                )
                activity.hostContainer.addView(
                    host,
                    FrameLayout.LayoutParams(
                        toPixels(screen.w).roundToInt(),
                        toPixels(screen.h).roundToInt(),
                    ),
                )
                stage.set(
                    DialogWindowGeometryStage(scenario, host, contentView, density, currentInsets),
                )
            }

            val presented = requireNotNull(stage.get())
            presented.awaitLayoutSnapshotFrozen()
            return presented
        }
    }

    /** 実効値が固定されるまで待つ。固定は描画の直前に行われるため、描画パスが回るのを待つ。 */
    private fun awaitLayoutSnapshotFrozen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + LAYOUT_TIMEOUT_SECONDS * 1_000
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val frozen = AtomicReference(false)
            scenario.onActivity { frozen.set(host.isLayoutSnapshotFrozen && host.isLaidOut) }
            if (frozen.get() == true) {
                return
            }
            Thread.sleep(FREEZE_POLLING_INTERVAL_MILLIS)
        }
        error("実効値が固定されなかった")
    }
}
