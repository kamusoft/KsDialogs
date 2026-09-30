package jp.kamusoft.ksdialogs.samples.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jp.kamusoft.ksdialogs.DialogPlacement

/**
 * 属性調整パネルの画面が持つ状態。
 *
 * パネルを閉じて開き直しても、タブを切り替えても調整値を保つため、画面の composition の外に置く。
 * 初期値は契約の既定値 (中央配置・移動なし・可視領域基準・余白 0) に揃える。
 */
internal class SampleLayoutPanelState {
    /** 水平方向の配置。 */
    var horizontalAlignment by mutableStateOf(SampleAlignmentChoice.CENTER)

    /** 垂直方向の配置。 */
    var verticalAlignment by mutableStateOf(SampleAlignmentChoice.CENTER)

    /** 水平方向の移動量の入力。 */
    var offsetX by mutableStateOf(OFFSET_INITIAL_VALUE)

    /** 垂直方向の移動量の入力。 */
    var offsetY by mutableStateOf(OFFSET_INITIAL_VALUE)

    /** サイズと位置の計算に使う基準領域。 */
    var layoutArea by mutableStateOf(SampleLayoutAreaChoice.VISIBLE_AREA)

    /** 全辺そろえの余白。 */
    var margin by mutableStateOf(SampleMarginChoice.ZERO)

    /** 表示中のタブ。 */
    var selectedTab by mutableStateOf(SampleLayoutPanelTab.PANEL)

    /** 直近の結果の表示文言。まだ一度もダイアログを閉じていない間は null。 */
    var lastResult by mutableStateOf<String?>(null)

    /** 調整中の置き場所。数値として読めない入力は 0 として扱う。 */
    fun placement(): DialogPlacement = DialogPlacement(
        horizontalAlignment = horizontalAlignment.alignment,
        verticalAlignment = verticalAlignment.alignment,
        offsetX = offsetX.toDoubleOrNull() ?: 0.0,
        offsetY = offsetY.toDoubleOrNull() ?: 0.0,
    )

    private companion object {
        const val OFFSET_INITIAL_VALUE = "0"
    }
}
