package jp.kamusoft.ksdialogs.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.ToastViewRegistry
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** 宣言的 UI で中身を登録し、型を渡して表示するカスタム Loading の ViewModel。 */
internal class ComposeTypedLoadingTestViewModel : LoadingViewModel {
    /** 中身の組み立てが読む表題。 */
    var title: String = "factory の既定"
}

/** 宣言的 UI で中身を登録し、型を渡して表示するカスタム Toast の ViewModel。 */
internal class ComposeTypedToastTestViewModel : ToastViewModel {
    /** 中身の組み立てが読む表題。 */
    var title: String = "factory の既定"
}

/**
 * 宣言的 UI で中身を登録した ViewModel 型でも、型を渡す表示 (core/ADR-0035) が
 * 従来 View 系の登録と同じに働くことを確かめる。
 *
 * 型を渡す表示はレジストリの ViewModel factory と View factory を引くだけなので、
 * 宣言的 UI 側に追加の登録面・表示面は要らない。
 */
@RunWith(AndroidJUnit4::class)
class ComposeTypedShowTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun LD_YA_02_Compose_登録でも型指定_show_が同じに働く() = runBlocking<Unit> {
        val observedTitle = AtomicReference<String>()
        LoadingViewRegistry.shared.registerViewModel(ComposeTypedLoadingTestViewModel::class) {
            ComposeTypedLoadingTestViewModel()
        }
        LoadingViewRegistry.shared.registerCompose(ComposeTypedLoadingTestViewModel::class) { viewModel ->
            observedTitle.set(viewModel.title)
            Box(Modifier.size(CONTENT_SIZE_DP.dp, CONTENT_SIZE_DP.dp))
        }

        Loading.instance.start(
            ComposeTypedLoadingTestViewModel::class,
            configure = { viewModel -> viewModel.title = CONFIGURED_TITLE },
        ) { _ ->
            assertEquals(
                "configure で設定した状態が表示に反映される",
                CONFIGURED_TITLE,
                awaitValue { observedTitle.get() },
            )
        }
    }

    @Test
    fun TS_YA_03_Compose_登録でも_Toast_の型指定_show_が同じに働く() = runBlocking<Unit> {
        val observedTitle = AtomicReference<String>()
        val observation = ComposeContentObservation()
        ToastViewRegistry.shared.registerViewModel(ComposeTypedToastTestViewModel::class) {
            ComposeTypedToastTestViewModel()
        }
        ToastViewRegistry.shared.registerCompose(ComposeTypedToastTestViewModel::class) { viewModel ->
            observedTitle.set(viewModel.title)
            ObserveHostView(observation)
            Box(Modifier.size(CONTENT_SIZE_DP.dp, CONTENT_SIZE_DP.dp))
        }

        Toast.instance.show(
            ComposeTypedToastTestViewModel::class,
            durationMs = DURATION_MILLIS,
            configure = { viewModel -> viewModel.title = CONFIGURED_TITLE },
        )

        assertEquals(
            "configure で設定した状態が表示に反映される",
            CONFIGURED_TITLE,
            awaitValue { observedTitle.get() },
        )

        // Toast は共有の表示器に載るため、この表示が消えるまで見届けてから終える
        val contentView = requireNotNull(
            awaitValue { observation.contentView.get()?.takeIf { it.isAttachedToWindow } },
        )
        assertEquals(
            "表示時間の経過で表示が取り外される",
            true,
            awaitValue { true.takeIf { !contentView.isAttachedToWindow } },
        )
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
        const val CONTENT_SIZE_DP = 160

        /** configure が設定する表題。factory の既定と区別できる文言にする。 */
        const val CONFIGURED_TITLE = "configure で設定"

        /** Toast の表示時間 (ミリ秒)。観察を終えてから消滅まで見届けられる長さ。 */
        const val DURATION_MILLIS = 600

        /** 観察の間隔 (ミリ秒)。 */
        const val POLLING_INTERVAL_MILLIS = 8L
    }
}
