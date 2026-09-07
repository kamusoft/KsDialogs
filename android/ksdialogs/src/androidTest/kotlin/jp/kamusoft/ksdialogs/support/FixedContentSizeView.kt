package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.view.View

/**
 * 指定した内容サイズを要求する中身の View。
 *
 * 共通ケース表の contentSize をそのまま内容サイズとして与えるために使う。
 * 与えられる余地が内容サイズより狭ければ、その余地いっぱいまでに収まる。
 *
 * @param contentWidth 要求する幅 (px)
 * @param contentHeight 要求する高さ (px)
 */
internal class FixedContentSizeView(
    context: Context,
    private val contentWidth: Int,
    private val contentHeight: Int,
) : View(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(contentWidth, widthMeasureSpec),
            resolveSize(contentHeight, heightMeasureSpec),
        )
    }
}
