package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View

/**
 * レジストリが保持する View 生成関数の型消去表現。
 *
 * Context と ViewModel と結果チャネルを受け取り、そのダイアログの中身となる View を新規に生成する。
 */
internal fun interface DialogViewFactory {
    fun createView(context: Context, viewModel: Any, resultChannel: DialogResultChannel): View
}

/**
 * 宣言結果型で型付いた factory を、提示層が扱う型消去表現へ変換する。
 *
 * 登録経路とインライン show 経路の双方がこの変換を通るため、
 * どちらから渡した factory も提示・結果・レイアウトの同じ1系統に載る (core/ADR-0011)。
 */
internal fun <R, VM : DialogViewModel<R>> erasedDialogViewFactory(
    factory: Context.(VM, DialogNotifier<R>) -> View,
): DialogViewFactory = DialogViewFactory { context, viewModel, resultChannel ->
    // 解決は viewModel の実クラスをキーに行われるため、ここでの型は渡された factory のものと一致する
    @Suppress("UNCHECKED_CAST")
    val typedViewModel = viewModel as VM
    factory(context, typedViewModel, DialogNotifier(resultChannel))
}

/**
 * ViewModel だけを受け取る factory を、提示層が扱う型消去表現へ変換する (core/ADR-0018)。
 *
 * 結果報告口は中身の中から `viewModel.notifier` で取り出す。
 * 紐付けは提示層がこの factory を呼ぶ前に済ませているため、factory 本体からも読める。
 */
internal fun <R, VM : DialogViewModel<R>> erasedDialogViewFactory(
    factory: Context.(VM) -> View,
): DialogViewFactory = DialogViewFactory { context, viewModel, _ ->
    @Suppress("UNCHECKED_CAST")
    val typedViewModel = viewModel as VM
    factory(context, typedViewModel)
}
