package jp.kamusoft.ksdialogs.samples.kmp.android

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * レイアウト属性を調整してからダイアログを表示する画面。
 *
 * 初期値は契約の既定値 (中央配置・移動なし・可視領域基準) に揃える。
 *
 * @param onBack メニュー画面へ戻る操作
 * @param onShow 調整した属性でダイアログを出す操作
 */
internal class SampleLayoutPanelView(
    context: Context,
    onBack: () -> Unit,
    onShow: () -> Unit,
) : LinearLayout(context) {

    private val horizontalSegments = SampleAlignmentSegmentsView(context, SampleText.HORIZONTAL_LABEL)
    private val verticalSegments = SampleAlignmentSegmentsView(context, SampleText.VERTICAL_LABEL)
    private val offsetXField: EditText
    private val offsetYField: EditText
    private val visibleAreaSwitch: Switch
    private val resultValue: TextView
    private val resultArea: LinearLayout

    init {
        offsetXField = offsetField(context, SampleText.OFFSET_X_LABEL)
        offsetYField = offsetField(context, SampleText.OFFSET_Y_LABEL)
        visibleAreaSwitch = visibleAreaSwitch(context)

        orientation = VERTICAL
        setBackgroundColor(SampleTheme.SURFACE)
        fitsSystemWindows = true

        addView(header(context, onBack, onShow))
        addView(divider(context))
        addView(settingRow(context, SampleText.HORIZONTAL_LABEL, horizontalSegments))
        addView(divider(context))
        addView(settingRow(context, SampleText.VERTICAL_LABEL, verticalSegments))
        addView(divider(context))
        addView(settingRow(context, SampleText.OFFSET_X_LABEL, offsetXField))
        addView(divider(context))
        addView(settingRow(context, SampleText.OFFSET_Y_LABEL, offsetYField))
        addView(divider(context))
        addView(settingRow(context, SampleText.USE_VISIBLE_AREA_LABEL, visibleAreaSwitch))
        addView(divider(context))

        resultValue = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_VALUE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            typeface = Typeface.MONOSPACE
        }
        resultArea = resultAreaView(context, resultValue)
        addView(resultArea)
    }

    /** サイズと位置の計算に可視領域を使うか。 */
    val usesVisibleArea: Boolean
        get() = visibleAreaSwitch.isChecked

    /** 調整中の置き場所。数値として読めない入力は 0 として扱う。 */
    fun placement(): DialogPlacement = DialogPlacement(
        horizontalAlignment = horizontalSegments.selection.alignment,
        verticalAlignment = verticalSegments.selection.alignment,
        offsetX = offsetXField.text.toString().toDoubleOrNull() ?: 0.0,
        offsetY = offsetYField.text.toString().toDoubleOrNull() ?: 0.0,
    )

    /** 直近の結果を表示する。 */
    fun showResult(result: String) {
        resultValue.text = result
        resultArea.visibility = View.VISIBLE
    }

    private fun header(context: Context, onBack: () -> Unit, onShow: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_TOP_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_BOTTOM_PADDING_DP),
            )

            val back = TextView(context).apply {
                text = BACK_CHEVRON
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, BACK_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
                // 記号1文字でも押しやすさの下限を満たす領域を確保する
                minimumWidth = context.dp(ACTION_MIN_SIZE_DP)
                minimumHeight = context.dp(ACTION_MIN_SIZE_DP)
                isClickable = true
                isFocusable = true
                // 記号のままでは読み上げが操作の意味を伝えないため、名前と役割を与える
                contentDescription = BACK_DESCRIPTION
                accessibilityDelegate = SampleButtonRoleDelegate
                setOnClickListener { onBack() }
            }
            val title = TextView(context).apply {
                text = SampleText.LAYOUT_DIALOG_ITEM
                setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
                typeface = Typeface.DEFAULT_BOLD
            }
            val show = Button(context).apply {
                text = SampleText.SHOW_ACTION
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, SHOW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                background = context.roundedFill(SHOW_CORNER_RADIUS_DP, SampleTheme.PRIMARY)
                // 既定のボタンは押下時に浮き上がる。平面的な見た目に合わせて外す
                stateListAnimator = null
                minWidth = 0
                minimumWidth = 0
                minHeight = context.dp(ACTION_MIN_SIZE_DP)
                minimumHeight = context.dp(ACTION_MIN_SIZE_DP)
                setPadding(context.dp(SHOW_HORIZONTAL_PADDING_DP), 0, context.dp(SHOW_HORIZONTAL_PADDING_DP), 0)
                setOnClickListener { onShow() }
            }

            addView(back)
            addView(
                title,
                LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = context.dp(TITLE_LEADING_GAP_DP)
                },
            )
            addView(show)
        }

    private fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(SampleTheme.DIVIDER)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.dp(DIVIDER_HEIGHT_DP))
        }

    private fun settingRow(context: Context, label: String, control: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // 調整用の行も押しやすさの下限を満たす高さを確保する
            minimumHeight = context.dp(ROW_MIN_HEIGHT_DP)
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
            )

            val title = TextView(context).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
            }
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(control)
        }

    private fun resultAreaView(context: Context, valueLabel: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            // 一度もダイアログを閉じていない間は表示するものがない
            visibility = View.GONE
            setBackgroundColor(SampleTheme.SURFACE_VARIANT)
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
            )

            val caption = TextView(context).apply {
                text = SampleText.RESULT_CAPTION
                setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_CAPTION_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE_MUTED)
            }
            addView(caption)
            addView(
                valueLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(RESULT_CAPTION_SPACING_DP)
                },
            )
        }

    /**
     * 移動量を入れる数値欄を作る。
     *
     * @param label 行の項目名。画面には出さず、読み上げ名として与える。
     *   入力中の値は欄の文字列としてそのまま読み上げ情報に乗る
     */
    private fun offsetField(context: Context, label: String): EditText = EditText(context).apply {
        setText(OFFSET_INITIAL_VALUE)
        contentDescription = label
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SIZE_SP)
        setTextColor(SampleTheme.ON_SURFACE)
        typeface = Typeface.MONOSPACE
        background = context.roundedStroke(FIELD_CORNER_RADIUS_DP, FIELD_STROKE_WIDTH_DP, SampleTheme.DIVIDER)
        setPadding(context.dp(FIELD_HORIZONTAL_PADDING_DP), 0, context.dp(FIELD_HORIZONTAL_PADDING_DP), 0)
        layoutParams = LayoutParams(context.dp(FIELD_WIDTH_DP), context.dp(FIELD_HEIGHT_DP))
    }

    /** 基準領域を切り替えるトグルを作る。初期は可視領域基準。 */
    private fun visibleAreaSwitch(context: Context): Switch = Switch(context).apply {
        isChecked = true
        text = ""
        // 行の項目名は別の View なので、トグル自身にも読み上げ名を与える。
        // ON / OFF の状態はトグルの役割としてそのまま読み上げ情報に乗る
        contentDescription = SampleText.USE_VISIBLE_AREA_LABEL
        // 既定のトラックは半透明で共有の配色どおりに出ないため、状態ごとの塗りを自前で与える
        trackTintList = null
        thumbTintList = null
        trackDrawable = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), context.switchTrack(SampleTheme.PRIMARY))
            addState(intArrayOf(), context.switchTrack(SampleTheme.DIVIDER))
        }
        thumbDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(SampleTheme.SURFACE)
            setSize(context.dp(THUMB_SIZE_DP), context.dp(THUMB_SIZE_DP))
        }
    }

    /** トグルのトラックの塗りを作る。 */
    private fun Context.switchTrack(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(TRACK_HEIGHT_DP / 2).toFloat()
        setColor(color)
        setSize(dp(TRACK_WIDTH_DP), dp(TRACK_HEIGHT_DP))
    }

    private companion object {
        const val BACK_CHEVRON = "‹"
        const val BACK_DESCRIPTION = "戻る"
        const val OFFSET_INITIAL_VALUE = "0"
        const val HORIZONTAL_PADDING_DP = 16
        const val TITLE_TOP_PADDING_DP = 20
        const val TITLE_BOTTOM_PADDING_DP = 12
        const val TITLE_TEXT_SIZE_SP = 17f
        const val TITLE_LEADING_GAP_DP = 8
        const val BACK_TEXT_SIZE_SP = 20f
        const val ACTION_MIN_SIZE_DP = 48
        const val SHOW_TEXT_SIZE_SP = 14f
        const val SHOW_CORNER_RADIUS_DP = 10
        const val SHOW_HORIZONTAL_PADDING_DP = 18
        const val DIVIDER_HEIGHT_DP = 1
        const val ROW_MIN_HEIGHT_DP = 48
        const val ROW_VERTICAL_PADDING_DP = 6
        const val ROW_TEXT_SIZE_SP = 14f
        const val FIELD_WIDTH_DP = 64
        const val FIELD_HEIGHT_DP = 36
        const val FIELD_CORNER_RADIUS_DP = 8
        const val FIELD_STROKE_WIDTH_DP = 1
        const val FIELD_HORIZONTAL_PADDING_DP = 10
        const val FIELD_TEXT_SIZE_SP = 14f
        const val TRACK_WIDTH_DP = 46
        const val TRACK_HEIGHT_DP = 28
        const val THUMB_SIZE_DP = 24
        const val RESULT_CAPTION_TEXT_SIZE_SP = 12f
        const val RESULT_CAPTION_SPACING_DP = 4
        const val RESULT_VALUE_TEXT_SIZE_SP = 14f
    }
}
