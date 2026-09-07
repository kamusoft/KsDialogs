package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * Basic Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * このクラス参照そのものがレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 */
internal class BasicDialogViewModel(val message: String) : DialogViewModel<Boolean>
