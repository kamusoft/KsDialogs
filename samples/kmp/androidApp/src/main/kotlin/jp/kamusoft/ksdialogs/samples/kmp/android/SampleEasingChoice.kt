package jp.kamusoft.ksdialogs.samples.kmp.android

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import jp.kamusoft.ksdialogs.samples.kmp.SampleEasingPreset
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * トランジションデモが選べるイージング。
 *
 * 契約はイージングを形態のネイティブ表現でそのまま受け取るため、
 * 選択肢は [Interpolator] への言い換えになる。
 *
 * @property label チップに表示する文言
 * @property preset 共有コードが運ぶ選択での言い換え
 */
internal enum class SampleEasingChoice(
    val label: String,
    val preset: SampleEasingPreset,
) {
    STANDARD(SampleText.EASING_STANDARD, SampleEasingPreset.STANDARD),
    LINEAR(SampleText.EASING_LINEAR, SampleEasingPreset.LINEAR),
    ACCELERATE(SampleText.EASING_ACCELERATE, SampleEasingPreset.ACCELERATE),
    DECELERATE(SampleText.EASING_DECELERATE, SampleEasingPreset.DECELERATE),
}

/** プリセットへ渡す時間曲線。 */
internal fun SampleEasingPreset.interpolator(): Interpolator = when (this) {
    SampleEasingPreset.STANDARD -> AccelerateDecelerateInterpolator()
    SampleEasingPreset.LINEAR -> LinearInterpolator()
    SampleEasingPreset.ACCELERATE -> AccelerateInterpolator()
    SampleEasingPreset.DECELERATE -> DecelerateInterpolator()
}
