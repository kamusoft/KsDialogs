package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * レイアウト検証の提示先になる画面。
 *
 * ステータスバーの見えを比べる検証のため、背景とステータスバーの色を明示して固定する
 * (端末の既定色に依存すると、覆いが敷かれたかどうかの判定が端末ごとに変わってしまう)。
 */
class DialogLayoutTestActivity : Activity() {

    /** 検証対象の View を載せる場所。 */
    lateinit var hostContainer: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.statusBarColor = STATUS_BAR_COLOR
        hostContainer = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        setContentView(hostContainer)
    }

    private companion object {
        /** 覆いが敷かれれば明らかに暗くなる、明るいステータスバーの色。 */
        const val STATUS_BAR_COLOR: Int = Color.WHITE
    }
}
