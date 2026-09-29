package jp.kamusoft.ksdialogs

import android.view.View
import jp.kamusoft.ksdialogs.support.BasicTestDialogViewModel
import jp.kamusoft.ksdialogs.support.ConfigurableTestDialogViewModel
import jp.kamusoft.ksdialogs.support.DialogTestHarness
import jp.kamusoft.ksdialogs.support.DialogTestRecorder
import jp.kamusoft.ksdialogs.support.DialogTestWaiting
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.UnregisteredTestDialogViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 提示先の出現を待つ Dialog の結末を確かめる。
 *
 * 提示先が無いまま呼ばれた show は失敗せず、結果報告口を紐付けたうえで提示先の出現を待つ。
 * 待ちは、提示先が現れて列の先頭に来たとき・呼び出し元が打ち切ったとき・待っている間に
 * 結果が確定したときのどれかで明ける。待っている Dialog は呼んだ順に 1 枚ずつ明ける。
 */
@DisplayName("提示先の出現を待つ Dialog")
class DialogHostWaitTests : DialogUiThreadTest() {

    /** 提示先の無い組み立てと、View factory の呼び出しの記録。 */
    private fun makeHarnessWithoutHost(): Pair<DialogTestHarness, DialogTestRecorder<Boolean>> {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)
        return harness to recorder
    }

    /** 提示先の出現の通知が UI スレッドで処理され終わるまで待つ。 */
    private suspend fun drainUiThread() {
        withContext(Dispatchers.Main) { }
    }

    @Test
    fun `PB-HW-02 待っている間に呼び出し元が打ち切ると、一度も表示されずに終わる`() = runBlocking {
        val (harness, recorder) = makeHarnessWithoutHost()
        val viewModel = BasicTestDialogViewModel("打ち切られる")

        val showTask = async { harness.dialogs.show(viewModel) }
        assertTrue(harness.waitForWaitingCount(1), "show は提示先を待つ列に並ぶ")

        showTask.cancel()

        assertThrows<CancellationException>("打ち切りはキャンセルとして伝播する") {
            runBlocking { showTask.await() }
        }
        assertTrue(harness.waitForWaitingCount(0), "列から外れている")
        assertNull(viewModel.notifier, "VM の紐付けは解除されている")

        harness.makeHostAppear()
        drainUiThread()

        assertTrue(recorder.createdViews.isEmpty(), "View factory は一度も呼ばれない")
        assertTrue(harness.presentedContainers.isEmpty(), "提示先が現れても表示されない")
        assertEquals(0, harness.presentationSurface.activeHostChangeRegistrationCount, "購読は解除されている")
    }

    @Test
    fun `PB-HW-03 待っている間に VM が結果を報告すると、表示されずにその結果が返る`() = runBlocking {
        val (harness, recorder) = makeHarnessWithoutHost()
        val viewModel = BasicTestDialogViewModel("表示の前に報告する")

        val showTask = async { harness.dialogs.show(viewModel) }
        assertTrue(harness.waitForWaitingCount(1))

        val notifier = requireNotNull(viewModel.notifier) { "待っている間も VM の報告口は紐付いている" }
        notifier.complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await(), "報告した結果がそのまま返る")
        assertTrue(harness.waitForWaitingCount(0), "列から外れている")

        harness.makeHostAppear()
        drainUiThread()

        assertTrue(recorder.createdViews.isEmpty(), "View factory は一度も呼ばれない")
        assertTrue(harness.presentedContainers.isEmpty(), "ダイアログは表示されない")
        assertEquals(0, harness.presentationSurface.activeHostChangeRegistrationCount, "購読は解除されている")
    }

    @Test
    fun `PB-HW-04 未登録の ViewModel 型は、提示先が無くても待たずに失敗する`() {
        val (harness, _) = makeHarnessWithoutHost()

        val failure = assertThrows<DialogException.ViewFactoryNotRegistered> {
            runBlocking { harness.dialogs.show(UnregisteredTestDialogViewModel()) }
        }

        assertEquals(UnregisteredTestDialogViewModel::class.qualifiedName, failure.viewModelTypeName)
        assertEquals(0, harness.presentationSurface.hostWaitQueue.waitingCount, "列に並ばない")
        assertEquals(0, harness.presentationSurface.totalHostChangeRegistrationCount, "入れ替わりも購読しない")
    }

    @Test
    fun `PB-HW-05 待っている間の同じ VM インスタンスの再 show は「表示中」として失敗する`() = runBlocking {
        val (harness, recorder) = makeHarnessWithoutHost()
        val viewModel = BasicTestDialogViewModel("待っている")

        val firstShow = async { harness.dialogs.show(viewModel) }
        assertTrue(harness.waitForWaitingCount(1))

        val failure = assertThrows<DialogException.ViewModelAlreadyShowing> {
            runBlocking { harness.dialogs.show(viewModel) }
        }
        assertEquals(BasicTestDialogViewModel::class.qualifiedName, failure.viewModelTypeName)
        assertEquals(1, harness.presentationSurface.hostWaitQueue.waitingCount, "1 回目の待ちは続く")

        // 1 回目は提示先が現れた時点で表示され、ふつうに結果を返す
        harness.makeHostAppear()
        recorder.awaitNotifier(0).complete(false)
        assertEquals(DialogResult.Completed(false), firstShow.await())
    }

    @Test
    fun `PB-HW-06 待っている Dialog が複数あると、呼んだ順に表示され、後から呼んだものが手前になる`() = runBlocking {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        val createdMessages = CopyOnWriteArrayList<String>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder) { createdMessages.add(it.message) }
        val viewModelA = BasicTestDialogViewModel("A")
        val viewModelB = BasicTestDialogViewModel("B")
        val callerScope = CoroutineScope(Dispatchers.Main)

        // UI スレッドから、間に待ち合わせを挟まずに A、B の順で呼ぶ。
        // UNDISPATCHED で起動すると、show が最初に中断するまでを呼び出しの中で観察できる。
        // show が UI スレッドの外へ移ってから戻る作りだと、呼び出しから戻った時点ではまだ列に並んでいない。
        // A は具体型の入口、B は DI で受け取る契約 (`KsDialog`) の入口から呼び、どちらの経路も確かめる
        val injected: KsDialog = harness.dialogs
        val (showA, showB, waitingCountAfterCalls) = withContext(Dispatchers.Main) {
            val a = callerScope.async(start = CoroutineStart.UNDISPATCHED) { harness.dialogs.show(viewModelA) }
            val b = callerScope.async(start = CoroutineStart.UNDISPATCHED) { injected.show(viewModelB) }
            Triple(a, b, harness.presentationSurface.hostWaitQueue.waitingCount)
        }
        assertEquals(2, waitingCountAfterCalls, "UI スレッドを離れずに列まで進み、呼び出しから戻る前に 2 件とも並んでいる")

        harness.makeHostAppear()

        assertTrue(harness.waitForPresentedContainers(2), "2 枚とも表示される")
        assertEquals(listOf("A", "B"), createdMessages.toList(), "A、B の順に中身が作られる")
        assertSame(recorder.createdViews[0], harness.presentedContainers[0].contentView, "A が先に表示される")
        assertSame(recorder.createdViews[1], harness.presentedContainers[1].contentView, "B が A の手前に重なる")
        assertEquals(
            0,
            harness.presentationSurface.activeHostChangeRegistrationCount,
            "待つ表示が無くなれば購読を解除する",
        )

        // それぞれが独立に結果を返す
        recorder.awaitNotifier(1).complete(true)
        assertEquals(DialogResult.Completed(true), showB.await())
        recorder.awaitNotifier(0).complete(false)
        assertEquals(DialogResult.Completed(false), showA.await())
        callerScope.cancel()
    }

    @Test
    fun `PB-HW-08 待っている Dialog があるうちに呼んだ show は、追い越さずに後ろに並ぶ`() = runBlocking {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        val createdMessages = CopyOnWriteArrayList<String>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder) { createdMessages.add(it.message) }
        val viewModelA = BasicTestDialogViewModel("A")
        val viewModelB = BasicTestDialogViewModel("B")
        val callerScope = CoroutineScope(Dispatchers.Main)

        val showA = async { harness.dialogs.show(viewModelA) }
        assertTrue(harness.waitForWaitingCount(1))

        // A の提示が始まり、器のウィンドウを追加し終える前に、提示先がある状態で B の show を呼ぶ
        var showB: Deferred<DialogResult<Boolean>>? = null
        var waitingCountWhenBCalled = -1
        var createdCountWhenBCalled = -1
        harness.presentationSurface.onPresenting = {
            harness.presentationSurface.onPresenting = null
            showB = callerScope.async(start = CoroutineStart.UNDISPATCHED) { harness.dialogs.show(viewModelB) }
            waitingCountWhenBCalled = harness.presentationSurface.hostWaitQueue.waitingCount
            createdCountWhenBCalled = recorder.createdViews.size
        }
        harness.makeHostAppear()

        assertTrue(harness.waitForPresentedContainers(2), "A の提示が終わってから B が表示される")
        assertEquals(1, waitingCountWhenBCalled, "提示先があっても、A の提示が終わるまでは後ろに並ぶ")
        assertEquals(0, createdCountWhenBCalled, "B の中身はまだ作られない")
        assertEquals(listOf("A", "B"), createdMessages.toList())
        assertSame(recorder.createdViews[1], harness.presentedContainers[1].contentView, "B が A の手前に重なる")

        recorder.awaitNotifier(1).complete(true)
        assertEquals(DialogResult.Completed(true), requireNotNull(showB).await())
        recorder.awaitNotifier(0).complete(true)
        assertEquals(DialogResult.Completed(true), showA.await())
        callerScope.cancel()
    }

    @Test
    fun `PB-HW-07 型指定 show の VM factory と configure は、待つ前に実行される`() = runBlocking {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val viewModelFactoryCalls = AtomicInteger()
        val configureCalls = AtomicInteger()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            viewModelFactoryCalls.incrementAndGet()
            ConfigurableTestDialogViewModel()
        }
        harness.registry.register(ConfigurableTestDialogViewModel::class) { _, notifier ->
            View(this).also { recorder.record(it, notifier) }
        }

        val showTask = async {
            harness.dialogs.show(ConfigurableTestDialogViewModel::class) { viewModel ->
                configureCalls.incrementAndGet()
                viewModel.message = "configure で設定"
            }
        }
        assertTrue(harness.waitForWaitingCount(1))

        assertEquals(1, viewModelFactoryCalls.get(), "VM factory は提示先が現れる前に実行されている")
        assertEquals(1, configureCalls.get(), "configure は提示先が現れる前に実行されている")
        assertTrue(recorder.createdViews.isEmpty(), "View factory は提示先が現れるまで呼ばれない")

        harness.makeHostAppear()
        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
        assertEquals(1, viewModelFactoryCalls.get())
        assertEquals(1, configureCalls.get())
    }

    @Test
    fun `提示先が無いと通知されただけでは、待っている Dialog は表示されない`() = runBlocking {
        val (harness, recorder) = makeHarnessWithoutHost()

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("待ち続ける")) }
        assertTrue(harness.waitForWaitingCount(1))

        // 入れ替わりの通知は「現れたかもしれない」ことだけを知らせるので、提示先を読み直して待ち続ける
        withContext(Dispatchers.Main) { harness.presentationSurface.fireHostChange() }
        drainUiThread()

        assertEquals(1, harness.presentationSurface.hostWaitQueue.waitingCount, "待ちは続く")
        assertTrue(recorder.createdViews.isEmpty())
        assertEquals(1, harness.presentationSurface.activeHostChangeRegistrationCount, "購読は張ったまま")

        harness.makeHostAppear()
        recorder.awaitNotifier(0).complete(true)
        assertEquals(DialogResult.Completed(true), showTask.await())
        assertTrue(DialogTestWaiting.waitUntil { harness.presentationSurface.activeHostChangeRegistrationCount == 0 })
    }

    @Test
    fun `提示先がある状態で列も空なら、待たずに表示し購読も張らない`() = runBlocking {
        val harness = DialogTestHarness()
        val recorder = DialogTestRecorder<Boolean>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder)

        val showTask = async { harness.dialogs.show(BasicTestDialogViewModel("すぐ出る")) }
        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showTask.await())
        assertEquals(0, harness.presentationSurface.totalHostChangeRegistrationCount)
    }

    @Test
    fun `待っている先頭の Dialog が打ち切られると、後ろの Dialog が明ける`() = runBlocking {
        val harness = DialogTestHarness(hasPresentationHost = false)
        val recorder = DialogTestRecorder<Boolean>()
        val createdMessages = CopyOnWriteArrayList<String>()
        harness.registerRecordingFactory(BasicTestDialogViewModel::class, recorder) { createdMessages.add(it.message) }

        val showA = async { harness.dialogs.show(BasicTestDialogViewModel("A")) }
        assertTrue(harness.waitForWaitingCount(1))
        val showB = async { harness.dialogs.show(BasicTestDialogViewModel("B")) }
        assertTrue(harness.waitForWaitingCount(2))

        showA.cancel()
        assertTrue(harness.waitForWaitingCount(1), "打ち切られた A は順番を待たずに列から外れる")

        harness.makeHostAppear()
        recorder.awaitNotifier(0).complete(true)

        assertEquals(DialogResult.Completed(true), showB.await())
        assertEquals(listOf("B"), createdMessages.toList(), "A の中身は作られない")
    }
}
