package jp.kamusoft.ksdialogs.support

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 画面を撮って、指定した領域に実際に見えている色を測る。
 *
 * ウィンドウどうしの重なり順は、載っているウィンドウの一覧を数えても分からない
 * (どちらが手前かは実際に描かれた画素にしか現れない)。そのため重なりに関わる観察は
 * 実機・エミュレータの画面をそのまま撮り、対象の領域の平均色で判定する。
 */
internal object ToastScreenObservation {

    /** 色が落ち着くまでの待ちの上限 (ミリ秒)。 */
    private const val SETTLE_TIMEOUT_MILLIS = 5_000L

    /** 落ち着き判定の間隔 (ミリ秒)。 */
    private const val SETTLE_INTERVAL_MILLIS = 100L

    /** 落ち着いたとみなす連続2回のチャンネルごとの差。 */
    private const val SETTLE_EPSILON = 2

    /** 測る領域を対象の中心へ寄せる比率。縁の角丸・影を巻き込まないようにする。 */
    private const val SAMPLE_RATIO = 0.4f

    /**
     * 画面の表示が落ち着くまで待ってから、領域の中心付近の平均色を返す。
     *
     * @param rect 測る対象の外形 (画面座標・px)
     */
    fun awaitStableMeanColor(rect: Rect): Int {
        var previous = measureMeanColor(rect)
        val deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(SETTLE_INTERVAL_MILLIS)
            val current = measureMeanColor(rect)
            if (isClose(current, previous, SETTLE_EPSILON)) {
                return current
            }
            previous = current
        }
        return previous
    }

    /** 2つの色がチャンネルごとに [tolerance] 以内で一致するか。 */
    fun isClose(left: Int, right: Int, tolerance: Int): Boolean =
        abs(Color.red(left) - Color.red(right)) <= tolerance &&
            abs(Color.green(left) - Color.green(right)) <= tolerance &&
            abs(Color.blue(left) - Color.blue(right)) <= tolerance

    /** 色を読みやすい文字列にする。失敗メッセージ用。 */
    fun describe(color: Int): String =
        "#%02X%02X%02X".format(Color.red(color), Color.green(color), Color.blue(color))

    /** その色の明るさ (0〜255)。 */
    fun brightness(color: Int): Double =
        (Color.red(color) + Color.green(color) + Color.blue(color)) / 3.0

    private fun measureMeanColor(rect: Rect): Int {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val screen = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "画面を撮れなかった"
        }
        return try {
            meanColor(screen, sampleRegion(rect, screen))
        } finally {
            screen.recycle()
        }
    }

    /** 対象の中心付近を、画面の内側へ収めた測定領域。 */
    private fun sampleRegion(rect: Rect, screen: Bitmap): Rect {
        val insetX = (rect.width() * (1 - SAMPLE_RATIO) / 2).toInt()
        val insetY = (rect.height() * (1 - SAMPLE_RATIO) / 2).toInt()
        val left = max(0, rect.left + insetX)
        val top = max(0, rect.top + insetY)
        val right = min(screen.width, max(left + 1, rect.right - insetX))
        val bottom = min(screen.height, max(top + 1, rect.bottom - insetY))
        return Rect(left, top, right, bottom)
    }

    private fun meanColor(screen: Bitmap, region: Rect): Int {
        val width = region.width()
        val height = region.height()
        require(width > 0 && height > 0) { "測る領域が画面の外にある: $region" }
        val pixels = IntArray(width * height)
        screen.getPixels(pixels, 0, width, region.left, region.top, width, height)
        var red = 0L
        var green = 0L
        var blue = 0L
        for (pixel in pixels) {
            red += Color.red(pixel)
            green += Color.green(pixel)
            blue += Color.blue(pixel)
        }
        val count = pixels.size
        return Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    }
}
