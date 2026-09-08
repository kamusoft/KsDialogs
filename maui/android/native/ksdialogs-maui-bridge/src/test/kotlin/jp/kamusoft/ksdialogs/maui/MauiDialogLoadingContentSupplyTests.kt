package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側が中身を作れなかった Dialog / Loading が、既存の失敗の通知に合流することの検証。
 *
 * 中身の供給は managed / native の境界を跨いで呼ばれるため、MAUI 側は失敗を例外のまま返さず
 * 中身なし (null) として返す。この面はその null を通常の失敗へ変換し、閉鎖 (Dialog) と
 * 完了 (Loading) の通知の失敗として呼び出し側へ届ける。元の失敗そのものは MAUI 側が
 * 呼び出し 1 回分だけ預かっていて、通知を受けた時点で呼び出し元へ返る。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MAUI 側が中身を作れなかった Dialog / Loading は失敗の通知になる")
class MauiDialogLoadingContentSupplyTests {

    /** 閉鎖の通知先へ届いた呼び出しを順に記録するもの。 */
    private class RecordingClosureListener : MauiDialogClosureListener {
        val closures: MutableList<String> = mutableListOf()

        override fun onCancelled() {
            closures += "cancelled"
        }

        override fun onDismissed() {
            closures += "dismissed"
        }

        override fun onPresentationHostUnavailable(message: String?) {
            closures += "hostUnavailable:$message"
        }

        override fun onFailed(message: String?) {
            closures += "failed:$message"
        }
    }

    /** 完了の通知先へ届いた呼び出しを順に記録するもの。 */
    private class RecordingCompletionListener : MauiLoadingCompletionListener {
        val completions: MutableList<String> = mutableListOf()

        override fun onCompleted() {
            completions += "completed"
        }

        override fun onFailure(message: String?) {
            completions += "failure:$message"
        }
    }

    @Test
    @DisplayName("[MB-MA-15] 中身なしで返った Dialog の供給は閉鎖の通知の失敗になる")
    fun `MB-MA-15 中身なしで返った Dialog の供給は閉鎖の通知の失敗になる`() = runTest {
        val listener = RecordingClosureListener()
        val viewModel = MauiDialogViewModel(
            MauiDialogContentProvider { null },
            MauiDialogPresentation(),
        )

        // 中身の生成は器が提示先を確保した後に呼ばれ、その失敗は表示の失敗として通知へ変換される。
        // ここで例外が外へ漏れれば、この呼び出し自体が失敗してテストが落ちる
        reportClosure(listener) {
            viewModel.createContentView()
            DialogResult.Completed(true)
        }

        assertEquals(1, listener.closures.size, "通知はちょうど1回だけ届くこと")
        assertTrue(
            listener.closures.single().startsWith("failed:"),
            "中身なしが失敗の通知にならずに紛れました: ${listener.closures.single()}",
        )
    }

    @Test
    @DisplayName("[MB-MA-15] 中身なしで返った Loading の供給は完了の通知の失敗になる")
    fun `MB-MA-15 中身なしで返った Loading の供給は完了の通知の失敗になる`() = runTest {
        val listener = RecordingCompletionListener()
        val viewModel = MauiLoadingViewModel(
            MauiLoadingContentProvider { null },
            progressReceiver = null,
        )

        reportLoadingCompletion(listener) { viewModel.createContentView() }

        assertEquals(1, listener.completions.size, "通知はちょうど1回だけ届くこと")
        assertTrue(
            listener.completions.single().startsWith("failure:"),
            "中身なしが失敗の通知にならずに紛れました: ${listener.completions.single()}",
        )
    }
}
