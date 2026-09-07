package jp.kamusoft.ksdialogs

import android.view.View
import jp.kamusoft.ksdialogs.support.ConfigurableTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.ModelBindingTestRecorder
import jp.kamusoft.ksdialogs.support.ValueClassTestDialogViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("ViewModel の型を渡す show")
class DialogTypedShowTests : DialogUiThreadTest() {

    /** configure が投げる、テスト専用の失敗。 */
    private class ConfigureFailure : RuntimeException("configure の失敗")

    /** ViewModel factory と、ViewModel だけを受け取る View factory の両方を登録する。 */
    private fun registerBothSlots(harness: DialogTestHarness, recorder: ModelBindingTestRecorder) {
        harness.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel("factory の既定")
        }
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }
    }

    @Test
    fun `MB-TS-01 型指定 show が生成から configure 表示 結果の一連で動く`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        registerBothSlots(harness, recorder)

        val showTask = async {
            harness.dialogs.show(ConfigurableTestDialogViewModel::class) { viewModel ->
                viewModel.message = "configure で設定"
            }
        }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        assertEquals("configure で設定", recorder.observedMessages[0], "configure の設定が中身に反映されていること")
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-TS-02 非同期 configure の完了まで中身の生成が始まらない`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        registerBothSlots(harness, recorder)
        val gate = CompletableDeferred<Unit>()
        val configureStarted = CompletableDeferred<Unit>()

        val showTask = async {
            harness.dialogs.show(ConfigurableTestDialogViewModel::class) { viewModel ->
                configureStarted.complete(Unit)
                gate.await()
                viewModel.message = "門を通ってから設定"
            }
        }
        // configure が始まったことを確認したうえで、門が閉じている間は中身が作られず提示も始まらないことを見る
        configureStarted.await()
        assertEquals(0, recorder.creationCount)
        assertTrue(harness.presentedContainers.isEmpty())

        gate.complete(Unit)
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        assertEquals("門を通ってから設定", recorder.observedMessages[0])
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-TS-03 ViewModel factory 未登録の型指定 show は構成ミスとして失敗する`() {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val failure = assertThrows<DialogException.ViewModelFactoryNotRegistered> {
            runBlocking { harness.dialogs.show(ConfigurableTestDialogViewModel::class) }
        }

        assertEquals(ConfigurableTestDialogViewModel::class.qualifiedName, failure.viewModelTypeName)
        assertEquals(0, recorder.creationCount)
        assertTrue(harness.presentedContainers.isEmpty())
    }

    @Test
    fun `MB-TS-04 configure 省略の型指定 show は ViewModel factory の生成物をそのまま表示する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        registerBothSlots(harness, recorder)

        val showTask = async { harness.dialogs.show(ConfigurableTestDialogViewModel::class) }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        assertEquals("factory の既定", recorder.observedMessages[0])
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-TS-05 configure の失敗は提示に進まず伝播する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        registerBothSlots(harness, recorder)

        assertThrows<ConfigureFailure> {
            runBlocking {
                harness.dialogs.show(ConfigurableTestDialogViewModel::class) { throw ConfigureFailure() }
            }
        }
        assertEquals(0, recorder.creationCount, "中身の生成へ進まないこと")
        assertTrue(harness.presentedContainers.isEmpty())

        // 同じ型のその後の型指定 show は正常に動く
        val showTask = async { harness.dialogs.show(ConfigurableTestDialogViewModel::class) }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-TS-06 再登録はスロット単位の後勝ちで他方を保持する`() = runBlocking {
        val harness = DialogTestHarness()
        val firstRecorder = ModelBindingTestRecorder()
        registerBothSlots(harness, firstRecorder)

        // View factory だけを別の factory で登録し直す。ViewModel factory のスロットは触らない
        val secondRecorder = ModelBindingTestRecorder()
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            secondRecorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val showTask = async { harness.dialogs.show(ConfigurableTestDialogViewModel::class) }
        val notifier = secondRecorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        assertEquals(0, firstRecorder.creationCount, "古い View factory は使われないこと")
        assertEquals("factory の既定", secondRecorder.observedMessages[0], "ViewModel factory は保持されていること")
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-AN-03 value class の VM は構成ミスとして拒否される`() {
        val harness = DialogTestHarness()

        val registrationFailure = assertThrows<DialogException.ValueClassViewModel> {
            harness.registry.register(ValueClassTestDialogViewModel::class) { _ -> View(this) }
        }
        assertEquals(
            ValueClassTestDialogViewModel::class.qualifiedName,
            registrationFailure.viewModelTypeName,
        )

        val viewModelRegistrationFailure = assertThrows<DialogException.ValueClassViewModel> {
            harness.registry.registerViewModel(ValueClassTestDialogViewModel::class) {
                ValueClassTestDialogViewModel("値型")
            }
        }
        assertEquals(
            ValueClassTestDialogViewModel::class.qualifiedName,
            viewModelRegistrationFailure.viewModelTypeName,
        )

        val showFailure = assertThrows<DialogException.ValueClassViewModel> {
            runBlocking { harness.dialogs.show(ValueClassTestDialogViewModel::class) }
        }
        assertEquals(ValueClassTestDialogViewModel::class.qualifiedName, showFailure.viewModelTypeName)
        assertTrue(harness.presentedContainers.isEmpty())
    }

    @Test
    fun `MB-AN-03 インライン factory の show でも value class の VM は拒否される`() {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()

        // 登録を通らない経路でも参照型限定は迂回できない
        val inlineShowFailure = assertThrows<DialogException.ValueClassViewModel> {
            runBlocking {
                harness.dialogs.show(ValueClassTestDialogViewModel("値型")) { viewModel, _ ->
                    recorder.record(null, viewModel.label)
                    View(this)
                }
            }
        }

        assertEquals(
            ValueClassTestDialogViewModel::class.qualifiedName,
            inlineShowFailure.viewModelTypeName,
        )
        assertEquals(0, recorder.creationCount, "中身の生成へ進まないこと")
        assertTrue(harness.presentedContainers.isEmpty())
    }

    @Test
    fun `MB-AN-03 インスタンス渡しの show でも value class の VM は拒否される`() {
        val harness = DialogTestHarness()

        // 登録できない型なので未登録失敗になりうるが、参照型限定の失敗が先に立つ
        val instanceShowFailure = assertThrows<DialogException.ValueClassViewModel> {
            runBlocking { harness.dialogs.show(ValueClassTestDialogViewModel("値型")) }
        }

        assertEquals(
            ValueClassTestDialogViewModel::class.qualifiedName,
            instanceShowFailure.viewModelTypeName,
        )
        assertTrue(harness.presentedContainers.isEmpty())
    }
}
