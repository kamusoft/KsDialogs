package jp.kamusoft.ksdialogs.samples.kmp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * Declarative Dialog の中身。
 *
 * 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
 * この composable をそのままレジストリへ渡すため、`View` へ載せ替える手当ては要らない。
 *
 * @param message 表示するメッセージ
 * @param onCancel キャンセル操作
 * @param onComplete 完了操作
 */
@Composable
internal fun DeclarativeDialogCard(
    message: String,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(CARD_WIDTH_DP.dp)
            .background(Color(SampleTheme.SURFACE), RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp))
            .padding(
                start = CARD_HORIZONTAL_PADDING_DP.dp,
                top = CARD_TOP_PADDING_DP.dp,
                end = CARD_HORIZONTAL_PADDING_DP.dp,
                bottom = CARD_BOTTOM_PADDING_DP.dp,
            ),
    ) {
        BasicText(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = MESSAGE_TEXT_SIZE_SP.sp,
                color = Color(SampleTheme.ON_SURFACE),
                textAlign = TextAlign.Center,
            ),
        )
        Row(
            modifier = Modifier.padding(top = MESSAGE_BOTTOM_SPACING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp),
        ) {
            ActionButton(
                label = SampleText.CANCEL_ACTION,
                fillColor = SampleTheme.SURFACE_VARIANT,
                textColor = SampleTheme.ON_SURFACE,
                fontWeight = FontWeight.Normal,
                onClick = onCancel,
            )
            ActionButton(
                label = SampleText.COMPLETE_ACTION,
                fillColor = SampleTheme.PRIMARY,
                textColor = SampleTheme.ON_PRIMARY,
                fontWeight = FontWeight.Bold,
                onClick = onComplete,
            )
        }
    }
}

/**
 * カードの操作ボタン。左右に等幅で並べる。
 *
 * @param label ボタンの文言
 * @param fillColor 塗りの色
 * @param textColor 文字の色
 * @param fontWeight 文字の太さ
 * @param onClick 押されたときの操作
 */
@Composable
private fun RowScope.ActionButton(
    label: String,
    fillColor: Int,
    textColor: Int,
    fontWeight: FontWeight,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            // ボタンの高さは Material の推奨タップ領域 48dp を下限にする
            .defaultMinSize(minHeight = BUTTON_MIN_HEIGHT_DP.dp)
            .background(Color(fillColor), RoundedCornerShape(BUTTON_CORNER_RADIUS_DP.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = BUTTON_TEXT_SIZE_SP.sp,
                color = Color(textColor),
                fontWeight = fontWeight,
            ),
        )
    }
}

private const val CARD_WIDTH_DP = 272
private const val CARD_CORNER_RADIUS_DP = 20
private const val CARD_TOP_PADDING_DP = 24
private const val CARD_HORIZONTAL_PADDING_DP = 20
private const val CARD_BOTTOM_PADDING_DP = 20
private const val MESSAGE_TEXT_SIZE_SP = 15
private const val MESSAGE_BOTTOM_SPACING_DP = 20
private const val BUTTON_GAP_DP = 10
private const val BUTTON_CORNER_RADIUS_DP = 10
private const val BUTTON_TEXT_SIZE_SP = 14
private const val BUTTON_MIN_HEIGHT_DP = 48
