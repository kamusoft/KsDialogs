package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.KsToast

/**
 * Toast は器メタ属性の設定プロパティを持たない (覆いも外側タップも存在せず、器にしか実現できない
 * 属性が Toast では空集合になる。core/ADR-0014・0031)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastOptions` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `Unresolved reference 'options'.`
 */
public object RejectsOptionsPropertyOnToast {
    public fun TS_AN_04_optionsProperty(toast: KsToast) {
        toast.options = DialogOptions()
    }
}
