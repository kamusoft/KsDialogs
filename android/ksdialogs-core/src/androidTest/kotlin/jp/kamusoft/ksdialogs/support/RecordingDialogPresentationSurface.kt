package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.ActivityDestroyObserver
import jp.kamusoft.ksdialogs.DialogContainer
import jp.kamusoft.ksdialogs.DialogHostRegistration
import jp.kamusoft.ksdialogs.DialogHostWaitQueue
import jp.kamusoft.ksdialogs.DialogPresentationRequest
import jp.kamusoft.ksdialogs.DialogPresentationSurface
import jp.kamusoft.ksdialogs.PresentedDialog
import jp.kamusoft.ksdialogs.ResumedActivityProvider
import jp.kamusoft.ksdialogs.ResumedActivityTracker

/**
 * 実際の画面へ器を出しつつ、その器をテストから掴めるようにする提示面。
 *
 * 提示・撤去の経路は本番の面と同じで、違うのは次の点だけ:
 *
 * - 出した器を記録し、状態と入力経路をテストから観察できるようにする
 * - [holdsWindowAttachment] を立てると、器をウィンドウに載せる時点を [attachHeldContainers] まで遅らせる
 *   (提示が始まる前の閉鎖信号を確実に作るため)
 * - [simulateHostLoss] で、提示先の画面が破棄されたときと同じ経路を呼び出せる
 * - [hidesPresentationHost] を立てると、resumed な Activity があっても提示先が無いものとして扱う。
 *   [fireHostChange] で提示先の入れ替わりの通知を送れる
 * - 提示先を待つ列は面ごとに専用のものを持ち、他のテストの show と干渉しない
 */
internal class RecordingDialogPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val destroyObserver: ActivityDestroyObserver = ResumedActivityTracker.shared,
) : DialogPresentationSurface {

    private val lock = Any()
    private val containers = mutableListOf<DialogContainer>()
    private val heldContainers = mutableListOf<DialogContainer>()

    private val hostChangeHandlers = mutableListOf<() -> Unit>()

    /** 器をウィンドウに載せるのを保留するか。 */
    var holdsWindowAttachment: Boolean = false

    /** resumed な Activity があっても提示先が無いものとして扱うか。 */
    @Volatile
    var hidesPresentationHost: Boolean = false

    override val canPresent: Boolean
        get() = !hidesPresentationHost && activityProvider.resumedActivity != null

    override val hostWaitQueue: DialogHostWaitQueue = DialogHostWaitQueue()

    /** 張られたままの入れ替わりの購読の数。 */
    val activeHostChangeRegistrationCount: Int
        get() = synchronized(lock) { hostChangeHandlers.size }

    /** 下から順に並んだ、提示中のダイアログの器。 */
    val presentedContainers: List<DialogContainer>
        get() = synchronized(lock) { containers.toList() }

    /** 一番手前のダイアログの器。 */
    val topmostContainer: DialogContainer?
        get() = presentedContainers.lastOrNull()

    override fun present(request: DialogPresentationRequest): PresentedDialog {
        val activity = checkNotNull(activityProvider.resumedActivity) { "提示先を確かめてから呼ばれる" }
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

    override fun observeHostChange(onHostChanged: () -> Unit): DialogHostRegistration {
        synchronized(lock) { hostChangeHandlers.add(onHostChanged) }
        return DialogHostRegistration { synchronized(lock) { hostChangeHandlers.remove(onHostChanged) } }
    }

    /** 提示先の入れ替わりを購読者へ伝える。UI スレッドから呼ぶ。 */
    fun fireHostChange() {
        synchronized(lock) { hostChangeHandlers.toList() }.forEach { it() }
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
