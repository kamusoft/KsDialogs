package jp.kamusoft.ksdialogs.apicheck

/**
 * ViewModel はレイアウト属性を持たない (core/ADR-0014)。
 *
 * このソースは `-Pksdialogs.negativeCheck.vmAttribute` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'proportionalWidth'.
 */
public object RejectsLayoutAttributeOnViewModel {
    public fun read(): Double = ConsumerDialogViewModel("こんにちは").proportionalWidth
}
