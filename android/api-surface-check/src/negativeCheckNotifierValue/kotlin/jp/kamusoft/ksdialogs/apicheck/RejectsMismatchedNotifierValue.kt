package jp.kamusoft.ksdialogs.apicheck

import android.view.View
import jp.kamusoft.ksdialogs.DialogViewRegistry

/**
 * 宣言結果型と異なる値では完了報告できない (core/ADR-0003)。
 *
 * このソースは `-Pksdialogs.negativeCheck.notifierValue` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Argument type mismatch: actual type is 'String', but 'Boolean' was expected.
 */
public object RejectsMismatchedNotifierValue {
    public fun register(registry: DialogViewRegistry) {
        registry.register(ConsumerDialogViewModel::class) { _, notifier ->
            notifier.complete("完了")
            View(this)
        }
    }
}
