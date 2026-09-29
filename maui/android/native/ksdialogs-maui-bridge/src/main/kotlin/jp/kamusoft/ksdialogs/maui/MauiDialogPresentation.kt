package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogNotifier
import kotlinx.coroutines.Job

/**
 * 提示したダイアログ1枚を閉じる・打ち切るための handle。
 *
 * 閉鎖は結果の報告によって起きる。中身を作る前 (提示先の出現を待っている間を含む) に閉鎖を
 * 求められたときは、中身を作らずに Native の show そのものを止める。中身を作ったあとは報告口で閉じる。
 * 閉鎖は何度求めても1回しか効かない (結果の確定がちょうど1回だから)。
 *
 * 打ち切りは Native の show を走らせている Job を止める。待っている間なら一度も表示されず、
 * 表示中なら閉じて、どちらも閉鎖の通知は cancelled で届く。
 */
public class MauiDialogPresentation internal constructor() {
    private val lock = Any()
    private var notifier: DialogNotifier<Boolean>? = null
    private var isDismissRequested = false
    private var isCancelRequested = false
    private var show: Job? = null

    /** 提示した1枚を閉じる。 */
    public fun dismiss() {
        var stopping: Job? = null
        val pending = synchronized(lock) {
            val current = notifier
            if (current == null) {
                isDismissRequested = true
                stopping = show
            } else {
                notifier = null
            }
            current
        }
        pending?.complete(true)
        // 中身を作る前なら、提示先の出現を待たずに show を止める
        stopping?.cancel()
    }

    /**
     * この show を打ち切る。待っている間なら一度も表示せず、表示中なら閉じる。
     * 閉鎖の通知は cancelled で届く。
     */
    public fun cancel() {
        val stopping = synchronized(lock) {
            isCancelRequested = true
            show
        }
        stopping?.cancel()
    }

    /**
     * Native の show を走らせている Job を結び付ける。
     * 結び付ける前に止めることを求められていたら、すぐに止める。
     */
    internal fun bind(job: Job) {
        val isStopRequested = synchronized(lock) {
            show = job
            isDismissRequested || isCancelRequested
        }
        if (isStopRequested) {
            job.cancel()
        }
    }

    /** 中身を作ってよいか。閉鎖か打ち切りをすでに求められていれば作らない。 */
    internal fun canSupplyContent(): Boolean = synchronized(lock) {
        !isDismissRequested && !isCancelRequested
    }

    /** 中身を作る前に閉鎖を求められて、show を止めたか。打ち切りでもあるときは、打ち切りとして扱う。 */
    internal fun wasDismissedBeforeContent(): Boolean = synchronized(lock) {
        isDismissRequested && !isCancelRequested
    }

    /** その提示の報告口を結び付ける。 */
    internal fun attach(notifier: DialogNotifier<Boolean>) {
        val immediate = synchronized(lock) {
            if (isDismissRequested) {
                // 中身を作り始めたあとに届いた閉鎖要求。作る中身はすぐに閉じる
                isDismissRequested = false
                true
            } else {
                this.notifier = notifier
                false
            }
        }
        if (immediate) {
            notifier.complete(true)
        }
    }
}
