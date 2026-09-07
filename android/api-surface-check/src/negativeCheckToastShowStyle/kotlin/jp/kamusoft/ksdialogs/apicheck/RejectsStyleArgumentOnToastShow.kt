package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsToast
import jp.kamusoft.ksdialogs.ToastStyle

/**
 * Toast の表示 API に style 引数はない (見た目はシングルトンの設定プロパティで一括設定し、
 * 器は各表示の受理時に読む。core/ADR-0032)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastShowStyle` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `None of the following candidates is applicable:` と
 * `No parameter with name 'style' found.` (show がオーバーロードされているため 2 件出る)
 */
public object RejectsStyleArgumentOnToastShow {
    public fun TS_AN_04_showStyle(toast: KsToast) {
        toast.show(message = "保存しました", style = ToastStyle())
    }
}
