package jp.kamusoft.ksdialogs

import android.content.Context

/**
 * Toast の器を載せる先を供給する面。
 *
 * 取り付け先はライブラリが自動解決するため、表示の呼び出し側はこの面に関与しない。
 * すべて UI スレッドから呼ばれる。
 */
internal interface ToastPresentationSurface {
    /**
     * 器と中身の生成に使う提示先。
     *
     * 取り付け先が無ければ null。表示は保留され、提示先の出現を待つ (core/ADR-0030)。
     */
    val hostContext: Context?

    /**
     * 提示先の入れ替わり (画面の再生成・破棄を含む) を購読する。
     *
     * Android の回転では Activity が作り直され、それに紐づくウィンドウも失われるため、
     * 表示を継続するには新しい提示先へ載せ直す必要がある。通知は UI スレッドで届く。
     */
    fun observeHostChange(onHostChanged: () -> Unit): ToastHostRegistration
}

/** 提示先の入れ替わりの購読1件。 */
internal fun interface ToastHostRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}

/**
 * 追跡中の resumed な Activity を提示先にする既定の面。
 *
 * 提示先の指定は要らず、表示を始めた時点で前面にある画面がそのまま提示先になる。
 */
internal class ActivityToastPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared,
) : ToastPresentationSurface {

    override val hostContext: Context?
        get() = activityProvider.resumedActivity

    override fun observeHostChange(onHostChanged: () -> Unit): ToastHostRegistration {
        val registration = changeObserver.observeResumedChange(onHostChanged)
        return ToastHostRegistration { registration.cancel() }
    }
}
