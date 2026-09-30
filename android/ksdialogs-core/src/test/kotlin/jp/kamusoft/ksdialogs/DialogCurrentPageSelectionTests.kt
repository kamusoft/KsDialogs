package jp.kamusoft.ksdialogs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * modifier の台帳から表示中のページを 1 つ選ぶ規則 (入れ子は内側・それ以外は最後に配置・窓外と空は除外)。
 *
 * 台帳への出入りとウィンドウの判定は実機の検証 (DialogCurrentPageLedgerTests / Compose の modifier の検証) が
 * 受け持ち、ここでは候補の並びから 1 つを選ぶ純粋な規則だけを確かめる。
 */
@DisplayName("表示中のページの候補の選び方")
class DialogCurrentPageSelectionTests {

    private val window = DialogScreenRect(0f, 0f, 400f, 800f)

    private fun candidate(left: Float, top: Float, right: Float, bottom: Float, order: Long) =
        DialogCurrentPageCandidate(DialogScreenRect(left, top, right, bottom), order)

    @Test
    fun `候補が 1 つならその矩形を選ぶ`() {
        val only = candidate(0f, 50f, 400f, 700f, order = 0)

        assertEquals(only.rect, chooseCurrentPage(listOf(only), window))
    }

    @Test
    fun `入れ子の候補は後から外側が配置されても内側を選ぶ`() {
        val inner = candidate(0f, 100f, 400f, 600f, order = 0)
        val outer = candidate(0f, 50f, 400f, 700f, order = 1)

        assertEquals(inner.rect, chooseCurrentPage(listOf(inner, outer), window))
    }

    @Test
    fun `入れ子でない候補どうしは最後に配置されたものを選ぶ`() {
        val first = candidate(0f, 50f, 200f, 700f, order = 0)
        val second = candidate(200f, 50f, 400f, 700f, order = 1)

        assertEquals(second.rect, chooseCurrentPage(listOf(first, second), window))
    }

    @Test
    fun `同じ矩形の候補どうしは最後に配置されたものを選ぶ`() {
        val first = candidate(0f, 50f, 400f, 700f, order = 0)
        val second = candidate(0f, 50f, 400f, 700f, order = 1)

        assertEquals(second.rect, chooseCurrentPage(listOf(first, second), window))
    }

    @Test
    fun `ウィンドウと重ならない候補は後から配置されても選ばない`() {
        val inside = candidate(0f, 50f, 400f, 700f, order = 0)
        val outside = candidate(400f, 50f, 800f, 700f, order = 1)

        assertEquals(inside.rect, chooseCurrentPage(listOf(inside, outside), window))
    }

    @Test
    fun `空の矩形の候補は選ばず内側扱いにもしない`() {
        val page = candidate(0f, 50f, 400f, 700f, order = 0)
        val empty = candidate(100f, 100f, 100f, 100f, order = 1)

        assertEquals(page.rect, chooseCurrentPage(listOf(page, empty), window))
    }

    @Test
    fun `候補が無ければ選ばない`() {
        assertNull(chooseCurrentPage(emptyList(), window))
    }
}
