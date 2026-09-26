package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 属性調整パネルのタブの中身。配置・移動量・基準領域の行と結果表示エリアを並べる。
 *
 * @param state 調整値を持つ画面の状態
 * @param modifier 中身の枠に掛ける修飾
 */
@Composable
internal fun SampleLayoutPanelForm(state: SampleLayoutPanelState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SampleSettingRow(SampleText.HORIZONTAL_LABEL) {
            SampleSegments(
                choices = SampleAlignmentChoice.entries,
                selection = state.horizontalAlignment,
                label = { it.label },
                onSelect = { state.horizontalAlignment = it },
                axisLabel = SampleText.HORIZONTAL_LABEL,
            )
        }
        SampleDivider()
        SampleSettingRow(SampleText.VERTICAL_LABEL) {
            SampleSegments(
                choices = SampleAlignmentChoice.entries,
                selection = state.verticalAlignment,
                label = { it.label },
                onSelect = { state.verticalAlignment = it },
                axisLabel = SampleText.VERTICAL_LABEL,
            )
        }
        SampleDivider()
        SampleSettingRow(SampleText.OFFSET_X_LABEL) {
            SampleOffsetField(SampleText.OFFSET_X_LABEL, state.offsetX) { state.offsetX = it }
        }
        SampleDivider()
        SampleSettingRow(SampleText.OFFSET_Y_LABEL) {
            SampleOffsetField(SampleText.OFFSET_Y_LABEL, state.offsetY) { state.offsetY = it }
        }
        SampleDivider()

        // 3 つの選択肢は横に並べると項目名と同じ行に収まらないため、項目名の下に全幅で置く
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = HORIZONTAL_PADDING_DP.dp,
                    top = AREA_TOP_PADDING_DP.dp,
                    end = HORIZONTAL_PADDING_DP.dp,
                    bottom = AREA_BOTTOM_PADDING_DP.dp,
                ),
        ) {
            BasicText(
                text = SampleText.LAYOUT_AREA_LABEL,
                style = TextStyle(fontSize = ROW_TEXT_SIZE_SP.sp, color = Color(SampleTheme.ON_SURFACE)),
            )
            SampleSegments(
                choices = SampleLayoutAreaChoice.entries,
                selection = state.layoutArea,
                label = { it.label },
                onSelect = { state.layoutArea = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AREA_LABEL_SPACING_DP.dp),
                fillsWidth = true,
            )
        }
        SampleDivider()

        state.lastResult?.let { SampleResultArea(it) }
    }
}

/** 行と行の間の区切り線。 */
@Composable
private fun SampleDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DIVIDER_HEIGHT_DP.dp)
            .background(Color(SampleTheme.DIVIDER)),
    )
}

private const val HORIZONTAL_PADDING_DP = 16
private const val AREA_TOP_PADDING_DP = 10
private const val AREA_BOTTOM_PADDING_DP = 12
private const val AREA_LABEL_SPACING_DP = 8
private const val ROW_TEXT_SIZE_SP = 14
private const val DIVIDER_HEIGHT_DP = 1
