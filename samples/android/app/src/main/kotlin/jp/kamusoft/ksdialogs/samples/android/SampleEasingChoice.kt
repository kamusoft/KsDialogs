package jp.kamusoft.ksdialogs.samples.android

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

/**
 * トランジションデモが選べるイージング。
 *
 * 契約はイージングを形態のネイティブ表現でそのまま受け取るため、
 * 選択肢は [Interpolator] への言い換えになる。
 *
 * @property label チップに表示する文言
 */
internal enum class SampleEasingChoice(val label: String) {
    STANDARD(SampleText.EASING_STANDARD),
    LINEAR(SampleText.EASING_LINEAR),
    ACCELERATE(SampleText.EASING_ACCELERATE),
    DECELERATE(SampleText.EASING_DECELERATE),
    ;

    /** プリセットへ渡す時間曲線。 */
    fun interpolator(): Interpolator = when (this) {
        STANDARD -> AccelerateDecelerateInterpolator()
        LINEAR -> LinearInterpolator()
        ACCELERATE -> AccelerateInterpolator()
        DECELERATE -> DecelerateInterpolator()
    }
}
