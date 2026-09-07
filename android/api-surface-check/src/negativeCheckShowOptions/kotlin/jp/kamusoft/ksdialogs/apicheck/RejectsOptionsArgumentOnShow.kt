package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.KsDialog

/**
 * show に options 引数はない (静的メタ属性は呼び出しごとに変えられない。core/ADR-0015)。
 *
 * このソースは `-Pksdialogs.negativeCheck.showOptions` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: No parameter with name 'options' found.
 */
public object RejectsOptionsArgumentOnShow {
    public suspend fun show(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel("こんにちは"), options = DialogOptions())
}
