package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsDialogs

/**
 * ダイアログ表示の契約に `KsDialogs` という型名はない (契約は `Ks` + 機能名の単数形)。
 *
 * このソースは `-Pksdialogs.negativeCheck.legacyContractName` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Unresolved reference 'KsDialogs'.
 */
public object RejectsLegacyContractName {
    public fun accept(dialogs: KsDialogs): Any = dialogs
}
