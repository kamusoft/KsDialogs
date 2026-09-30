package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.ToastContainer
import jp.kamusoft.ksdialogs.windowVisibleAreaInsets
import kotlin.math.roundToInt

/**
 * ウィンドウに載った Toast の器のレイアウト結果を観察する。
 *
 * 実効値の固定はレイアウトパスの完了時点で行われるため、外形を読む前に固定とレイアウトの
 * 両方が済むのを待つ。
 */
internal object ToastLayoutObservation {

    /** 実効値の固定とレイアウトの完了を待つ上限 (ミリ秒)。 */
    private const val SETTLE_TIMEOUT_MILLIS = 10_000L

    /** 待ち合わせの間隔 (ミリ秒)。 */
    private const val POLLING_INTERVAL_MILLIS = 16L

    /** 実効値が固定され、レイアウトが終わるまで待つ。 */
    fun awaitSettled(container: ToastContainer) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val settled = LoadingLayoutObservation.readOnMain {
                container.layoutHost.isLayoutSnapshotFrozen && container.layoutHost.isLaidOut
            }
            if (settled) {
                return
            }
            Thread.sleep(POLLING_INTERVAL_MILLIS)
        }
        error("Toast の器の実効値が固定されなかった")
    }

    /** 中身の外形 (器の面の座標・px)。 */
    fun contentRect(container: ToastContainer): Rect = LoadingLayoutObservation.readOnMain {
        val holder = container.layoutHost.contentHolder
        Rect(holder.left, holder.top, holder.right, holder.bottom)
    }

    /**
     * 器の面が基準にする可視領域 (器の面の座標・px)。
     *
     * 器が配置の計算に使うのと同じ求め方 (ウィンドウが報告するシステム領域の幅) で、
     * 面の矩形からシステム領域を除いた範囲を返す。
     */
    fun visibleArea(container: ToastContainer): Rect = LoadingLayoutObservation.readOnMain {
        val host = container.layoutHost
        val insets = windowVisibleAreaInsets(host)
        Rect(
            insets.left.roundToInt(),
            insets.top.roundToInt(),
            host.width - insets.right.roundToInt(),
            host.height - insets.bottom.roundToInt(),
        )
    }

    /** 中身の外形 (画面座標・px)。実入力の注入と画面の実測に使う。 */
    fun contentRectOnScreen(container: ToastContainer): Rect = LoadingLayoutObservation.readOnMain {
        LoadingLayoutObservation.rectOnScreen(container.layoutHost.contentHolder)
    }
}
