package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.ToastContainer

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

    /** 中身の外形 (画面座標・px)。実入力の注入と画面の実測に使う。 */
    fun contentRectOnScreen(container: ToastContainer): Rect = LoadingLayoutObservation.readOnMain {
        LoadingLayoutObservation.rectOnScreen(container.layoutHost.contentHolder)
    }
}
