package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * 状態を持たない最小の ViewModel。
 *
 * メタ属性は中身の View への添付で供給するため、ViewModel 側には何も宣言しない。
 */
internal class PlainTestDialogViewModel(val message: String = "こんにちは") : DialogViewModel<Boolean>
