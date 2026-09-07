package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogAlignment

/**
 * 属性調整パネルが選べる配置。
 *
 * 契約の配置には有効領域いっぱいに広げる選択肢もあるが、パネルは寄せ先の3択だけを扱う。
 *
 * @property label セグメントに表示する文言
 * @property alignment 契約の配置での言い換え
 */
internal enum class SampleAlignmentChoice(
    val label: String,
    val alignment: DialogAlignment,
) {
    START(SampleText.ALIGNMENT_START, DialogAlignment.START),
    CENTER(SampleText.ALIGNMENT_CENTER, DialogAlignment.CENTER),
    END(SampleText.ALIGNMENT_END, DialogAlignment.END),
}
