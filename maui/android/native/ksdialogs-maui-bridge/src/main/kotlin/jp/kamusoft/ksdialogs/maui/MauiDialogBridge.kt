package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * MAUI 形態のための互換面 (maui/ADR-0001)。
 *
 * MAUI 側で実体化された platform view を中身にして、Native ライブラリのダイアログとして提示する。
 * ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
 * 表面は「提示する」「閉じる」の2操作と、提示1回ごとの通知1本だけで構成する。
 */
public class MauiDialogBridge private constructor(private val dialogs: KsDialog) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        // 中身は提示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        dialogs.registry.register(MauiDialogViewModel::class) { viewModel, notifier ->
            viewModel.presentation.attach(notifier)
            viewModel.createContentView()
        }
    }

    /**
     * 中身の View をダイアログとして提示し、閉じたときに通知をちょうど1回返す。
     *
     * 提示先はライブラリが自動解決するため、呼び出し側は提示先を渡さない。
     * 提示先が存在しない場合は通知が失敗として届き、中身の供給も呼ばれない。
     * 中身の供給は提示先が確保できた後に UI スレッドで呼ばれるため、
     * 呼び出し側は任意のスレッドからこの操作を呼べる。
     * 中身の供給が例外を投げた場合も、その理由が失敗の通知として届く。
     * メタ属性は供給された中身への添付として、そのまま Native ライブラリへ渡る。
     *
     * @param contentProvider ダイアログの中身と、それに効くメタ属性を新規に供給するもの
     * @param listener 閉鎖の通知先。ちょうど1回だけ呼ばれる
     * @return 提示した1枚を閉じるための handle
     */
    public fun present(
        contentProvider: MauiDialogContentProvider,
        listener: MauiDialogClosureListener,
    ): MauiDialogPresentation {
        val presentation = MauiDialogPresentation()
        val viewModel = MauiDialogViewModel(contentProvider, presentation)
        scope.launch {
            reportClosure(listener) { dialogs.show(viewModel) }
        }
        return presentation
    }

    public companion object {
        /** 既定の共有インスタンス。 */
        @JvmStatic
        public val shared: MauiDialogBridge = MauiDialogBridge(Dialog())
    }
}

/**
 * ダイアログの表示を行い、その結末を通知先へちょうど1回だけ伝える。
 *
 * 通知が1つも届かないと呼び出し元 (MAUI facade) が結果を待ち続けるため、
 * 想定していない失敗もすべて通知へ変換する。コルーチンのキャンセルだけは
 * 呼び出し元の構造に従って伝播させ、通知には変換しない。
 *
 * @param listener 閉鎖の通知先
 * @param show ダイアログを表示して結果を待つ処理
 */
@JvmSynthetic
internal suspend fun reportClosure(
    listener: MauiDialogClosureListener,
    show: suspend () -> DialogResult<Boolean>,
) {
    try {
        when (show()) {
            // 完了はこの面が閉鎖のために使う経路なので、利用者操作によるキャンセルと区別する
            is DialogResult.Completed -> listener.onDismissed()
            is DialogResult.Cancelled -> listener.onCancelled()
        }
    } catch (unavailable: DialogException.PresentationHostUnavailable) {
        listener.onPresentationHostUnavailable(unavailable.message)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        listener.onFailed(failure.message)
    }
}

/**
 * MAUI 側から供給される View をそのまま中身にするダイアログの ViewModel。
 *
 * この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの1種類が使われる。
 * 宣言結果型の [Boolean] は「この面が閉鎖のために完了を報告した」ことを表すだけで、
 * 利用者の結果値は MAUI 側の報告口が受け持つ。
 *
 * ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
 * 器へ渡る (core/ADR-0015)。この面が計算に関与することはない。
 */
internal class MauiDialogViewModel(
    private val contentProvider: MauiDialogContentProvider,
    val presentation: MauiDialogPresentation,
) : DialogViewModel<Boolean> {

    /**
     * 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
     * 提示先が確保できた後に UI スレッドで呼ばれる。
     *
     * MAUI 側が中身を作れなかったときは失敗を投げる。その失敗は閉鎖の通知の失敗に合流し、
     * MAUI 側が預かっている元の失敗が呼び出し元へ返る。
     */
    fun createContentView(): View {
        val content = contentProvider.createContent()
            ?: error("The MAUI side could not create the presentation content.")
        MauiDialogContent.applyAttributes(content.view, content.options, content.placement)
        return content.view
    }
}
