package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignment
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentCenter
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentEnd
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentFill
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentStart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 置き場所つきの委譲経路を確かめるための ViewModel。View factory は登録せずに使う。 */
private class PlacementProbeViewModel : DialogViewModel<Boolean>

/**
 * 共有コードの置き場所が、互換面の輸送形として値を保ったまま境界を渡ることの実測。
 *
 * 配置の計算は iOS Native ライブラリの担当なので、ここで見るのは輸送の値保存だけである (core/ADR-0001)。
 * 輸送形は ObjC の実体なので、書き込んだ値を境界越しに読み返して確かめる。
 */
@OptIn(ExperimentalForeignApi::class)
class InteropPlacementTransportTests {

    @Test
    fun `置き場所は判別と数値のまま互換面へ渡る`() {
        val transported = DialogPlacement(
            horizontalAlignment = DialogAlignment.END,
            verticalAlignment = DialogAlignment.START,
            offsetX = 12.5,
            offsetY = -8.0,
        ).toInterop()

        assertEquals(KSDInteropDialogAlignmentEnd, transported.horizontalAlignment)
        assertEquals(KSDInteropDialogAlignmentStart, transported.verticalAlignment)
        assertEquals(12.5, transported.offsetX)
        assertEquals(-8.0, transported.offsetY)
    }

    @Test
    fun `既定値の置き場所も同じ値で渡る`() {
        val transported = DialogPlacement().toInterop()

        assertEquals(KSDInteropDialogAlignmentCenter, transported.horizontalAlignment)
        assertEquals(KSDInteropDialogAlignmentCenter, transported.verticalAlignment)
        assertEquals(0.0, transported.offsetX)
        assertEquals(0.0, transported.offsetY)
    }

    @Test
    fun `placement つきの show も互換面の同じ経路を通る`() {
        val failure = runPumpingMainLoop {
            runCatching {
                Dialog.instance.show(
                    PlacementProbeViewModel(),
                    DialogPlacement(horizontalAlignment = DialogAlignment.END, offsetX = 12.5),
                )
            }
        }.exceptionOrNull()

        // 提示先の画面を持たないテストランナーでは表示まで到達しないため、
        // 互換面へ届いたことは「その先で起きる構成エラーが共有コードへ返る」ことで判定する
        val exception = assertNotNull(failure, "提示先が無いのに結果が返りました。")
        assertTrue(exception is DialogException, "構成エラーが $exception として届きました。")
    }

    @Test
    fun `配置の4値はすべて互換面の同じ判別へ写る`() {
        val transported: List<KSDInteropDialogAlignment> = DialogAlignment.entries.map { alignment ->
            DialogPlacement(horizontalAlignment = alignment).toInterop().horizontalAlignment
        }

        assertEquals(
            listOf(
                KSDInteropDialogAlignmentStart,
                KSDInteropDialogAlignmentCenter,
                KSDInteropDialogAlignmentEnd,
                KSDInteropDialogAlignmentFill,
            ),
            transported,
        )
    }
}
