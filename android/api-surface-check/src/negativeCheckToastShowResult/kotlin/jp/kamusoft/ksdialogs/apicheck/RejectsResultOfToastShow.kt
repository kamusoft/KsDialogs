package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsToast

/**
 * Toast の show は戻り値を持たない (表示の終了を待つ手段も結果も契約に無い。core/ADR-0031)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastShowResult` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `Initializer type mismatch: expected 'String', actual 'Unit'.`
 */
public object RejectsResultOfToastShow {
    public fun TS_AN_04_showResult(toast: KsToast): String {
        val handle: String = toast.show(message = "保存しました")
        return handle
    }
}
