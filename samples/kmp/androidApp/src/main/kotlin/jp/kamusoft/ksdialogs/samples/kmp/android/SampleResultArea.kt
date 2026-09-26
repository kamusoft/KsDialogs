package jp.kamusoft.ksdialogs.samples.kmp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * 属性調整パネル内で直近の結果を表示するエリア。設定行の連なりと切り分けるため地の色を敷く。
 *
 * @param result 直近の結果の表示文言
 */
@Composable
internal fun SampleResultArea(result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(SampleTheme.SURFACE_VARIANT))
            .padding(horizontal = HORIZONTAL_PADDING_DP.dp, vertical = VERTICAL_PADDING_DP.dp),
    ) {
        BasicText(
            text = SampleText.RESULT_CAPTION,
            style = TextStyle(fontSize = CAPTION_TEXT_SIZE_SP.sp, color = Color(SampleTheme.ON_SURFACE_MUTED)),
        )
        BasicText(
            text = result,
            modifier = Modifier.padding(top = CAPTION_SPACING_DP.dp),
            style = TextStyle(
                fontSize = VALUE_TEXT_SIZE_SP.sp,
                color = Color(SampleTheme.ON_SURFACE),
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

private const val HORIZONTAL_PADDING_DP = 16
private const val VERTICAL_PADDING_DP = 6
private const val CAPTION_TEXT_SIZE_SP = 12
private const val CAPTION_SPACING_DP = 4
private const val VALUE_TEXT_SIZE_SP = 14
