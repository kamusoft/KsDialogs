package jp.kamusoft.ksdialogs

import android.app.Activity

/**
 * ダイアログの提示起点となる Activity を供給する。
 *
 * Activity の追跡手段を差し替えられるようにするための内部の継ぎ目。
 */
internal interface ResumedActivityProvider {
    /** 現在 resumed 状態の Activity。存在しなければ null。 */
    val resumedActivity: Activity?
}

/**
 * resumed な Activity の入れ替わりを購読する。
 *
 * 画面の再生成 (回転) では Activity が破棄されて作り直されるため、その画面に紐づくウィンドウは
 * 失われる。表示を継続する器は、この購読を契機に新しい画面へ載せ直す。
 */
internal interface ResumedActivityChangeObserver {
    /**
     * resumed な Activity が変わったこと (破棄で不在になった場合を含む) を購読する。
     *
     * 通知は UI スレッドで届く。何が現在の提示先かは購読者が [ResumedActivityProvider] から読み直す。
     */
    fun observeResumedChange(onChanged: () -> Unit): ResumedActivityChangeRegistration
}

/** resumed な Activity の入れ替わりの購読1件。 */
internal fun interface ResumedActivityChangeRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}
