package jp.kamusoft.ksdialogs.samples.kmp.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import jp.kamusoft.ksdialogs.compose.markAsDialogCurrentPage

/**
 * レイアウト属性を調整してからダイアログを表示する画面。
 *
 * 下部のタブバーで、ナビゲーションバーを持つパネルのタブと、タイトルバーを持たない説明のタブを切り替える。
 * 基準領域「表示中のページ」がバーを除いた領域になることを 2 つのタブで見比べられるよう、
 * 各タブの中身の枠 (Scaffold のバーの内側) を表示中のページとして名乗らせる。
 *
 * @param state 調整値と表示中のタブを持つ画面の状態
 * @param onBack メニュー画面へ戻る操作
 * @param onShow 調整した属性でダイアログを出す操作
 */
@Composable
internal fun SampleLayoutPanelScreen(state: SampleLayoutPanelState, onBack: () -> Unit, onShow: () -> Unit) {
    MaterialTheme(colorScheme = SampleColorScheme) {
        Scaffold(
            containerColor = Color(SampleTheme.SURFACE),
            bottomBar = { SampleLayoutPanelTabBar(state.selectedTab) { state.selectedTab = it } },
        ) { tabBarPadding ->
            when (state.selectedTab) {
                SampleLayoutPanelTab.PANEL -> PanelTab(state, tabBarPadding, onBack, onShow)
                SampleLayoutPanelTab.INFO -> SampleLayoutInfoPage(
                    onShow = onShow,
                    modifier = Modifier
                        .padding(tabBarPadding)
                        .fillMaxSize()
                        .markAsDialogCurrentPage(),
                )
            }
        }
    }
}

/**
 * パネルのタブ。タブバーの上に、ナビゲーションバーを持つ入れ子の Scaffold を置く。
 *
 * タブバーが受け持った下端の余白は消費済みとして渡し、入れ子の側で二重に空けないようにする。
 */
@Composable
private fun PanelTab(
    state: SampleLayoutPanelState,
    tabBarPadding: PaddingValues,
    onBack: () -> Unit,
    onShow: () -> Unit,
) {
    val bottomPadding = PaddingValues(bottom = tabBarPadding.calculateBottomPadding())
    Scaffold(
        modifier = Modifier
            .padding(bottomPadding)
            .consumeWindowInsets(bottomPadding),
        containerColor = Color(SampleTheme.SURFACE),
        topBar = { SampleLayoutPanelTopBar(onBack = onBack, onShow = onShow) },
    ) { topBarPadding ->
        SampleLayoutPanelForm(
            state = state,
            modifier = Modifier
                .padding(topBarPadding)
                .fillMaxSize()
                .markAsDialogCurrentPage(),
        )
    }
}

/** 共有の配色を Material の配色に写したもの。部品の既定色が OS 固有の配色に倒れないようにする。 */
private val SampleColorScheme = lightColorScheme(
    primary = Color(SampleTheme.PRIMARY),
    onPrimary = Color(SampleTheme.ON_PRIMARY),
    background = Color(SampleTheme.SURFACE),
    onBackground = Color(SampleTheme.ON_SURFACE),
    surface = Color(SampleTheme.SURFACE),
    onSurface = Color(SampleTheme.ON_SURFACE),
    surfaceVariant = Color(SampleTheme.SURFACE_VARIANT),
    onSurfaceVariant = Color(SampleTheme.ON_SURFACE_MUTED),
    outline = Color(SampleTheme.DIVIDER),
    outlineVariant = Color(SampleTheme.DIVIDER),
)
