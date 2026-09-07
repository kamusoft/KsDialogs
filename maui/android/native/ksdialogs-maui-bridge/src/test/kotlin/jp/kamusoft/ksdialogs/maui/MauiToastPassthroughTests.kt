package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.ToastStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側で設定された Toast の一括設定と表示指定が、値のまま Native ライブラリの型へ渡ることの検証。
 *
 * 見えの実装も duration の丸めも Native ライブラリが受け持つため、この面に課されるのは輸送の
 * 値保存だけになる。色は同じ 32bit 整数の項目が並ぶので、項目ごとに違う値を与えて取り違えが
 * 起きないことまで見る。
 */
@DisplayName("Toast の設定は値のまま Native ライブラリの型へ渡る")
class MauiToastPassthroughTests {

    @Test
    @DisplayName("[TS-MA-02] 指定した一括設定がそのまま ToastStyle になる")
    fun `TS-MA-02 指定した一括設定がそのまま ToastStyle になる`() {
        val style = MauiToastStyle().apply {
            backgroundColorArgb = 0x11223344
            textColorArgb = 0x55667788
            fontSize = 18.5
            cornerRadius = 8.0
            defaultDuration = 3000
            defaultPlacement = MauiDialogPlacement().apply {
                verticalAlignment = MauiDialogAlignment.END
                offsetY = -80.0
            }
        }

        val converted = style.toToastStyle()

        assertEquals(0x11223344, converted.backgroundColor, "ピルの地色が入れ替わりました")
        assertEquals(0x55667788, converted.textColor, "メッセージの文字色が入れ替わりました")
        assertEquals(18.5, converted.fontSize)
        assertEquals(8.0, converted.cornerRadius)
        assertEquals(3000, converted.defaultDuration)
        assertEquals(
            DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -80.0),
            converted.defaultPlacement,
        )
    }

    @Test
    @DisplayName("[TS-MA-02] 何も設定しない一括設定は Native ライブラリの既定値と同じ値で渡る")
    fun `TS-MA-02 何も設定しない一括設定は Native ライブラリの既定値と同じ値で渡る`() {
        val defaults = ToastStyle()

        val converted = MauiToastStyle().toToastStyle()

        assertEquals(defaults, converted)
    }

    @Test
    @DisplayName("[TS-MA-02] デフォルト View の指定はメッセージと duration・置き場所だけを運ぶ")
    fun `TS-MA-02 デフォルト View の指定はメッセージと duration・置き場所だけを運ぶ`() {
        val placement = MauiDialogPlacement().apply {
            horizontalAlignment = MauiDialogAlignment.END
            verticalAlignment = MauiDialogAlignment.FILL
            offsetX = -12.5
            offsetY = 34.0
        }

        val content = MauiToastContent("保存しました", 2500, placement)

        assertEquals("保存しました", content.message)
        assertEquals(2500, content.durationMs)
        assertNull(content.provider, "デフォルト View では中身の供給を持たないこと")
        assertEquals(
            DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.FILL,
                offsetX = -12.5,
                offsetY = 34.0,
            ),
            content.placement?.toDialogPlacement(),
        )
    }

    @Test
    @DisplayName("[TS-MA-02] カスタム Toast の指定は供給元をそのまま運ぶ")
    fun `TS-MA-02 カスタム Toast の指定は供給元をそのまま運ぶ`() {
        val provider = MauiToastContentProvider { error("中身の供給はこの検証では呼ばれない") }

        val content = MauiToastContent(provider, durationMs = null, placement = null)

        assertSame(provider, content.provider)
        assertNull(content.message, "カスタム Toast ではメッセージを運ばないこと")
        assertNull(content.durationMs, "省略した duration は運ばれないこと")
        assertNull(content.placement, "置き場所を渡さなければ中身への添付が使われること")
    }
}
