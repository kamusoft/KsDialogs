package jp.kamusoft.ksdialogs

import android.app.Activity
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
     * アプリが前面にいるか。
     *
     * 前面にいるのに提示先が無い間 (画面を開いている途中・割り込みの最中) に受け付けた表示は、
     * 期限を決めずに待ち、提示先に載った時点から表示時間を数える (core/ADR-0043)。
     */
    val isAppInForeground: Boolean

    /**
     * 提示先の入れ替わり (画面の再生成・破棄を含む) と、アプリが前面を離れたことを購読する。
     *
     * Android の回転では Activity が作り直され、それに紐づくウィンドウも失われるため、
     * 表示を継続するには新しい提示先へ載せ直す必要がある。前面を離れたことは、期限を決めずに
     * 待っている表示の数え始めに使う。どちらの通知も同じ口で届くので、購読者は [hostContext] と
     * [isAppInForeground] を読み直す。通知は UI スレッドで届く。
     */
    fun observeHostChange(onHostChanged: () -> Unit): ToastHostRegistration

    /**
     * 提示先が無くなったときに、[host] に載っている器を付けたまま残すか。
     *
     * 背面へ下がる・上に別の画面が開くなどで提示先でなくなっても、[host] が破棄されていなければ残す。
     * 残さない面では、提示先が無くなった時点で器を外し、次の提示先へ載せ直す。
     */
    fun retainsAttachment(host: Context): Boolean = false
}

/** 提示先の入れ替わりの購読1件。 */
internal fun interface ToastHostRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}

/**
 * 追跡中の、resumed で描画済みの Activity を提示先にする既定の面。
 *
 * 提示先の指定は要らず、表示を始めた時点で前面にある画面がそのまま提示先になる。
 * 前面かどうかと器を残すかどうかも同じ追跡役から読むので、提示先・画面の破棄の判定と食い違わない。
 */
internal class ActivityToastPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val foregroundProvider: AppForegroundProvider = ResumedActivityTracker.shared,
    private val changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared,
    private val attachmentRetention: AttachedHostRetention = ResumedActivityTracker.shared,
) : ToastPresentationSurface {

    override val hostContext: Context?
        get() = activityProvider.resumedActivity

    override val isAppInForeground: Boolean
        get() = foregroundProvider.isInForeground

    override fun observeHostChange(onHostChanged: () -> Unit): ToastHostRegistration {
        val registration = changeObserver.observeResumedChange(onHostChanged)
        return ToastHostRegistration { registration.cancel() }
    }

    override fun retainsAttachment(host: Context): Boolean =
        (host as? Activity)?.let(attachmentRetention::retainsAttachment) ?: false
}
