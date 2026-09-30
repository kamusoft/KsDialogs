package jp.kamusoft.ksdialogs.support

import android.content.Context
import jp.kamusoft.ksdialogs.LoadingHostRegistration
import jp.kamusoft.ksdialogs.LoadingPresentationSurface

/**
 * Loading の提示先を差し替える面。
 *
 * 実装との対応は次のとおり:
 *
 * - [hostContext] は追跡中の、resumed で描画済みの Activity に対応する。null にすると提示先不在を再現できる
 * - 提示先を失った画面に載っている器は残さない ([retainsAttachment] は既定のまま)。[changeHost] に null を渡すと、
 *   画面が破棄されたときと同じく器が外れる
 * - [changeHost] は画面の再生成 (回転) に対応し、提示先が入れ替わったことを購読者へ伝える
 */
internal class LoadingTestPresentationSurface(
    override var hostContext: Context?,
) : LoadingPresentationSurface {

    private val lock = Any()
    private val observations = mutableListOf<() -> Unit>()

    /** 現在張られている購読の数。表示を終えた後に購読が残っていないことを見るために使う。 */
    val observerCount: Int
        get() = synchronized(lock) { observations.size }

    override fun observeHostChange(onHostChanged: () -> Unit): LoadingHostRegistration {
        synchronized(lock) { observations.add(onHostChanged) }
        return LoadingHostRegistration {
            synchronized(lock) { observations.remove(onHostChanged) }
        }
    }

    /** 提示先の入れ替わりを起こす。通知は呼び出したスレッドで届く。 */
    fun changeHost(newHost: Context?) {
        hostContext = newHost
        synchronized(lock) { observations.toList() }.forEach { it() }
    }
}
