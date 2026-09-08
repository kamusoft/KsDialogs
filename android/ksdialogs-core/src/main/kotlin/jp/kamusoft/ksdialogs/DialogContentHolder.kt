package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * 中身の View を包み、ダイアログの外形そのものになる面。
 *
 * 受け持つのは2つ:
 *
 * - 外形の大きさ: 親から与えられた制約をそのまま中身へ渡し、自分の大きさを中身に合わせる
 * - タップの遮り: 中身の上のタップが背後の覆いへ抜けないようにする
 *
 * 角丸や枠線といった見た目は中身の View 自身の責務であり、この面は手を加えない (core/ADR-0014)。
 *
 * @param contentView 中身として表示する View
 */
internal class DialogContentHolder(
    context: Context,
    private val contentView: View,
) : ViewGroup(context) {

    init {
        // 中身の上のタップを覆いへ通さないため、この面でタップを受け止める
        isClickable = true
        // 出入りの演出で中身が外形の外へ動く (滑り出し等) ため、中身を外形で切り取らない
        clipChildren = false
        addView(contentView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val layoutParams = contentView.layoutParams
        contentView.measure(
            contentMeasureSpec(widthMeasureSpec, layoutParams?.width ?: LayoutParams.WRAP_CONTENT),
            contentMeasureSpec(heightMeasureSpec, layoutParams?.height ?: LayoutParams.WRAP_CONTENT),
        )
        setMeasuredDimension(
            resolveSize(contentView.measuredWidth, widthMeasureSpec),
            resolveSize(contentView.measuredHeight, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        contentView.layout(0, 0, contentView.measuredWidth, contentView.measuredHeight)
    }

    /**
     * 中身に渡す測定条件を決める。
     *
     * サイズが決まっている軸では中身をその大きさいっぱいに広げ、
     * 内容サイズに委ねる軸では中身の指定 (LayoutParams) に従って測る。
     */
    private fun contentMeasureSpec(measureSpec: Int, contentDimension: Int): Int =
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.EXACTLY) {
            measureSpec
        } else {
            getChildMeasureSpec(measureSpec, 0, contentDimension)
        }
}
