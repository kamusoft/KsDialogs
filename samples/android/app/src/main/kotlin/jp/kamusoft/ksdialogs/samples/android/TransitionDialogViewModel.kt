package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * Transition Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * このクラス参照そのものがレジストリの登録キーになる。
 * 演出は呼び出しごとに変わるため、選ばれた組をここに載せて View factory へ運ぶ。
 *
 * @property message ダイアログに表示するメッセージ
 * @property transition 中身へ添付する出入りの演出
 */
internal class TransitionDialogViewModel(
    val message: String,
    val transition: DialogTransition,
) : DialogViewModel<Boolean>
