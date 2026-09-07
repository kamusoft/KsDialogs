package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.notifier

/**
 * Model Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * 表示するメッセージは型指定 show の configure から設定される。
 * 結果は中身から渡される報告口ではなく、この ViewModel 自身が `notifier` から報告する
 * (core/ADR-0018)。
 */
internal class ModelDialogViewModel : DialogViewModel<Boolean> {
    /** ダイアログに表示するメッセージ。表示の直前に configure から設定する。 */
    var message: String = ""

    /** 完了を報告する。 */
    fun complete() {
        notifier?.complete(true)
    }

    /** キャンセルを報告する。 */
    fun cancel() {
        notifier?.cancel()
    }
}
