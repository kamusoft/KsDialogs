package jp.kamusoft.ksdialogs.samples.kmp.android

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 属性調整パネルの1行。左に項目名、右に操作部を置く。
 *
 * @param title 項目名
 * @param control 行の右端に置く操作部
 */
@Composable
internal fun SampleSettingRow(title: String, control: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // 調整用の行も押しやすさの下限を満たす高さを確保する
            .defaultMinSize(minHeight = ROW_MIN_HEIGHT_DP.dp)
            .padding(horizontal = HORIZONTAL_PADDING_DP.dp, vertical = ROW_VERTICAL_PADDING_DP.dp),
    ) {
        BasicText(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = ROW_TEXT_SIZE_SP.sp, color = Color(SampleTheme.ON_SURFACE)),
        )
        Spacer(Modifier.width(ROW_GAP_DP.dp))
        control()
    }
}

private const val ROW_MIN_HEIGHT_DP = 48
private const val HORIZONTAL_PADDING_DP = 16
private const val ROW_VERTICAL_PADDING_DP = 6
private const val ROW_TEXT_SIZE_SP = 14
private const val ROW_GAP_DP = 8
