package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import kotlin.time.Duration.Companion.milliseconds

/**
 * トランジションデモが選べる演出。
 *
 * プリセット7種に、自作フックの実演を1つ足したものを並べる。
 *
 * @property label チップに表示する文言
 * @property usesAdjustments 時間とイージングの調整を使う演出か。
 *   無演出と自作フックは調整値を受け取らないため、画面では調整部を操作できなくする
 */
internal enum class SampleTransitionChoice(
    val label: String,
    val usesAdjustments: Boolean,
) {
    FADE(SampleText.TRANSITION_FADE, usesAdjustments = true),
    SLIDE_UP(SampleText.TRANSITION_SLIDE_UP, usesAdjustments = true),
    SLIDE_DOWN(SampleText.TRANSITION_SLIDE_DOWN, usesAdjustments = true),
    SLIDE_START(SampleText.TRANSITION_SLIDE_START, usesAdjustments = true),
    SLIDE_END(SampleText.TRANSITION_SLIDE_END, usesAdjustments = true),
    ZOOM(SampleText.TRANSITION_ZOOM, usesAdjustments = true),
    NONE(SampleText.TRANSITION_NONE, usesAdjustments = false),
    CUSTOM_HOOK(SampleText.TRANSITION_CUSTOM_HOOK, usesAdjustments = false),
    ;

    /**
     * 選択中の調整値と組み合わせて、中身へ添付する演出を作る。
     *
     * @param durationMilliseconds 片道の時間 (ミリ秒)
     * @param easing 時間に対する進み方
     */
    fun transition(durationMilliseconds: Int, easing: SampleEasingChoice): DialogTransition {
        val duration = durationMilliseconds.milliseconds
        val interpolator = easing.interpolator()
        return when (this) {
            FADE -> DialogTransition.fade(duration, interpolator)
            SLIDE_UP -> DialogTransition.slide(DialogTransitionEdge.BOTTOM, duration, interpolator)
            SLIDE_DOWN -> DialogTransition.slide(DialogTransitionEdge.TOP, duration, interpolator)
            SLIDE_START -> DialogTransition.slide(DialogTransitionEdge.START, duration, interpolator)
            SLIDE_END -> DialogTransition.slide(DialogTransitionEdge.END, duration, interpolator)
            ZOOM -> DialogTransition.zoom(duration, interpolator)
            NONE -> DialogTransition.none()
            CUSTOM_HOOK -> SampleCustomTransition.create()
        }
    }
}
