package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側で指定されたメタ属性が、値のまま Native ライブラリの型へ渡ることの検証。
 *
 * レイアウト計算も無効値の丸めも Native ライブラリが受け持つため、この面に課されるのは輸送の値保存だけになる。
 * 写した値を中身の View へ添付する経路は実 View を要するため、instrumented な検証 (Native 側) が受け持つ。
 */
@DisplayName("メタ属性は値のまま Native ライブラリの型へ渡る")
class MauiDialogLayoutPassthroughTests {

    @Test
    fun `指定した静的メタ属性がそのまま DialogOptions になる`() {
        val options = MauiDialogOptions().apply {
            layoutArea = MauiDialogLayoutArea.WINDOW
            marginTop = 2.0
            marginLeft = 1.0
            marginBottom = 4.0
            marginRight = 3.0
            proportionalWidth = 1.5
            proportionalHeight = 0.25
            overlayColorArgb = 0x88556677.toInt()
            isCanceledOnTouchOutside = false
        }

        assertEquals(
            DialogOptions(
                layoutArea = DialogLayoutArea.WINDOW,
                dialogMargin = DialogEdgeInsets(top = 2.0, left = 1.0, bottom = 4.0, right = 3.0),
                proportionalWidth = 1.5,
                proportionalHeight = 0.25,
                overlayColor = 0x88556677.toInt(),
                isCanceledOnTouchOutside = false,
            ),
            options.toDialogOptions(),
        )
    }

    @Test
    fun `指定した置き場所がそのまま DialogPlacement になる`() {
        val placement = MauiDialogPlacement().apply {
            horizontalAlignment = MauiDialogAlignment.END
            verticalAlignment = MauiDialogAlignment.FILL
            offsetX = -12.5
            offsetY = 34.0
        }

        assertEquals(
            DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.FILL,
                offsetX = -12.5,
                offsetY = 34.0,
            ),
            placement.toDialogPlacement(),
        )
    }

    @Test
    fun `何も設定しない属性は Native ライブラリの既定値と同じ値で渡る`() {
        assertEquals(DialogOptions(), MauiDialogOptions().toDialogOptions())
        assertEquals(DialogPlacement(), MauiDialogPlacement().toDialogPlacement())
    }

    @Test
    fun `有効域を外れた値も丸めずに渡る`() {
        val options = MauiDialogOptions().apply {
            proportionalWidth = 2.0
            proportionalHeight = -5.0
            marginLeft = -30.0
            marginTop = Double.NaN
        }
        val placement = MauiDialogPlacement().apply {
            offsetX = Double.NaN
        }

        val delegatedOptions = options.toDialogOptions()
        assertEquals(2.0, delegatedOptions.proportionalWidth)
        assertEquals(-5.0, delegatedOptions.proportionalHeight)
        assertEquals(-30.0, delegatedOptions.dialogMargin.left)
        assertEquals(Double.NaN, delegatedOptions.dialogMargin.top)
        assertEquals(Double.NaN, placement.toDialogPlacement().offsetX)
    }
}
