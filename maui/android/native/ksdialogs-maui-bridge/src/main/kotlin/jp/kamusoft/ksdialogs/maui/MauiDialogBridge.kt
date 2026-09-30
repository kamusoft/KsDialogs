package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * MAUI 形態のための互換面 (maui/ADR-0001)。
 *
 * MAUI 側で実体化された platform view を中身にして、Native ライブラリのダイアログとして提示する。
 * ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
 * 表面は「提示する」「閉じる」「打ち切る」の3操作と、提示1回ごとの通知1本だけで構成する。
 */
public class MauiDialogBridge private constructor(private val dialogs: KsDialog) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        // 中身は提示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        dialogs.registry.register(MauiDialogViewModel::class) { viewModel, notifier ->
            // 提示先を待っている間に閉鎖か打ち切りを求められていたら、中身を作らずに show を止める。
            // その show の Job は止められているので、閉鎖の通知は止められた show として届く
            if (!viewModel.presentation.canSupplyContent()) {
                throw CancellationException("The Dialog was stopped before its content was created.")
            }
            viewModel.presentation.attach(notifier)
            viewModel.createContentView()
        }
    }

    /**
     * 中身の View をダイアログとして提示し、閉じたときに通知をちょうど1回返す。
     *
     * 提示先はライブラリが自動解決するため、呼び出し側は提示先を渡さない。
     * 提示先が存在しない場合は、提示先が現れるまで待ってから中身の供給を呼ぶ。
     * 中身の供給は提示先が確保できた後に UI スレッドで呼ばれるため、
     * 呼び出し側は任意のスレッドからこの操作を呼べる。
     * 中身の供給が例外を投げた場合も、その理由が失敗の通知として届く。
     * 返した handle で打ち切ると、通知は cancelled として届く。
     * メタ属性は供給された中身への添付として、そのまま Native ライブラリへ渡る。
     *
     * @param contentProvider ダイアログの中身と、それに効くメタ属性を新規に供給するもの
     * @param listener 閉鎖の通知先。ちょうど1回だけ呼ばれる
     * @return 提示した1枚を閉じる・打ち切るための handle
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    public fun present(
        contentProvider: MauiDialogContentProvider,
        listener: MauiDialogClosureListener,
    ): MauiDialogPresentation {
        val presentation = MauiDialogPresentation()
        val viewModel = MauiDialogViewModel(contentProvider, presentation)
        // 走り出す前に打ち切られても本体を必ず走らせ、閉鎖の通知を取りこぼさない。
        // 打ち切りは本体の最初の中断点で効き、通知を出してから伝播する
        val show = scope.launch(start = CoroutineStart.ATOMIC) {
            reportClosure(listener, stoppedByDismissal = presentation::wasDismissedBeforeContent) {
                dialogs.show(viewModel)
            }
        }
        presentation.bind(show)
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
 * 想定していない失敗もすべて通知へ変換する。コルーチンのキャンセル (handle による打ち切り・
 * 中身を作る前の閉鎖要求) も通知してから、呼び出し元の構造に従って投げ直す。
 *
 * @param listener 閉鎖の通知先
 * @param stoppedByDismissal キャンセルが中身を作る前の閉鎖要求によるものか。そうなら閉鎖要求として通知する
 * @param show ダイアログを表示して結果を待つ処理
 */
@JvmSynthetic
internal suspend fun reportClosure(
    listener: MauiDialogClosureListener,
    stoppedByDismissal: () -> Boolean = { false },
    show: suspend () -> DialogResult<Boolean>,
) {
    try {
        when (show()) {
            // 完了はこの面が閉鎖のために使う経路なので、利用者操作によるキャンセルと区別する
            is DialogResult.Completed -> listener.onDismissed()
            is DialogResult.Cancelled -> listener.onCancelled()
        }
    } catch (cancellation: CancellationException) {
        if (stoppedByDismissal()) {
            listener.onDismissed()
        } else {
            listener.onCancelled()
        }
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
     * MAUI 側が預かっている元の失敗が呼び出し元へ返る (core/ADR-0033・core/ADR-0036)。
     */
    fun createContentView(): View {
        val content = contentProvider.createContent()
            ?: error("The MAUI side could not create the presentation content.")
        MauiDialogContent.applyAttributes(content.view, content.options, content.placement)
        return content.view
    }
}
