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
 * バーが再出現しないのは、この引き継ぎによる。Dialog / Loading / Toast の器はどれも全画面のウィンドウで、
 * 前面にあるだけでステータスバーの明暗やバーの制御を左右し得るため、3 つの器がすべてこの引き継ぎを通す
 * (器は提示先の画面のシステムバーの指定を変えない。core/ADR-0039)。
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

    /**
     * 器のウィンドウが画面に載った時点で、提示先の画面からシステムバーの指定を1回だけ引き継ぐ。
     *
     * 器が作られた時点の提示先ではなく、載った時点の提示先から読む。画面の作り直しで器を載せ替えるときは
     * 新しい器が作り直し後の画面を提示先にして作られるので、載せ替え先の画面の指定が採られる (core/ADR-0039)。
     *
     * @param hostWindow 提示先の画面のウィンドウを求める方法。載った時点で1回だけ呼ぶ
     */
    fun inheritSystemBarStateWhenAttached(dialogWindow: Window, hostWindow: () -> Window?) {
        dialogWindow.decorView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    inheritSystemBarState(dialogWindow, hostWindow())
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            },
        )
    }

    /** Android 11 以上の引き継ぎ。可視状態・作法・明暗を提示先から写す。 */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun inheritWithInsetsController(dialogWindow: Window, hostWindow: Window) {
        val hostController = hostWindow.insetsController ?: return
        val dialogController = dialogWindow.insetsController ?: return
        dialogController.setSystemBarsAppearance(hostAppearance(hostWindow, hostController), APPEARANCE_MASK)
        // 提示先が作法を指定していないとき、Android 11〜13 は 0 (「触れたら出す」の定数と同じ値) を返す。
        // Android 12 以降は既定の作法 (スワイプで出す) が 0 とは別の値なので、そのまま写すと器だけが
        // 「触れたら出す」を明示したことになり、提示先の既定の作法とずれる。Android 11 は 0 が既定
        // そのものなので、写しても写さなくても結果は変わらない。
        // そこで 0 のときは写さず、器も OS の既定に任せる。「触れたら出す」を明示した提示先はこの扱いで
        // 取りこぼすが、非推奨の作法なので受け入れる (core/ADR-0039)
        val hostBehavior = hostController.systemBarsBehavior
        if (hostBehavior != UNSPECIFIED_BEHAVIOR) {
            dialogController.systemBarsBehavior = hostBehavior
        }
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

    /**
     * 提示先の画面が指定しているアイコンの明暗。
     *
     * OS が返す値は、systemBarsAppearance で指定された明暗だけを含み、旧来のフラグ
     * (systemUiVisibility の明るいステータスバー / ナビゲーションバーのフラグ) で指定された明暗を含まない。
     * 旧来のフラグだけで明暗を指定する画面でも指定を変えないよう、両方を合わせて読む (core/ADR-0039)。
     * 旧来のフラグは、OS が明暗の判定に使うのと同じく、画面の View 全体から集めた値を読む。
     *
     * ただし旧来のフラグには、テーマ (windowLightStatusBar / windowLightNavigationBar) が立てたものも
     * 混ざる。OS は、systemBarsAppearance で明暗を明示したビットでは旧来のフラグを無視し、明示していない
     * ビットでは旧来のフラグから見えを作る。
     * - Android 15 以上: OS の返す値は、明示したビットでは明示どおり、明示していないビットではテーマの値になる
     *   (旧来のフラグの操作は反映されない)。明示の有無はビットごとには公開 API で読めないので、テーマの属性で
     *   ビットを分けて読む
     *   - テーマが明るい地向けにしていないビット: テーマ由来の値は無いので、OS の値と旧来のフラグの
     *     どちらかが立っていれば明るい地向けと読む (旧来のフラグだけで明暗を指定する画面を拾う)
     *   - テーマが明るい地向けにしているビット: OS の値と旧来のフラグの両方が立っているときだけ明るい地向けと
     *     読む。コードで暗い地向けを明示した画面は OS の値が 0 になり、旧来のフラグの代入でテーマの明るい
     *     地向けを外した画面は旧来のフラグが 0 になるので、どちらも暗い地向けとして引き継げる。
     *     プラットフォームの WindowInsetsController で明るい地向けを明示しながら旧来のフラグも外した画面だけは
     *     暗い地向けと読み違えるが、androidx の WindowInsetsControllerCompat は両方をそろえて切り替えるので
     *     この形にはならない (core/ADR-0039)
     * - Android 11〜14: OS の返す値からテーマ由来と明示を見分けられないため、旧来のフラグをそのまま足す。
     *   テーマが明るい地向けで、プラットフォームの WindowInsetsController から暗い地向けを明示した画面では、
     *   器が明るい地向けになることを受け入れる (よくある「テーマで明るい地向けにし、コードでは何も
     *   指定しない」画面を正しく保つほうを採る。androidx の WindowInsetsControllerCompat 経由の指定は
     *   旧来のフラグも切り替えるので、この食い違いは起きない。core/ADR-0039)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun hostAppearance(hostWindow: Window, hostController: WindowInsetsController): Int {
        val reported = hostController.systemBarsAppearance
        @Suppress("DEPRECATION")
        val legacyAppearance = lightBarsOfLegacyFlags(hostWindow.decorView.windowSystemUiVisibility)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return reported or legacyAppearance
        }
        val themeBits = themeLightBars(hostWindow)
        val withoutTheme = (reported or legacyAppearance) and themeBits.inv()
        val withTheme = reported and legacyAppearance and themeBits
        return withoutTheme or withTheme
    }

    /** 旧来のフラグが表す明暗を、systemBarsAppearance の表現で返す。 */
    @RequiresApi(Build.VERSION_CODES.R)
    @Suppress("DEPRECATION")
    private fun lightBarsOfLegacyFlags(flags: Int): Int {
        var appearance = 0
        if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0) {
            appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        }
        if (flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR != 0) {
            appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        }
        return appearance
    }

    /** 提示先の画面のテーマが指定している明暗を、systemBarsAppearance の表現で返す。 */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun themeLightBars(hostWindow: Window): Int {
        val attributes = hostWindow.context.obtainStyledAttributes(THEME_LIGHT_BAR_ATTRIBUTES)
        return try {
            var appearance = 0
            if (attributes.getBoolean(0, false)) {
                appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            }
            if (attributes.getBoolean(1, false)) {
                appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            }
            appearance
        } finally {
            attributes.recycle()
        }
    }

    /** テーマの明暗の属性。並びは [themeLightBars] の読み出し位置と対応する。 */
    private val THEME_LIGHT_BAR_ATTRIBUTES: IntArray = intArrayOf(
        android.R.attr.windowLightStatusBar,
        android.R.attr.windowLightNavigationBar,
    )

    /** 作法を指定していないウィンドウについて OS が返し得る値。 */
    private const val UNSPECIFIED_BEHAVIOR: Int = 0

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
