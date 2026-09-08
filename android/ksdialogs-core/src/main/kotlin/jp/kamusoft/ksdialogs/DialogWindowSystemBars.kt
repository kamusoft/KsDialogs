package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * ダイアログのウィンドウがシステムバーの見えを変えないようにする調整。
 *
 * システムバーの背景を自分で描かないウィンドウには、システムが既定の暗い覆いを敷く。
 * 覆いの色に透明を指定してもステータスバーだけが暗転して見えるのはこれが理由なので、
 * ウィンドウ自身がシステムバーの背景を描く構成に切り替え、その色を透明にする。
 * 提示の構成 (Gravity や padding) には手を加えないため、配置規則は覆いの色によらず同じになる (core/ADR-0008)。
 *
 * あわせてウィンドウの範囲を画面全体に広げる。基準領域 `window` はウィンドウ全体を指す契約であり、
 * システムバーの分だけ狭い範囲を割り当てられると、同じ属性でも OS ごとに rect が変わってしまう
 * (システムバーを除いた領域は基準領域 `visibleArea` が受け持つ)。
 *
 * システムバーの表示/非表示・隠れたバーの再表示の作法・アイコンの明暗 (明るい背景向けの濃色アイコンか
 * どうか) も、提示先の画面の設定をそのまま引き継ぐ。システムバーを隠している画面でダイアログを出しても
 * バーが再出現しないのは、この引き継ぎによる。
 */
internal object DialogWindowSystemBars {

    /**
     * システムバーの背景をウィンドウ自身の責任にし、色を透明にしたうえで、
     * ウィンドウの範囲と中身の置き場を画面全体に広げる。ウィンドウを画面へ載せる前に行う。
     */
    @Suppress("DEPRECATION")
    fun makeSystemBarBackgroundsTransparent(dialogWindow: Window) {
        dialogWindow.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                // ウィンドウの範囲を画面全体にする
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // システム領域の幅は insets として受け取り続ける
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
        )
        dialogWindow.statusBarColor = Color.TRANSPARENT
        dialogWindow.navigationBarColor = Color.TRANSPARENT
        // 既定では中身がシステム領域の分だけ内側に寄せられる。
        // 基準領域の使い分けはこのライブラリ側の計算で行うため、寄せずに画面全体を中身の置き場にする
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialogWindow.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            dialogWindow.decorView.systemUiVisibility =
                dialogWindow.decorView.systemUiVisibility or CONTENT_UNDER_SYSTEM_BARS
        }
    }

    /**
     * システムバーの表示状態と見た目を提示先の画面から引き継ぐ。
     *
     * 引き継ぎ元を読めるのはウィンドウが画面に載った後なので、載った時点で呼ぶ。
     * 引き継ぎはこの1回だけで、以降に提示先が状態を変えても追随しない
     * (追随させると提示先と器の二重管理になるため。core/ADR-0006 の委譲の考え方)。
     *
     * Android 11 以上では status / navigation それぞれの可視状態と、隠れたバーの再表示の作法
     * (systemBarsBehavior)、アイコンの明暗を個別に引き継ぐ。
     * Android 11 未満では systemUiVisibility の丸ごとのコピーが、可視状態と明暗の両方を運ぶ。
     *
     * @param hostWindow 提示先の画面のウィンドウ。取得できない場合は何もしない
     */
    fun inheritSystemBarState(dialogWindow: Window, hostWindow: Window?) {
        if (hostWindow == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            inheritWithInsetsController(dialogWindow, hostWindow)
        } else {
            @Suppress("DEPRECATION")
            dialogWindow.decorView.systemUiVisibility =
                hostWindow.decorView.systemUiVisibility or CONTENT_UNDER_SYSTEM_BARS
        }
    }

    /** Android 11 以上の引き継ぎ。可視状態・作法・明暗を提示先から写す。 */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun inheritWithInsetsController(dialogWindow: Window, hostWindow: Window) {
        val hostController = hostWindow.insetsController ?: return
        val dialogController = dialogWindow.insetsController ?: return
        dialogController.setSystemBarsAppearance(hostController.systemBarsAppearance, APPEARANCE_MASK)
        dialogController.systemBarsBehavior = hostController.systemBarsBehavior
        // 可視状態は提示先のウィンドウに実際に届いている insets から読む。
        // どちらのバーが隠れているかは種類ごとに違うため、種類ごとに写す
        val hostInsets = hostWindow.decorView.rootWindowInsets ?: return
        inheritVisibility(dialogController, hostInsets, WindowInsets.Type.statusBars())
        inheritVisibility(dialogController, hostInsets, WindowInsets.Type.navigationBars())
    }

    /** バー1種類分の可視状態を写す。 */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun inheritVisibility(
        dialogController: WindowInsetsController,
        hostInsets: WindowInsets,
        type: Int,
    ) {
        if (hostInsets.isVisible(type)) {
            dialogController.show(type)
        } else {
            dialogController.hide(type)
        }
    }

    /** 引き継ぐ見た目 (アイコンの明暗) の範囲。 */
    private val APPEARANCE_MASK: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        } else {
            0
        }

    /** 中身をシステムバーの下まで置くための指定 (Android 11 未満の経路)。 */
    @Suppress("DEPRECATION")
    private const val CONTENT_UNDER_SYSTEM_BARS: Int =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
}

/** この Context がたどれる画面 (Activity)。画面に紐づかない Context なら null。 */
internal fun Context.hostActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) {
            return current
        }
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
