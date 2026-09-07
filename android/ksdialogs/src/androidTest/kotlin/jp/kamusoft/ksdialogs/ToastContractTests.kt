package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun 提示先が無い間は表示を保留し現れたら表示する() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null)
        val harness = newHarness(surface)

        harness.toast.show("提示先待ち", durationMs = LONG_DURATION_MILLIS)

        assertTrue(
            "提示先が無くても受理は成立する",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertFalse("まだ器は取り付かない", harness.isPresenting)

        withContext(Dispatchers.Main) { surface.changeHost(currentActivity()) }

        assertTrue("提示先の出現で表示される", harness.waitUntilPresenting())
        assertEquals("提示先待ち", harness.defaultContentViews.single().displayedText)
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun 提示先が現れないまま期限が来た表示は表示されずに破棄される() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null)
        val harness = newHarness(surface)

        harness.toast.show("届かない", durationMs = SHORT_DURATION_MILLIS)

        assertTrue(
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertTrue("期限の満了で破棄される", harness.waitUntilEmpty())
        assertFalse("一度も表示されない", harness.isPresenting)
    }

    @Test
    fun 期限を過ぎた保留表示は提示先の復帰がタイマーより先でも表示されない() = runBlocking<Unit> {
        val surface = ToastTestPresentationSurface(null)
        val harness = newHarness(surface)
        val activity = currentActivity()

        harness.toast.show("期限切れ", durationMs = SHORT_DURATION_MILLIS)
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
        assertTrue(
            "表示されない Toast の文言が読み上げへ流れている",
            harness.announcer.announcedMessages.isEmpty(),
        )
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

        /** 期限を確実に越えたと言える超過分 (ミリ秒)。 */
        const val DEADLINE_OVERSHOOT_MILLIS = 200L
    }
}
