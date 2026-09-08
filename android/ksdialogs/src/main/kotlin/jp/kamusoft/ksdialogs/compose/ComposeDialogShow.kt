package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.KsDialog

/**
 * 登録せずに、その場で渡した Compose のコンテンツを表示して結果を待つ (core/ADR-0013)。
 *
 * 従来 View 系のインライン show と別名なのは、登録だけ別名で表示は同名という非対称を避けるためである。
 * 挙動は従来 View 系のインライン show と同じで、**レジストリの状態は一切変わらない** —
 * 同じ ViewModel 型の登録があってもそれは使われず、登録内容もこの呼び出しの前後で変わらない。
 * 同じ型のインライン表示を並行させても、コンテンツ・[DialogNotifier]・結果はそれぞれ独立する。
 *
 * @param viewModel 表示する ViewModel
 * @param placement この呼び出しでの置き場所。渡すと中身に添付された [DialogPlacement] を
 *   オブジェクトまるごと置換する (core/ADR-0015)
 * @param content 中身として組み立てる composable
 */
public suspend fun <R, VM : DialogViewModel<R>> KsDialog.showCompose(
    viewModel: VM,
    placement: DialogPlacement? = null,
    content: @Composable (VM, DialogNotifier<R>) -> Unit,
): DialogResult<R> = show(viewModel, placement) { suppliedViewModel, notifier ->
    DialogComposeContentView(this) { content(suppliedViewModel, notifier) }
}
