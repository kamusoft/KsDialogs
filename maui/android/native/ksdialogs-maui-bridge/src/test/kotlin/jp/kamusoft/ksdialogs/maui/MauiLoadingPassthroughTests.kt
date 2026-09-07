package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.LoadingStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側で設定された Loading の見た目と表示指定が、値のまま Native ライブラリの型へ渡ることの検証。
 *
 * 見えの実装も進捗の丸めも Native ライブラリが受け持つため、この面に課されるのは輸送の値保存だけになる。
 * 色は同じ 32bit 整数の項目が並ぶので、項目ごとに違う値を与えて取り違えが起きないことまで見る。
 */
@DisplayName("Loading の設定は値のまま Native ライブラリの型へ渡る")
class MauiLoadingPassthroughTests {

    @Test
    @DisplayName("[LD-MA-03] 指定したスタイルがそのまま LoadingStyle になる")
    fun `指定したスタイルがそのまま LoadingStyle になる`() {
        val style = MauiLoadingStyle().apply {
            indicatorColorArgb = 0x11223344
            messageFontSize = 18.5
            messageColorArgb = 0x55667788
            defaultMessage = "しばらくお待ちください"
        }

        val converted = style.toLoadingStyle()

        assertEquals(0x11223344, converted.indicatorColor, "インジケータの色が入れ替わりました")
        assertEquals(0x55667788, converted.messageColor, "メッセージの文字色が入れ替わりました")
        assertEquals(18.5, converted.messageFontSize)
        assertEquals("しばらくお待ちください", converted.defaultMessage)
    }

    @Test
    @DisplayName("[LD-MA-03] 何も設定しないスタイルは Native ライブラリの既定値と同じ値で渡る")
    fun `何も設定しないスタイルは Native ライブラリの既定値と同じ値で渡る`() {
        val defaults = LoadingStyle()

        val converted = MauiLoadingStyle().toLoadingStyle()

        assertEquals(defaults.indicatorColor, converted.indicatorColor)
        assertEquals(defaults.messageFontSize, converted.messageFontSize)
        assertEquals(defaults.messageColor, converted.messageColor)
        assertEquals(defaults.defaultMessage, converted.defaultMessage)
    }

    @Test
    @DisplayName("[LD-MA-03] 組み立て方を設定しなければ Native ライブラリの既定の組み立て方が使われる")
    fun `組み立て方を設定しなければ Native ライブラリの既定の組み立て方が使われる`() {
        val converted = MauiLoadingStyle().toLoadingStyle()

        assertSame(LoadingStyle.DEFAULT_PROGRESS_FORMAT, converted.progressFormat)
    }

    @Test
    @DisplayName("[LD-MA-03] 設定した組み立て方へメッセージと進捗が値のまま渡る")
    fun `設定した組み立て方へメッセージと進捗が値のまま渡る`() {
        val arguments = mutableListOf<Pair<String?, Double?>>()
        val style = MauiLoadingStyle().apply {
            progressFormat = MauiLoadingProgressFormat { message, progress ->
                arguments += message to progress
                "組み立て済み"
            }
        }

        val converted = style.toLoadingStyle()
        val reported = converted.progressFormat("読み込み中", 0.25)
        val unreported = converted.progressFormat(null, null)

        assertEquals("組み立て済み", reported)
        assertEquals("組み立て済み", unreported)
        assertEquals(listOf<Pair<String?, Double?>>("読み込み中" to 0.25, null to null), arguments)
    }

    @Test
    @DisplayName("[LD-MA-03] 既定ローディングの指定はメッセージと置き場所だけを運ぶ")
    fun `既定ローディングの指定はメッセージと置き場所だけを運ぶ`() {
        val placement = MauiDialogPlacement().apply {
            horizontalAlignment = MauiDialogAlignment.END
            verticalAlignment = MauiDialogAlignment.FILL
            offsetX = -12.5
            offsetY = 34.0
        }

        val content = MauiLoadingContent("読み込み中", placement)

        assertEquals("読み込み中", content.message)
        assertNull(content.provider, "既定ローディングでは中身の供給を持たないこと")
        assertNull(content.progressReceiver, "既定ローディングでは進捗の転送先を持たないこと")
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
    @DisplayName("[LD-MA-03] カスタム Loading の指定は供給元と進捗の転送先をそのまま運ぶ")
    fun `カスタム Loading の指定は供給元と進捗の転送先をそのまま運ぶ`() {
        val reported = mutableListOf<Double>()
        val provider = MauiLoadingContentProvider { error("中身の供給はこの検証では呼ばれない") }
        val receiver = MauiLoadingProgressReceiver { progress -> reported += progress }

        val content = MauiLoadingContent(provider, placement = null, progressReceiver = receiver)
        content.progressReceiver?.onProgress(0.25)

        assertSame(provider, content.provider)
        assertSame(receiver, content.progressReceiver)
        assertNull(content.message, "カスタム Loading ではメッセージを運ばないこと")
        assertNull(content.placement, "置き場所を渡さなければ中身への添付が使われること")
        assertEquals(listOf(0.25), reported, "進捗は値のまま転送先へ渡ること")
    }
}
