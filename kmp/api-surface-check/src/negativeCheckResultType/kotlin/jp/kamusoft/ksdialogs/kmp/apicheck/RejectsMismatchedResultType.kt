package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog

/**
 * 宣言結果型と異なる型では受け取れない (core/ADR-0003)。
 *
 * このソースは `-Pksdialogs.negativeCheck.resultType` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Return type mismatch: expected 'DialogResult<String>', actual 'DialogResult<Boolean>'.
 */
public object RejectsMismatchedResultType {
    public suspend fun show(dialogs: KsDialog): DialogResult<String> =
        dialogs.show(ConsumerDialogViewModel())
}
