package jp.kamusoft.ksdialogs.compose

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.ToastViewRegistry
import jp.kamusoft.ksdialogs.compose.support.ComposeContentObservation
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.FixedSizeContentView
import jp.kamusoft.ksdialogs.compose.support.ObserveHostView
import jp.kamusoft.ksdialogs.compose.support.PRESENTATION_TIMEOUT_MILLIS
import jp.kamusoft.ksdialogs.compose.support.rectOnScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** 宣言的 UI で登録するカスタム Toast の ViewModel。 */
internal class ComposeToastTestViewModel : ToastViewModel

/** 対照となる従来 View 系で登録するカスタム Toast の ViewModel。 */
internal class ViewToastTestViewModel : ToastViewModel

/**
 * 宣言的 UI で登録したカスタム Toast が、従来 View 系の登録と同じ観察可能挙動
 * (表示・配置・消滅) になることを確かめる。
 *
 * 本体モジュールは宣言的 UI に依存しないため、Compose 系の登録面とその検証はこのモジュールに置く。
 * 提示は公開 API だけを使い、実際に Toast のウィンドウを出す経路で行う。
 */
@RunWith(AndroidJUnit4::class)
class ComposeToastCustomViewTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun TS_AN_02_Compose_登録のカスタム_Toast_が従来_View_登録と同じに働く() = runBlocking {
        val viewObservation = observeDisplay { grip ->
            ToastViewRegistry.shared.register(ViewToastTestViewModel::class) { _ ->
                val density = resources.displayMetrics.density
                FixedSizeContentView(
                    context = this,
                    contentWidth = (CONTENT_SIZE_DP * density).toInt(),
                    contentHeight = (CONTENT_SIZE_DP * density).toInt(),
                ).also(grip.hostView::set)
            }
            Toast.instance.show(ViewToastTestViewModel(), durationMs = DURATION_MILLIS)
        }
        val composeObservation = observeDisplay { grip ->
            ToastViewRegistry.shared.registerCompose(ComposeToastTestViewModel::class) { _ ->
                ObserveHostView(grip.observation)
                Box(Modifier.size(CONTENT_SIZE_DP.dp, CONTENT_SIZE_DP.dp))
            }
            Toast.instance.show(ComposeToastTestViewModel(), durationMs = DURATION_MILLIS)
        }

        assertEquals(
            "表示・配置・消滅の観察が一致する",
            viewObservation,
            composeObservation,
        )
    }

    /** 表示・配置・消滅の観察をひとまとめにしたもの。登録の技術が変わっても同じ値になる。 */
    private data class DisplayObservation(
        val didAttachToWindow: Boolean,
        val rectOnScreen: Rect,
        val didDetachAfterDuration: Boolean,
    )

    /** 中身を載せている View の掴み方。従来 View 系と宣言的 UI 系で掴み方だけが違う。 */
    private class ContentViewGrip {
        /** 従来 View 系で factory が返した中身。 */
        val hostView: AtomicReference<View> = AtomicReference()

        /** 宣言的 UI で組み立ての土台になっている View の観察先。 */
        val observation: ComposeContentObservation = ComposeContentObservation()

        fun contentView(): View? = hostView.get() ?: observation.contentView.get()
    }

    /** 渡した登録と表示で duration の経過まで通し、観察できた結果を返す。 */
    private suspend fun observeDisplay(register: (ContentViewGrip) -> Unit): DisplayObservation {
        val grip = ContentViewGrip()
        register(grip)

        // 画面に載った直後はまだレイアウトが走っておらず、位置も大きさも 0 のことがある。
        // 配置を観察するので、実際に大きさが決まるまで待つ
        val contentView = requireNotNull(
            awaitValue {
                grip.contentView()?.takeIf { it.isAttachedToWindow && it.width > 0 && it.height > 0 }
            },
        )
        val didAttachToWindow = contentView.isAttachedToWindow
        val rect = contentView.rectOnScreen()

        val detached = awaitValue { true.takeIf { !contentView.isAttachedToWindow } } ?: false

        return DisplayObservation(
            didAttachToWindow = didAttachToWindow,
            rectOnScreen = rect,
            didDetachAfterDuration = detached,
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
        const val CONTENT_SIZE_DP = 160.0

        /** 表示時間 (ミリ秒)。 */
        const val DURATION_MILLIS = 600

        /** 観察の間隔 (ミリ秒)。 */
        const val POLLING_INTERVAL_MILLIS = 8L
    }
}
