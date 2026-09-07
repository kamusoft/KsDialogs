package jp.kamusoft.ksdialogs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("メタ属性の既定値と無効値の正規化")
class DialogAttributeDefaultsTests {

    private companion object {
        /** 契約が定める覆いの既定色 (黒の 40% 不透明)。 */
        const val DEFAULT_OVERLAY_COLOR = 0x66000000

        /** 契約が定める余白の既定値 (dp)。 */
        const val DEFAULT_MARGIN_DP = 24.0
    }

    @Test
    fun `何も指定しない値オブジェクトが契約の既定値を持つ`() {
        val options = DialogOptions()
        val placement = DialogPlacement()

        assertEquals(DialogLayoutArea.VISIBLE_AREA, options.layoutArea)
        assertEquals(DialogEdgeInsets(DEFAULT_MARGIN_DP), options.dialogMargin)
        assertEquals(-1.0, options.proportionalWidth, 0.0)
        assertEquals(-1.0, options.proportionalHeight, 0.0)
        assertEquals(DEFAULT_OVERLAY_COLOR, options.overlayColor)
        assertTrue(options.isCanceledOnTouchOutside)
        assertEquals(DialogAlignment.CENTER, placement.horizontalAlignment)
        assertEquals(DialogAlignment.CENTER, placement.verticalAlignment)
        assertEquals(0.0, placement.offsetX, 0.0)
        assertEquals(0.0, placement.offsetY, 0.0)
    }

    @Test
    fun `必要なフィールドだけを変えた値オブジェクトは残りが既定値のまま`() {
        val options = DialogOptions().copy(proportionalWidth = 0.5)

        assertEquals(0.5, options.proportionalWidth, 0.0)
        assertEquals(DialogEdgeInsets(DEFAULT_MARGIN_DP), options.dialogMargin)
        assertEquals(DialogLayoutArea.VISIBLE_AREA, options.layoutArea)
        assertEquals(DEFAULT_OVERLAY_COLOR, options.overlayColor)
        assertTrue(options.isCanceledOnTouchOutside)
    }

    @Test
    fun `供給がなければ実効値はすべて契約の既定値になる`() {
        val layout = DialogLayout()

        assertNull(layout.proportionalWidth)
        assertNull(layout.proportionalHeight)
        assertEquals(DialogAlignment.CENTER, layout.horizontalAlignment)
        assertEquals(DialogAlignment.CENTER, layout.verticalAlignment)
        assertEquals(0.0, layout.offsetX, 0.0)
        assertEquals(0.0, layout.offsetY, 0.0)
        assertEquals(DialogEdgeInsets(DEFAULT_MARGIN_DP), layout.dialogMargin)
        assertEquals(DialogLayoutArea.VISIBLE_AREA, layout.layoutArea)
        assertEquals(DEFAULT_OVERLAY_COLOR, layout.overlayColor)
        assertTrue(layout.isCanceledOnTouchOutside)
    }

    @Test
    fun `比率は 0 以下と非有限値が未指定になり 1 超は 1 に丸められる`() {
        assertNull(proportionalWidthOf(0.0), "0 は未指定")
        assertNull(proportionalWidthOf(-0.5), "負値は未指定")
        assertNull(proportionalWidthOf(Double.NaN), "NaN は未指定")
        assertNull(proportionalWidthOf(Double.POSITIVE_INFINITY), "無限大は未指定")
        assertEquals(1.0, proportionalWidthOf(1.5))
        assertEquals(0.75, proportionalWidthOf(0.75))
    }

    @Test
    fun `移動量の非有限値は 0 になる`() {
        val layout = DialogLayout(
            placement = DialogPlacement(offsetX = Double.NaN, offsetY = Double.NEGATIVE_INFINITY),
        )

        assertEquals(0.0, layout.offsetX, 0.0)
        assertEquals(0.0, layout.offsetY, 0.0)
    }

    @Test
    fun `余白は辺ごとに 負が 0 へ 非有限値が既定値へ丸められる`() {
        val layout = DialogLayout(
            options = DialogOptions(
                dialogMargin = DialogEdgeInsets(
                    top = -10.0,
                    left = Double.NaN,
                    bottom = Double.POSITIVE_INFINITY,
                    right = 8.0,
                ),
            ),
        )

        assertEquals(DialogEdgeInsets(top = 0.0, left = DEFAULT_MARGIN_DP, bottom = DEFAULT_MARGIN_DP, right = 8.0), layout.dialogMargin)
    }

    /** 比率だけを与えたときの実効値。 */
    private fun proportionalWidthOf(value: Double): Double? =
        DialogLayout(options = DialogOptions(proportionalWidth = value)).proportionalWidth
}
