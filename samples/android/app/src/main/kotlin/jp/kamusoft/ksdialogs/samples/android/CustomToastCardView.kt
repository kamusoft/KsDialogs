package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 登録経路のカスタム Toast の中身。
 *
 * Toast には覆いが無く、デフォルト View も使わないため、この中身が自分で地を描く。
 *
 * @param viewModel メッセージを運ぶ ViewModel
 */
internal class CustomToastCardView(
    context: Context,
    viewModel: CustomToastViewModel,
) : LinearLayout(context) {

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = context.roundedFill(CORNER_RADIUS_DP, SampleTheme.PRIMARY)
        setPadding(
            context.dp(HORIZONTAL_PADDING_DP),
            context.dp(VERTICAL_PADDING_DP),
            context.dp(HORIZONTAL_PADDING_DP),
            context.dp(VERTICAL_PADDING_DP),
        )

        addView(
            badge(context),
            LayoutParams(context.dp(BADGE_SIZE_DP), context.dp(BADGE_SIZE_DP)),
        )
        addView(
            messageLabel(context, viewModel.message),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = context.dp(BADGE_SPACING_DP)
            },
        )
    }

    private fun badge(context: Context): TextView =
        TextView(context).apply {
            text = BADGE_MARK
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, BADGE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            // 一辺と同じ半径の角丸は円になる
            background = context.roundedFill(BADGE_SIZE_DP / 2, SampleTheme.ON_PRIMARY)
        }

    private fun messageLabel(context: Context, message: String): TextView =
        TextView(context).apply {
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
        }

    private companion object {
        /** バッジの中に出す記号。文言ではなく見た目の記号なので View 側に持つ。 */
        const val BADGE_MARK = "✓"
        const val CORNER_RADIUS_DP = 12
        const val HORIZONTAL_PADDING_DP = 18
        const val VERTICAL_PADDING_DP = 12
        const val BADGE_SIZE_DP = 20
        const val BADGE_SPACING_DP = 10
        const val BADGE_TEXT_SIZE_SP = 13f
        const val MESSAGE_TEXT_SIZE_SP = 14f
    }
}
