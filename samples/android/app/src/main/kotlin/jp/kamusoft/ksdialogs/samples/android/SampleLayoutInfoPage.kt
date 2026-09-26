package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * タイトルバーを持たないタブの中身。説明文と、パネルの設定をそのまま使う表示操作を置く。
 *
 * @param onShow パネルの設定でダイアログを出す操作
 * @param modifier 中身の枠に掛ける修飾
 */
@Composable
internal fun SampleLayoutInfoPage(onShow: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(
            start = HORIZONTAL_PADDING_DP.dp,
            top = TOP_PADDING_DP.dp,
            end = HORIZONTAL_PADDING_DP.dp,
        ),
    ) {
        BasicText(
            text = SampleText.INFO_TAB_BODY,
            style = TextStyle(
                fontSize = BODY_TEXT_SIZE_SP.sp,
                lineHeight = BODY_LINE_HEIGHT_SP.sp,
                color = Color(SampleTheme.ON_SURFACE),
            ),
        )
        SampleShowButton(
            onClick = onShow,
            modifier = Modifier.padding(top = BUTTON_TOP_SPACING_DP.dp),
        )
    }
}

private const val HORIZONTAL_PADDING_DP = 16
private const val TOP_PADDING_DP = 24
private const val BODY_TEXT_SIZE_SP = 14
private const val BODY_LINE_HEIGHT_SP = 22
private const val BUTTON_TOP_SPACING_DP = 20
