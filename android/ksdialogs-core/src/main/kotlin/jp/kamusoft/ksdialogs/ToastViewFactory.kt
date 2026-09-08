package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View

/**
 * Toast のレジストリが保持する View 生成関数の型消去表現。
 *
 * 結果報告口も進捗の受け口も持たないこと以外は [DialogViewFactory] と同じ役割で、
 * Context と ViewModel を受け取り、その Toast の中身となる View を新規に生成する。
 */
internal fun interface ToastViewFactory {
    fun createView(context: Context, viewModel: Any): View
}

/**
 * ViewModel 型で型付いた factory を、提示層が扱う型消去表現へ変換する。
 *
 * 登録経路とインライン表示経路の双方がこの変換を通るため、
 * どちらから渡した factory も同じ提示・レイアウトの1系統に載る (core/ADR-0011)。
 */
internal fun <VM : ToastViewModel> erasedToastViewFactory(
    factory: Context.(VM) -> View,
): ToastViewFactory = ToastViewFactory { context, viewModel ->
    // 解決は viewModel の実クラスをキーに行われるため、ここでの型は渡された factory のものと一致する
    @Suppress("UNCHECKED_CAST")
    val typedViewModel = viewModel as VM
    factory(context, typedViewModel)
}
