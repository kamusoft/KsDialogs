package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.KsToast

/**
 * 共有コードのレジストリから View factory の登録はできない (中身の View の型が境界を渡らないため、
 * View の登録は各 OS 側の受け口で行う。core/ADR-0029・kmp/ADR-0003)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastRegistration` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'register'.
 */
public object RejectsToastRegistration {
    public fun TS_KT_02_registerViewFactory(toast: KsToast) {
        toast.registry.register(ConsumerToastViewModel::class) { Any() }
    }
}
