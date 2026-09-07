package jp.kamusoft.ksdialogs.compose.support

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout

/**
 * 宣言的 UI の中身を実際にダイアログとして提示するための画面。
 *
 * ライブラリは提示先を「前面にある画面」として自動解決するため、この画面が resumed であることが
 * 提示の前提になる。画面そのものは何も表示しない。
 */
class ComposeDialogTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.WHITE) })
    }
}
