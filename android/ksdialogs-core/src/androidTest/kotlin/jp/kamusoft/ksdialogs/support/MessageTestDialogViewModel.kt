package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.DialogViewModel

/**
 * 初期状態として本文を持ち、その内容で高さが決まる ViewModel。
 *
 * @param message 中身の View に適用される本文
 */
internal class MessageTestDialogViewModel(val message: String) : DialogViewModel<Boolean>
