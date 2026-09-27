package jp.kamusoft.ksdialogs

import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.ImmersiveModeConfirmation
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.SystemBarsObservation
import jp.kamusoft.ksdialogs.support.SystemBarsObservation.readOnMain
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity
import jp.kamusoft.ksdialogs.support.SystemBarsTestActivity.Appearance
import jp.kamusoft.ksdialogs.support.ToastTestHarness
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
 * Toast の器が、提示先の画面のシステムバーの指定 (アイコンの明暗・バーの表示/非表示・
 * 隠れたバーの再表示の作法) を変えないことを、実ウィンドウで確かめる (core/ADR-0040)。
 *
 * Toast の器はフォーカスを取らないので、バーの表示/非表示の制御は提示先に残る。一方で器は全画面の
 * ウィンドウなので、明暗は器のウィンドウの値がステータスバーに使われ得る。器が可視状態を引き継いでも、
 * 提示先のバーの状態が変わらないこと (写しても無害であること) もあわせて確かめる。
 *
 * 器の指定は、この検証が提示先に与えた指定から組み立てた期待値と比べる。提示先のウィンドウから
 * OS が返す値とは比べない (OS が値を返さない指定のしかたがあるため)。
 */
@RunWith(AndroidJUnit4::class)
class ToastSystemBarsTests {

    @Test
    fun TS_SB_01_明るい地向けの明暗を指定した画面で_Toast_を出しても明暗の指定は変わらない() = runBlocking<Unit> {
        launchHost(Appearance.LIGHT_BARS).use { scenario ->
            val host = hostActivity(scenario)
            assertEquals(
                "前提: 提示先が明るい地向けの明暗を指定している",
                LIGHT_BARS,
                readOnMain { SystemBarsObservation.requestedLightBars(host.window) },
            )
            val harness = ToastTestHarness(host)
            try {
                harness.toast.show("明暗の確認", durationMs = DURATION_MILLIS)
                val container = awaitShown(harness)
                val window = requireNotNull(container.window)

                val adopted = readOnMain { SystemBarsObservation.requestedLightBars(window) }
                assertStillShown(harness, container)
                assertEquals(
                    "Toast の器のステータスバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_STATUS,
                    adopted and LIGHT_STATUS,
                )
                assertEquals(
                    "Toast の器のナビゲーションバーの明暗が提示先の指定 (明るい地向け) と違う",
                    LIGHT_NAVIGATION,
                    adopted and LIGHT_NAVIGATION,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    assertEquals(
                        "Toast の器でステータスバーの表示状態が提示先と違う",
                        true,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.statusBars()) },
                    )
                    assertEquals(
                        "Toast の器でナビゲーションバーの表示状態が提示先と違う",
                        true,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.navigationBars()) },
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // 提示先は作法を指定していないので、期待値は作法を指定していないウィンドウの OS の既定
                    assertEquals(
                        "Toast の器の作法が OS の既定になっていない",
                        SystemBarsObservation.WINDOW_MANAGER_DEFAULT_BEHAVIOR,
                        SystemBarsObservation.windowManagerBehavior("APPLICATION"),
                    )
                }
                assertStillShown(harness, container)
            } finally {
                withdraw(harness)
            }
        }
    }

    @Test
    fun TS_SB_02_バーを隠した画面で_Toast_を出してもバーは隠れたまま() = runBlocking<Unit> {
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
                val harness = ToastTestHarness(host)
                try {
                    val recorder = SystemBarsObservation.BarVisibilityRecorder(host.window)
                    val watcher = launch(Dispatchers.Default) {
                        while (isActive) {
                            recorder.sample()
                            delay(SAMPLING_INTERVAL_MILLIS)
                        }
                    }
                    harness.toast.show("バーの確認", durationMs = DURATION_MILLIS)
                    val container = awaitShown(harness)
                    // 器が取り付いたあと、バーの出入りの演出が起きれば終わるまで見続ける
                    delay(BARS_ANIMATION_ALLOWANCE_MILLIS)
                    watcher.cancelAndJoin()
                    recorder.sample()
                    // 観測の始めから終わりまで同じ器が表示状態のままだったことを確かめる。
                    // 期限切れで消えた回を、バーの退行と取り違えないため
                    assertStillShown(harness, container)

                    assertFalse(
                        "Toast の表示中に提示先のバーが再出現した\n観測履歴:\n${recorder.history.format()}",
                        recorder.sawVisibleBar,
                    )
                    val window = requireNotNull(container.window)
                    assertEquals(
                        "Toast の器のウィンドウ側でステータスバーが表示されている",
                        false,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.statusBars()) },
                    )
                    assertEquals(
                        "Toast の器のウィンドウ側でナビゲーションバーが表示されている",
                        false,
                        readOnMain { SystemBarsObservation.isBarVisible(window, WindowInsets.Type.navigationBars()) },
                    )
                    assertEquals(
                        "Toast の器の作法が提示先の指定と違う",
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
                        readOnMain { SystemBarsObservation.behavior(window) },
                    )
                } finally {
                    withdraw(harness)
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
    fun TS_SB_01_載せ替えた器は作り直し後の画面から明暗を引き継ぐ() = runBlocking<Unit> {
        assumeInsetsControllerPath()
        launchHost(Appearance.UNSPECIFIED).use { scenario ->
            val harness = ToastTestHarness(hostActivity(scenario))
            try {
                harness.toast.show("載せ替えの確認", durationMs = DURATION_MILLIS)
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
                    harness.changeHost(recreated)
                }
                assertTrue(
                    "作り直し後の画面へ器が載せ替えられない",
                    InstrumentedDialogWaiting.waitUntil(REATTACH_TIMEOUT_MILLIS) {
                        val current = harness.containers.singleOrNull()
                        current != null && current !== before &&
                            current.window?.decorView?.isAttachedToWindow == true
                    },
                )
                val after = harness.containers.single()
                assertNotSame("器は作り直される", before, after)
                val adopted = readOnMain { SystemBarsObservation.requestedLightBars(requireNotNull(after.window)) }
                assertStillShown(harness, after)
                assertEquals(
                    "載せ替えた器が作り直し後の画面の明暗を採っていない",
                    LIGHT_BARS,
                    adopted,
                )
            } finally {
                withdraw(harness)
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

    /** 器が1枚だけ画面に載り、入りの演出を終えて表示状態になるまで待つ。 */
    private suspend fun awaitShown(harness: ToastTestHarness): ToastContainer {
        assertTrue(
            "Toast の器が表示状態にならなかった",
            InstrumentedDialogWaiting.waitUntil(SHOW_TIMEOUT_MILLIS) {
                harness.containers.singleOrNull()?.containerState == DialogContainerState.SHOWN
            },
        )
        return harness.containers.single()
    }

    /**
     * 判定の時点でも、その器が表示状態のまま載っていることを確かめる。
     *
     * 表示期限は観測を終えるまで切れない長さにしてあるが、それでも消えていたら、システムバーの退行ではなく
     * 期限切れ (待ちの長引き) として区別できる説明で落とす。
     */
    private fun assertStillShown(harness: ToastTestHarness, container: ToastContainer) {
        assertTrue(
            "前提: 判定の時点で Toast が表示中でない (期限切れか取り外し。システムバーの判定は行っていない)",
            harness.containers.singleOrNull() === container &&
                container.containerState == DialogContainerState.SHOWN,
        )
    }

    /**
     * 表示中の Toast を器・表示・計時ごと捨て、何も残っていないことを確かめる。
     * 期限を長くとっているので、放っておくと後続の検証の間も表示が終わった画面を握り続けるため。
     */
    private suspend fun withdraw(harness: ToastTestHarness) {
        withContext(Dispatchers.Main) { harness.coordinator.discardAll() }
        assertEquals(
            "後始末: Toast の表示が残っている",
            0,
            withContext(Dispatchers.Main) { harness.displayCount },
        )
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

        /** 器が取り付いたあと、バーの出入りの演出が起きれば終わるまでの見届けの時間 (ミリ秒)。 */
        const val BARS_ANIMATION_ALLOWANCE_MILLIS = 1_000L

        /** 器が表示状態になるのを待つ上限 (ミリ秒)。 */
        const val SHOW_TIMEOUT_MILLIS = 10_000L

        /** 画面の作り直しのあと、器が載せ替えられるのを待つ上限 (ミリ秒)。 */
        const val REATTACH_TIMEOUT_MILLIS = 10_000L

        /** 画面の作り直し (recreate) そのものにかかり得る時間の見込み (ミリ秒)。 */
        const val RECREATE_ALLOWANCE_MILLIS = 10_000L

        /**
         * 表示時間 (ミリ秒)。期限は受理の時点から数えるので、受理から判定までに積み上がり得る待ちの合計
         * (表示待ちの上限 + 作り直しの見込み + 載せ替え待ちの上限 + バーの観測) より長くとる。
         * 負荷で待ちが長引いても、判定の前に正常な Toast が期限で消えないように。
         */
        const val DURATION_MILLIS: Int = (
            2 * (
                SHOW_TIMEOUT_MILLIS + RECREATE_ALLOWANCE_MILLIS + REATTACH_TIMEOUT_MILLIS +
                    BARS_ANIMATION_ALLOWANCE_MILLIS
                )
            ).toInt()

        /** 提示先のバーの見えを読む間隔 (ミリ秒)。 */
        const val SAMPLING_INTERVAL_MILLIS = 16L
    }
}
