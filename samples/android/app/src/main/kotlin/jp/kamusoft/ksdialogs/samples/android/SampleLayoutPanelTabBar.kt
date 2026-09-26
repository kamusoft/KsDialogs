package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 属性調整パネルの画面の下部に置くタブバー。
 *
 * @param selected 表示中のタブ
 * @param onSelect タブが押されたときの操作
 */
@Composable
internal fun SampleLayoutPanelTabBar(selected: SampleLayoutPanelTab, onSelect: (SampleLayoutPanelTab) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DIVIDER_HEIGHT_DP.dp)
                .background(Color(SampleTheme.DIVIDER)),
        )
        NavigationBar(containerColor = Color(SampleTheme.SURFACE)) {
            TabItem(SampleLayoutPanelTab.PANEL, SampleText.PANEL_TAB, Icons.AutoMirrored.Filled.List, selected, onSelect)
            TabItem(SampleLayoutPanelTab.INFO, SampleText.INFO_TAB, Icons.Outlined.Info, selected, onSelect)
        }
    }
}

/** タブバーの 1 項目。アイコンと文字を並べ、選択中は primary で示す。 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    tab: SampleLayoutPanelTab,
    label: String,
    icon: ImageVector,
    selected: SampleLayoutPanelTab,
    onSelect: (SampleLayoutPanelTab) -> Unit,
) {
    NavigationBarItem(
        selected = tab == selected,
        onClick = { onSelect(tab) },
        // 文字が名前を持つので、アイコンは読み上げない
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(SampleTheme.PRIMARY),
            selectedTextColor = Color(SampleTheme.PRIMARY),
            indicatorColor = Color(SampleTheme.SURFACE_VARIANT),
            unselectedIconColor = Color(SampleTheme.ON_SURFACE_MUTED),
            unselectedTextColor = Color(SampleTheme.ON_SURFACE_MUTED),
        ),
    )
}

private const val DIVIDER_HEIGHT_DP = 1
