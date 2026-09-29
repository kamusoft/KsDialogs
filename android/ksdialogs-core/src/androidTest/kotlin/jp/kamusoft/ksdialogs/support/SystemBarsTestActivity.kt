package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * 器がシステムバーの指定を変えないことを確かめる検証の提示先になる画面。
 *
 * 背景とステータスバーの帯を白に固定し、アイコンの明暗の指定のしかたを起動時の引数で選べる。
 * 明るい地に明るい地向け (暗い色) のアイコンを出せるので、白地に白いアイコン同士を比べるような
 * 差の出ない状態を避けられる。
 *
 * 隠れたバーの再表示の作法はこの画面では指定しない (作法を指定しない提示先として使えるように)。
 * バーを隠す・作法を指定する操作は、各検証が起動後に行う。
 */
class SystemBarsTestActivity : Activity() {

    /** アイコンの明暗の指定のしかた。 */
    enum class Appearance {
        /** 明暗を指定しない。 */
        UNSPECIFIED,

        /**
         * ステータスバーとナビゲーションバーのアイコンを明るい地向けに指定する。
         * Android 11 以上は WindowInsetsController で、それ未満は旧来のフラグで指定する。
         */
        LIGHT_BARS,

        /** 明るい地向けの明暗を、旧来のフラグ (systemUiVisibility) だけで指定する。 */
        LIGHT_BARS_BY_LEGACY_FLAGS_ONLY,

        /**
         * テーマ (windowLightStatusBar / windowLightNavigationBar) で明るい地向けにしたうえで、
         * プラットフォームの WindowInsetsController から暗い地向けを明示する (Android 11 以上)。
         * テーマが立てた旧来のフラグは残るが、明示があるので OS はそれを無視し、見えは暗い地向けになる。
         */
        LIGHT_THEME_DARK_BY_CONTROLLER,

        /**
         * テーマで明るい地向けにしたうえで、旧来のフラグを代入し直して明るい地向けのフラグを外す
         * (旧来の edge-to-edge の書き方。WindowInsetsController は呼ばない)。
         * OS は明示の無いビットの見えを旧来のフラグから作るので、見えは暗い地向けになる。
         */
        LIGHT_THEME_LEGACY_FLAGS_CLEARED,
    }

    /** 検証対象の View を載せる場所。 */
    lateinit var hostContainer: FrameLayout
        private set

    /** この画面自身の描画が画面へ送り出されたか。起動時の表示と見分けるために使う。 */
    @Volatile
    var hasCommittedOwnFrame: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearance = requestedAppearance()
        if (appearance == Appearance.LIGHT_THEME_DARK_BY_CONTROLLER ||
            appearance == Appearance.LIGHT_THEME_LEGACY_FLAGS_CLEARED
        ) {
            // テーマの明暗はウィンドウの装飾が作られるときに読まれるので、それより前に切り替える
            setTheme(jp.kamusoft.ksdialogs.test.R.style.KsDialogsTest_LightBarsTheme)
        }
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.WHITE
        hostContainer = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        setContentView(hostContainer)
        applyAppearance(appearance)
        watchOwnFrameCommit()
    }

    private fun requestedAppearance(): Appearance =
        intent.getStringExtra(EXTRA_APPEARANCE)?.let { Appearance.valueOf(it) } ?: Appearance.UNSPECIFIED

    private fun applyAppearance(appearance: Appearance) {
        when (appearance) {
            Appearance.UNSPECIFIED -> Unit
            Appearance.LIGHT_BARS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.setSystemBarsAppearance(
                        LIGHT_BARS_APPEARANCE,
                        LIGHT_BARS_APPEARANCE,
                    )
                } else {
                    applyLegacyLightFlags()
                }
            Appearance.LIGHT_BARS_BY_LEGACY_FLAGS_ONLY -> applyLegacyLightFlags()
            Appearance.LIGHT_THEME_DARK_BY_CONTROLLER ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.setSystemBarsAppearance(0, LIGHT_BARS_APPEARANCE)
                }
            Appearance.LIGHT_THEME_LEGACY_FLAGS_CLEARED -> {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLegacyLightFlags() {
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or flags
    }

    /** この画面自身の最初の描画が送り出された時点を記録する。 */
    private fun watchOwnFrameCommit() {
        val observer = window.decorView.viewTreeObserver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            observer.registerFrameCommitCallback { hasCommittedOwnFrame = true }
        } else {
            // 描画の直後に積んだ処理が走る時点を、送り出しの近似として使う
            observer.addOnDrawListener(
                object : ViewTreeObserver.OnDrawListener {
                    override fun onDraw() {
                        window.decorView.post {
                            hasCommittedOwnFrame = true
                        }
                    }
                },
            )
        }
    }

    companion object {
        /** 明暗の指定のしかたを渡す引数名。値は [Appearance] の名前。 */
        private const val EXTRA_APPEARANCE: String = "systemBarsAppearance"

        /** 明るい地向けのステータスバーとナビゲーションバーを表す明暗の値 (Android 11 以上の表現)。 */
        const val LIGHT_BARS_APPEARANCE: Int =
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS

        /** 明暗の指定のしかたを選んでこの画面を起動する Intent。 */
        fun intent(context: Context, appearance: Appearance): Intent =
            Intent(context, SystemBarsTestActivity::class.java)
                .putExtra(EXTRA_APPEARANCE, appearance.name)
    }
}
