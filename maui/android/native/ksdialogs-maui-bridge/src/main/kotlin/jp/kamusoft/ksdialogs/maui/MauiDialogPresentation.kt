package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogNotifier

/**
 * 提示したダイアログ1枚を閉じるための handle。
 *
 * 閉鎖は結果の報告によって起きるため、報告口が渡ってくるまでは閉鎖要求を保持し、
 * 渡った時点で実行する (提示の直後に閉鎖を求められても取りこぼさない)。
 * 閉鎖は何度求めても1回しか効かない (結果の確定がちょうど1回だから)。
 */
public class MauiDialogPresentation internal constructor() {
    private val lock = Any()
    private var notifier: DialogNotifier<Boolean>? = null
    private var isDismissRequested = false

    /** 提示した1枚を閉じる。 */
    public fun dismiss() {
        val pending = synchronized(lock) {
            val current = notifier
            if (current == null) {
                isDismissRequested = true
            } else {
                notifier = null
            }
            current
        }
        pending?.complete(true)
    }

    /** その提示の報告口を結び付ける。 */
    internal fun attach(notifier: DialogNotifier<Boolean>) {
        val immediate = synchronized(lock) {
            if (isDismissRequested) {
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
