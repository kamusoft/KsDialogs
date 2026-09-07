package jp.kamusoft.ksdialogs.kmp.apicheck

/**
 * 共有コードには静的メタ属性の型がない (各 OS の View 定義側で完結する。core/ADR-0015)。
 *
 * このソースは `-Pksdialogs.negativeCheck.optionsType` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'DialogOptions'.
 */
public object RejectsOptionsType {
    public fun create(): Any = jp.kamusoft.ksdialogs.kmp.DialogOptions()
}
