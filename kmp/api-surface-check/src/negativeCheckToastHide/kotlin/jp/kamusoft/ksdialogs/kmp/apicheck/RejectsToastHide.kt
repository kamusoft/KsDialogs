package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.KsToast

/**
 * Toast には閉じる操作がない — 消滅の契機は duration の経過だけである (core/ADR-0031)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastHide` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'hide'.
 */
public object RejectsToastHide {
    public fun TS_KM_03_hide(toast: KsToast) {
        toast.hide()
    }
}
