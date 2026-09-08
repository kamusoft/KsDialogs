package jp.kamusoft.ksdialogs

import android.view.View
import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogTestWaiting
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.SimpleFacedTestDialogViewModel
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
import java.util.concurrent.CopyOnWriteArrayList

@DisplayName("登録せずにその場で表示するインライン show")
class DialogInlineShowTests : DialogUiThreadTest() {

    @Test
    fun `未登録の VM 型でも渡した factory で表示され型付き結果が返る`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()

        val showTask = async {
            harness.dialogs.show(UnregisteredTestDialogViewModel()) { _, notifier ->
                View(this).also { recorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(1))
        assertSame(recorder.createdViews.first(), harness.topmostContainer?.contentView)

        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
    }

    @Test
    fun `真偽値の顔で宣言した VM でもインライン show の結果は真偽値になる`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()

        val showTask = async {
            harness.dialogs.show(SimpleFacedTestDialogViewModel("こんにちは")) { _, notifier ->
                View(this).also { recorder.record(it, notifier) }
            }
        }
        recorder.awaitNotifier(0).cancel()

        assertEquals(DialogResult.Cancelled, showTask.await())
    }

    @Test
    fun `インライン show は既存の登録を使わず前後で登録も変えない`() = runBlocking {
        val harness = DialogTestHarness()
        val registeredRecorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, registeredRecorder)
        val inlineRecorder = DialogTestRecorder<Boolean>()

        val inlineTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("インライン")) { _, notifier ->
                View(this).also { inlineRecorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(1))

        // 登録済みの factory は呼ばれず、その場の factory が使われる
        assertEquals(0, registeredRecorder.createdViews.size)
        assertSame(inlineRecorder.createdViews.first(), harness.topmostContainer?.contentView)

        inlineRecorder.awaitNotifier(0).complete(true)
        inlineTask.await()
        assertTrue(harness.waitForPresentedContainers(0))

        // 登録内容はインライン show の前後で変わらない
        val registeredTask = async { harness.dialogs.show(BasicTestDialogViewModel("登録経由")) }
        assertTrue(harness.waitForPresentedContainers(1))
        assertSame(registeredRecorder.createdViews.first(), harness.topmostContainer?.contentView)

        registeredRecorder.awaitNotifier(0).complete(false)
        assertEquals(DialogResult.Completed(false), registeredTask.await())
    }

    @Test
    fun `インライン show をしても登録済みにはならない`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()

        val inlineTask = async {
            harness.dialogs.show(UnregisteredTestDialogViewModel()) { _, notifier ->
                View(this).also { recorder.record(it, notifier) }
            }
        }
        recorder.awaitNotifier(0).complete(true)
        inlineTask.await()

        // レジストリを一時的にも触らないため、あとからレジストリ経由で呼べば未登録の失敗になる
        assertThrows<DialogException.ViewFactoryNotRegistered> {
            runBlocking { harness.dialogs.show(UnregisteredTestDialogViewModel()) }
        }
        Unit
    }

    @Test
    fun `同じ VM 型の並行インライン show は factory も結果も独立する`() = runBlocking {
        val harness = DialogTestHarness()
        val firstRecorder = DialogTestRecorder<Boolean>()
        val secondRecorder = DialogTestRecorder<Boolean>()

        val firstTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("1枚目")) { _, notifier ->
                View(this).also { firstRecorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(1))
        val secondTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("2枚目")) { _, notifier ->
                View(this).also { secondRecorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(2))

        assertNotSame(firstRecorder.createdViews.first(), secondRecorder.createdViews.first())

        // 片方を閉じても、もう片方は表示されたまま未確定
        secondRecorder.awaitNotifier(0).complete(true)
        assertEquals(DialogResult.Completed(true), secondTask.await())
        assertTrue(harness.waitForPresentedContainers(1))
        assertTrue(firstTask.isActive)

        firstRecorder.awaitNotifier(0).complete(false)
        assertEquals(DialogResult.Completed(false), firstTask.await())
    }

    @Test
    fun `インライン show でも登録済み VM 型と結果報告口が混ざらない`() = runBlocking {
        val harness = DialogTestHarness()
        val registeredRecorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, registeredRecorder)
        val inlineRecorder = DialogTestRecorder<Boolean>()
        val results = CopyOnWriteArrayList<String>()

        val registeredTask = async {
            results += "登録経由: ${harness.dialogs.show(BasicTestDialogViewModel("登録経由"))}"
        }
        assertTrue(harness.waitForPresentedContainers(1))
        val inlineTask = async {
            val result = harness.dialogs.show(BasicTestDialogViewModel("インライン")) { _, notifier ->
                View(this).also { inlineRecorder.record(it, notifier) }
            }
            results += "インライン: $result"
        }
        assertTrue(harness.waitForPresentedContainers(2))

        inlineRecorder.awaitNotifier(0).complete(true)
        inlineTask.await()
        registeredRecorder.awaitNotifier(0).complete(false)
        registeredTask.await()

        assertEquals(
            listOf("インライン: ${DialogResult.Completed(true)}", "登録経由: ${DialogResult.Completed(false)}"),
            results.toList(),
        )
    }

    @Test
    fun `インライン show の placement は器へそのまま渡る`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        val placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0)

        val showTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("こんにちは"), placement) { _, notifier ->
                View(this).also { recorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(1))

        assertEquals(listOf<DialogPlacement?>(placement), harness.presentationSurface.presentedPlacements)

        recorder.awaitNotifier(0).complete(true)
        showTask.await()
        Unit
    }

    @Test
    fun `提示先が無ければインライン show も View を作らずに失敗する`() {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val createdViews = CopyOnWriteArrayList<View>()

        assertThrows<DialogException.PresentationHostUnavailable> {
            runBlocking {
                harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) { _, _ ->
                    View(this).also { createdViews.add(it) }
                }
            }
        }

        assertTrue(createdViews.isEmpty())
        assertTrue(harness.presentedContainers.isEmpty())
    }

    @Test
    fun `インライン show でも器が外れれば cancelled で確定する`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()

        val showTask = async {
            harness.dialogs.show(BasicTestDialogViewModel("こんにちは")) { _, notifier ->
                View(this).also { recorder.record(it, notifier) }
            }
        }
        assertTrue(harness.waitForPresentedContainers(1))
        val container = requireNotNull(harness.topmostContainer)
        harness.presentationSurface.detachExternally(container)

        assertEquals(DialogResult.Cancelled, showTask.await())
        assertTrue(DialogTestWaiting.waitUntil { harness.presentedContainers.isEmpty() })
    }
}
