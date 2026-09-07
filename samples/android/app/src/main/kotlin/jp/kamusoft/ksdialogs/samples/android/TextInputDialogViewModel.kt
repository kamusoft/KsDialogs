package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * Text Input Dialog の ViewModel。
 *
 * 結果型は [String] で、完了操作は入力された文字列を報告する。
 * このクラス参照そのものがレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 */
internal class TextInputDialogViewModel(val message: String) : DialogViewModel<String>
