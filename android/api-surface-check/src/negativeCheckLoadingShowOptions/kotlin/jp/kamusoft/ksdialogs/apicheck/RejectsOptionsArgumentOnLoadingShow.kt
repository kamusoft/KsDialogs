package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.KsLoading

/**
 * 既定ローディングの表示 API に options 引数はない (器メタ属性はシングルトンの設定プロパティで
 * 渡す。器メタ属性のうち表示 API から渡せるのは placement だけ。core/ADR-0015・ADR-0023)。
 * スコープ形が受け取る actionThread は処理を始めるスレッドの指定で、器メタ属性ではない。
 *
 * このソースは `-Pksdialogs.negativeCheck.loadingShowOptions` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `None of the following candidates is applicable:` の 1 件だけ
 * (引数名不明の診断は出ない。2026-09-25 実測)
 */
public object RejectsOptionsArgumentOnLoadingShow {
    public suspend fun show(loading: KsLoading) {
        loading.show(message = "読み込み中", options = DialogOptions())
    }
}
