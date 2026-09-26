package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 属性調整パネルの表示操作。ナビゲーションバーの中と説明のタブで同じ見た目を使う。
 *
 * @param onClick 押されたときの操作
 * @param modifier ボタンに掛ける修飾
 */
@Composable
internal fun SampleShowButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = BUTTON_MIN_HEIGHT_DP.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_RADIUS_DP.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(SampleTheme.PRIMARY),
            contentColor = Color(SampleTheme.ON_PRIMARY),
        ),
    ) {
        Text(text = SampleText.SHOW_ACTION, fontSize = BUTTON_TEXT_SIZE_SP.sp, fontWeight = FontWeight.Bold)
    }
}

private const val BUTTON_MIN_HEIGHT_DP = 48
private const val BUTTON_CORNER_RADIUS_DP = 10
private const val BUTTON_TEXT_SIZE_SP = 14
