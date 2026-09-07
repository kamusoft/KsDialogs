package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.KsLoading

/**
 * 既定ローディングの表示 API に options 引数はない (器メタ属性はシングルトンの設定プロパティで
 * 渡す。表示 API から渡せるのは placement だけ。core/ADR-0015・ADR-0023)。
 *
 * このソースは `-Pksdialogs.negativeCheck.loadingShowOptions` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `None of the following candidates is applicable:` と
 * `No parameter with name 'options' found.` (show がオーバーロードされているため 2 件出る)
 */
public object RejectsOptionsArgumentOnLoadingShow {
    public suspend fun show(loading: KsLoading) {
        loading.show(message = "読み込み中", options = DialogOptions())
    }
}
