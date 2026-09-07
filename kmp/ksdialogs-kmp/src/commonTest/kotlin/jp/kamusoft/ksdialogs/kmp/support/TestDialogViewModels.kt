package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * 結果型に Boolean を宣言する検証用 ViewModel。
 *
 * @property message ダイアログに出す文言
 */
internal class BooleanTestDialogViewModel(val message: String = "確認") : DialogViewModel<Boolean>

/** 結果型に String を宣言する検証用 ViewModel。宣言結果型の違いを見るために使う。 */
internal class StringTestDialogViewModel : DialogViewModel<String>
