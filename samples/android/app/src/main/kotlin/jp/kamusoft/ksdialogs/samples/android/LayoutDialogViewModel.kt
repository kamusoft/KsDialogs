package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * Layout Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * このクラス参照そのものがレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 * @property usesVisibleArea サイズと位置の計算に可視領域を使うか。
 *   静的メタ属性は中身の性質なので、View factory が作った View への添付として供給する
 */
internal class LayoutDialogViewModel(
    val message: String,
    val usesVisibleArea: Boolean,
) : DialogViewModel<Boolean>
