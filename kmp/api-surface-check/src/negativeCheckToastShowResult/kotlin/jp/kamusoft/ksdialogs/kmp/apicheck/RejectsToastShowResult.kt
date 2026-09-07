package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsToast

/**
 * Toast の show は結果を返さない — 表示の終了を待つ手段も結末を受け取る手段も無い
 * (fire-and-forget。core/ADR-0031)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastShowResult` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: Initializer type mismatch: expected 'DialogResult<Boolean>', actual 'Unit'.
 */
public object RejectsToastShowResult {
    public fun TS_KM_03_receive(toast: KsToast) {
        val result: DialogResult<Boolean> = toast.show("保存しました")
        println(result)
    }
}
