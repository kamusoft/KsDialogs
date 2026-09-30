package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference

/**
 * 提示先と同じ Activity で出した、Activity とは別のウィンドウ (モーダル) を模す。
 *
 * MAUI の Android 版がモーダルのページを載せるダイアログウィンドウにあたる。ウィンドウは画面の左上から
 * ずらして置き、Activity のメインウィンドウとは原点が違うようにする。ウィンドウの中身全体がページの View。
 */
internal class SameActivityModalWindow private constructor(
    private val dialog: Dialog,
    /** モーダルのウィンドウに載ったページの View。 */
    val pageView: View,
) {

    /** モーダルのウィンドウの根の View。 */
    val windowRoot: View
        get() = pageView.rootView

    /** モーダルを閉じ、閉じ終わるまで待つ。 */
    fun dismiss() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { dialog.dismiss() }
        instrumentation.waitForIdleSync()
    }

    companion object {
        /**
         * モーダルを出し、ページの View がレイアウトされるまで待つ。
         *
         * @param activity モーダルを出す画面。提示先と同じ Activity を渡す
         * @param leftDp ウィンドウの左端の位置 (dp)
         * @param topDp ウィンドウの上端の位置 (dp)。ステータスバーより下になる値にする
         * @param widthDp ウィンドウの幅 (dp)
         * @param heightDp ウィンドウの高さ (dp)
         */
        fun show(
            activity: Activity,
            leftDp: Int,
            topDp: Int,
            widthDp: Int,
            heightDp: Int,
        ): SameActivityModalWindow {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val shown = AtomicReference<SameActivityModalWindow>()
            instrumentation.runOnMainSync {
                val density = activity.resources.displayMetrics.density
                fun toPixels(dp: Int): Int = (dp * density).toInt()

                val page = FrameLayout(activity).apply { setBackgroundColor(Color.YELLOW) }
                val dialog = Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar).apply {
                    setContentView(page)
                    window?.apply {
                        setBackgroundDrawable(ColorDrawable(Color.YELLOW))
                        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        setLayout(toPixels(widthDp), toPixels(heightDp))
                        attributes = attributes.apply {
                            gravity = Gravity.TOP or Gravity.START
                            x = toPixels(leftDp)
                            y = toPixels(topDp)
                        }
                    }
                }
                dialog.show()
                shown.set(SameActivityModalWindow(dialog, page))
            }
            instrumentation.waitForIdleSync()
            val modal = requireNotNull(shown.get())
            val laidOut = AtomicReference(false)
            instrumentation.runOnMainSync { laidOut.set(modal.pageView.isLaidOut && modal.pageView.width > 0) }
            check(laidOut.get() == true) { "モーダルのページの View がレイアウトされなかった" }
            return modal
        }
    }
}
