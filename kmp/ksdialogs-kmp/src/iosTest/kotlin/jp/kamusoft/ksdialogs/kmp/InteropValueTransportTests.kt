package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.TestDialogGateway
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSMutableArray
import platform.Foundation.NSNumber
import platform.Foundation.numberWithBool
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 互換面が結果値を型消去して運ぶときの表現と、宣言結果型への復元の往復を実測する。
 *
 * 互換面の結果値は `id` として渡るため、Kotlin の値と Swift 由来の値がそれぞれどう見えるかで
 * 復元が成り立つかが決まる。ObjC のコンテナを通した往復で同じ経路を再現する。
 */
@OptIn(BetaInteropApi::class)
class InteropValueTransportTests {

    @Test
    fun `Kotlin の結果値は型を消して運んでも宣言結果型のまま戻る`() = runTest {
        val transported = roundTripThroughObjC(true)
        println("[実測] Kotlin 由来の結果値の実体: ${describe(transported)}")

        val restored = restoreAsBooleanResult(transported)

        assertEquals(DialogResult.Completed(true), restored)
    }

    @Test
    fun `Swift 由来の結果値も宣言結果型へ戻る`() = runTest {
        val transported = roundTripThroughObjC(NSNumber.numberWithBool(true))
        println("[実測] Swift 由来の結果値の実体: ${describe(transported)}")

        val restored = restoreAsBooleanResult(transported)

        assertEquals(DialogResult.Completed(true), restored)
    }

    /** ObjC のコンテナへ入れて取り出し、`id` を経由した値の見え方を再現する。 */
    private fun roundTripThroughObjC(value: Any?): Any? {
        val container = NSMutableArray()
        container.addObject(value)
        return container.objectAtIndex(0u)
    }

    /** 型を消して運ばれた結果値を、宣言結果型が Boolean の ViewModel の結果として復元する。 */
    private suspend fun restoreAsBooleanResult(value: Any?): DialogResult<Boolean> =
        GatewayKsDialog(TestDialogGateway.completing(value)).show(BooleanTestDialogViewModel())

    private fun describe(value: Any?): String = "${value?.let { it::class.simpleName }} ($value)"
}
