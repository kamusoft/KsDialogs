package jp.kamusoft.ksdialogs.samples.kmp.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * Sample のメニュー画面。デモ項目の一覧と直近の結果を表示する。
 *
 * @param onBasicDialogSelected デモ項目 Basic Dialog の起動操作
 * @param onLayoutDialogSelected デモ項目 Layout Dialog の起動操作
 * @param onDeclarativeDialogSelected デモ項目 Declarative Dialog の起動操作
 * @param onTextInputDialogSelected デモ項目 Text Input Dialog の起動操作
 * @param onInlineDialogSelected デモ項目 Inline Dialog の起動操作
 * @param onTransitionDialogSelected デモ項目 Transition Dialog の起動操作
 * @param onModelDialogSelected デモ項目 Model Dialog の起動操作
 * @param onDefaultLoadingSelected デモ項目 Default Loading の起動操作
 * @param onCustomLoadingSelected デモ項目 Custom Loading の起動操作
 * @param onDefaultToastSelected デモ項目 Default Toast の起動操作
 * @param onCustomToastSelected デモ項目 Custom Toast の起動操作
 * @param onToastStackSelected デモ項目 Toast Stack の起動操作
 * @param onToastPlacementSelected デモ項目 Toast Placement の起動操作
 * @param onToastOverlapSelected デモ項目 Toast Overlap の起動操作
 */
internal class SampleMenuView(
    context: Context,
    onBasicDialogSelected: () -> Unit,
    onLayoutDialogSelected: () -> Unit,
    onDeclarativeDialogSelected: () -> Unit,
    onTextInputDialogSelected: () -> Unit,
    onInlineDialogSelected: () -> Unit,
    onTransitionDialogSelected: () -> Unit,
    onModelDialogSelected: () -> Unit,
    onDefaultLoadingSelected: () -> Unit,
    onCustomLoadingSelected: () -> Unit,
    onDefaultToastSelected: () -> Unit,
    onCustomToastSelected: () -> Unit,
    onToastStackSelected: () -> Unit,
    onToastPlacementSelected: () -> Unit,
    onToastOverlapSelected: () -> Unit,
) : LinearLayout(context) {

    private val resultArea: LinearLayout
    private val resultValue: TextView

    init {
        orientation = VERTICAL
        setBackgroundColor(SampleTheme.SURFACE)
        fitsSystemWindows = true

        resultValue = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_VALUE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            typeface = Typeface.MONOSPACE
        }
        resultArea = resultAreaView(context, resultValue)

        // デモ項目が画面の高さを超える端末でも結果表示エリアまで到達できるよう、
        // 見出しから下をスクロールできる容器に入れる (トランジションのデモ画面と同じ構成)
        val scrollContent = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(menuItem(context, SampleText.BASIC_DIALOG_ITEM, onBasicDialogSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.LAYOUT_DIALOG_ITEM, onLayoutDialogSelected))
            addView(divider(context))
            addView(
                menuItem(context, SampleText.DECLARATIVE_DIALOG_ITEM, onDeclarativeDialogSelected),
            )
            addView(divider(context))
            addView(menuItem(context, SampleText.TEXT_INPUT_DIALOG_ITEM, onTextInputDialogSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.INLINE_DIALOG_ITEM, onInlineDialogSelected))
            addView(divider(context))
            addView(
                menuItem(context, SampleText.TRANSITION_DIALOG_ITEM, onTransitionDialogSelected),
            )
            addView(divider(context))
            addView(menuItem(context, SampleText.MODEL_DIALOG_ITEM, onModelDialogSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.DEFAULT_LOADING_ITEM, onDefaultLoadingSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.CUSTOM_LOADING_ITEM, onCustomLoadingSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.DEFAULT_TOAST_ITEM, onDefaultToastSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.CUSTOM_TOAST_ITEM, onCustomToastSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.TOAST_STACK_ITEM, onToastStackSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.TOAST_PLACEMENT_ITEM, onToastPlacementSelected))
            addView(divider(context))
            addView(menuItem(context, SampleText.TOAST_OVERLAP_ITEM, onToastOverlapSelected))
            addView(divider(context))
            addView(resultArea)
        }

        addView(header(context))
        addView(divider(context))
        addView(
            ScrollView(context).apply { addView(scrollContent) },
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f),
        )
    }

    /** 直近の結果を表示する。 */
    fun showResult(result: String) {
        resultValue.text = result
        resultArea.visibility = View.VISIBLE
    }

    private fun header(context: Context): TextView =
        TextView(context).apply {
            text = SampleText.MENU_TITLE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_TOP_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_BOTTOM_PADDING_DP),
            )
        }

    private fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(SampleTheme.DIVIDER)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.dp(DIVIDER_HEIGHT_DP))
        }

    private fun menuItem(context: Context, title: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            // 一覧の行も押しやすさの下限を満たす高さを確保する
            minimumHeight = context.dp(ROW_MIN_HEIGHT_DP)
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(ROW_VERTICAL_PADDING_DP),
            )
            setOnClickListener { onClick() }

            val label = TextView(context).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
            }
            val chevron = TextView(context).apply {
                text = CHEVRON
                setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE_MUTED)
            }
            addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(chevron, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }

    private fun resultAreaView(context: Context, valueLabel: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            // 一度もダイアログを閉じていない間は表示するものがない
            visibility = View.GONE
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

    private companion object {
        const val CHEVRON = "›"
        const val HORIZONTAL_PADDING_DP = 16
        const val TITLE_TOP_PADDING_DP = 20
        const val TITLE_BOTTOM_PADDING_DP = 12
        const val TITLE_TEXT_SIZE_SP = 17f
        const val DIVIDER_HEIGHT_DP = 1
        const val ROW_VERTICAL_PADDING_DP = 14
        const val ROW_MIN_HEIGHT_DP = 48
        const val ROW_TEXT_SIZE_SP = 15f
        const val RESULT_CAPTION_TEXT_SIZE_SP = 12f
        const val RESULT_CAPTION_SPACING_DP = 4
        const val RESULT_VALUE_TEXT_SIZE_SP = 14f
    }
}
