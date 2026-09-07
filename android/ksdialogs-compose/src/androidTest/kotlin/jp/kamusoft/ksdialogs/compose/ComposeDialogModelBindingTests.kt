package jp.kamusoft.ksdialogs.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeModelBindingTestDialogViewModel
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.awaitPresented
import jp.kamusoft.ksdialogs.notifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 宣言的 UI の中身でも、ViewModel 主導の呼び出し (VM 経由の結果報告口・型指定 show) が
 * 従来 View 系と同じに成り立つことを確かめる。
 *
 * 提示は公開 API だけを使い、実際のダイアログのウィンドウを出す経路で行う。
 */
@RunWith(AndroidJUnit4::class)
class ComposeDialogModelBindingTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun MB_AN_02_Compose_登録でも_VM_供給と型指定呼び出しが同じに働く() = runBlocking {
        val suppliedNotifier = CompletableDeferred<DialogNotifier<Boolean>>()
        val observedMessage = CompletableDeferred<String>()

        DialogViewRegistry.shared.registerViewModel(ComposeModelBindingTestDialogViewModel::class) {
            ComposeModelBindingTestDialogViewModel("factory の既定")
        }
        DialogViewRegistry.shared.registerCompose(ComposeModelBindingTestDialogViewModel::class) { viewModel ->
            // 組み立て中に ViewModel から報告口を引ける。取り出しは組み立てが確定した後に行う
            val notifier = viewModel.notifier
            val message = viewModel.message
            SideEffect {
                observedMessage.complete(message)
                notifier?.let { suppliedNotifier.complete(it) }
            }
            Box(Modifier.size(CONTENT_SIZE_DP.dp))
        }

        coroutineScope {
            val showTask = async {
                Dialog.instance.show(ComposeModelBindingTestDialogViewModel::class) { viewModel ->
                    viewModel.message = "configure で設定"
                }
            }

            assertEquals("configure で設定", observedMessage.awaitPresented())
            suppliedNotifier.awaitPresented().complete(true)
            val result: DialogResult<Boolean> = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }

            assertEquals(DialogResult.Completed(true), result)
        }
    }

    private companion object {
        /** 中身が要求する大きさ (dp)。見えていれば足りるので最小限にする。 */
        const val CONTENT_SIZE_DP = 120
    }
}
