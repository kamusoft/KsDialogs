package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.view.View

/**
 * 初回のレイアウトパスの最中に、自分自身へメタ属性を添付する中身の View。
 *
 * 宣言的 UI からの供給 (値が届くのがレイアウトパスの中になる機構) を模した供給元で、
 * 「View の生成後・初回レイアウトパスの完了前」に書き換わる添付を作るために使う。
 *
 * @param contentWidth 要求する幅 (px)
 * @param contentHeight 要求する高さ (px)
 * @param attachDuringLayout 初回のレイアウトパスの中で1度だけ呼ばれる添付処理
 */
internal class LateAttachingContentView(
    context: Context,
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val attachDuringLayout: (View) -> Unit,
) : View(context) {

    /** レイアウトパスの中で添付を行った回数。1度だけ供給したことの確認に使う。 */
    var attachCount: Int = 0
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (attachCount == 0) {
            attachCount++
            attachDuringLayout(this)
        }
        setMeasuredDimension(
            resolveSize(contentWidth, widthMeasureSpec),
            resolveSize(contentHeight, heightMeasureSpec),
        )
    }
}
