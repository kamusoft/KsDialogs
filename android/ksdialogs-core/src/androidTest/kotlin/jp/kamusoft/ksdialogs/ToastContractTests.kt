package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.ConfigurableToastTestViewModel
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionGate
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestPresentationSurface
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import jp.kamusoft.ksdialogs.support.UnregisteredToastTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Toast の公開面と fire-and-forget の契約 (core/ADR-0028・0029・0031) を確かめる。
 * iOS Native の ToastContractTests のミラー。
 *
 * show は戻り値を持たず、消滅の契機は duration の経過だけである。
 * 構成ミス (未登録の ViewModel 型) は受理そのものの失敗として呼び出し元へ返り、
 * 受理より後の失敗は表示1枚の破棄に留まる。
 *
 * 表示時間は、背面で受理した表示なら受理の時点から、前面にいるのに提示先が無い間 (前面の待ち) に
 * 受理した表示なら提示先に載った時点か背面へ下がった時点から数える (core/ADR-0043)。
 */
@RunWith(AndroidJUnit4::class)
class ToastContractTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_CO_01_show_で表示され_duration_経過で自動的に消える() = runBlocking<Unit> {
        val harness = newHarness()

        harness.toast.show("保存しました", durationMs = SHORT_DURATION_MILLIS)

        assertTrue(harness.waitUntilPresenting())
        assertEquals("保存しました", harness.defaultContentViews.single().displayedText)
        assertTrue("duration の経過で自動的に消える", harness.waitUntilEmpty())
        assertFalse(harness.isPresenting)
    }

    @Test
    fun TS_CO_02_duration_省略時は_ToastStyle_の既定が使われる() = runBlocking<Unit> {
        val harness = newHarness()

        // 内蔵既定より十分に長い既定 duration を設定すると、省略した表示はその長さまで残る
        harness.toast.style = ToastStyle(defaultDuration = LONG_DURATION_MILLIS)
        harness.toast.show("長い既定")
        assertTrue(harness.waitUntilPresenting())
        delay(BUILTIN_DEFAULT_OVERSHOOT_MILLIS)
        assertTrue("style の既定 duration が使われていない", harness.isPresenting)

        // 短い既定に変えると、次の表示はその長さで消える
        harness.toast.style = ToastStyle(defaultDuration = SHORT_DURATION_MILLIS)
        harness.toast.show("短い既定")
        assertTrue(InstrumentedDialogWaiting.waitUntil { harness.displayCount == 2 })
        assertTrue(
            "短い既定の表示だけが先に消える",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertEquals("長い既定", harness.defaultContentViews.single().displayedText)

        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_CO_03_0_以下の_duration_は_style_の既定に丸められる() = runBlocking<Unit> {
        val style = ToastStyle(defaultDuration = SHORT_DURATION_MILLIS)
        assertEquals(SHORT_DURATION_MILLIS, ToastCoordinator.effectiveDuration(0, style))
        assertEquals(SHORT_DURATION_MILLIS, ToastCoordinator.effectiveDuration(-1, style))
        assertEquals(
            "style の既定自体が 0 以下なら内蔵既定へ丸める",
            ToastStyle.BUILTIN_DEFAULT_DURATION,
            ToastCoordinator.effectiveDuration(0, ToastStyle(defaultDuration = 0)),
        )

        val harness = newHarness()
        harness.toast.style = style
        harness.toast.show("丸められる", durationMs = 0)

        assertTrue("即時消滅にならず表示される", harness.waitUntilPresenting())
        assertTrue("永続表示にならず既定 duration で消える", harness.waitUntilEmpty())
    }

    @Test
    fun TS_CO_04_未登録の_ViewModel_型は_fail_fast() = runBlocking<Unit> {
        val harness = newHarness()

        val failure = runCatching {
            harness.toast.show(UnregisteredToastTestViewModel())
        }.exceptionOrNull()

        assertTrue(
            "未登録の型は構成ミスとして失敗する (実際: $failure)",
            failure is DialogException.ViewFactoryNotRegistered,
        )
        assertEquals("表示は行われない", 0, harness.displayCount)
    }

    @Test
    fun TS_CO_05_インライン経路はレジストリの状態を変えない() = runBlocking<Unit> {
        val harness = newHarness()
        val registeredCalls = AtomicInteger(0)
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            registeredCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }
        val registeredFactory = harness.registry.factory(ToastTestViewModel::class)
        val inlineCalls = AtomicInteger(0)

        harness.toast.show(
            ToastTestViewModel("インライン"),
            durationMs = SHORT_DURATION_MILLIS,
        ) { viewModel ->
            inlineCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }

        assertTrue(harness.waitUntilPresenting())
        assertEquals("インラインの factory が使われる", 1, inlineCalls.get())
        assertEquals("登録 factory は使われない", 0, registeredCalls.get())
        assertSame(
            "レジストリの登録内容は変わらない",
            registeredFactory,
            harness.registry.factory(ToastTestViewModel::class),
        )

        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_CO_06_異なる入口が同じレジストリと_style_を共有する() = runBlocking<Unit> {
        val harness = newHarness()
        val anotherEntry: KsToast = Toast(harness.coordinator)

        harness.toast.style = ToastStyle(defaultDuration = SHORT_DURATION_MILLIS)
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            newTextContent(viewModel.message)
        }

        assertSame("レジストリは共有される", harness.registry, anotherEntry.registry)
        assertEquals("style も共有される", harness.toast.style, anotherEntry.style)

        anotherEntry.show(ToastTestViewModel("別の入口"))
        assertTrue("別の入口の表示も同じ状態の正に載る", harness.waitUntilPresenting())
        assertTrue(
            "共有した style の既定 duration で消える",
            harness.waitUntilEmpty(),
        )
    }

    @Test
    fun TS_CO_07_受理後の_factory_失敗は破棄と資源解放() = runBlocking<Unit> {
        val harness = newHarness()

        harness.toast.show(ToastTestViewModel("失敗"), durationMs = LONG_DURATION_MILLIS) { _ ->
            throw IllegalStateException("中身を作れない")
        }

        assertTrue(
            "呼び出しは失敗せず、その表示だけが破棄される",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 0 },
        )
        assertFalse(harness.isPresenting)

        harness.toast.show("後続", durationMs = SHORT_DURATION_MILLIS)
        assertTrue("後続の show は正常に表示される", harness.waitUntilPresenting())
        assertEquals("後続", harness.defaultContentViews.single().displayedText)
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_CO_08_計時は受理時点から進み入りの途中でも_duration_到達で出へ移る() = runBlocking<Unit> {
        // 提示先がある状態で受理するので、計時は受理の時点から進む
        val harness = newHarness()
        val probe = DialogTransitionProbe()
        val presentationGate = DialogTransitionGate()
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            newTextContent(viewModel.message).apply {
                ksDialogTransition = DialogTransition(
                    presentation = probe.gatedHook(
                        DialogTransitionProbe.Phase.PRESENTATION,
                        presentationGate,
                    ),
                    dismissal = probe.immediateHook(DialogTransitionProbe.Phase.DISMISSAL),
                )
            }
        }

        harness.toast.show(ToastTestViewModel("入りの途中"), durationMs = SHORT_DURATION_MILLIS)

        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                probe.callCount(DialogTransitionProbe.Phase.PRESENTATION) == 1
            },
        )
        assertFalse(
            "入りのフックはまだ完了していない",
            probe.hasEvent(
                DialogTransitionProbe.Event.Finished(DialogTransitionProbe.Phase.PRESENTATION),
            ),
        )
        assertTrue(
            "入りの完了を待たずに出へ移る",
            InstrumentedDialogWaiting.waitUntil {
                probe.callCount(DialogTransitionProbe.Phase.DISMISSAL) == 1
            },
        )
        assertTrue(
            "出のフックの完了後に撤去される (表示が duration を大きく超えて残らない)",
            harness.waitUntilEmpty(),
        )
        presentationGate.open()
    }

    @Test
    fun TS_HW_01_背面で受理された_Toast_は_提示先が現れた時点で表示され_受理時点から数えた_duration_で消える() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null, isAppInForeground = false)
        val harness = newHarness(surface)
        val viewFactoryCalls = AtomicInteger(0)
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            viewFactoryCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }

        val acceptedAt = SystemClock.uptimeMillis()
        harness.toast.show(ToastTestViewModel("提示先待ち"), durationMs = HOST_WAIT_DURATION_MILLIS)

        assertTrue(
            "提示先が無くても受理は成立する",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertFalse("まだ器は取り付かない", harness.isPresenting)
        assertEquals("提示先が無い間は View factory が呼ばれない", 0, viewFactoryCalls.get())

        delay(HOST_APPEARANCE_DELAY_MILLIS)
        val activity = currentActivity()
        withContext(Dispatchers.Main) { surface.changeHost(activity) }

        assertTrue("提示先の出現で表示される", harness.waitUntilPresenting())
        assertEquals("提示先が現れた時点で中身が作られる", 1, viewFactoryCalls.get())
        assertTrue(harness.waitUntilEmpty())
        val elapsed = SystemClock.uptimeMillis() - acceptedAt
        assertTrue(
            "受理時点から数えた duration の到達で消える (経過 $elapsed ms)",
            elapsed < HOST_WAIT_DURATION_MILLIS + HOST_APPEARANCE_DELAY_MILLIS,
        )
    }

    @Test
    fun TS_HW_02_提示先が現れないまま満了した_Toast_は_中身が作られずに破棄される() = runBlocking<Unit> {
        // 背面で受理するので、提示先が無い間も期限へ向けて時間が進む
        val surface = ToastTestPresentationSurface(null, isAppInForeground = false)
        val harness = newHarness(surface)
        val viewModelFactoryCalls = AtomicInteger(0)
        val configureCalls = AtomicInteger(0)
        val viewFactoryCalls = AtomicInteger(0)
        harness.registry.registerViewModel(ConfigurableToastTestViewModel::class) {
            viewModelFactoryCalls.incrementAndGet()
            ConfigurableToastTestViewModel()
        }
        harness.registry.register(ConfigurableToastTestViewModel::class) { viewModel ->
            viewFactoryCalls.incrementAndGet()
            newTextContent(viewModel.title)
        }

        harness.toast.show(ConfigurableToastTestViewModel::class, durationMs = SHORT_DURATION_MILLIS) { viewModel ->
            configureCalls.incrementAndGet()
            viewModel.title = "届かない"
        }

        assertTrue(
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertTrue("期限の満了で表示リストから外れる", harness.waitUntilEmpty())
        assertFalse("一度も表示されない", harness.isPresenting)
        assertEquals("VM factory は呼ばれない", 0, viewModelFactoryCalls.get())
        assertEquals("configure は呼ばれない", 0, configureCalls.get())
        assertEquals("View factory は呼ばれない", 0, viewFactoryCalls.get())
    }

    @Test
    fun TS_HW_03_期限を過ぎた保留表示は_提示先の出現が期限の処理より先でも表示されない() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null, isAppInForeground = false)
        val harness = newHarness(surface)
        val activity = currentActivity()
        val viewFactoryCalls = AtomicInteger(0)
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            viewFactoryCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }

        harness.toast.show(ToastTestViewModel("期限切れ"), durationMs = SHORT_DURATION_MILLIS)
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )

        // 期限の到達と提示先の復帰の間に期限のタイマーを走らせない状況を作る。
        // UI スレッドを占有したまま期限を越え、そのまま提示先を戻すことで、
        // 復帰の処理がタイマーより先に走る順序を再現する
        withContext(Dispatchers.Main) {
            Thread.sleep(SHORT_DURATION_MILLIS + DEADLINE_OVERSHOOT_MILLIS)
            surface.changeHost(activity)
            assertFalse("期限に達した表示は器を取り付けない", harness.isPresenting)
        }

        assertTrue("その場で破棄される", harness.waitUntilEmpty())
        assertEquals("中身は作られない", 0, viewFactoryCalls.get())
        assertTrue(
            "表示されない Toast の文言が読み上げへ流れている",
            harness.announcer.announcedMessages.isEmpty(),
        )
    }

    @Test
    fun TS_HW_04_前面の待ちの間に受理された_Toast_は_duration_より長く待っても破棄されず_載った時点から数えた_duration_で消える() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null, isAppInForeground = true)
        val harness = newHarness(surface)
        val viewFactoryCalls = AtomicInteger(0)
        harness.registerImmediateContent(viewFactoryCalls)

        harness.toast.show(ToastTestViewModel("前面の待ち"), durationMs = FOREGROUND_WAIT_DURATION_MILLIS)
        assertTrue(
            "提示先が無くても受理は成立する",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )

        // duration より長く提示先が無いまま置く
        delay(FOREGROUND_WAIT_DURATION_MILLIS + OVER_DURATION_WAIT_MILLIS)
        assertEquals("duration より長く待っても破棄されない", 1, harness.displayCount)
        assertFalse("まだ器は取り付かない", harness.isPresenting)
        assertEquals("待っている間は View factory が呼ばれない", 0, viewFactoryCalls.get())

        val activity = currentActivity()
        val appearedAt = SystemClock.uptimeMillis()
        withContext(Dispatchers.Main) { surface.changeHost(activity) }

        assertTrue("提示先の出現で表示される", harness.waitUntilPresenting())
        assertEquals("提示先が現れた時点で中身が作られる", 1, viewFactoryCalls.get())
        assertTrue(harness.waitUntilEmpty())
        val elapsed = SystemClock.uptimeMillis() - appearedAt
        assertTrue(
            "載った時点から数えた duration の到達で消える (載ってから $elapsed ms)",
            elapsed >= FOREGROUND_WAIT_DURATION_MILLIS &&
                elapsed < FOREGROUND_WAIT_DURATION_MILLIS + DEADLINE_ALLOWANCE_MILLIS,
        )
    }

    @Test
    fun TS_HW_05_前面の待ちのまま背面へ下がった_Toast_は_下がった時点から数え始める() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null, isAppInForeground = true)
        val harness = newHarness(surface)
        val viewFactoryCalls = AtomicInteger(0)
        harness.registerImmediateContent(viewFactoryCalls)

        harness.toast.show(ToastTestViewModel("背面へ"), durationMs = FOREGROUND_WAIT_DURATION_MILLIS)
        assertTrue(InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 })

        delay(FOREGROUND_WAIT_DURATION_MILLIS + OVER_DURATION_WAIT_MILLIS)
        assertEquals("背面へ下がるまでは破棄されない", 1, harness.displayCount)

        val leftAt = SystemClock.uptimeMillis()
        withContext(Dispatchers.Main) { surface.leaveForeground() }
        assertEquals("下がった直後はまだ破棄されない", 1, harness.displayCount)

        assertTrue(harness.waitUntilEmpty())
        val elapsed = SystemClock.uptimeMillis() - leftAt
        assertTrue(
            "下がった時点から数えた duration の到達で破棄される (下がってから $elapsed ms)",
            elapsed >= FOREGROUND_WAIT_DURATION_MILLIS &&
                elapsed < FOREGROUND_WAIT_DURATION_MILLIS + DEADLINE_ALLOWANCE_MILLIS,
        )
        assertFalse("一度も表示されない", harness.isPresenting)
        assertEquals("中身は一度も作られない", 0, viewFactoryCalls.get())
    }

    @Test
    fun TS_HW_06_背面へ下がった後_期限の前に提示先が現れたら_下がった時点から数えた期限まで表示される() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null, isAppInForeground = true)
        val harness = newHarness(surface)
        val viewFactoryCalls = AtomicInteger(0)
        harness.registerImmediateContent(viewFactoryCalls)

        harness.toast.show(ToastTestViewModel("背面から戻る"), durationMs = FOREGROUND_WAIT_DURATION_MILLIS)
        assertTrue(InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 })
        delay(FOREGROUND_WAIT_DURATION_MILLIS + OVER_DURATION_WAIT_MILLIS)

        val leftAt = SystemClock.uptimeMillis()
        withContext(Dispatchers.Main) { surface.leaveForeground() }

        // 下がってから duration に達する前に、前面へ戻って提示先が現れる
        delay(BACKGROUND_STAY_MILLIS)
        val activity = currentActivity()
        withContext(Dispatchers.Main) { surface.changeHost(activity) }

        assertTrue("提示先が現れた時点で表示される", harness.waitUntilPresenting())
        assertEquals("提示先が現れた時点で中身が作られる", 1, viewFactoryCalls.get())
        assertTrue(harness.waitUntilEmpty())
        val elapsed = SystemClock.uptimeMillis() - leftAt
        // 載った時点から数え直していれば、下がってから BACKGROUND_STAY_MILLIS + duration より後に消える
        assertTrue(
            "背面へ下がった時点から数えた duration の到達で消える (下がってから $elapsed ms)",
            elapsed >= FOREGROUND_WAIT_DURATION_MILLIS &&
                elapsed < FOREGROUND_WAIT_DURATION_MILLIS + BACKGROUND_STAY_MILLIS,
        )
    }

    @Test
    fun 提示先を待っている間に_View_factory_を登録し直しても_受理の時点の登録で表示される() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null)
        val harness = newHarness(surface)
        val acceptedFactoryCalls = AtomicInteger(0)
        val reregisteredFactoryCalls = AtomicInteger(0)
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            acceptedFactoryCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }

        harness.toast.show(ToastTestViewModel("受理の時点の登録"), durationMs = HOST_WAIT_DURATION_MILLIS)
        assertTrue(
            "提示先が無いので表示は保留される",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )

        // 受理の後、提示先が現れる前に同じ ViewModel 型の登録を置き換える
        harness.registry.register(ToastTestViewModel::class) { viewModel ->
            reregisteredFactoryCalls.incrementAndGet()
            newTextContent(viewModel.message)
        }
        val activity = currentActivity()
        withContext(Dispatchers.Main) { surface.changeHost(activity) }

        assertTrue("提示先の出現で表示される", harness.waitUntilPresenting())
        assertEquals("受理の時点で登録されていた factory で中身が作られる", 1, acceptedFactoryCalls.get())
        assertEquals("受理の後に登録し直した factory は使われない", 0, reregisteredFactoryCalls.get())
        assertTrue(harness.waitUntilEmpty())
    }

    /**
     * 出入りの演出を待たずに進む中身を登録し、View factory の呼び出し回数を数える。
     *
     * 撤去が期限の到達の直後に終わるので、表示リストが空になった時点を期限の到達として読める。
     */
    private fun ToastTestHarness.registerImmediateContent(viewFactoryCalls: AtomicInteger) {
        registry.register(ToastTestViewModel::class) { viewModel ->
            viewFactoryCalls.incrementAndGet()
            newTextContent(viewModel.message).apply {
                ksDialogTransition = DialogTransition(presentation = {}, dismissal = {})
            }
        }
    }

    /** 表示テキストを持つカスタム Toast の中身。 */
    private fun Context.newTextContent(message: String): View =
        TextView(this).apply { text = message }

    private fun newHarness(): ToastTestHarness = ToastTestHarness(currentActivity())

    private fun newHarness(surface: ToastTestPresentationSurface): ToastTestHarness =
        ToastTestHarness(surface)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 待ち時間を短く保つための表示時間 (ミリ秒)。 */
        const val SHORT_DURATION_MILLIS = 400

        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 6_000

        /** 内蔵既定 (1500) を明らかに超えたと言える待ち時間 (ミリ秒)。 */
        const val BUILTIN_DEFAULT_OVERSHOOT_MILLIS = 2_200L

        /** 提示先の出現を待たせたうえで表示を確かめる表示時間 (ミリ秒)。 */
        const val HOST_WAIT_DURATION_MILLIS = 3_000

        /** 受理から提示先を出現させるまでの待ち時間 (ミリ秒)。 */
        const val HOST_APPEARANCE_DELAY_MILLIS = 1_000L

        /** 期限を確実に越えたと言える超過分 (ミリ秒)。 */
        const val DEADLINE_OVERSHOOT_MILLIS = 200L

        /** 前面の待ちを確かめる表示時間 (ミリ秒)。 */
        const val FOREGROUND_WAIT_DURATION_MILLIS = 1_500

        /** 前面の待ちで、duration を明らかに超えたと言えるまで余分に置く時間 (ミリ秒)。 */
        const val OVER_DURATION_WAIT_MILLIS = 1_000L

        /** 背面へ下がってから提示先を出すまでの時間 (ミリ秒)。duration より短い。 */
        const val BACKGROUND_STAY_MILLIS = 1_000L

        /** 期限の到達から表示リストが空になるまでの遅れとして許す幅 (ミリ秒)。 */
        const val DEADLINE_ALLOWANCE_MILLIS = 1_000L
    }
}
