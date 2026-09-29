package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * 属性調整パネルが選べる余白 (全辺そろえ)。
 *
 * 0 は契約の既定値、24 は既定 Toast のデフォルト View が自分に持つ余白と同じ値。
 *
 * @property label セグメントに表示する文言
 * @property value 全辺にそろえて添付する余白の値 (dp)。共有コードへはこの値をそのまま運ぶ
 */
internal enum class SampleMarginChoice(
    val label: String,
    val value: Double,
) {
    ZERO(SampleText.MARGIN_0, 0.0),
    TWENTY_FOUR(SampleText.MARGIN_24, 24.0),
    FORTY_EIGHT(SampleText.MARGIN_48, 48.0),
}
