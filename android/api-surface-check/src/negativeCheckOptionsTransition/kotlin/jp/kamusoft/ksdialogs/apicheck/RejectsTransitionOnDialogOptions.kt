package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogOptions

/**
 * DialogOptions は出入りの演出を持たない (演出は第3の添付スロット。core/ADR-0015・core/ADR-0017)。
 *
 * このソースは `-Pksdialogs.negativeCheck.optionsTransition` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 */
public object RejectsTransitionOnDialogOptions {
    public fun read(options: DialogOptions): Any? = options.transition
}
