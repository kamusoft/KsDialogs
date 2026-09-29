package jp.kamusoft.ksdialogs

import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.ImmersiveModeConfirmation
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.InstrumentedStateSettling
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestPresentationSurface
import jp.kamusoft.ksdialogs.support.SystemBarsObservation
import jp.kamusoft.ksdialogs.support.SystemBarsObservation.readOnMain
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity.Appearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Loading の器が、提示先の画面のシステムバーの指定 (アイコンの明暗・バーの表示/非表示・
 * 隠れたバーの再表示の作法) を変えないことを、実ウィンドウで確かめる (core/ADR-0040)。
 *
 * Loading の器はフォーカスを取るので、表示中はバーの制御が器のウィンドウへ移る。器が提示先の指定を
 * 引き継いでいなければ、アイコンの明暗が器の既定に変わり、隠れていたバーが再出現する。
 *
 * 器の指定は、この検証が提示先に与えた指定から組み立てた期待値と比べる。提示先のウィンドウから
 * OS が返す値とは比べない (OS が値を返さない指定のしかたがあるため)。
 */
@RunWith(AndroidJUnit4::class)
class LoadingSystemBarsTests {

    @Test
    fun LD_SB_01_明るい地向けの明暗を指定した画面で_Loading_を出しても明暗の指定は変わらない() = runBlocking<Unit> {
        launchHost(Appearance.LIGHT_BARS).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先が明るい地向けの明暗を指定している",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.requestedLightBars(host.window) },
            )
            val harness = LoadingTestHarness(host)
            try {
                harness.loading.show()
                val container = awaitShown(harness)
                val window = requireNotNull(container.window)

                val adopted = readOnMain { SystemBarsObservation.requestedLightBars(window) }
                assertEquals(
                    "Loading の器のステータスバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_STATUS,
                    adopted and LIGHT_STATUS,
                )
                assertEquals(
                    "Loading の器のナビゲーションバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_NAVIGATION,
                    adopted and LIGHT_NAVIGATION,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    assertEquals(
                        "Loading の器でステータスバーの表示状態が提示先と違う",
                        true,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.statusBars()) },
                    )
                    assertEquals(
                        "Loading の器でナビゲーションバーの表示状態が提示先と違う",
                        true,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.navigationBars()) },
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // 提示先は作法を指定していないので、期待値は作法を指定していないウィンドウの OS の既定
                    assertEquals(
                        "Loading の器の作法が OS の既定になっていない",
                        SystemBarsObservation.WINDOW_MANAGER_DEFAULT_BEHAVIOR,
                        SystemBarsObservation.windowManagerBehavior("APPLICATION"),
                    )
                }
            } finally {
                harness.tearDown()
            }
        }
    }

    @Test
    fun LD_SB_02_バーを隠した画面で_Loading_を出してもバーは隠れたまま() = runBlocking<Unit> {
        assumeInsetsControllerPath()
        // バーを隠した提示先では OS の全画面表示の確認ウィンドウが割り込み得るので、この検証の間は止めておく
        ImmersiveModeConfirmation.whileSuppressed {
            launchHost(Appearance.LIGHT_BARS).use { scenario ->
                val host = hostActivity(scenario)
                SystemBarsObservation.hideHostBars(host)
                assertEquals(
                    "前提: 提示先が隠れたバーの再表示の作法を指定している",
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
                    readOnMain { SystemBarsObservation.behavior(host.window) },
                )
                val harness = LoadingTestHarness(host)
                val recorder = SystemBarsObservation.BarVisibilityRecorder(host.window)
                try {
                    val watcher = launch(Dispatchers.Default) {
                        while (isActive) {
                            recorder.sample()
                            delay(SAMPLING_INTERVAL_MILLIS)
                        }
                    }
                    harness.loading.show()
                    val container = awaitShown(harness)
                    // フォーカス (= バーの制御) が器へ移り終えてから、バーの出入りが落ち着くまで見続ける
                    InstrumentedStateSettling.assertWindowFocused(
                        container.layoutHost,
                        "前提: Loading の器が入力の宛先になっている",
                    )
                    delay(BARS_ANIMATION_ALLOWANCE_MILLIS)
                    watcher.cancelAndJoin()
                    recorder.sample()

                    assertFalse(
                        "Loading の表示中に提示先のバーが再出現した\n観測履歴:\n${recorder.history.format()}",
                        recorder.sawVisibleBar,
                    )
                    val window = requireNotNull(container.window)
                    assertEquals(
                        "Loading の器のウィンドウ側でステータスバーが表示されている",
                        false,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.statusBars()) },
                    )
                    assertEquals(
                        "Loading の器のウィンドウ側でナビゲーションバーが表示されている",
                        false,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.navigationBars()) },
                    )
                    assertEquals(
                        "Loading の器の作法が提示先の指定と違う",
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
                        readOnMain { SystemBarsObservation.behavior(window) },
                    )
                } finally {
                    harness.tearDown()
                }
            }
        }
    }

    /**
     * 画面の作り直しで器を載せ替えるとき、新しい器は作り直し後の画面から指定を読み直す。
     * 作り直しの前後で提示先に異なる明暗を与え、載せ替え後の器が作り直し後の指定を採ったことを見る
     * (前の器の値を持ち越しただけでは通らないように)。
     */
    @Test
    fun LD_SB_01_載せ替えた器は作り直し後の画面から明暗を引き継ぐ() = runBlocking<Unit> {
        assumeInsetsControllerPath()
        launchHost(Appearance.UNSPECIFIED).use { scenario ->
            val surface = LoadingTestPresentationSurface(hostActivity(scenario))
            val harness = LoadingTestHarness(surface)
            try {
                harness.loading.show()
                val before = awaitShown(harness)
                assertEquals(
                    "前提: 作り直し前の提示先は明暗を指定しておらず、器も明るい地向けでない",
                    0,
                    readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(before.window)) },
                )

                scenario.recreate()
                val recreated = hostActivity(scenario)
                // 作り直し後の画面だけに明るい地向けの明暗を指定してから、提示先の入れ替わりを伝える
                withContext(Dispatchers.Main) {
                    recreated.window.insetsController?.setSystemBarsAppearance(LIGHT_BARS, LIGHT_BARS)
                    surface.changeHost(recreated)
                }
                assertTrue(
                    "作り直し後の画面へ器が載せ替えられない",
                    InstrumentedDialogWaiting.waitUntil {
                        val current = harness.container
                        current != null && current !== before &&
                            current.window?.decorView?.isAttachedToWindow == true
                    },
                )
                val after = requireNotNull(harness.container)
                assertNotSame("器は作り直される", before, after)
                assertEquals(
                    "載せ替えた器が作り直し後の画面の明暗を採っていない",
                    LIGHT_BARS,
                    readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(after.window)) },
                )
            } finally {
                harness.tearDown()
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

    /** 器が画面に載り、入りの演出を終えて表示状態になるまで待つ。 */
    private suspend fun awaitShown(harness: LoadingTestHarness): LoadingContainer {
        assertTrue(
            "Loading の器が表示状態にならなかった",
            InstrumentedDialogWaiting.waitUntil {
                harness.container?.containerState == DialogContainerState.SHOWN
            },
        )
        return requireNotNull(harness.container)
    }

    private fun assumeInsetsControllerPath() {
        assumeTrue(
            "WindowInsetsController の経路は Android 11 以上にしかない",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    private companion object {
        const val LIGHT_BARS: Int = SystemBarsTestActivity.LIGHT_BARS_APPEARANCE
        const val LIGHT_STATUS: Int = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        const val LIGHT_NAVIGATION: Int = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS

        /** 提示先のバーの見えを読む間隔 (ミリ秒)。 */
        const val SAMPLING_INTERVAL_MILLIS = 16L

        /** フォーカスが移ったあと、バーの出入りの演出が起きれば終わるまでの見届けの時間 (ミリ秒)。 */
        const val BARS_ANIMATION_ALLOWANCE_MILLIS = 1_000L
    }
}
