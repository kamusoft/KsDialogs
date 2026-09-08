package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.KsToast
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.showCompose

/**
 * Compose の呼び出し面は本体 (ksdialogs-core) には無い。宣言的 UI の登録・表示は
 * Compose 系の配布物 ksdialogs 側だけが持ち、本体は Compose 非依存を保つ (android/ADR-0001)。
 *
 * このソースは `-Pksdialogs.negativeCheck.toastComposeFromCore` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 * 期待する診断: `Unresolved reference 'showCompose'.` (import と呼び出しの 2 件出る)
 */
public object RejectsComposeToastApiFromCoreModule {
    public fun TS_AN_04_composeApiFromCore(toast: KsToast, viewModel: ToastViewModel) {
        toast.showCompose(viewModel) {}
    }
}
