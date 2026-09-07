package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.KsLoading

/**
 * 共有コードの Loading の公開面には見た目の設定プロパティがない (スタイルも既定ローディングの
 * 器メタ属性も各 OS 側で設定する。core/ADR-0023)。
 *
 * このソースは `-Pksdialogs.negativeCheck.loadingStyleProperty` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'style'.
 */
public object RejectsLoadingStyleProperty {
    public fun read(loading: KsLoading): Any = loading.style
}
