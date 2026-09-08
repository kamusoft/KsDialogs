package jp.kamusoft.ksdialogs

import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.SharedRegistryTestDialogViewModel
import jp.kamusoft.ksdialogs.support.UnregisteredTestDialogViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("VM 型キーによる View 解決と毎回生成")
class DialogRegistryTests : DialogUiThreadTest() {

    @Test
    fun `登録済み VM 型の show で View が表示される`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) }
        assertTrue(harness.waitForPresentedContainers(1))

        val container = harness.topmostContainer
        assertSame(recorder.createdViews.first(), container?.contentView)

        recorder.awaitNotifier(0).complete(true)
        showTask.await()
        Unit
    }

    @Test
    fun `未登録 VM 型の show は即エラー`() {
        val harness = DialogTestHarness()

        val exception = assertThrows<DialogException.ViewFactoryNotRegistered> {
            runBlocking { harness.dialogs.show(UnregisteredTestDialogViewModel()) }
        }

        assertEquals(UnregisteredTestDialogViewModel::class.qualifiedName, exception.viewModelTypeName)
        assertTrue(harness.presentedContainers.isEmpty())
    }

    @Test
    fun `show ごとに View は新規生成される`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val firstTask = async { harness.dialogs.show(BasicTestDialogViewModel("1回目")) }
        recorder.awaitNotifier(0).complete(true)
        firstTask.await()
        assertTrue(harness.waitForPresentedContainers(0))

        val secondTask = async { harness.dialogs.show(BasicTestDialogViewModel("2回目")) }
        recorder.awaitNotifier(1).complete(true)
        secondTask.await()

        assertEquals(2, recorder.createdViews.size)
        assertNotSame(recorder.createdViews[0], recorder.createdViews[1])
    }

    @Test
    fun `別インスタンスの同一 VM 型は独立した重ね出しになる`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        // 結果報告口はインスタンスごとに紐付くため、重ねるときは別インスタンスを使う (core/ADR-0018)
        val firstTask = async { harness.dialogs.show(BasicTestDialogViewModel("1枚目")) }
        assertTrue(harness.waitForPresentedContainers(1))
        val secondTask = async { harness.dialogs.show(BasicTestDialogViewModel("2枚目")) }
        assertTrue(harness.waitForPresentedContainers(2))

        assertEquals(2, recorder.createdViews.size)
        assertNotSame(recorder.createdViews[0], recorder.createdViews[1])

        recorder.notifiers[1].complete(true)
        recorder.notifiers[0].complete(false)
        assertEquals(DialogResult.Completed(true), secondTask.await())
        assertEquals(DialogResult.Completed(false), firstTask.await())
    }

    @Test
    fun `片方の入口の登録がもう片方から見える`() = runBlocking {
        // 既定 singleton エントリのレジストリへ登録し、DI 注入相当の別インスタンスから show する
        val recorder = DialogTestRecorder<Boolean>()
        Dialog.instance.registry.register(SharedRegistryTestDialogViewModel::class) { _, notifier ->
            android.view.View(this).also { recorder.record(it, notifier) }
        }

        val harness = DialogTestHarness(registry = DialogViewRegistry.shared)
        val injected: KsDialog = harness.dialogs

        val showTask = async { injected.show(SharedRegistryTestDialogViewModel()) }
        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }
}
