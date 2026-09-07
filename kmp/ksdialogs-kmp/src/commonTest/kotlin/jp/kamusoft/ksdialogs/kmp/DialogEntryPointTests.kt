package jp.kamusoft.ksdialogs.kmp

import kotlin.test.Test
import kotlin.test.assertSame

/**
 * 既定 singleton エントリと契約 interface 経由の入口が同じレジストリを見ることの確認。
 *
 * View factory の紐付けが Native ライブラリ側の1個だけであること自体は、
 * OS ごとの委譲面のテストが受け持つ (ViewModel factory の表は共有コードが持つ)。
 */
class DialogEntryPointTests {

    @Test
    fun `既定エントリと契約 interface 経由でレジストリを共有する`() {
        val viaDefaultEntry = Dialog.instance
        val viaContract: KsDialog = Dialog.instance

        assertSame(viaDefaultEntry, viaContract, "既定エントリが呼ぶたびに別の実体になりました。")
        assertSame(viaDefaultEntry.registry, viaContract.registry)
    }
}
