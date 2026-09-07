package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.KsToast

/**
 * 共有コードの Toast の公開面には見た目の設定プロパティがない (スタイルもアプリ既定配置も
 * 各 OS 側で設定する。core/ADR-0032)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastStyleProperty` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'style'.
 */
public object RejectsToastStyleProperty {
    public fun TS_KM_03_read(toast: KsToast): Any = toast.style
}
