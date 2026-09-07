package jp.kamusoft.ksdialogs.samples.kmp.android

import android.content.Context
import android.util.TypedValue
import android.widget.TextView

/**
 * インライン経路のカスタム Toast の中身。
 *
 * 登録経路と同じく地を自分で描き、登録経路と見分けが付くよう無彩色の配色にする。
 *
 * @param viewModel メッセージを運ぶ ViewModel
 */
internal class InlineToastCardView(
    context: Context,
    viewModel: InlineToastViewModel,
) : TextView(context) {

    init {
        text = viewModel.message
        setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP)
        setTextColor(SampleTheme.ON_SURFACE)
        background = context.roundedFillStroke(
            cornerRadiusDp = CORNER_RADIUS_DP,
            fillColor = SampleTheme.SURFACE_VARIANT,
            strokeWidthDp = STROKE_WIDTH_DP,
            strokeColor = SampleTheme.DIVIDER,
        )
        setPadding(
            context.dp(HORIZONTAL_PADDING_DP),
            context.dp(VERTICAL_PADDING_DP),
            context.dp(HORIZONTAL_PADDING_DP),
            context.dp(VERTICAL_PADDING_DP),
        )
    }

    private companion object {
        const val CORNER_RADIUS_DP = 12
        const val HORIZONTAL_PADDING_DP = 18
        const val VERTICAL_PADDING_DP = 11
        const val STROKE_WIDTH_DP = 1
        const val MESSAGE_TEXT_SIZE_SP = 14f
    }
}
