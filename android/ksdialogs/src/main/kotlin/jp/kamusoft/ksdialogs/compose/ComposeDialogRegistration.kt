package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.DialogViewRegistry
import kotlin.reflect.KClass

/**
 * ViewModel 型に対する Compose のコンテンツを登録する (core/ADR-0011)。
 *
 * 従来 View 系の登録と別名なのは、Kotlin では `@Composable` 付きの関数型と通常の関数型を
 * 同名で並べると呼び出し側の型推論が曖昧になるためである。
 *
 * コンテンツは show のたびに組み立て直され、受け取る [DialogNotifier] は
 * ViewModel が宣言した結果型に固定される。ホスティングは内部で行うため、
 * 利用者は composable をそのまま書けばよい。
 * 器のメタ属性は composable の冒頭で [KsDialogAttributes] を宣言して供給する。
 *
 * @param viewModelClass 登録キーになる ViewModel のクラス参照
 * @param content 中身として組み立てる composable
 */
public fun <R, VM : DialogViewModel<R>> DialogViewRegistry.registerCompose(
    viewModelClass: KClass<VM>,
    content: @Composable (VM, DialogNotifier<R>) -> Unit,
) {
    register(viewModelClass) { viewModel, notifier ->
        DialogComposeContentView(this) { content(viewModel, notifier) }
    }
}

/**
 * ViewModel 型に対する Compose のコンテンツを、ViewModel だけを受け取る形で登録する (core/ADR-0018)。
 *
 * 結果報告口はコンポーザブルの中から `viewModel.notifier` で取り出す。
 * 紐付けは提示層がコンテンツを組み立てる前に済ませているため、組み立て中からも読める。
 * それ以外は報告口を引数で受け取る登録と同じで、器のメタ属性の供給の仕方も変わらない。
 *
 * @param viewModelClass 登録キーになる ViewModel のクラス参照
 * @param content 中身として組み立てる composable
 */
public fun <R, VM : DialogViewModel<R>> DialogViewRegistry.registerCompose(
    viewModelClass: KClass<VM>,
    content: @Composable (VM) -> Unit,
) {
    register(viewModelClass) { viewModel ->
        DialogComposeContentView(this) { content(viewModel) }
    }
}
