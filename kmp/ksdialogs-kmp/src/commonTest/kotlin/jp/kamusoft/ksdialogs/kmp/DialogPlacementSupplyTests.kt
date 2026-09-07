package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.TestDialogGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * 共有コードから供給した置き場所が、値を保ったまま委譲面へ渡ることの検証。
 *
 * 配置の計算そのものは各 OS の Native 実装の担当なので、ここでは輸送の値保存だけを見る (core/ADR-0001)。
 * この検証は iOS / Android の両ターゲットで実行される。
 */
class DialogPlacementSupplyTests {

    @Test
    fun `show の placement は同じ値のまま委譲面へ渡る`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val placement = DialogPlacement(
            horizontalAlignment = DialogAlignment.END,
            verticalAlignment = DialogAlignment.START,
            offsetX = 12.5,
            offsetY = -8.0,
        )

        GatewayKsDialog(gateway).show(BooleanTestDialogViewModel(), placement)

        assertSame(placement, gateway.presentedPlacements.single(), "placement が包み直されて委譲されました。")
        assertEquals(placement, gateway.presentedPlacements.single())
    }

    @Test
    fun `placement を省略した show は委譲面へ何も渡さない`() = runTest {
        val gateway = TestDialogGateway.completing(true)

        GatewayKsDialog(gateway).show(BooleanTestDialogViewModel())

        assertNull(
            gateway.presentedPlacements.single(),
            "省略した placement が既定値のオブジェクトに化けました (添付が効かなくなります)。",
        )
    }

    @Test
    fun `呼び出しごとに違う placement を渡せる`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val first = DialogPlacement(horizontalAlignment = DialogAlignment.START)
        val second = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 4.0)

        dialogs.show(BooleanTestDialogViewModel("1枚目"), first)
        dialogs.show(BooleanTestDialogViewModel("2枚目"), second)

        assertEquals<List<DialogPlacement?>>(listOf(first, second), gateway.presentedPlacements)
    }

    @Test
    fun `placement の既定値は中央配置で移動なし`() {
        val placement = DialogPlacement()

        assertEquals(DialogAlignment.CENTER, placement.horizontalAlignment)
        assertEquals(DialogAlignment.CENTER, placement.verticalAlignment)
        assertEquals(0.0, placement.offsetX)
        assertEquals(0.0, placement.offsetY)
    }
}
