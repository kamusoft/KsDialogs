package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.ConfigurableToastTestViewModel
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.ToastLayoutObservation
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTypedShowRecorder
import jp.kamusoft.ksdialogs.support.ValueClassToastTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * ViewModel の型を渡して表示する Toast の経路 (core/ADR-0035) を確かめる。
 * iOS Native の ToastTypedShowTests のミラー。
 *
 * レジストリは View factory と ViewModel factory の2スロットを持つ。
 * ViewModel factory 未登録だけが呼び出し時点の同期失敗で、
 * ViewModel factory と configure の失敗は受理後の失敗 (警告 + その1枚の破棄) になる。
 */
@RunWith(AndroidJUnit4::class)
class ToastTypedShowTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    /** configure と ViewModel factory が投げる、テスト専用の失敗。 */
    private class TypedShowFailure(message: String) : RuntimeException(message)

    // MARK: - レジストリの2スロット

    @Test
    fun TS_TY_01_VM_factory_の再登録は_View_factory_を保持する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerBothSlots(harness, recorder, factoryTitle = "最初の VM")

        // ViewModel factory だけを登録し直す。View factory のスロットは触らない
        harness.registry.registerViewModel(ConfigurableToastTestViewModel::class) {
            ConfigurableToastTestViewModel("後の VM").also(recorder::recordCreation)
        }

        harness.toast.show(ConfigurableToastTestViewModel::class, durationMs = SHORT_DURATION_MILLIS)

        assertTrue(harness.waitUntilPresenting())
        assertEquals("新しい VM factory の生成物が使われる", listOf("後の VM"), recorder.observedTitles)
        assertSame(recorder.lastView, harness.contentViews.single())
        assertTrue(harness.waitUntilEmpty())

        // 逆向き。View factory だけを登録し直しても ViewModel factory は残る
        val secondRecorder = ToastTypedShowRecorder()
        harness.registry.register(ConfigurableToastTestViewModel::class) { viewModel ->
            newTextContent(viewModel.title).also { secondRecorder.recordView(viewModel.title, it) }
        }

        harness.toast.show(ConfigurableToastTestViewModel::class, durationMs = SHORT_DURATION_MILLIS)

        assertTrue(harness.waitUntilPresenting())
        assertEquals("ViewModel factory は保持されている", listOf("後の VM"), secondRecorder.observedTitles)
        assertTrue(harness.waitUntilEmpty())
    }

    // MARK: - 型を渡す表示

    @Test
    fun TS_TY_02_型指定_show_が生成から_configure_表示の一連で動く() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerBothSlots(harness, recorder)

        harness.toast.show(
            ConfigurableToastTestViewModel::class,
            durationMs = SHORT_DURATION_MILLIS,
        ) { viewModel ->
            viewModel.title = "configure で設定"
        }

        assertTrue(harness.waitUntilPresenting())
        assertEquals("configure の設定が中身の生成から読める", listOf("configure で設定"), recorder.observedTitles)
        assertSame(recorder.lastView, harness.contentViews.single())
        assertTrue("duration の経過で消える", harness.waitUntilEmpty())
    }

    @Test
    fun TS_TY_03_VM_factory_未登録の型指定_show_は呼び出し時点で失敗する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerViewFactory(harness, recorder)

        val failure = runCatching {
            harness.toast.show(ConfigurableToastTestViewModel::class)
        }.exceptionOrNull()

        assertTrue(
            "ViewModel factory 未登録の構成ミスとして同期に失敗する (実際: $failure)",
            failure is DialogException.ViewModelFactoryNotRegistered,
        )
        assertEquals("表示は行われない", 0, harness.displayCount)
        assertEquals(0, recorder.viewCreationCount)
    }

    @Test
    fun TS_TY_04_configure_の失敗は受理後の失敗としてその1枚だけを破棄する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerBothSlots(harness, recorder)

        // 別の Toast を表示中にしておき、巻き添えにならないことを見る
        harness.toast.show("別の表示", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())

        harness.toast.show(
            ConfigurableToastTestViewModel::class,
            durationMs = LONG_DURATION_MILLIS,
        ) {
            throw TypedShowFailure("configure の失敗")
        }

        awaitAcceptance()
        assertEquals("呼び出しは失敗せず、その1枚だけが破棄される", 1, harness.displayCount)
        assertEquals("中身は作られない", 0, recorder.viewCreationCount)
        assertEquals(
            "表示中の別の Toast は残る",
            "別の表示",
            harness.defaultContentViews.single().displayedText,
        )

        harness.toast.show("後続", durationMs = SHORT_DURATION_MILLIS)
        assertTrue(
            "後続の show は正常に表示される",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 2 },
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_TY_05_configure_省略の型指定_show_は生成物をそのまま表示する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerBothSlots(harness, recorder, factoryTitle = "factory の既定")

        harness.toast.show(ConfigurableToastTestViewModel::class, durationMs = SHORT_DURATION_MILLIS)

        assertTrue(harness.waitUntilPresenting())
        assertEquals(listOf("factory の既定"), recorder.observedTitles)
        assertSame(recorder.lastView, harness.contentViews.single())
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_TY_06_型指定_show_でも_duration_と置き場所の引数が効く() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerBothSlots(harness, recorder)

        val defaultRect = showTypedAndMeasure(harness, placement = null)
        val topRect = showTypedAndMeasure(
            harness,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
        )

        assertTrue(
            "契約既定 (下部) より上へ移っていない (既定 $defaultRect / 引数 $topRect)",
            topRect.top < defaultRect.top,
        )
    }

    @Test
    fun TS_TY_07_型指定_show_は呼び出し時点のエントリで_View_まで作る() = runBlocking<Unit> {
        val harness = newHarness()
        val firstRecorder = ToastTypedShowRecorder()
        registerBothSlots(harness, firstRecorder, factoryTitle = "最初の VM")
        val secondRecorder = ToastTypedShowRecorder()

        // 受理の待ち行列を止めて、UI スレッドでの処理が始まる前に再登録できるようにする
        val gate = CountDownLatch(1)
        val occupied = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            occupied.countDown()
            gate.await()
        }
        occupied.await()

        harness.toast.show(ConfigurableToastTestViewModel::class, durationMs = LONG_DURATION_MILLIS)
        registerBothSlots(harness, secondRecorder, factoryTitle = "後の VM")
        gate.countDown()

        assertTrue(harness.waitUntilPresenting())
        assertEquals(
            "最初に取得した VM factory と View factory の組で表示される",
            listOf("最初の VM"),
            firstRecorder.observedTitles,
        )
        assertEquals("再登録後の VM factory は使われない", 0, secondRecorder.createdViewModels.size)
        assertEquals("再登録後の View factory も使われない", 0, secondRecorder.viewCreationCount)
    }

    @Test
    fun TS_TY_08_VM_factory_の失敗は受理後の失敗としてその1枚だけを破棄する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        registerViewFactory(harness, recorder)
        val configureRuns = AtomicInteger(0)
        harness.registry.registerViewModel(ConfigurableToastTestViewModel::class) {
            throw TypedShowFailure("ViewModel factory の失敗")
        }

        harness.toast.show("別の表示", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())

        harness.toast.show(
            ConfigurableToastTestViewModel::class,
            durationMs = LONG_DURATION_MILLIS,
        ) {
            configureRuns.incrementAndGet()
        }

        awaitAcceptance()
        assertEquals("呼び出しは失敗せず、その1枚だけが破棄される", 1, harness.displayCount)
        assertEquals("configure は呼ばれない", 0, configureRuns.get())
        assertEquals("中身も作られない", 0, recorder.viewCreationCount)
        assertEquals(
            "表示中の別の Toast は残る",
            "別の表示",
            harness.defaultContentViews.single().displayedText,
        )
    }

    // MARK: - 呼び出しコンテキスト

    @Test
    fun TS_TY_09_型指定_show_は任意スレッドから呼べ_UI_スレッドで生成される() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = ToastTypedShowRecorder()
        val creationOnMain = AtomicBoolean(false)
        val configureOnMain = AtomicBoolean(false)
        harness.registry.registerViewModel(ConfigurableToastTestViewModel::class) {
            creationOnMain.set(isOnMainThread())
            ConfigurableToastTestViewModel().also(recorder::recordCreation)
        }
        registerViewFactory(harness, recorder)

        val calledOffMainThread = withContext(Dispatchers.Default) {
            val offMainThread = !isOnMainThread()
            harness.toast.show(
                ConfigurableToastTestViewModel::class,
                durationMs = SHORT_DURATION_MILLIS,
            ) { viewModel ->
                configureOnMain.set(isOnMainThread())
                viewModel.title = "背面スレッドからの表示"
            }
            offMainThread
        }

        assertTrue("呼び出しは UI スレッド外から同期に戻る", calledOffMainThread)
        assertTrue(harness.waitUntilPresenting())
        assertTrue("ViewModel factory は UI スレッドで実行される", creationOnMain.get())
        assertTrue("configure は UI スレッドで実行される", configureOnMain.get())
        assertEquals(listOf("背面スレッドからの表示"), recorder.observedTitles)
        assertTrue(harness.waitUntilEmpty())
    }

    // MARK: - 参照型限定

    @Test
    fun TS_YA_02_value_class_の_VM_は登録と型指定_show_で拒否される() = runBlocking<Unit> {
        val harness = newHarness()

        val registrationFailure = runCatching {
            harness.registry.registerViewModel(ValueClassToastTestViewModel::class) {
                ValueClassToastTestViewModel("値型")
            }
        }.exceptionOrNull()
        assertTrue(registrationFailure is DialogException.ValueClassViewModel)

        val showFailure = runCatching {
            harness.toast.show(ValueClassToastTestViewModel::class)
        }.exceptionOrNull()
        assertTrue(showFailure is DialogException.ValueClassViewModel)

        assertEquals("表示は行われない", 0, harness.displayCount)
        assertFalse(harness.isPresenting)
    }

    // MARK: - 補助

    /** View factory と ViewModel factory の両方を登録する。 */
    private fun registerBothSlots(
        harness: ToastTestHarness,
        recorder: ToastTypedShowRecorder,
        factoryTitle: String = "factory の既定",
    ) {
        harness.registry.registerViewModel(ConfigurableToastTestViewModel::class) {
            ConfigurableToastTestViewModel(factoryTitle).also(recorder::recordCreation)
        }
        registerViewFactory(harness, recorder)
    }

    /** View factory だけを登録する。 */
    private fun registerViewFactory(harness: ToastTestHarness, recorder: ToastTypedShowRecorder) {
        harness.registry.register(ConfigurableToastTestViewModel::class) { viewModel ->
            newTextContent(viewModel.title).also { recorder.recordView(viewModel.title, it) }
        }
    }

    /** 型を渡す表示を1枚出して外形を測り、消えるまで待つ。 */
    private suspend fun showTypedAndMeasure(
        harness: ToastTestHarness,
        placement: DialogPlacement?,
    ): Rect {
        harness.toast.show(
            ConfigurableToastTestViewModel::class,
            durationMs = SHORT_DURATION_MILLIS,
            placement = placement,
        )
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        ToastLayoutObservation.awaitSettled(container)
        val rect = ToastLayoutObservation.contentRect(container)
        assertTrue("指定した duration の経過で消える", harness.waitUntilEmpty())
        return rect
    }

    /** 表示テキストを持つカスタム Toast の中身。 */
    private fun Context.newTextContent(title: String): View =
        TextView(this).apply { text = title }

    /**
     * 受理の待ち行列が空になるまで待つ。
     *
     * 受理は UI スレッドへ積まれた順に走るため、後から積んだ空の仕事の完了を待てば、
     * 直前の show の受理が済んだことになる。
     */
    private suspend fun awaitAcceptance() {
        withContext(Dispatchers.Main) { }
    }

    /** 呼び出しが UI スレッド上か。 */
    private fun isOnMainThread(): Boolean = Looper.myLooper() === Looper.getMainLooper()

    private fun newHarness(): ToastTestHarness = ToastTestHarness(currentActivity())

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
    }
}
