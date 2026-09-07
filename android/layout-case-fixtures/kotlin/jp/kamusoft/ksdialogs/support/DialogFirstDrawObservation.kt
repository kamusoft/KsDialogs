package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred

/**
 * 最初に描かれる直前の外形 (画面座標・px) を記録する。
 *
 * 提示の時点でどう出ていたかは、描かれる直前の実測でしか確かめられない。
 */
internal fun View.observeFirstDrawRect(destination: CompletableDeferred<Rect>) {
    viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                val location = IntArray(2)
                getLocationOnScreen(location)
                destination.complete(
                    Rect(location[0], location[1], location[0] + width, location[1] + height),
                )
                return true
            }
        },
    )
}

/**
 * 画面へ実際のタップを送る。
 *
 * 外側タップの扱いは覆いのヒットテストとクリック検出を通した実際の操作でしか確かめられないため、
 * 器の関数を直に呼ばず、入力そのものを注入する。
 */
internal object DialogTouchInjection {

    /** 押してから離すまでの間隔 (ミリ秒)。長押しにならない程度に置く。 */
    private const val TAP_DURATION_MILLIS = 50L

    /**
     * 画面座標の1点をタップする。
     *
     * @param x 画面座標の x (px)
     * @param y 画面座標の y (px)
     */
    fun tap(x: Float, y: Float) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        val down = touchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
        val up = touchEvent(downTime, downTime + TAP_DURATION_MILLIS, MotionEvent.ACTION_UP, x, y)
        try {
            instrumentation.sendPointerSync(down)
            instrumentation.sendPointerSync(up)
        } finally {
            down.recycle()
            up.recycle()
        }
        instrumentation.waitForIdleSync()
    }

    private fun touchEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
    ): MotionEvent = MotionEvent.obtain(downTime, eventTime, action, x, y, 0).apply {
        source = InputDevice.SOURCE_TOUCHSCREEN
    }
}

/**
 * ダイアログの外形の外側にあたる点 (画面座標)。
 *
 * 中身の左側に十分な余地があればそこを、なければ下側を選ぶ。
 *
 * @param contentRect 中身の外形 (画面座標)
 */
internal fun outsidePointOf(contentRect: Rect): Pair<Float, Float> = when {
    contentRect.left > MINIMUM_OUTSIDE_MARGIN_PIXELS ->
        (contentRect.left / 2f) to contentRect.exactCenterY()

    else -> contentRect.exactCenterX() to (contentRect.bottom + MINIMUM_OUTSIDE_MARGIN_PIXELS).toFloat()
}

/** 外側と判断するのに十分な余地 (px)。 */
private const val MINIMUM_OUTSIDE_MARGIN_PIXELS = 40
