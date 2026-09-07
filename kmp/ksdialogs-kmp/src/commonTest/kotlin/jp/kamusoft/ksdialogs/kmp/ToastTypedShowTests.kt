package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestToastViewModel
import jp.kamusoft.ksdialogs.kmp.support.DerivedTestToastViewModel
import jp.kamusoft.ksdialogs.kmp.support.TestToastGateway
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ViewModel の型を渡す Toast 表示の検証。
 *
 * 本番の前段 ([GatewayKsToast]) に委譲面の差し替えを挿し、
 * 生成と configure が呼び出し元のスレッドで同期に行われることを見る。
 */
class ToastTypedShowTests {

    @Test
    fun `PB-KT-01 既定エントリと契約 interface 経由で VM factory のレジストリを共有する`() {
        val gateway = TestToastGateway()
        val entry = GatewayKsToast(gateway)
        val contract: KsToast = entry

        assertSame(entry.registry, contract.registry, "入口ごとに別のレジストリになりました。")
        entry.registry.registerViewModel(ConfigurableTestToastViewModel::class) {
            ConfigurableTestToastViewModel(message = "登録した factory の産物")
        }

        contract.show(ConfigurableTestToastViewModel::class)

        val shown = gateway.shownViewModels.single() as ConfigurableTestToastViewModel
        assertEquals("登録した factory の産物", shown.message, "登録した factory が使われませんでした。")
    }

    @Test
    fun `TS-KT-01 型を渡す show が同期に委譲面へ流れ失敗も同期に伝播する`() {
        val gateway = TestToastGateway()
        val toast = GatewayKsToast(gateway)
        toast.registry.registerViewModel(ConfigurableTestToastViewModel::class) {
            ConfigurableTestToastViewModel()
        }

        toast.show(
            ConfigurableTestToastViewModel::class,
            durationMs = 3000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START, offsetY = 24.0),
        ) { viewModel -> viewModel.message = "configure で入れた文言" }

        val shown = gateway.shownViewModels.single() as ConfigurableTestToastViewModel
        assertEquals("configure で入れた文言", shown.message, "configure の状態が委譲面へ届きませんでした。")
        assertContentEquals(listOf(3000), gateway.shownDurations)
        assertContentEquals(
            listOf(DialogPlacement(verticalAlignment = DialogAlignment.START, offsetY = 24.0)),
            gateway.shownPlacements,
        )

        val failure = assertFailsWith<IllegalStateException> {
            toast.show(ConfigurableTestToastViewModel::class) { error("configure の失敗") }
        }

        assertEquals("configure の失敗", failure.message)
        assertEquals(1, gateway.shownViewModels.size, "configure が失敗したのに委譲面が呼ばれました。")
    }

    @Test
    fun `PB-KT-05 VM factory 未登録の型を渡す show は構成ミスとして失敗する`() {
        val gateway = TestToastGateway()
        val toast = GatewayKsToast(gateway)

        val failure = assertFailsWith<DialogException> {
            toast.show(ConfigurableTestToastViewModel::class)
        }

        assertTrue(
            failure.message?.contains("No ViewModel factory is registered") == true,
            "未登録が構成ミスとして説明されませんでした: ${failure.message}",
        )
        assertTrue(gateway.shownViewModels.isEmpty(), "未登録なのに委譲面が呼ばれました。")
    }

    @Test
    fun `PB-KT-13 VM factory の生成物の型が登録キーと違えば型不一致として失敗する`() {
        val gateway = TestToastGateway()
        val toast = GatewayKsToast(gateway)
        toast.registry.registerViewModel(ConfigurableTestToastViewModel::class) {
            DerivedTestToastViewModel()
        }

        val failure = assertFailsWith<DialogException> {
            toast.show(ConfigurableTestToastViewModel::class)
        }

        assertTrue(
            failure.message?.contains("DerivedTestToastViewModel") == true,
            "型不一致の説明に生成物の型が出ませんでした: ${failure.message}",
        )
        assertTrue(gateway.shownViewModels.isEmpty(), "型不一致なのに委譲面が呼ばれました。")
    }
}
