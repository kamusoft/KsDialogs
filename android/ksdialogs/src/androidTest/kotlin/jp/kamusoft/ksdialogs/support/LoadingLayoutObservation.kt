package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.LoadingContainer
import java.util.concurrent.atomic.AtomicReference

/**
 * ウィンドウに載った Loading の器のレイアウト結果を観察する。
 *
 * 実効値の固定はレイアウトパスの完了時点で行われるため、外形を読む前に固定とレイアウトの
 * 両方が済むのを待つ。
 */
internal object LoadingLayoutObservation {

    /** 実効値の固定とレイアウトの完了を待つ上限 (ミリ秒)。 */
    private const val SETTLE_TIMEOUT_MILLIS = 10_000L

    /** 待ち合わせの間隔 (ミリ秒)。 */
    private const val POLLING_INTERVAL_MILLIS = 16L

    /** 実効値が固定され、レイアウトが終わるまで待つ。 */
    fun awaitSettled(container: LoadingContainer) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val settled = readOnMain {
                container.layoutHost.isLayoutSnapshotFrozen && container.layoutHost.isLaidOut
            }
            if (settled) {
                return
            }
            Thread.sleep(POLLING_INTERVAL_MILLIS)
        }
        error("Loading の器の実効値が固定されなかった")
    }

    /** 中身の外形 (器の面の座標・px)。 */
    fun contentRect(container: LoadingContainer): Rect = readOnMain {
        val holder = container.layoutHost.contentHolder
        Rect(holder.left, holder.top, holder.right, holder.bottom)
    }

    /** 器の面の大きさ (px)。 */
    fun hostSize(container: LoadingContainer): Pair<Int, Int> = readOnMain {
        container.layoutHost.width to container.layoutHost.height
    }

    /** 中身の外形 (画面座標・px)。実入力の注入に使う。 */
    fun contentRectOnScreen(container: LoadingContainer): Rect = readOnMain {
        rectOnScreen(container.layoutHost.contentHolder)
    }

    /** View の外形 (画面座標・px)。 */
    fun rectOnScreen(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    /** UI スレッドで値を読む。 */
    fun <T> readOnMain(read: () -> T): T {
        val value = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value.set(read()) }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }
}
