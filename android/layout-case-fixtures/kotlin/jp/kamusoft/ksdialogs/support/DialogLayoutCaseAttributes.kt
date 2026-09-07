package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement

/**
 * 共通ケース表の1ケースが指定するレイアウト属性 (供給を合成した後の実効値)。
 *
 * 書かれていない属性は null で、そのまま契約の既定値を意味する。
 */
class DialogLayoutCaseAttributes(
    val layoutArea: String? = null,
    val dialogMargin: DialogEdgeInsets? = null,
    val proportionalWidth: Double? = null,
    val proportionalHeight: Double? = null,
    val horizontalAlignment: String? = null,
    val verticalAlignment: String? = null,
    val offsetX: Double? = null,
    val offsetY: Double? = null,
    val overlayColor: Int? = null,
) {
    /** 属性を1つも指定していない (すべて契約の既定値で動く) ケースか。 */
    fun isEmpty(): Boolean = listOf(
        layoutArea,
        dialogMargin,
        proportionalWidth,
        proportionalHeight,
        horizontalAlignment,
        verticalAlignment,
        offsetX,
        offsetY,
        overlayColor,
    ).all { it == null }

    /**
     * ケースの実効値のうち、静的メタ属性にあたる分。
     *
     * @param overlayColorOverride 覆いの色だけを差し替える。覆いの色が外形に影響しないことの確認に使う
     */
    fun options(overlayColorOverride: Int? = null): DialogOptions {
        val defaults = DialogOptions()
        return DialogOptions(
            layoutArea = layoutArea(layoutArea) ?: defaults.layoutArea,
            dialogMargin = dialogMargin ?: defaults.dialogMargin,
            proportionalWidth = proportionalWidth ?: defaults.proportionalWidth,
            proportionalHeight = proportionalHeight ?: defaults.proportionalHeight,
            overlayColor = overlayColorOverride ?: overlayColor ?: defaults.overlayColor,
        )
    }

    /** ケースの実効値のうち、動的メタ属性にあたる分。 */
    fun placement(): DialogPlacement {
        val defaults = DialogPlacement()
        return DialogPlacement(
            horizontalAlignment = alignment(horizontalAlignment) ?: defaults.horizontalAlignment,
            verticalAlignment = alignment(verticalAlignment) ?: defaults.verticalAlignment,
            offsetX = offsetX ?: defaults.offsetX,
            offsetY = offsetY ?: defaults.offsetY,
        )
    }

    private companion object {
        fun alignment(name: String?): DialogAlignment? = when (name) {
            "start" -> DialogAlignment.START
            "center" -> DialogAlignment.CENTER
            "end" -> DialogAlignment.END
            "fill" -> DialogAlignment.FILL
            null -> null
            else -> throw AssertionError("配置の指定を解釈できなかった: $name")
        }

        fun layoutArea(name: String?): DialogLayoutArea? = when (name) {
            "window" -> DialogLayoutArea.WINDOW
            "visibleArea" -> DialogLayoutArea.VISIBLE_AREA
            null -> null
            else -> throw AssertionError("基準領域の指定を解釈できなかった: $name")
        }
    }
}
