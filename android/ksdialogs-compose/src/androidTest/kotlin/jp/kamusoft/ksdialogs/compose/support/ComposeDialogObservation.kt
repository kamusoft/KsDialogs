package jp.kamusoft.ksdialogs.compose.support

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.CompletableDeferred

/**
 * 最初に**描かれた**時点の外形 (画面座標・px) を記録する。
 *
 * 描画の直前 (preDraw) では、実効値を固定するために描画を見送って組み直す回が混ざり得るため、
 * 提示された姿を確かめるには実際に描かれた回で測る。
 */
internal fun View.observeFirstDrawnRect(destination: CompletableDeferred<Rect>) {
    val observer = viewTreeObserver
    lateinit var listener: ViewTreeObserver.OnDrawListener
    listener = ViewTreeObserver.OnDrawListener {
        if (destination.isCompleted) {
            return@OnDrawListener
        }
        val location = IntArray(2)
        getLocationOnScreen(location)
        destination.complete(Rect(location[0], location[1], location[0] + width, location[1] + height))
        // 描画の通知中は登録を外せないため、通知を抜けてから外す。
        // 抜けるまでにダイアログが閉じていれば観察先ごと消えているので、そのときは何もしない
        post {
            if (observer.isAlive) {
                observer.removeOnDrawListener(listener)
            }
        }
    }
    observer.addOnDrawListener(listener)
}

/** 現在の外形 (画面座標・px)。 */
internal fun View.rectOnScreen(): Rect {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Rect(location[0], location[1], location[0] + width, location[1] + height)
}
