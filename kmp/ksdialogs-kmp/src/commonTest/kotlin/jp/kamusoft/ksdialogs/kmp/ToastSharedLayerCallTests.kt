package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.FakeKsToast
import jp.kamusoft.ksdialogs.kmp.support.PlainTestToastViewModel
import jp.kamusoft.ksdialogs.kmp.support.SaveNotifier
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertSame

/**
 * UI 層を参照しない共有コードからの Toast 呼び出しの検証。
 *
 * 共有ソースにはプラットフォームの View 型もスタイルの型も存在しないため、
 * 「View も色も知らないコードが通知を出せる」ことはこの置き場で書けること自体が示している。
 * 実際に画面へ出ることは各 OS のテストと Sample が担う。
 */
class ToastSharedLayerCallTests {

    @Test
    fun `既定エントリと契約 interface 経由で同じ実体を指す`() {
        val viaDefaultEntry = Toast.instance
        val viaContract: KsToast = Toast.instance

        assertSame(viaDefaultEntry, viaContract, "既定エントリが呼ぶたびに別の実体になりました。")
    }

    @Test
    fun `共有コードはメッセージと duration だけで通知を出せる`() {
        val toast = FakeKsToast()

        SaveNotifier(toast).notifySaved()

        assertContentEquals(listOf("保存しました"), toast.shownMessages)
        assertContentEquals(listOf(2000), toast.shownDurations)
        assertContentEquals(listOf(null), toast.shownViewModels)
    }

    @Test
    fun `共有 VM は包み直されずに委譲面へ渡る`() {
        val toast = FakeKsToast()
        val viewModel = PlainTestToastViewModel()

        SaveNotifier(toast).notifySaved(viewModel)

        assertSame(viewModel, toast.shownViewModels.single(), "共有 VM がそのまま委譲されていません。")
        assertContentEquals(listOf(DialogPlacement(offsetY = 8.0)), toast.shownPlacements)
    }

    @Test
    fun `duration と置き場所を省略した表示は委譲面へ null のまま渡る`() {
        val toast = FakeKsToast()

        toast.show("保存しました")
        toast.show(PlainTestToastViewModel())

        assertContentEquals(listOf(null, null), toast.shownDurations)
        assertContentEquals(listOf(null, null), toast.shownPlacements)
    }
}
