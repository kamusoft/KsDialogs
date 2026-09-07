package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.FakeKsLoading
import jp.kamusoft.ksdialogs.kmp.support.PlainTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.ProgressReceivingTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.UploadPresenter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

/**
 * UI 層を参照しない共有コードからのローディング呼び出しの検証。
 *
 * 共有ソースにはプラットフォームの View 型もスタイルの型も存在しないため、
 * 「View も色も知らないコードが表示・進捗・終了を回せる」ことはこの置き場で書けること自体が示している。
 * 実際に画面へ出ることは各 OS のテストと Sample が担う。
 */
class LoadingSharedLayerCallTests {

    @Test
    fun `LD-KM-01 共有コードの start が表示と進捗を回して戻り値を受け取る`() = runTest {
        val loading = FakeKsLoading()

        val received = UploadPresenter(loading).upload(steps = 4)

        assertEquals("完了", received, "処理の戻り値が共有コードへ届きませんでした。")
        assertContentEquals(listOf("アップロード中"), loading.startedMessages)
        assertContentEquals(listOf(0.25, 0.5, 0.75, 1.0), loading.reportedProgress)
        assertFalse(loading.isPresenting, "処理の完了で合流1件の終了が数えられていません。")
    }

    @Test
    fun `LD-KM-01 処理が失敗しても合流1件は終了し失敗が呼び出し元へ伝播する`() = runTest {
        val loading = FakeKsLoading()

        assertFailsWith<IllegalStateException> {
            loading.start<Unit> { error("処理の失敗") }
        }

        assertFalse(loading.isPresenting, "失敗した処理でも終了が数えられていません。")
    }

    @Test
    fun `LD-KM-03 共有 VM を渡した start の進捗が VM の受け口へ届く`() = runTest {
        val loading = FakeKsLoading()
        val viewModel = ProgressReceivingTestLoadingViewModel()

        val received = UploadPresenter(loading).upload(viewModel, steps = 2)

        assertEquals("完了", received)
        assertSame(viewModel, loading.startedViewModels.single(), "共有 VM がそのまま委譲されていません。")
        assertContentEquals(listOf(0.5, 1.0), viewModel.receivedProgress)
    }

    @Test
    fun `LD-KM-03 受け口を持たない共有 VM でも進捗の報告は誤りにならない`() = runTest {
        val loading = FakeKsLoading()

        val received = UploadPresenter(loading).upload(PlainTestLoadingViewModel(), steps = 2)

        assertEquals("完了", received)
        assertContentEquals(listOf(0.5, 1.0), loading.reportedProgress)
    }

    @Test
    fun `置き場所を省略した表示は委譲面へ null のまま渡る`() = runTest {
        val loading = FakeKsLoading()

        loading.show()
        loading.show(PlainTestLoadingViewModel(), placement = DialogPlacement(offsetY = 8.0))

        assertContentEquals(listOf(null, DialogPlacement(offsetY = 8.0)), loading.startedPlacements)
    }
}
