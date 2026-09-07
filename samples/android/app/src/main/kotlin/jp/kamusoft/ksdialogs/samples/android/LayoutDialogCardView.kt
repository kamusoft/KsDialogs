package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Layout Dialog の中身。
 *
 * 覆い (scrim) と配置はライブラリの器が受け持つため、このカード自体だけを描く。
 * 寄せ先の違いが見て取れるよう、幅は Basic Dialog のカードより狭く取る。
 *
 * @param message 表示するメッセージ
 * @param onCancel キャンセル操作
 * @param onComplete 完了操作
 */
internal class LayoutDialogCardView(
    context: Context,
    message: String,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        background = context.roundedFill(CARD_CORNER_RADIUS_DP, SampleTheme.SURFACE)
        setPadding(
            context.dp(CARD_HORIZONTAL_PADDING_DP),
            context.dp(CARD_TOP_PADDING_DP),
            context.dp(CARD_HORIZONTAL_PADDING_DP),
            context.dp(CARD_BOTTOM_PADDING_DP),
        )
        addView(messageLabel(context, message))
        addView(
            buttonRow(context, onCancel, onComplete),
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(MESSAGE_BOTTOM_SPACING_DP)
            },
        )
    }

    /**
     * カードの横幅を固定する。
     *
     * 器は中身を wrap content で配置するため、幅の決定はこの View 自身が行う。
     * 画面が狭い端末では器が確保した幅に収める。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = context.dp(CARD_WIDTH_DP)
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            desiredWidth
        } else {
            minOf(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    private fun messageLabel(context: Context, message: String): TextView =
        TextView(context).apply {
            text = message
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

    private fun buttonRow(context: Context, onCancel: () -> Unit, onComplete: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            val cancelButton = actionButton(
                context = context,
                label = SampleText.CANCEL_ACTION,
                fillColor = SampleTheme.SURFACE_VARIANT,
                textColor = SampleTheme.ON_SURFACE,
                bold = false,
                onClick = onCancel,
            )
            val completeButton = actionButton(
                context = context,
                label = SampleText.COMPLETE_ACTION,
                fillColor = SampleTheme.PRIMARY,
                textColor = SampleTheme.ON_PRIMARY,
                bold = true,
                onClick = onComplete,
            )
            addView(cancelButton, equalWidthParams(context, leadingGap = false))
            addView(completeButton, equalWidthParams(context, leadingGap = true))
        }

    private fun equalWidthParams(context: Context, leadingGap: Boolean): LayoutParams =
        LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            if (leadingGap) {
                leftMargin = context.dp(BUTTON_GAP_DP)
            }
        }

    private fun actionButton(
        context: Context,
        label: String,
        fillColor: Int,
        textColor: Int,
        bold: Boolean,
        onClick: () -> Unit,
    ): Button = Button(context).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, BUTTON_TEXT_SIZE_SP)
        setTextColor(textColor)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        background = context.roundedFill(BUTTON_CORNER_RADIUS_DP, fillColor)
        // 既定のボタンは押下時に浮き上がる。カードの平面的な見た目に合わせて外す
        stateListAnimator = null
        minWidth = 0
        minimumWidth = 0
        // ボタンの高さは Material の推奨タップ領域 48dp を下限にする
        minHeight = context.dp(BUTTON_MIN_HEIGHT_DP)
        minimumHeight = context.dp(BUTTON_MIN_HEIGHT_DP)
        setPadding(0, context.dp(BUTTON_VERTICAL_PADDING_DP), 0, context.dp(BUTTON_VERTICAL_PADDING_DP))
        setOnClickListener { onClick() }
    }

    private companion object {
        const val CARD_WIDTH_DP = 240
        const val CARD_CORNER_RADIUS_DP = 20
        const val CARD_TOP_PADDING_DP = 24
        const val CARD_HORIZONTAL_PADDING_DP = 20
        const val CARD_BOTTOM_PADDING_DP = 20
        const val MESSAGE_TEXT_SIZE_SP = 15f
        const val MESSAGE_BOTTOM_SPACING_DP = 16
        const val BUTTON_GAP_DP = 10
        const val BUTTON_CORNER_RADIUS_DP = 10
        const val BUTTON_TEXT_SIZE_SP = 14f
        const val BUTTON_VERTICAL_PADDING_DP = 10
        const val BUTTON_MIN_HEIGHT_DP = 48
    }
}
