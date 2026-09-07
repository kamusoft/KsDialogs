package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.StringTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.TextPromptPresenter
import jp.kamusoft.ksdialogs.kmp.support.TestDialogGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * UI 層を参照しない共有コードからの呼び出しの検証。
 *
 * 共有ソースにはプラットフォームの View 型が存在しないため、「View を知らないコードが
 * ViewModel を渡して型付きの結果を受け取る」ことはこの置き場で書けること自体が示している。
 * 委譲面は Native 側の立場で、View が結果報告口から報告した値をそのまま返す。
 *
 * このテストは iOS / Android の両ターゲットで実行される。
 * Native 側の View が結果報告口を ViewModel から引く経路そのものは、
 * 各 OS のテスト (iOS は `KsDialogsKmpModelBindingTests`、Android は `KmpViewModelSupplyTests`) が担う。
 */
class SharedLayerCallTests {

    @Test
    fun `MB-KM-03 UI 層参照なしの共有コードが型付き結果を受け取る`() = runTest {
        val gateway = TestDialogGateway.completing("入力")

        val received = TextPromptPresenter(GatewayKsDialog(gateway)).prompt()

        assertEquals("入力", received, "宣言結果型のまま共有コードへ届きませんでした。")
        assertEquals(1, gateway.presentedViewModels.size)
    }

    @Test
    fun `共有コードは中身の View を渡さずに ViewModel だけを委譲する`() = runTest {
        val gateway = TestDialogGateway.completing("入力")
        val viewModel = StringTestDialogViewModel()

        GatewayKsDialog(gateway).show(viewModel)

        assertSame(viewModel, gateway.presentedViewModels.single())
    }
}
