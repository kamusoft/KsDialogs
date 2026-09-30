package jp.kamusoft.ksdialogs.support

import android.view.View
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogContainer
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.DialogViewRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * レジストリ・提示面・[Dialog] を組み立てて、show の結果と器の重なりを観察できるようにする。
 *
 * @param registry 使用するレジストリ。既定は他のテストと干渉しない新規レジストリ
 * @param hasPresentationHost false にすると提示先が存在しない状況になる
 */
internal class DialogTestHarness(
    val registry: DialogViewRegistry = DialogViewRegistry(),
    hasPresentationHost: Boolean = true,
) {
    val presentationSurface = DialogTestPresentationSurface()
    val dialogs: Dialog

    init {
        presentationSurface.isPresentationHostAvailable = hasPresentationHost
        dialogs = Dialog(registry, presentationSurface)
    }

    /** 提示されているダイアログの器を、下から順に並べたもの。 */
    val presentedContainers: List<DialogContainer>
        get() = presentationSurface.presentedContainers

    /** 一番手前のダイアログの器。 */
    val topmostContainer: DialogContainer?
        get() = presentationSurface.topmostContainer

    /** 提示先を待つ列に並んだ show の数が指定数になるまで待つ。 */
    suspend fun waitForWaitingCount(count: Int): Boolean =
        DialogTestWaiting.waitUntil { presentationSurface.hostWaitQueue.waitingCount == count }

    /** 提示先を用意してから、UI スレッドで提示先の入れ替わりを通知する。 */
    suspend fun makeHostAppear() {
        withContext(Dispatchers.Main) {
            presentationSurface.isPresentationHostAvailable = true
            presentationSurface.fireHostChange()
        }
    }

    /** ダイアログが指定枚数提示されるまで待つ。 */
    suspend fun waitForPresentedContainers(count: Int): Boolean =
        DialogTestWaiting.waitUntil { presentedContainers.size == count }

    /**
     * 生成された View と結果報告口を記録する factory を登録する。
     *
     * @param onCreate factory の呼び出し時に追加で行う観察
     */
    fun <R, VM : DialogViewModel<R>> registerRecordingFactory(
        viewModelClass: KClass<VM>,
        recorder: DialogTestRecorder<R>,
        onCreate: (VM) -> Unit = {},
    ) {
        registry.register(viewModelClass) { viewModel, notifier ->
            onCreate(viewModel)
            View(this).also { recorder.record(it, notifier) }
        }
    }
}
