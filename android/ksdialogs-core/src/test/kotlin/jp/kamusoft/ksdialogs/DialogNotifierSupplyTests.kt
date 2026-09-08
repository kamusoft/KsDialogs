package jp.kamusoft.ksdialogs

import android.view.View
import jp.kamusoft.ksdialogs.support.ConfigurableTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.EquatableTestDialogViewModel
import jp.kamusoft.ksdialogs.support.ModelBindingTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("show 中の ViewModel から結果報告口を引ける")
class DialogNotifierSupplyTests : DialogUiThreadTest() {

    @Test
    fun `MB-NI-01 VM 引数のみの factory の中身が VM 経由の報告口で結果を返す`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        // 中身の生成中に引けることが要件。生成後に紐付ける実装ではここが null になる
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val viewModel = ConfigurableTestDialogViewModel("報告口の供給")
        val showTask = async { harness.dialogs.show(viewModel) }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-NI-02 show 前の VM からは報告口を引けない`() {
        val viewModel = ConfigurableTestDialogViewModel("未表示")

        assertNull(viewModel.notifier)
    }

    @Test
    fun `MB-NI-03 結果配送後の VM からは報告口を引けない`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val viewModel = ConfigurableTestDialogViewModel("配送後")
        val showTask = async { harness.dialogs.show(viewModel) }
        recorder.awaitSuppliedNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
        assertNull(viewModel.notifier, "結果が呼び出し元へ渡る時点で紐付けが外れていること")
    }

    @Test
    fun `MB-NI-04 同一 VM インスタンスの並行 show は構成ミスとして失敗する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val viewModel = ConfigurableTestDialogViewModel("並行")
        val showTask = async { harness.dialogs.show(viewModel) }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))

        val failure = assertThrows<DialogException.ViewModelAlreadyShowing> {
            runBlocking { harness.dialogs.show(viewModel) }
        }

        assertEquals(ConfigurableTestDialogViewModel::class.qualifiedName, failure.viewModelTypeName)
        // 先行 show のダイアログは残ったままで、報告した結果はそちらへ配送される
        assertEquals(1, harness.presentedContainers.size)
        assertEquals(1, recorder.creationCount)
        notifier.complete(true)
        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-NI-05 2引数 factory でも VM 経由の報告口は同じ配送先を指す`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        // factory 引数の報告口は使わず、VM から引いた報告口だけを記録する
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel, _ ->
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val viewModel = ConfigurableTestDialogViewModel("2引数")
        val showTask = async { harness.dialogs.show(viewModel) }
        recorder.awaitSuppliedNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `MB-NI-06 等価な別インスタンスの VM は互いに干渉しない`() = runBlocking {
        val harness = DialogTestHarness()
        harness.registry.register(EquatableTestDialogViewModel::class) { _ -> View(this) }

        val first = EquatableTestDialogViewModel("同じラベル")
        val second = EquatableTestDialogViewModel("同じラベル")
        assertEquals(first, second, "等価比較は一致すること")
        assertTrue(first !== second, "別インスタンスであること")

        val firstTask = async { harness.dialogs.show(first) }
        assertTrue(harness.waitForPresentedContainers(1))
        val secondTask = async { harness.dialogs.show(second) }
        assertTrue(harness.waitForPresentedContainers(2))

        val secondNotifier = requireNotNull(second.notifier) { "表示中の VM から報告口が引けること" }
        secondNotifier.complete(true)

        assertEquals(DialogResult.Completed(true), secondTask.await())
        assertTrue(harness.waitForPresentedContainers(1))
        assertNotNull(first.notifier, "報告していない側は表示中のままであること")

        firstTask.cancel()
        Unit
    }


    @Test
    fun `MB-NI-07 異常終了でも紐付けが外れ同じ VM を再 show できる`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = ModelBindingTestRecorder()
        // 1回目の中身の生成だけ失敗する factory。生成の失敗は show の失敗として伝播する
        var creationAttempts = 0
        harness.registry.register(ConfigurableTestDialogViewModel::class) { viewModel ->
            creationAttempts += 1
            check(creationAttempts > 1) { "1回目の中身の生成は失敗する" }
            recorder.record(viewModel.notifier, viewModel.message)
            View(this)
        }

        val viewModel = ConfigurableTestDialogViewModel("再表示")
        assertThrows<IllegalStateException> {
            runBlocking { harness.dialogs.show(viewModel) }
        }
        assertNull(viewModel.notifier, "失敗直後に紐付けが外れていること")

        val showTask = async { harness.dialogs.show(viewModel) }
        val notifier = recorder.awaitSuppliedNotifier(0)
        assertTrue(harness.waitForPresentedContainers(1))
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }
}
