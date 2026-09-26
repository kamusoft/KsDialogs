package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 選択肢を横一列に並べて 1 つを選ぶセグメント。
 *
 * @param choices 並べる選択肢 (並び順どおりに置く)
 * @param selection 選択中の選択肢
 * @param label 選択肢に表示する文言
 * @param onSelect 選択肢が押されたときの操作
 * @param modifier セグメント全体に掛ける修飾
 * @param axisLabel このセグメントが属する行の項目名。同じ文言の選択肢が複数の行に並ぶときだけ渡し、
 *   読み上げ名を行ごとに一意にする。null なら選択肢の文言だけを読み上げ名にする
 * @param fillsWidth 選択肢の幅を等分して、与えられた幅いっぱいに広げるか
 */
@Composable
internal fun <T> SampleSegments(
    choices: List<T>,
    selection: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    axisLabel: String? = null,
    fillsWidth: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(Color(SampleTheme.SURFACE_VARIANT), RoundedCornerShape(TRACK_CORNER_RADIUS_DP.dp))
            .padding(TRACK_PADDING_DP.dp),
    ) {
        choices.forEach { choice ->
            val isSelected = choice == selection
            val name = if (axisLabel == null) label(choice) else "$axisLabel ${label(choice)}"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .then(if (fillsWidth) Modifier.weight(1f) else Modifier)
                    .defaultMinSize(minHeight = SEGMENT_MIN_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(SEGMENT_CORNER_RADIUS_DP.dp))
                    .background(if (isSelected) Color(SampleTheme.PRIMARY) else Color.Transparent)
                    // 選択状態は役割と併せて読み上げ情報に乗せる
                    .selectable(selected = isSelected, role = Role.Button, onClick = { onSelect(choice) })
                    // 同じ文言の選択肢が2行に並ぶ場合は、読み上げ名を行の文言と組にして一意にする
                    .semantics { contentDescription = name }
                    .padding(horizontal = SEGMENT_HORIZONTAL_PADDING_DP.dp),
            ) {
                BasicText(
                    text = label(choice),
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = SEGMENT_TEXT_SIZE_SP.sp,
                        color = Color(if (isSelected) SampleTheme.ON_PRIMARY else SampleTheme.ON_SURFACE_MUTED),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

private const val TRACK_CORNER_RADIUS_DP = 10
private const val TRACK_PADDING_DP = 3
private const val SEGMENT_CORNER_RADIUS_DP = 8
private const val SEGMENT_MIN_HEIGHT_DP = 34
private const val SEGMENT_HORIZONTAL_PADDING_DP = 12
private const val SEGMENT_TEXT_SIZE_SP = 13
