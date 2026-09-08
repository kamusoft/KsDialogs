package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.KsToast
import jp.kamusoft.ksdialogs.ToastViewModel

/**
 * 登録せずに、その場で渡した Compose のコンテンツをカスタム Toast として表示する (core/ADR-0013)。
 *
 * 従来 View 系のインライン表示と別名なのは、登録だけ別名で表示は同名という非対称を避けるためである。
 * 挙動は従来 View 系のインライン表示と同じで、**レジストリの状態は一切変わらない** —
 * 同じ ViewModel 型の登録があってもそれは使われず、登録内容もこの呼び出しの前後で変わらない。
 *
 * @param viewModel 表示するカスタム Toast の ViewModel
 * @param durationMs 表示するミリ秒。null なら一括設定の既定 duration
 * @param placement この呼び出しでの置き場所。渡すと中身に宣言された [DialogPlacement] を
 *   オブジェクトまるごと置換する (core/ADR-0015)
 * @param content 中身として組み立てる composable
 */
public fun <VM : ToastViewModel> KsToast.showCompose(
    viewModel: VM,
    durationMs: Int? = null,
    placement: DialogPlacement? = null,
    content: @Composable (VM) -> Unit,
) {
    show(viewModel, durationMs, placement) { suppliedViewModel ->
        DialogComposeContentView(this) { content(suppliedViewModel) }
    }
}
