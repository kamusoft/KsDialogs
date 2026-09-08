package jp.kamusoft.ksdialogs

import android.app.Activity

/**
 * 提示先の画面 (Activity) が破棄されたことを購読する。
 *
 * 画面の破棄ではダイアログのウィンドウが取り除かれるだけで閉鎖の通知は届かないため、
 * 結果を確定させる契機をこの購読で得る。
 */
internal interface ActivityDestroyObserver {
    /**
     * 指定した画面の破棄を購読する。
     *
     * 通知は UI スレッドで届く。購読はダイアログを閉じた時点で解除する。
     */
    fun observeDestroy(activity: Activity, onDestroyed: () -> Unit): ActivityDestroyRegistration
}

/** 破棄の購読1件。 */
internal fun interface ActivityDestroyRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}
