package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.DialogCurrentPage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側が登録した「表示中のページ」の関数が、そのまま Native ライブラリの登録口へ渡ることの検証。
 *
 * ページの選び方と矩形の求め方は MAUI 側と Native ライブラリがそれぞれ受け持つため、
 * この面に課されるのは登録・解除の中継と、呼ばれるたびに MAUI 側の関数へ問い合わせることだけになる。
 */
@DisplayName("表示中のページの関数は Native ライブラリの登録口へ中継される")
class MauiDialogCurrentPageRegistrationTests {

    @AfterEach
    fun clearRegistration() {
        DialogCurrentPage.provider = null
    }

    @Test
    fun `登録した関数が返す View を Native ライブラリの登録口が返す`() {
        val page = View(null)
        var calls = 0
        MauiDialogCurrentPage.setProvider {
            calls++
            page
        }

        val registered = DialogCurrentPage.provider
        assertNotNull(registered, "Native ライブラリの登録口に関数が入る")
        assertSame(page, registered!!())
        assertSame(page, registered())
        assertEquals(2, calls, "問い合わせのたびに MAUI 側の関数が呼ばれる")
    }

    @Test
    fun `MAUI 側の関数が null を返せば登録口も null を返す`() {
        MauiDialogCurrentPage.setProvider { null }

        assertNull(DialogCurrentPage.provider!!())
    }

    @Test
    fun `null を渡すと Native ライブラリの登録が解除される`() {
        MauiDialogCurrentPage.setProvider { null }

        MauiDialogCurrentPage.setProvider(null)

        assertNull(DialogCurrentPage.provider)
    }
}
