package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.samples.kmp.SampleEasingPreset
import jp.kamusoft.ksdialogs.samples.kmp.SampleText
import jp.kamusoft.ksdialogs.samples.kmp.SampleTransitionPreset
import kotlin.time.Duration.Companion.milliseconds

/**
 * トランジションデモが選べる演出。
 *
 * プリセット7種に、自作フックの実演を1つ足したものを並べる。
 *
 * @property label チップに表示する文言
 * @property usesAdjustments 時間とイージングの調整を使う演出か。
 *   無演出と自作フックは調整値を受け取らないため、画面では調整部を操作できなくする
 * @property preset 共有コードが運ぶ選択での言い換え
 */
internal enum class SampleTransitionChoice(
    val label: String,
    val usesAdjustments: Boolean,
    val preset: SampleTransitionPreset,
) {
    FADE(SampleText.TRANSITION_FADE, true, SampleTransitionPreset.FADE),
    SLIDE_UP(SampleText.TRANSITION_SLIDE_UP, true, SampleTransitionPreset.SLIDE_UP),
    SLIDE_DOWN(SampleText.TRANSITION_SLIDE_DOWN, true, SampleTransitionPreset.SLIDE_DOWN),
    SLIDE_START(SampleText.TRANSITION_SLIDE_START, true, SampleTransitionPreset.SLIDE_START),
    SLIDE_END(SampleText.TRANSITION_SLIDE_END, true, SampleTransitionPreset.SLIDE_END),
    ZOOM(SampleText.TRANSITION_ZOOM, true, SampleTransitionPreset.ZOOM),
    NONE(SampleText.TRANSITION_NONE, false, SampleTransitionPreset.NONE),
    CUSTOM_HOOK(SampleText.TRANSITION_CUSTOM_HOOK, false, SampleTransitionPreset.CUSTOM_HOOK),
}

/**
 * 共有コードが運んできた選択と調整値から、中身へ添付する演出を作る。
 *
 * @param durationMilliseconds 片道の時間 (ミリ秒)
 * @param easing 時間に対する進み方
 */
internal fun SampleTransitionPreset.transition(
    durationMilliseconds: Int,
    easing: SampleEasingPreset,
): DialogTransition {
    val duration = durationMilliseconds.milliseconds
    val interpolator = easing.interpolator()
    return when (this) {
        SampleTransitionPreset.FADE -> DialogTransition.fade(duration, interpolator)
        SampleTransitionPreset.SLIDE_UP ->
            DialogTransition.slide(DialogTransitionEdge.BOTTOM, duration, interpolator)
        SampleTransitionPreset.SLIDE_DOWN ->
            DialogTransition.slide(DialogTransitionEdge.TOP, duration, interpolator)
        SampleTransitionPreset.SLIDE_START ->
            DialogTransition.slide(DialogTransitionEdge.START, duration, interpolator)
        SampleTransitionPreset.SLIDE_END ->
            DialogTransition.slide(DialogTransitionEdge.END, duration, interpolator)
        SampleTransitionPreset.ZOOM -> DialogTransition.zoom(duration, interpolator)
        SampleTransitionPreset.NONE -> DialogTransition.none()
        SampleTransitionPreset.CUSTOM_HOOK -> SampleCustomTransition.create()
    }
}
