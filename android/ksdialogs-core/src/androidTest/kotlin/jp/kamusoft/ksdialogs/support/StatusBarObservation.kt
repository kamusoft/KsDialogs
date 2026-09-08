package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs

/**
 * 画面を撮ってステータスバー領域の明るさを測る。
 *
 * ステータスバーが暗転したかどうかは実際の見えでしか判定できないため、
 * 実機・エミュレータの画面をそのまま撮って帯の明るさを比べる。
 * 時計などの表示更新で数画素は変わるので、判定には帯全体の平均を使う。
 */
internal object StatusBarObservation {

    /** 明るさが落ち着くまでの待ちの上限 (ミリ秒)。 */
    private const val SETTLE_TIMEOUT_MILLIS = 5_000L

    /** 落ち着き判定の間隔 (ミリ秒)。 */
    private const val SETTLE_INTERVAL_MILLIS = 150L

    /** 落ち着いたとみなす連続2回の差。 */
    private const val SETTLE_EPSILON = 0.5

    /** ステータスバー領域の高さ (px)。取得できなければ 0。 */
    fun heightPixels(activity: Activity): Int {
        val windowInsets = activity.window.decorView.rootWindowInsets ?: return 0
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsets.getInsets(WindowInsets.Type.statusBars()).top
        } else {
            @Suppress("DEPRECATION")
            windowInsets.systemWindowInsetTop
        }
    }

    /**
     * 画面の表示が落ち着くまで待ってから、ステータスバー領域の平均の明るさ (0〜255) を返す。
     *
     * @param heightPixels 測る帯の高さ
     */
    fun awaitStableMeanBrightness(heightPixels: Int): Double {
        require(heightPixels > 0) { "ステータスバー領域の高さが取れていない" }
        var previous = measureMeanBrightness(heightPixels)
        val deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(SETTLE_INTERVAL_MILLIS)
            val current = measureMeanBrightness(heightPixels)
            if (abs(current - previous) < SETTLE_EPSILON) {
                return current
            }
            previous = current
        }
        return previous
    }

    /** 現時点のステータスバー領域の平均の明るさ (0〜255)。 */
    private fun measureMeanBrightness(heightPixels: Int): Double {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val screen = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "画面を撮れなかった"
        }
        return try {
            meanBrightness(screen, heightPixels)
        } finally {
            screen.recycle()
        }
    }

    private fun meanBrightness(screen: Bitmap, heightPixels: Int): Double {
        val stripHeight = minOf(heightPixels, screen.height)
        val pixels = IntArray(screen.width * stripHeight)
        screen.getPixels(pixels, 0, screen.width, 0, 0, screen.width, stripHeight)
        var total = 0L
        for (pixel in pixels) {
            total += Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
        }
        return total.toDouble() / (pixels.size * 3)
    }
}
