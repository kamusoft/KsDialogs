package jp.kamusoft.ksdialogs

/**
 * 追跡中の resumed な Activity を提示先としてダイアログを出す面。
 *
 * 提示先の指定は要らず、show を呼んだ時点で前面にある画面がそのまま提示先になる。
 * 提示先の画面が破棄されるときは、器を閉じて結果を確定させる。
 */
internal class ActivityDialogPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val destroyObserver: ActivityDestroyObserver = ResumedActivityTracker.shared,
) : DialogPresentationSurface {

    override val canPresent: Boolean
        get() = activityProvider.resumedActivity != null

    override fun present(request: DialogPresentationRequest): PresentedDialog {
        val activity = activityProvider.resumedActivity ?: throw DialogException.PresentationHostUnavailable()
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
}
