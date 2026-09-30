package jp.kamusoft.ksdialogs

import android.app.Activity

/**
 * ダイアログの提示起点となる Activity を供給する。
 *
 * Activity の追跡手段を差し替えられるようにするための内部の継ぎ目。
 * 提示先は resumed で、かつ start の後に一度でも描画された Activity である (core/ADR-0044)。
 */
internal interface ResumedActivityProvider {
    /**
     * 提示先の Activity。存在しなければ null。
     *
     * resumed でも、まだ描画されていない画面 (起動画面の下にある画面など) は提示先にならない。
     */
    val resumedActivity: Activity?
}

/**
 * アプリが前面にいるかどうかを供給する。
 *
 * 前面の間に提示先が無い (起動の途中・割り込みの最中など) かどうかを、提示先の有無と合わせて判定するための口
 * (core/ADR-0043)。
 */
internal interface AppForegroundProvider {
    /**
     * アプリが前面にいるか。
     *
     * 作成済みで、まだ stop も破棄もされていない Activity が 1 つ以上あれば前面とする。
     * Activity が 1 つも作られていない間は前面ではない。前面を離れたことは、
     * [ResumedActivityChangeObserver] の入れ替わりの通知で知らせる。
     */
    val isInForeground: Boolean
}

/**
 * 提示先の入れ替わりを購読する。
 *
 * 画面の再生成 (回転) では Activity が破棄されて作り直されるため、その画面に紐づくウィンドウは
 * 失われる。表示を継続する器は、この購読を契機に新しい画面へ載せ直す。
 */
internal interface ResumedActivityChangeObserver {
    /**
     * 提示先が変わった (破棄で不在になった場合を含む) ことと、アプリが前面を離れたことを購読する。
     *
     * 通知は UI スレッドで届く。何が現在の提示先か、前面にいるかは、購読者が [ResumedActivityProvider] と
     * [AppForegroundProvider] から読み直す。
     */
    fun observeResumedChange(onChanged: () -> Unit): ResumedActivityChangeRegistration
}

/** 提示先の入れ替わりの購読1件。 */
internal fun interface ResumedActivityChangeRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}
