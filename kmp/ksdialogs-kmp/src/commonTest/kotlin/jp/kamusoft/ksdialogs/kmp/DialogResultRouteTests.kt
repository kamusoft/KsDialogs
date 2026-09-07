package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.TestDialogGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.fail

/**
 * 共有コードから見た結果経路の検証。
 *
 * 委譲面を差し替えて、Native 実装なしに「completed / cancelled / 構成エラー」の3経路を通す。
 * この検証は iOS / Android の両ターゲットで実行される。
 */
class DialogResultRouteTests {

    @Test
    fun `完了操作で completed が返る`() = runTest {
        val dialogs = GatewayKsDialog(TestDialogGateway.completing(true))

        val result = dialogs.show(BooleanTestDialogViewModel())

        assertEquals(DialogResult.Completed(true), result)
    }

    @Test
    fun `完了の結果値は宣言結果型で受け取れる`() = runTest {
        val dialogs = GatewayKsDialog(TestDialogGateway.completing(false))

        when (val result = dialogs.show(BooleanTestDialogViewModel())) {
            // 受け取り側で型を書ける = 宣言結果型が結ばれている
            is DialogResult.Completed -> {
                val value: Boolean = result.value
                assertEquals(false, value)
            }

            DialogResult.Cancelled -> fail("completed が返りませんでした。")
        }
    }

    @Test
    fun `キャンセル操作で cancelled が返る`() = runTest {
        val dialogs = GatewayKsDialog(TestDialogGateway.cancelling())

        val result = dialogs.show(BooleanTestDialogViewModel())

        assertEquals(DialogResult.Cancelled, result)
    }

    @Test
    fun `構成エラーでは結果を返さずに失敗する`() = runTest {
        val dialogs = GatewayKsDialog(TestDialogGateway.failing("View factory が登録されていません。"))

        val failure = assertFailsWith<DialogException> { dialogs.show(BooleanTestDialogViewModel()) }

        assertEquals("View factory が登録されていません。", failure.message)
    }

    @Test
    fun `show は委譲を1回だけ行い ViewModel をそのまま渡す`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val viewModel = BooleanTestDialogViewModel()

        GatewayKsDialog(gateway).show(viewModel)

        assertEquals(1, gateway.presentedViewModels.size)
        assertSame(viewModel, gateway.presentedViewModels.single(), "ViewModel が包み直されて委譲されました。")
    }

    @Test
    fun `重ねて show しても各呼び出しが自分の結果を受け取る`() = runTest {
        val results = mutableListOf<DialogResult<Boolean>>()
        val gateway = TestDialogGateway { DialogOutcome.Completed(results.isEmpty()) }
        val dialogs = GatewayKsDialog(gateway)

        results += dialogs.show(BooleanTestDialogViewModel("1枚目"))
        results += dialogs.show(BooleanTestDialogViewModel("2枚目"))

        assertEquals<List<DialogResult<Boolean>>>(
            listOf(DialogResult.Completed(true), DialogResult.Completed(false)),
            results,
        )
        assertEquals(2, gateway.presentedViewModels.size)
    }

    @Test
    fun `レジストリは委譲先のものを指す`() {
        val gateway = TestDialogGateway.completing(true)

        val dialogs: KsDialog = GatewayKsDialog(gateway)

        assertSame(gateway.registry, dialogs.registry)
    }
}
