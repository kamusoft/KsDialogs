package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 配置を3択から選ぶセグメント。
 *
 * @param axisLabel このセグメントが属する行の項目名。読み上げ名を行ごとに一意にするために使う
 */
internal class SampleAlignmentSegmentsView(
    context: Context,
    private val axisLabel: String,
) : LinearLayout(context) {

    private val segments = LinkedHashMap<SampleAlignmentChoice, TextView>()

    /** 選択中の配置。 */
    var selection: SampleAlignmentChoice = SampleAlignmentChoice.CENTER
        set(value) {
            field = value
            refreshSelection()
        }

    init {
        orientation = HORIZONTAL
        background = context.roundedFill(TRACK_CORNER_RADIUS_DP, SampleTheme.SURFACE_VARIANT)
        val inset = context.dp(TRACK_PADDING_DP)
        setPadding(inset, inset, inset, inset)

        SampleAlignmentChoice.entries.forEach { choice ->
            val segment = segment(context, choice)
            segments[choice] = segment
            addView(segment)
        }
        refreshSelection()
    }

    private fun segment(context: Context, choice: SampleAlignmentChoice): TextView =
        TextView(context).apply {
            text = choice.label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, SEGMENT_TEXT_SIZE_SP)
            minimumHeight = context.dp(SEGMENT_MIN_HEIGHT_DP)
            setPadding(
                context.dp(SEGMENT_HORIZONTAL_PADDING_DP),
                0,
                context.dp(SEGMENT_HORIZONTAL_PADDING_DP),
                0,
            )
            isClickable = true
            isFocusable = true
            // 同じ文言の選択肢が2行に並ぶため、読み上げ名は行の文言と組にして一意にする
            contentDescription = "$axisLabel ${choice.label}"
            accessibilityDelegate = SampleButtonRoleDelegate
            setOnClickListener { selection = choice }
        }

    private fun refreshSelection() {
        segments.forEach { (choice, segment) ->
            val isSelected = choice == selection
            // 選択状態は View の状態として持たせ、読み上げ情報にもそのまま乗せる
            segment.isSelected = isSelected
            segment.background = if (isSelected) {
                context.roundedFill(SEGMENT_CORNER_RADIUS_DP, SampleTheme.PRIMARY)
            } else {
                null
            }
            segment.setTextColor(
                if (isSelected) SampleTheme.ON_PRIMARY else SampleTheme.ON_SURFACE_MUTED,
            )
            segment.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private companion object {
        const val TRACK_CORNER_RADIUS_DP = 10
        const val TRACK_PADDING_DP = 3
        const val SEGMENT_CORNER_RADIUS_DP = 8
        const val SEGMENT_MIN_HEIGHT_DP = 34
        const val SEGMENT_HORIZONTAL_PADDING_DP = 12
        const val SEGMENT_TEXT_SIZE_SP = 13f
    }
}
