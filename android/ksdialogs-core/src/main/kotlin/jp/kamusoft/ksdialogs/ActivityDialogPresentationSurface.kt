package jp.kamusoft.ksdialogs

/**
 * 追跡中の resumed な Activity を提示先としてダイアログを出す面。
 *
 * 提示先の指定は要らず、提示の時点で前面にある画面がそのまま提示先になる。
 * 提示先の画面が破棄されるときは、器を閉じて結果を確定させる。
 *
 * @param hostWaitQueue 提示先を待つ show の列。既定はアプリケーションの提示先を共有する全 show の列
 */
internal class ActivityDialogPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val destroyObserver: ActivityDestroyObserver = ResumedActivityTracker.shared,
    private val changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared,
    override val hostWaitQueue: DialogHostWaitQueue = DialogHostWaitQueue.application,
) : DialogPresentationSurface {

    override val canPresent: Boolean
        get() = activityProvider.resumedActivity != null

    override fun present(request: DialogPresentationRequest): PresentedDialog {
        // 提示先を確かめてから呼ばれるまでの間に提示先が消えていれば、器は載せられない
        val activity = activityProvider.resumedActivity ?: return notPresented(request.resultChannel)
        val container = DialogContainer(
            context = activity,
            contentView = request.createContentView(activity),
            resultChannel = request.resultChannel,
            placement = request.placement,
        )
        // 画面の破棄ではウィンドウが取り除かれるだけで閉鎖の通知が届かないため、破棄を購読して器を閉じる
        val registration = destroyObserver.observeDestroy(activity) { container.closeOnHostDestroyed() }
        // 器の撤去はこの面ではなく器自身が進めるため、購読の解除も撤去に合わせて器から呼んでもらう
        container.onRemoved = { registration.cancel() }
        container.show()
        return PresentedDialog { handler -> container.onDelivery(handler) }
    }

    /**
     * 器を載せられなかった 1 枚を、器の消失と同じく確定させる。
     *
     * 中身は作らない。未確定なら cancelled で確定し、確定済みの結果をそのまま届ける。
     */
    private fun notPresented(resultChannel: DialogResultChannel): PresentedDialog {
        resultChannel.settle(DialogOutcome.Cancelled, DialogDismissalOrigin.HOST_LOST)
        return PresentedDialog { handler -> handler(resultChannel.settledOutcome ?: DialogOutcome.Cancelled) }
    }

    override fun observeHostChange(onHostChanged: () -> Unit): DialogHostRegistration {
        val registration = changeObserver.observeResumedChange(onHostChanged)
        return DialogHostRegistration { registration.cancel() }
    }
}
