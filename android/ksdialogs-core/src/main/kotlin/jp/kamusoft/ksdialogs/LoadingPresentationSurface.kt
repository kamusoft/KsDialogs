package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context

/**
 * Loading の器を載せる先を供給する面。
 *
 * 取り付け先はライブラリが自動解決するため、表示の呼び出し側はこの面に関与しない。
 * すべて UI スレッドから呼ばれる。
 */
internal interface LoadingPresentationSurface {
    /**
     * 器と中身の生成に使う提示先。
     *
     * 取り付け先が無ければ null。表示は成立しないが、渡された処理は通常どおり実行される
     * (提示環境の不在は構成ミスではない — core/ADR-0024)。
     */
    val hostContext: Context?

    /**
     * 提示先の入れ替わり (画面の再生成・破棄を含む) を購読する。
     *
     * Android の回転では Activity が作り直され、それに紐づくウィンドウも失われるため、
     * 表示を継続するには新しい提示先へ載せ直す必要がある。通知は UI スレッドで届く。
     */
    fun observeHostChange(onHostChanged: () -> Unit): LoadingHostRegistration

    /**
     * 提示先が無くなったときに、[host] に載っている器を付けたまま残すか。
     *
     * 背面へ下がる・上に別の画面が開くなどで提示先でなくなっても、[host] が破棄されていなければ残す。
     * 残さない面では、提示先が無くなった時点で器を外し、次の提示先へ載せ直す。
     */
    fun retainsAttachment(host: Context): Boolean = false
}

/** 提示先の入れ替わりの購読1件。 */
internal fun interface LoadingHostRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}

/**
 * 追跡中の、resumed で描画済みの Activity を提示先にする既定の面 (core/ADR-0044)。
 *
 * 提示先の指定は要らず、表示を始めた時点で前面にある画面がそのまま提示先になる。
 * 器を残すかどうかも同じ追跡役から読むので、画面の破棄の判定と食い違わない。
 */
internal class ActivityLoadingPresentationSurface(
    private val activityProvider: ResumedActivityProvider = ResumedActivityTracker.shared,
    private val changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared,
    private val attachmentRetention: AttachedHostRetention = ResumedActivityTracker.shared,
) : LoadingPresentationSurface {

    override val hostContext: Context?
        get() = activityProvider.resumedActivity

    override fun observeHostChange(onHostChanged: () -> Unit): LoadingHostRegistration {
        val registration = changeObserver.observeResumedChange(onHostChanged)
        return LoadingHostRegistration { registration.cancel() }
    }

    override fun retainsAttachment(host: Context): Boolean =
        (host as? Activity)?.let(attachmentRetention::retainsAttachment) ?: false
}
