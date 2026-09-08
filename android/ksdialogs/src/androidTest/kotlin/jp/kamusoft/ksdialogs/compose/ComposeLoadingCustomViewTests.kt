package jp.kamusoft.ksdialogs.compose

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeLoadingTestViewModel
import jp.kamusoft.ksdialogs.compose.support.FixedSizeContentView
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.RecordingLoadingTestViewModel
import jp.kamusoft.ksdialogs.compose.support.ViewLoadingTestViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 宣言的 UI で登録したカスタム Loading が、従来 View 系の登録と同じ観察可能挙動
 * (表示・進捗転送・撤去) になることを確かめる。
 *
 * 本体モジュールは宣言的 UI に依存しないため、Compose 系の登録面とその検証はこのモジュールに置く。
 * 提示は公開 API だけを使い、実際に Loading のウィンドウを出す経路で行う。
 */
@RunWith(AndroidJUnit4::class)
class ComposeLoadingCustomViewTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun LD_CV_02_宣言的_UI_系の登録でも同じに働く() = runBlocking {
        val viewObservation = observeDisplay(ViewLoadingTestViewModel()) { observed ->
            LoadingViewRegistry.shared.register(ViewLoadingTestViewModel::class) { _ ->
                val density = resources.displayMetrics.density
                FixedSizeContentView(
                    context = this,
                    contentWidth = (CONTENT_SIZE_DP * density).toInt(),
                    contentHeight = (CONTENT_SIZE_DP * density).toInt(),
                ).also(observed.hostView::set)
            }
        }
        val composeObservation = observeDisplay(ComposeLoadingTestViewModel()) { observed ->
            LoadingViewRegistry.shared.registerCompose(ComposeLoadingTestViewModel::class) { supplied ->
                ObserveHostView(observed.observation)
                // 受け口が書き換える状態を読むことで、転送のたびに中身が組み立て直される
                val progress = supplied.progress
                Box(Modifier.size(CONTENT_SIZE_DP.dp, (CONTENT_SIZE_DP * (1.0 - progress / 2)).dp))
            }
        }

        assertEquals(
            "表示・進捗転送・撤去の観察が一致する",
            viewObservation,
            composeObservation,
        )
    }

    /** 表示・進捗転送・撤去の観察をひとまとめにしたもの。登録の技術が変わっても同じ値になる。 */
    private data class DisplayObservation(
        val didAttachToWindow: Boolean,
        val receivedProgress: List<Double>,
        val didDetachAfterScope: Boolean,
    )

    /** 中身を載せている View の掴み方。従来 View 系と宣言的 UI 系で掴み方だけが違う。 */
    private class ContentViewGrip {
        /** 従来 View 系で factory が返した中身。 */
        val hostView: AtomicReference<View> = AtomicReference()

        /** 宣言的 UI で組み立ての土台になっている View の観察先。 */
        val observation: ComposeContentObservation = ComposeContentObservation()

        fun contentView(): View? = hostView.get() ?: observation.contentView.get()
    }

    /** 渡した登録で表示・進捗報告・撤去まで通し、観察できた結果を返す。 */
    private suspend fun observeDisplay(
        viewModel: RecordingLoadingTestViewModel,
        register: (ContentViewGrip) -> Unit,
    ): DisplayObservation {
        val grip = ContentViewGrip()
        register(grip)
        val gate = CompletableDeferred<Unit>()
        val report = AtomicReference<(Double) -> Unit>()

        return coroutineScope {
            val scope = async {
                Loading.instance.start(viewModel) { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            val contentView = requireNotNull(
                awaitValue { grip.contentView()?.takeIf { it.isAttachedToWindow } },
            )
            val didAttachToWindow = contentView.isAttachedToWindow

            requireNotNull(awaitValue { report.get() })(REPORTED_PROGRESS)
            awaitValue { viewModel.receivedProgress.takeIf { it.isNotEmpty() } }

            gate.complete(Unit)
            scope.await()

            DisplayObservation(
                didAttachToWindow = didAttachToWindow,
                receivedProgress = viewModel.receivedProgress,
                didDetachAfterScope = !contentView.isAttachedToWindow,
            )
        }
    }

    /** 値が得られるまで上限つきで待つ。 */
    private suspend fun <T : Any> awaitValue(probe: () -> T?): T? =
        withTimeoutOrNull(PRESENTATION_TIMEOUT_MILLIS) {
            var value = probe()
            while (value == null) {
                delay(POLLING_INTERVAL_MILLIS)
                value = probe()
            }
            value
        }

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 転送を確かめるために報告する進捗値。 */
        const val REPORTED_PROGRESS = 0.6

        /** 観察の間隔 (ミリ秒)。 */
        const val POLLING_INTERVAL_MILLIS = 8L
    }
}
