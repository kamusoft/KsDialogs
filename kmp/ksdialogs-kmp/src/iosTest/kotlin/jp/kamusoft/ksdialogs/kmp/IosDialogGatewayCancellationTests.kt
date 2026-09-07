package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 委譲先の表示面の代わりに、show の開始と取り消しを記録する面。
 *
 * 提示先の画面を持たないテストランナーでは実際の表示まで到達しないため、
 * 「呼び出し元のキャンセルが当該 show の取り消しへ届くか」を、この面が受けた操作で判定する。
 * 結果は [complete] を呼んだときだけ届くので、表示中のダイアログをそのまま再現できる。
 *
 * @param onShowStarted show の開始時に呼ばれる。引数はその show の番号 (0 始まり)
 */
private class RecordingShowSurface(
    private val onShowStarted: (Int) -> Unit = {},
) : IosDialogShowSurface {
    private val completions = mutableListOf<(Result<DialogOutcome>) -> Unit>()
    private val cancelledShows = mutableListOf<Int>()

    /** show に渡された ViewModel を呼ばれた順に記録したもの。 */
    val shownViewModels: MutableList<DialogViewModel<*>> = mutableListOf()

    /** 開始された show の数。 */
    val showCount: Int get() = completions.size

    override fun show(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
        completion: (Result<DialogOutcome>) -> Unit,
    ): IosDialogShowCancellation {
        shownViewModels += viewModel
        completions += completion
        val index = completions.lastIndex
        onShowStarted(index)
        return IosDialogShowCancellation { cancelledShows += index }
    }

    /** その show の取り消しが呼ばれた回数。 */
    fun cancelCount(index: Int): Int = cancelledShows.count { it == index }

    /** その show へ結果を届ける。 */
    fun complete(index: Int, outcome: DialogOutcome) {
        completions[index](Result.success(outcome))
    }
}

/**
 * 共有コードからの suspend な show が、呼び出し元コルーチンのキャンセルに追随することの検証。
 *
 * 実際に表示が閉じるところは iOS Native ライブラリの担当なので、ここでは委譲面の契約
 * 「当該 show だけの取り消しがちょうど1回届き、呼び出し元には `CancellationException` が伝播する」を見る
 * (kmp/ADR-0005)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosDialogGatewayCancellationTests {

    @Test
    fun `PB-KC-01 表示中のコルーチンをキャンセルすると当該ダイアログだけが閉じ CancellationException が伝播する`() =
        runTest {
            val surface = RecordingShowSurface()
            val dialogs = GatewayKsDialog(IosDialogGateway(surface))
            val keptViewModel = BooleanTestDialogViewModel(message = "残す")
            val cancelledViewModel = BooleanTestDialogViewModel(message = "取り消す")
            val kept = async { dialogs.show(keptViewModel) }
            val cancelled: Deferred<DialogResult<Boolean>> = async { dialogs.show(cancelledViewModel) }
            runCurrent()
            assertEquals(2, surface.showCount, "2枚とも表示が始まっていません。")
            assertSame(cancelledViewModel, surface.shownViewModels[1], "show の並びが記録と食い違っています。")

            cancelled.cancel()
            runCurrent()

            assertFailsWith<CancellationException>("呼び出し元へ CancellationException が伝播しませんでした。") {
                cancelled.await()
            }
            assertEquals(
                1,
                surface.cancelCount(1),
                "キャンセルした show の取り消しがちょうど1回呼ばれていません。",
            )
            assertEquals(0, surface.cancelCount(0), "他の表示中ダイアログまで取り消されました。")

            surface.complete(0, DialogOutcome.Completed(true))
            assertEquals(
                DialogResult.Completed(true),
                kept.await(),
                "取り消していないダイアログの結果が届きませんでした。",
            )
        }

    @Test
    fun `PB-KC-02 提示が始まる前に来たキャンセルでも取り消しが届く`() = runTest {
        var showing: Deferred<DialogResult<Boolean>>? = null
        // 取り消し操作を受け取るより前にキャンセルが来る状況を、show の最中にキャンセルして再現する
        val surface = RecordingShowSurface(onShowStarted = { showing?.cancel() })
        val dialogs = GatewayKsDialog(IosDialogGateway(surface))

        showing = async { dialogs.show(BooleanTestDialogViewModel()) }
        runCurrent()

        assertEquals(1, surface.showCount, "show が始まっていません。")
        assertEquals(1, surface.cancelCount(0), "提示が始まる前のキャンセルが取りこぼされました。")
        val pending = assertNotNull(showing, "show を待つコルーチンがありません。")
        assertFailsWith<CancellationException>("呼び出し元へ CancellationException が伝播しませんでした。") {
            pending.await()
        }
    }

    @Test
    fun `PB-KC-02 キャンセルの後に届いた結果は呼び出し元へ配送されない`() = runTest {
        val surface = RecordingShowSurface()
        val dialogs = GatewayKsDialog(IosDialogGateway(surface))
        val showing = async { dialogs.show(BooleanTestDialogViewModel()) }
        runCurrent()

        showing.cancel()
        runCurrent()
        surface.complete(0, DialogOutcome.Completed(true))
        runCurrent()

        assertTrue(showing.isCancelled, "キャンセルの後に届いた結果で show が完了しました。")
        assertFailsWith<CancellationException>("呼び出し元へ CancellationException が伝播しませんでした。") {
            showing.await()
        }
    }
}
