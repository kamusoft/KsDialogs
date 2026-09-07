package jp.kamusoft.ksdialogs.kmp.apicheck

/**
 * 共有コードには既定ローディングのスタイルの型がない (色が境界を渡らないため各 OS 側で設定する。
 * core/ADR-0023)。
 *
 * このソースは `-Pksdialogs.negativeCheck.loadingStyleType` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'LoadingStyle'.
 */
public object RejectsLoadingStyleType {
    public fun create(): Any = jp.kamusoft.ksdialogs.kmp.LoadingStyle()
}
