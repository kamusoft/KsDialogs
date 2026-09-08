package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.KsLoading
import jp.kamusoft.ksdialogs.LoadingViewModel

/**
 * 登録せずに、その場で渡した Compose のコンテンツをカスタム Loading として表示する (core/ADR-0013)。
 *
 * 従来 View 系のインライン表示と別名なのは、登録だけ別名で表示は同名という非対称を避けるためである。
 * 挙動は従来 View 系のインライン表示と同じで、**レジストリの状態は一切変わらない** —
 * 同じ ViewModel 型の登録があってもそれは使われず、登録内容もこの呼び出しの前後で変わらない。
 *
 * @param viewModel 表示するカスタム Loading の ViewModel
 * @param placement この呼び出しでの置き場所。渡すと中身に宣言された [DialogPlacement] を
 *   オブジェクトまるごと置換する (core/ADR-0015)
 * @param content 中身として組み立てる composable
 */
public suspend fun <VM : LoadingViewModel> KsLoading.showCompose(
    viewModel: VM,
    placement: DialogPlacement? = null,
    content: @Composable (VM) -> Unit,
) {
    show(viewModel, placement) { suppliedViewModel ->
        DialogComposeContentView(this) { content(suppliedViewModel) }
    }
}

/**
 * 登録せずに、その場で渡した Compose のコンテンツを表示したまま処理を実行する (core/ADR-0013)。
 *
 * レジストリの状態は一切変わらない。それ以外は登録経路のスコープ形とまったく同じである。
 *
 * @param viewModel 表示するカスタム Loading の ViewModel
 * @param placement この呼び出しでの置き場所。渡すと中身に宣言された [DialogPlacement] を
 *   オブジェクトまるごと置換する (core/ADR-0015)
 * @param content 中身として組み立てる composable
 * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
 */
public suspend fun <VM : LoadingViewModel, T> KsLoading.startCompose(
    viewModel: VM,
    placement: DialogPlacement? = null,
    content: @Composable (VM) -> Unit,
    action: suspend ((Double) -> Unit) -> T,
): T = start(
    viewModel,
    placement,
    factory = { suppliedViewModel ->
        DialogComposeContentView(this) { content(suppliedViewModel) }
    },
    action = action,
)
