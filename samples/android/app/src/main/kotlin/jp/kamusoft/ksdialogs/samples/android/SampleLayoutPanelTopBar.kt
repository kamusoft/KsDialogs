package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 属性調整パネルのタブのナビゲーションバー。メニューへ戻る操作と表示操作を持つ。
 *
 * @param onBack メニュー画面へ戻る操作
 * @param onShow 調整した属性でダイアログを出す操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SampleLayoutPanelTopBar(onBack: () -> Unit, onShow: () -> Unit) {
    TopAppBar(
        title = { Text(SampleText.LAYOUT_DIALOG_ITEM) },
        navigationIcon = {
            // 記号のままでは読み上げが操作の意味を伝えないため、名前を与える (役割はボタン)
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = BACK_DESCRIPTION }) {
                BasicText(
                    text = BACK_CHEVRON,
                    style = TextStyle(fontSize = BACK_TEXT_SIZE_SP.sp, color = Color(SampleTheme.ON_SURFACE)),
                )
            }
        },
        actions = {
            SampleShowButton(onClick = onShow, modifier = Modifier.padding(end = ACTION_END_PADDING_DP.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(SampleTheme.SURFACE),
            scrolledContainerColor = Color(SampleTheme.SURFACE),
            titleContentColor = Color(SampleTheme.ON_SURFACE),
            navigationIconContentColor = Color(SampleTheme.ON_SURFACE),
        ),
    )
}

private const val BACK_CHEVRON = "‹"
private const val BACK_DESCRIPTION = "戻る"
private const val BACK_TEXT_SIZE_SP = 20
private const val ACTION_END_PADDING_DP = 8
