package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * Declarative Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 */
class DeclarativeDialogViewModel(val message: String) : DialogViewModel<Boolean>
