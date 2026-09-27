package jp.kamusoft.ksdialogs

import android.os.Build
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.SystemBarsObservation
import jp.kamusoft.ksdialogs.support.SystemBarsObservation.readOnMain
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity.Appearance
import jp.kamusoft.ksdialogs.support.attach
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * ダイアログの器が、提示先の画面のアイコンの明暗と隠れたバーの再表示の作法を変えないことを、
 * 実ウィンドウで確かめる (core/ADR-0039)。
 *
 * 器の指定は、この検証が提示先に与えた指定から組み立てた期待値と比べる。提示先のウィンドウから
 * OS が返す値とは比べない — 旧来のフラグだけで明暗を指定した提示先や、作法を指定していない提示先では
 * OS が 0 を返し、正しく引き継いだ器の値と食い違うため。比べる前に、提示先の指定が狙いどおりに
 * なっていることを前提として確かめる (差の出ない状態どうしを比べて空振りで通らないように)。
 *
 * 表示/非表示の引き継ぎは [DialogSystemBarsTests] が確かめている。
 */
@RunWith(AndroidJUnit4::class)
class DialogSystemBarAppearanceTests {

    @Test
    fun PB_SB_08_明るい地向けの明暗を指定した画面でダイアログを出しても明暗の指定は変わらない() {
        launchHost(Appearance.LIGHT_BARS).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先が明るい地向けの明暗を指定している",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.requestedLightBars(host.window) },
            )

            val container = presentContainer(scenario)
            try {
                val adopted = readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(container.window)) }
                assertEquals(
                    "ダイアログの器のステータスバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_STATUS,
                    adopted and LIGHT_STATUS,
                )
                assertEquals(
                    "ダイアログの器のナビゲーションバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_NAVIGATION,
                    adopted and LIGHT_NAVIGATION,
                )
            } finally {
                dismiss(scenario, container)
            }
        }
    }

    @Test
    fun PB_SB_09_旧来のフラグだけで明暗を指定した画面でもダイアログは明暗を引き継ぐ() {
        assumeInsetsControllerPath()
        launchHost(Appearance.LIGHT_BARS_BY_LEGACY_FLAGS_ONLY).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先が旧来のフラグで明るい地向けの明暗を指定している",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.legacyLightBars(host.window) },
            )

            val container = presentContainer(scenario)
            try {
                val adopted = readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(container.window)) }
                assertEquals(
                    "旧来のフラグで指定したステータスバーの明暗がダイアログの器へ引き継がれていない",
                    LIGHT_STATUS,
                    adopted and LIGHT_STATUS,
                )
                assertEquals(
                    "旧来のフラグで指定したナビゲーションバーの明暗がダイアログの器へ引き継がれていない",
                    LIGHT_NAVIGATION,
                    adopted and LIGHT_NAVIGATION,
                )
            } finally {
                dismiss(scenario, container)
            }
        }
    }

    /**
     * 旧来のフラグを合わせて読んでも、テーマが立てた旧来のフラグで明示の指定を上書きしない。
     *
     * テーマで明るい地向けにし、プラットフォームの WindowInsetsController で暗い地向けを明示した画面は、
     * 旧来のフラグが残っていても見えは暗い地向けになる。Android 15 以上はテーマ由来の明暗と明示を
     * 見分けられるので、器も暗い地向けになることを確かめる (Android 11〜14 では見分けられず、
     * 器は明るい地向けになることを受け入れている。core/ADR-0039)。
     */
    @Test
    fun PB_SB_09_テーマの明暗をコードで暗い地向けに明示した画面ではダイアログも暗い地向けになる() {
        assumeTrue(
            "テーマ由来の明暗と明示を見分けられるのは Android 15 以上",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
        )
        launchHost(Appearance.LIGHT_THEME_DARK_BY_CONTROLLER).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先にテーマ由来の明るい地向けの旧来のフラグが残っている",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.legacyLightBars(host.window) },
            )
            assertEquals(
                "前提: 提示先が WindowInsetsController で暗い地向けを明示している",
                0,
                readOnMain { SystemBarsObservation.requestedLightBars(host.window) },
            )

            val container = presentContainer(scenario)
            try {
                assertEquals(
                    "テーマの旧来のフラグで、提示先が明示した暗い地向けが上書きされた",
                    0,
                    readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(container.window)) },
                )
            } finally {
                dismiss(scenario, container)
            }
        }
    }

    /**
     * テーマの明るい地向けを、旧来のフラグの代入し直しで外した画面では、見えは暗い地向けになる。
     * このとき Android 15 以上の OS は、明示の無いビットにテーマの値 (明るい地向け) を返し続けるので、
     * OS の値だけを写すと器が明るい地向けになる。テーマが立てるビットは旧来のフラグとも突き合わせて
     * 読むことで、器も暗い地向けになることを確かめる (core/ADR-0039)。
     */
    @Test
    fun PB_SB_09_テーマの明暗を旧来のフラグの代入で外した画面ではダイアログも暗い地向けになる() {
        assumeTrue(
            "OS がテーマ由来の明暗を返すのは Android 15 以上",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
        )
        launchHost(Appearance.LIGHT_THEME_LEGACY_FLAGS_CLEARED).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先の旧来のフラグから明るい地向けが外れている",
                0,
                readOnMain { SystemBarsObservation.legacyLightBars(host.window) },
            )
            assertEquals(
                "前提: 提示先について OS はテーマ由来の明るい地向けを返している",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.requestedLightBars(host.window) },
            )

            val container = presentContainer(scenario)
            try {
                assertEquals(
                    "旧来のフラグで外したテーマの明るい地向けが、ダイアログの器で復活した",
                    0,
                    readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(container.window)) },
                )
            } finally {
                dismiss(scenario, container)
            }
        }
    }

    /**
     * 作法の既定 (BEHAVIOR_DEFAULT) は Android 12 で定義された。作法を指定していない提示先について
     * アプリから 0 が返る (少なくとも Android 12〜13) のに対し、ウィンドウ管理は既定の作法で扱う。
     * この食い違いを器が 0 のまま写さないことを、ウィンドウ管理が保持する器の作法で確かめる。
     * Android 11 では 0 そのものが既定の作法なので、この検証は成り立たない。
     */
    @Test
    fun PB_SB_10_作法を指定していない画面ではダイアログの作法は_OS_の既定になる() {
        assumeTrue(
            "作法の既定 (BEHAVIOR_DEFAULT) は Android 12 以上にしかない",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
        launchHost(Appearance.UNSPECIFIED).use { scenario ->
            assertEquals(
                "前提: 作法を指定していない提示先が、ウィンドウ管理では既定の作法として扱われている",
                SystemBarsObservation.WINDOW_MANAGER_DEFAULT_BEHAVIOR,
                SystemBarsObservation.windowManagerBehavior("BASE_APPLICATION"),
            )

            val container = presentContainer(scenario)
            try {
                // 期待値は「作法を指定していないウィンドウの OS の既定」。提示先に作法を与えていないので既定になる
                assertEquals(
                    "ダイアログの器の作法が OS の既定になっていない",
                    SystemBarsObservation.WINDOW_MANAGER_DEFAULT_BEHAVIOR,
                    SystemBarsObservation.windowManagerBehavior("APPLICATION"),
                )
            } finally {
                dismiss(scenario, container)
            }
        }
    }

    // 組み立て

    private fun launchHost(appearance: Appearance): ActivityScenario<SystemBarsTestActivity> =
        ActivityScenario.launch(
            SystemBarsTestActivity.intent(InstrumentationRegistry.getInstrumentation().targetContext, appearance),
        )

    private fun hostActivity(scenario: ActivityScenario<SystemBarsTestActivity>): SystemBarsTestActivity {
        val activity = AtomicReference<SystemBarsTestActivity>()
        scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    /** 既定の覆いの色でダイアログを1枚出し、画面に載って表示状態になるまで待つ。 */
    private fun presentContainer(scenario: ActivityScenario<SystemBarsTestActivity>): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        scenario.onActivity { activity ->
            DialogContainer(
                context = activity,
                contentView = TextView(activity).apply { text = "明暗の確認" }.attach(options = null, placement = null),
                resultChannel = DialogResultChannel(),
            ).also {
                container.set(it)
                it.show()
            }
        }
        val presented = requireNotNull(container.get())
        val shown = runBlocking {
            InstrumentedDialogWaiting.waitUntil { presented.containerState == DialogContainerState.SHOWN }
        }
        check(shown) { "ダイアログが表示状態にならなかった" }
        return presented
    }

    private fun dismiss(scenario: ActivityScenario<SystemBarsTestActivity>, container: DialogContainer) {
        scenario.onActivity { container.dismiss() }
    }

    private fun assumeInsetsControllerPath() {
        assumeTrue(
            "WindowInsetsController の経路は Android 11 以上にしかない",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    private companion object {
        const val LIGHT_BARS: Int = SystemBarsTestActivity.LIGHT_BARS_APPEARANCE
        const val LIGHT_STATUS: Int = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        const val LIGHT_NAVIGATION: Int = android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
    }
}
