package jp.kamusoft.ksdialogs.support

import android.content.Context
import jp.kamusoft.ksdialogs.ToastHostRegistration
import jp.kamusoft.ksdialogs.ToastPresentationSurface

/**
 * Toast の提示先と、アプリが前面にいるかを差し替える面。
 *
 * 実装との対応は次のとおり:
 *
 * - [hostContext] は追跡中の、resumed で描画済みの Activity に対応する。null にすると提示先不在を再現できる
 * - [isAppInForeground] は追跡役の前面の判定に対応する。提示先が無く前面にいる状態が「前面の待ち」、
 *   提示先が無く前面にいない状態が「背面」になる
 * - [changeHost] は画面の再生成 (回転) と提示先の出現・消失に対応し、提示先が入れ替わったことを購読者へ伝える。
 *   提示先を失った画面に載っている器は残さない ([retainsAttachment] は既定のまま) ので、null を渡すと
 *   画面が破棄されたときと同じく器が外れる
 * - [leaveForeground] は前面の画面がすべて stop した (背面へ下がった) ことに対応し、購読者へ伝える
 *
 * @param hostContext 最初の提示先
 * @param isAppInForeground 最初の前面の判定。提示先が無いまま前面の待ちか背面かを選ぶ
 */
internal class ToastTestPresentationSurface(
    hostContext: Context?,
    isAppInForeground: Boolean = true,
) : ToastPresentationSurface {

    private val lock = Any()
    private val observations = mutableListOf<() -> Unit>()

    @Volatile
    override var hostContext: Context? = hostContext
        private set

    @Volatile
    override var isAppInForeground: Boolean = isAppInForeground
        private set

    /** 現在張られている購読の数。表示を終えた後に購読が残っていないことを見るために使う。 */
    val observerCount: Int
        get() = synchronized(lock) { observations.size }

    override fun observeHostChange(onHostChanged: () -> Unit): ToastHostRegistration {
        synchronized(lock) { observations.add(onHostChanged) }
        return ToastHostRegistration {
            synchronized(lock) { observations.remove(onHostChanged) }
        }
    }

    /**
     * 提示先の入れ替わりを起こす。通知は呼び出したスレッドで届く。
     *
     * 提示先があるのは前面にいる間だけなので、提示先を出すと前面の判定も前面に戻す。
     * null を渡したときは前面の判定を変えない (割り込みや作り直しで提示先だけを失った状態)。
     */
    fun changeHost(newHost: Context?) {
        hostContext = newHost
        if (newHost != null) isAppInForeground = true
        notifyObservers()
    }

    /** 提示先を失ったまま背面へ下がったことを起こす。通知は呼び出したスレッドで届く。 */
    fun leaveForeground() {
        hostContext = null
        isAppInForeground = false
        notifyObservers()
    }

    private fun notifyObservers() {
        synchronized(lock) { observations.toList() }.forEach { it() }
    }
}
