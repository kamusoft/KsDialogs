package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.ActivityDestroyObserver
import jp.kamusoft.ksdialogs.DialogContainer
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.DialogPresentationRequest
import jp.kamusoft.ksdialogs.DialogPresentationSurface
import jp.kamusoft.ksdialogs.PresentedDialog
import jp.kamusoft.ksdialogs.ResumedActivityProvider
import jp.kamusoft.ksdialogs.ResumedActivityTracker

/**
 * 実際の画面へ器を出しつつ、その器をテストから掴めるようにする提示面。
 *
 * 提示・撤去の経路は本番の面と同じで、違うのは次の3点だけ:
 *
 * - 出した器を記録し、状態と入力経路をテストから観察できるようにする
 * - [holdsWindowAttachment] を立てると、器をウィンドウに載せる時点を [attachHeldContainers] まで遅らせる
 *   (提示が始まる前の閉鎖信号を確実に作るため)
 * - [simulateHostLoss] で、提示先の画面が破棄されたときと同じ経路を呼び出せる
 */
internal class RecordingDialogPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val destroyObserver: ActivityDestroyObserver = ResumedActivityTracker.shared,
) : DialogPresentationSurface {

    private val lock = Any()
    private val containers = mutableListOf<DialogContainer>()
    private val heldContainers = mutableListOf<DialogContainer>()

    /** 器をウィンドウに載せるのを保留するか。 */
    var holdsWindowAttachment: Boolean = false

    override val canPresent: Boolean
        get() = activityProvider.resumedActivity != null

    /** 下から順に並んだ、提示中のダイアログの器。 */
    val presentedContainers: List<DialogContainer>
        get() = synchronized(lock) { containers.toList() }

    /** 一番手前のダイアログの器。 */
    val topmostContainer: DialogContainer?
        get() = presentedContainers.lastOrNull()

    override fun present(request: DialogPresentationRequest): PresentedDialog {
        val activity = activityProvider.resumedActivity ?: throw DialogException.PresentationHostUnavailable()
        val container = DialogContainer(
            context = activity,
            contentView = request.createContentView(activity),
            resultChannel = request.resultChannel,
            placement = request.placement,
        )
        val registration = destroyObserver.observeDestroy(activity) { container.closeOnHostDestroyed() }
        container.onRemoved = {
            registration.cancel()
            synchronized(lock) { containers.remove(container) }
        }
        synchronized(lock) { containers.add(container) }
        if (holdsWindowAttachment) {
            synchronized(lock) { heldContainers.add(container) }
        } else {
            container.show()
        }
        return PresentedDialog { handler -> container.onDelivery(handler) }
    }

    /** 保留していた器をまとめてウィンドウへ載せる。UI スレッドから呼ぶ。 */
    fun attachHeldContainers() {
        val held = synchronized(lock) { heldContainers.toList().also { heldContainers.clear() } }
        held.forEach { it.show() }
    }

    /** 提示先の画面が破棄されたときと同じ経路で器を失わせる。UI スレッドから呼ぶ。 */
    fun simulateHostLoss(container: DialogContainer) {
        container.closeOnHostDestroyed()
    }
}
