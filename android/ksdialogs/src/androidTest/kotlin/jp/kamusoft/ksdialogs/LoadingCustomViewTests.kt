package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DualRegistryLoadingTestViewModel
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.LoadingTestViewRecorder
import jp.kamusoft.ksdialogs.support.RecordingDialogPresentationSurface
import jp.kamusoft.ksdialogs.support.UnregisteredLoadingTestViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * カスタム Loading View の登録と表示 (core/ADR-0005・0011・0013・0022) を確かめる。
 * iOS Native の LoadingCustomViewTests のミラー。
 *
 * 登録は Loading 専用のレジストリで行い、Dialog のレジストリとは独立している。
 * 表示は登録済み ViewModel のインスタンス渡しと、その場の factory によるインライン版を持ち、
 * View は表示のたびに作り直す使い捨てである。
 */
@RunWith(AndroidJUnit4::class)
class LoadingCustomViewTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_CV_01_登録済み_VM_でカスタム_Loading_が表示される() = runBlocking<Unit> {
        val harness = newHarness()
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().also(viewRecorder::record)
        }

        harness.loading.show(LoadingTestViewModel())

        val presentedView = requireNotNull(harness.contentView)
        assertSame("factory が生成した View が表示される", viewRecorder.lastView, presentedView)
        assertTrue(
            "器のウィンドウに載る",
            InstrumentedDialogWaiting.waitUntil { presentedView.isAttachedToWindow },
        )
        assertNull("既定ローディングは使われない", harness.builtinContentView)

        harness.loading.hide()

        assertFalse(harness.isPresenting)
        assertNull("hide で消える", presentedView.parent)
    }

    @Test
    fun LD_CV_03_インライン_factory_表示はレジストリを変えない() = runBlocking<Unit> {
        val harness = newHarness()
        val registeredRecorder = LoadingTestViewRecorder()
        val inlineRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().also(registeredRecorder::record)
        }

        harness.loading.show(LoadingTestViewModel()) { _ ->
            newContentView().also(inlineRecorder::record)
        }

        assertSame("インラインの View が使われる", inlineRecorder.lastView, harness.contentView)
        assertEquals("登録済み factory は呼ばれない", 0, registeredRecorder.createdCount)

        harness.loading.hide()
        harness.loading.show(LoadingTestViewModel())

        assertSame(
            "その後の通常表示には登録済み factory の View が使われる",
            registeredRecorder.lastView,
            harness.contentView,
        )
        assertEquals(1, registeredRecorder.createdCount)
        assertEquals("インライン表示は登録を書き換えない", 1, inlineRecorder.createdCount)

        harness.loading.hide()
    }

    @Test
    fun LD_CV_04_未登録_VM_の表示は構成ミスとして失敗し_action_は実行されない() = runBlocking<Unit> {
        val harness = newHarness()
        val actionRuns = AtomicInteger(0)

        val showFailure = runCatching {
            harness.loading.show(UnregisteredLoadingTestViewModel())
        }.exceptionOrNull()
        assertTrue(
            "構成ミスとして失敗する",
            showFailure is DialogException.ViewFactoryNotRegistered,
        )
        assertFalse("表示されない", harness.isPresenting)

        val startFailure = runCatching {
            harness.loading.start(UnregisteredLoadingTestViewModel()) { _ ->
                actionRuns.incrementAndGet()
            }
        }.exceptionOrNull()
        assertTrue(startFailure is DialogException.ViewFactoryNotRegistered)
        assertEquals("action は実行されない", 0, actionRuns.get())
        assertFalse(harness.isPresenting)
        assertEquals("合流も始まらない", 0, harness.coalescedUseCount)
    }

    @Test
    fun LD_CV_05_Dialog_と_Loading_のレジストリは独立している() = runBlocking<Unit> {
        val harness = newHarness()
        val dialogRegistry = DialogViewRegistry()
        val dialogSurface = RecordingDialogPresentationSurface()
        val dialogs = Dialog(dialogRegistry, dialogSurface)
        val dialogRecorder = LoadingTestViewRecorder()
        val firstLoadingRecorder = LoadingTestViewRecorder()
        val secondLoadingRecorder = LoadingTestViewRecorder()
        val dialogNotifier = AtomicReference<DialogNotifier<Boolean>>()

        dialogRegistry.register(DualRegistryLoadingTestViewModel::class) { _, notifier ->
            dialogNotifier.set(notifier)
            newContentView().also(dialogRecorder::record)
        }
        harness.registry.register(DualRegistryLoadingTestViewModel::class) { _ ->
            newContentView().also(firstLoadingRecorder::record)
        }
        // Loading 側だけを別の factory で再登録する (後勝ち)
        harness.registry.register(DualRegistryLoadingTestViewModel::class) { _ ->
            newContentView().also(secondLoadingRecorder::record)
        }

        harness.loading.show(DualRegistryLoadingTestViewModel())
        assertSame(
            "Loading は再登録後の View を使う",
            secondLoadingRecorder.lastView,
            harness.contentView,
        )
        assertEquals(0, firstLoadingRecorder.createdCount)
        harness.loading.hide()

        coroutineScope {
            val showTask = async { dialogs.show(DualRegistryLoadingTestViewModel()) }
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { dialogSurface.presentedContainers.size == 1 },
            )
            val dialogContainer = requireNotNull(dialogSurface.topmostContainer)

            assertSame(
                "Dialog は Loading 側の再登録に影響されない",
                dialogRecorder.lastView,
                dialogContainer.contentView,
            )

            requireNotNull(dialogNotifier.get()).complete(true)
            assertEquals(DialogResult.Completed(true), showTask.await())
        }
    }

    @Test
    fun LD_CV_06_View_は表示のたびに生成される() = runBlocking<Unit> {
        val harness = newHarness()
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().also(viewRecorder::record)
        }
        val viewModel = LoadingTestViewModel()

        harness.loading.show(viewModel)
        val firstView = requireNotNull(harness.contentView)
        harness.loading.hide()

        harness.loading.show(viewModel)
        val secondView = requireNotNull(harness.contentView)
        harness.loading.hide()

        assertEquals("factory は表示のたびに呼ばれる", 2, viewRecorder.createdCount)
        assertNotSame("毎回新しい View が使われる", firstView, secondView)
    }

    /** 共通ケース表と同じ内容サイズを持つ、カスタム Loading の中身。 */
    private fun Context.newContentView(): FixedContentSizeView =
        FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)

    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160
    }
}
