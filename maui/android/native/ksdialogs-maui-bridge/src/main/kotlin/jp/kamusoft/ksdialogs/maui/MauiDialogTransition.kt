package jp.kamusoft.ksdialogs.maui

import android.view.View
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MAUI 側の演出を実行する口の形。
 *
 * 第1引数はコンテンツのホスト View、第2引数は演出が終わったときに呼ぶ完了通知。
 * Java 互換面では suspend を直接表せないため、完了をコールバックで返す形にしてある。
 */
public fun interface MauiDialogTransitionRunner {
    /**
     * 演出を開始する。UI スレッドから呼ばれる。
     *
     * @param hostView 演出の対象になるコンテンツのホスト View
     * @param completion 演出が終わったときに呼ぶ完了通知
     */
    public fun run(hostView: View, completion: Runnable)
}

/**
 * 完了通知が何度届いても1回だけ通す入れ物。
 *
 * 完了が2回以上届くと器の進行が二重に走るため、この面で1回に切り詰める。
 */
internal class MauiDialogSingleCompletion(private val completion: Runnable) {
    private val isCompleted = AtomicBoolean(false)

    /** 完了を通す。2回目以降は何も起こさない。 */
    fun complete() {
        if (isCompleted.compareAndSet(false, true)) {
            completion.run()
        }
    }
}
