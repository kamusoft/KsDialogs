package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.KsDialog

/**
 * show に transition 引数はない (出入りの演出は添付でのみ供給する。core/ADR-0017)。
 *
 * このソースは `-Pksdialogs.negativeCheck.showTransition` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 */
public object RejectsTransitionArgumentOnShow {
    public suspend fun show(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel("こんにちは"), transition = DialogTransition.none())
}
