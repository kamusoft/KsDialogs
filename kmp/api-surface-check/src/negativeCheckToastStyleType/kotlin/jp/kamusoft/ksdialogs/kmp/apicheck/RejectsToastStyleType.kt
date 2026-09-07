package jp.kamusoft.ksdialogs.kmp.apicheck

/**
 * 共有コードには Toast のスタイルの型がない (色が境界を渡らないため各 OS 側で設定する。
 * core/ADR-0032)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastStyleType` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'ToastStyle'.
 */
public object RejectsToastStyleType {
    public fun TS_KM_03_create(): Any = jp.kamusoft.ksdialogs.kmp.ToastStyle()
}
