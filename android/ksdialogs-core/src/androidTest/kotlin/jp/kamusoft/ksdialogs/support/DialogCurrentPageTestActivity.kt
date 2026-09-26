package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.roundToInt

/**
 * 表示中のページを基準にした配置を確かめる提示先。下部バーとページ領域を持つ画面を模す。
 *
 * 画面の構成は上から「上部バー (既定は高さ 0)」「ページ領域」「下部バー」の縦並びで、
 * ページ領域の View がアプリの登録する「表示中のページ」にあたる。画面の回転では作り直さず
 * (マニフェストで構成の変化を自分で受ける)、同じ画面のまま寸法だけが変わる。
 *
 * 起動引数で次を切り替える。
 *
 * - [EXTRA_EDGE_TO_EDGE]: 画面をシステムバーの下まで広げる (ページ領域がステータスバーの下まで伸びる)
 * - [EXTRA_TOP_BAR_HEIGHT_DP]: 上部バーの高さ (dp)。ページ領域の上端をずらすのに使う
 */
class DialogCurrentPageTestActivity : Activity() {

    /** 表示中のページにあたる View。 */
    lateinit var pageView: FrameLayout
        private set

    /** 下部バー (ボトムナビゲーション相当)。 */
    lateinit var bottomBar: View
        private set

    /** 画面全体の根。別の View を重ねて置くときに使う。 */
    lateinit var rootContainer: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_EDGE_TO_EDGE, false)) {
            extendIntoSystemBars()
        }
        val density = resources.displayMetrics.density
        fun toPixels(dp: Int): Int = (dp * density).roundToInt()

        val topBar = View(this).apply { setBackgroundColor(Color.DKGRAY) }
        pageView = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        bottomBar = View(this).apply { setBackgroundColor(Color.BLUE) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                topBar,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    toPixels(intent.getIntExtra(EXTRA_TOP_BAR_HEIGHT_DP, 0)),
                ),
            )
            addView(pageView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(
                bottomBar,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, toPixels(BOTTOM_BAR_HEIGHT_DP)),
            )
        }
        rootContainer = FrameLayout(this).apply { addView(column) }
        setContentView(rootContainer)
    }

    /** 画面をシステムバーの下まで広げる。 */
    private fun extendIntoSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    companion object {
        /** 画面をシステムバーの下まで広げるか (Boolean)。 */
        const val EXTRA_EDGE_TO_EDGE: String = "edgeToEdge"

        /** 上部バーの高さ (dp、Int)。 */
        const val EXTRA_TOP_BAR_HEIGHT_DP: String = "topBarHeightDp"

        /**
         * 下部バーの高さ (dp)。
         *
         * 画面がシステムバーの下まで広がる端末でも、ページ領域の下端がナビゲーションバーより上に来る高さにする。
         */
        const val BOTTOM_BAR_HEIGHT_DP: Int = 80
    }
}
