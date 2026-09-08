package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import android.view.View
import jp.kamusoft.ksdialogs.ActivityDialogPresentationSurface
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.DialogViewRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * 実際に show したダイアログが、最初に描かれる時点で持っていた外形を観察する。
 */
internal object DialogPresentationProbe {

    /** 提示と結果確定を待つ上限 (ミリ秒)。 */
    const val PRESENTATION_TIMEOUT_MILLIS = 10_000L

    /**
     * ダイアログを1枚出し、最初に描かれる時点の外形 (画面座標・px) を返す。
     *
     * 観察が済んだら結果を報告して閉じるため、呼び出しは提示を残さない。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param viewModel 提示する ViewModel
     * @param placement show の引数で渡す配置。null なら中身への添付が使われる
     * @param createContentView 中身の View を組み立てる
     */
    suspend fun <VM : DialogViewModel<Boolean>> captureRectAtFirstDraw(
        viewModelClass: KClass<VM>,
        viewModel: VM,
        placement: DialogPlacement? = null,
        createContentView: (VM, android.content.Context) -> View,
    ): Rect {
        val registry = DialogViewRegistry()
        val dialogs = Dialog(registry, ActivityDialogPresentationSurface())
        val rectAtFirstDraw = CompletableDeferred<Rect>()
        val notifier = AtomicReference<DialogNotifier<Boolean>>()

        registry.register(viewModelClass) { registeredViewModel, dialogNotifier ->
            notifier.set(dialogNotifier)
            createContentView(registeredViewModel, this).also { contentView ->
                contentView.observeFirstDrawRect(rectAtFirstDraw)
            }
        }

        return coroutineScope {
            val showTask = async { dialogs.show(viewModel, placement) }
            val rect = withTimeout(PRESENTATION_TIMEOUT_MILLIS) { rectAtFirstDraw.await() }
            notifier.get().complete(true)
            withTimeout(PRESENTATION_TIMEOUT_MILLIS) { showTask.await() }
            rect
        }
    }
}
