package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsLoading
import jp.kamusoft.ksdialogs.LoadingStyle

/**
 * 既定ローディングの表示 API に style 引数はない (見た目はシングルトンの設定プロパティで
 * 一括設定し、器は各表示の開始時に読む。core/ADR-0023)。
 *
 * このソースは `-Pksdialogs.negativeCheck.loadingShowStyle` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `None of the following candidates is applicable:` と
 * `No parameter with name 'style' found.` (show がオーバーロードされているため 2 件出る)
 */
public object RejectsStyleArgumentOnLoadingShow {
    public suspend fun show(loading: KsLoading) {
        loading.show(message = "読み込み中", style = LoadingStyle())
    }
}
