package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import kotlin.reflect.KClass

/**
 * ViewModel 型に対する Compose のコンテンツをカスタム Loading として登録する (core/ADR-0011)。
 *
 * 従来 View 系の登録と別名なのは、Kotlin では `@Composable` 付きの関数型と通常の関数型を
 * 同名で並べると呼び出し側の型推論が曖昧になるためである。
 *
 * Loading は結果を返さないライフサイクルなので、コンテンツは ViewModel だけを受け取る。
 * 進捗を中身へ届けたい場合は ViewModel に進捗の受け口
 * ([jp.kamusoft.ksdialogs.LoadingProgressReceiver]) を実装し、その状態を composable から観測する。
 * コンテンツは表示のたびに組み立て直され、器のメタ属性は composable の冒頭で
 * [KsDialogAttributes] を宣言して供給する。
 *
 * @param viewModelClass 登録キーになる ViewModel のクラス参照
 * @param content 中身として組み立てる composable
 */
public fun <VM : LoadingViewModel> LoadingViewRegistry.registerCompose(
    viewModelClass: KClass<VM>,
    content: @Composable (VM) -> Unit,
) {
    register(viewModelClass) { viewModel ->
        DialogComposeContentView(this) { content(viewModel) }
    }
}
