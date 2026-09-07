package jp.kamusoft.ksdialogs.kmp

import kotlin.test.Test
import kotlin.test.assertSame

/**
 * 既定 singleton エントリと契約 interface 経由の入口が同じ実体を指すことの確認。
 *
 * 表示状態が 1 プロセスに 1 つであること自体は、OS ごとの委譲面と Native 側のテストが受け持つ。
 */
class LoadingEntryPointTests {

    @Test
    fun `既定エントリと契約 interface 経由で同じ実体を指す`() {
        val viaDefaultEntry = Loading.instance
        val viaContract: KsLoading = Loading.instance

        assertSame(viaDefaultEntry, viaContract, "既定エントリが呼ぶたびに別の実体になりました。")
    }
}
