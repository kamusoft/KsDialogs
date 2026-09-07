package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog

/**
 * show に options 引数はない (静的メタ属性は呼び出しごとに変えられない。core/ADR-0015)。
 *
 * このソースは `-Pksdialogs.negativeCheck.showOptions` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 引数の型には公開されている [DialogPlacement] を使い、「options という名前の引数が無い」ことだけを突く。
 * 期待する診断: No parameter with name 'options' found.
 */
public object RejectsOptionsArgumentOnShow {
    public suspend fun show(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel(), options = DialogPlacement())
}
