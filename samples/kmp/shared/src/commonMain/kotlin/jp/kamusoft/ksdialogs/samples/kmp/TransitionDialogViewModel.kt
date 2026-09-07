package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * Transition Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 * @property preset 選ばれた演出。演出の組み立ては各 OS の View 定義側で完結するため、
 *   共有コードは選択だけを運び、View factory が作った View への添付として供給する
 * @property durationMilliseconds 片道の時間 (ミリ秒)
 * @property easing 時間に対する進み方
 */
class TransitionDialogViewModel(
    val message: String,
    val preset: SampleTransitionPreset,
    val durationMilliseconds: Int,
    val easing: SampleEasingPreset,
) : DialogViewModel<Boolean>
