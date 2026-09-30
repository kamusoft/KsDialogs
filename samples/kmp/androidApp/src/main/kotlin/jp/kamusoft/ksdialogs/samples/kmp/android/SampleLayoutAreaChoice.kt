package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.samples.kmp.SampleLayoutAreaPreset
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * 属性調整パネルが選べる基準領域。
 *
 * @property label セグメントに表示する文言
 * @property preset 共有コードが運ぶ選択での言い換え
 */
internal enum class SampleLayoutAreaChoice(
    val label: String,
    val preset: SampleLayoutAreaPreset,
) {
    WINDOW(SampleText.LAYOUT_AREA_WINDOW, SampleLayoutAreaPreset.WINDOW),
    VISIBLE_AREA(SampleText.LAYOUT_AREA_VISIBLE_AREA, SampleLayoutAreaPreset.VISIBLE_AREA),
    CURRENT_PAGE(SampleText.LAYOUT_AREA_CURRENT_PAGE, SampleLayoutAreaPreset.CURRENT_PAGE),
}

/** 共有コードが運んできた選択を、中身へ添付する基準領域へ言い換える。 */
internal fun SampleLayoutAreaPreset.toLayoutArea(): DialogLayoutArea = when (this) {
    SampleLayoutAreaPreset.WINDOW -> DialogLayoutArea.WINDOW
    SampleLayoutAreaPreset.VISIBLE_AREA -> DialogLayoutArea.VISIBLE_AREA
    SampleLayoutAreaPreset.CURRENT_PAGE -> DialogLayoutArea.CURRENT_PAGE
}
