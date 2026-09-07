package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsToast

/**
 * Toast に閉じる口はない (消滅の契機は duration の経過だけの fire-and-forget。core/ADR-0031)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastHide` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `Unresolved reference 'hide'.`
 */
public object RejectsHideOnToast {
    public fun TS_AN_04_hide(toast: KsToast) {
        toast.hide()
    }
}
