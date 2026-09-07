package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * Text Input Dialog の ViewModel。
 *
 * 結果型は [String] で、完了操作は入力された文字列を報告する。
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 */
class TextInputDialogViewModel(val message: String) : DialogViewModel<String>
