package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogLayoutArea

/**
 * 属性調整パネルが選べる基準領域。
 *
 * @property label セグメントに表示する文言
 * @property layoutArea 契約の基準領域での言い換え
 */
internal enum class SampleLayoutAreaChoice(
    val label: String,
    val layoutArea: DialogLayoutArea,
) {
    WINDOW(SampleText.LAYOUT_AREA_WINDOW, DialogLayoutArea.WINDOW),
    VISIBLE_AREA(SampleText.LAYOUT_AREA_VISIBLE_AREA, DialogLayoutArea.VISIBLE_AREA),
    CURRENT_PAGE(SampleText.LAYOUT_AREA_CURRENT_PAGE, DialogLayoutArea.CURRENT_PAGE),
}
