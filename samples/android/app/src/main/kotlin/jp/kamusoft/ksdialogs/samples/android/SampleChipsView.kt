package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 選択肢をチップで並べて1つ選ばせる操作部。
 *
 * @param labels チップの文言。読み上げ名にもそのまま使う
 * @param columns 1行に並べるチップの数
 * @param minHeightDp チップの高さの下限
 * @param textSizeSp 文言の大きさ
 * @param onSelected 選択が変わったときに呼ばれる。引数は [labels] の位置
 */
internal class SampleChipsView(
    context: Context,
    private val labels: List<String>,
    private val columns: Int,
    private val minHeightDp: Int,
    private val textSizeSp: Float,
    private val onSelected: (Int) -> Unit,
) : LinearLayout(context) {

    private val chips = mutableListOf<TextView>()

    /** 選択中の位置。 */
    var selectedIndex: Int = 0
        set(value) {
            field = value
            refreshSelection()
        }

    init {
        orientation = VERTICAL
        labels.chunked(columns).forEachIndexed { rowIndex, rowLabels ->
            addView(
                chipRow(context, rowIndex, rowLabels),
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    if (rowIndex > 0) {
                        topMargin = context.dp(CHIP_GAP_DP)
                    }
                },
            )
        }
        refreshSelection()
    }

    private fun chipRow(context: Context, rowIndex: Int, rowLabels: List<String>): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            rowLabels.forEachIndexed { columnIndex, label ->
                val index = rowIndex * columns + columnIndex
                val chip = chip(context, label, index)
                chips.add(chip)
                addView(
                    chip,
                    LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (columnIndex > 0) {
                            leftMargin = context.dp(CHIP_GAP_DP)
                        }
                    },
                )
            }
        }

    private fun chip(context: Context, label: String, index: Int): TextView =
        TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            minimumHeight = context.dp(minHeightDp)
            setPadding(context.dp(CHIP_HORIZONTAL_PADDING_DP), 0, context.dp(CHIP_HORIZONTAL_PADDING_DP), 0)
            isClickable = true
            isFocusable = true
            // 文字を並べただけの操作部なので、ボタンとしての役割を補う
            contentDescription = label
            accessibilityDelegate = SampleButtonRoleDelegate
            setOnClickListener {
                selectedIndex = index
                onSelected(index)
            }
        }

    private fun refreshSelection() {
        chips.forEachIndexed { index, chip ->
            val isSelected = index == selectedIndex
            // 選択状態は View の状態として持たせ、読み上げ情報にもそのまま乗せる
            chip.isSelected = isSelected
            chip.background = context.roundedFillStroke(
                cornerRadiusDp = CHIP_CORNER_RADIUS_DP,
                fillColor = if (isSelected) SampleTheme.PRIMARY else SampleTheme.SURFACE,
                strokeWidthDp = CHIP_STROKE_WIDTH_DP,
                strokeColor = if (isSelected) SampleTheme.PRIMARY else SampleTheme.DIVIDER,
            )
            chip.setTextColor(if (isSelected) SampleTheme.ON_PRIMARY else SampleTheme.ON_SURFACE)
            chip.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private companion object {
        const val CHIP_GAP_DP = 8
        const val CHIP_CORNER_RADIUS_DP = 10
        const val CHIP_STROKE_WIDTH_DP = 1
        const val CHIP_HORIZONTAL_PADDING_DP = 6
    }
}
