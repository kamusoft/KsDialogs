package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.KsLoading
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 表示 1 回ごとに 1 度だけ届く完了の通知先。
 *
 * 呼ばれるのは 2 つのうちちょうど 1 つで、いずれも UI スレッドから呼ばれる。
 */
public interface MauiLoadingCompletionListener {
    /** 要求した操作が完了した。 */
    public fun onCompleted()

    /**
     * 操作が完了しなかった。
     *
     * Dialog 側の通知先と同じ名前にすると binding 生成器が同名の EventArgs を二重に作るため、
     * この面では別の名前にしてある。
     *
     * @param message 失敗の説明
     */
    public fun onFailure(message: String?)
}

/** スコープ形の処理が進捗を報告する口。任意のスレッドから呼べる。 */
public fun interface MauiLoadingProgressReport {
    /**
     * 進捗を報告する。
     *
     * @param progress 0〜1 の進捗値。範囲外は丸められ、非有限値は無視される
     */
    public fun report(progress: Double)
}

/** MAUI 側が持つスコープ形の処理。 */
public fun interface MauiLoadingAction {
    /**
     * 処理を開始する。UI スレッドから呼ばれる。
     *
     * 処理の成否は MAUI 側が自分で扱うため、この面へは伝えない。
     * 成否によらず [completion] を 1 回だけ呼ぶと、合流 1 件の終了として数えられる。
     *
     * @param report 進捗の報告口
     * @param completion 処理が終わったときに呼ぶ完了通知
     */
    public fun run(report: MauiLoadingProgressReport, completion: Runnable)
}

/**
 * MAUI 形態の Loading のための互換面 (maui/ADR-0001)。
 *
 * 合流カウント・表示世代・最新のメッセージと進捗はすべて Native ライブラリの coordinator が持ち、
 * この面は MAUI 側の呼び出しをそこへ渡すだけである (core/ADR-0024)。
 * ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
 * カスタム Loading の中身は表示のたびに MAUI 側から供給される。
 */
public class MauiLoadingBridge private constructor(private val loading: KsLoading) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        // 中身は表示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        loading.registry.register(MauiLoadingViewModel::class) { viewModel ->
            viewModel.createContentView()
        }
    }

    /**
     * 既定ローディングの見た目を設定する。器は各表示の開始時にこの値を読む (core/ADR-0023)。
     *
     * @param style MAUI 側で設定されたスタイル
     */
    public fun applyStyle(style: MauiLoadingStyle) {
        loading.style = style.toLoadingStyle()
    }

    /**
     * 既定ローディングの器メタ属性を設定する。器は各表示の開始時にこの値を読む。
     *
     * @param options MAUI 側で設定された静的メタ属性
     */
    public fun applyOptions(options: MauiDialogOptions) {
        loading.options = options.toDialogOptions()
    }

    /**
     * 合流 1 件を開始して表示する。
     *
     * 通知が届くのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
     * 中身の供給が失敗した場合はその理由が失敗の通知として届く。
     *
     * @param content 表示する中身の指定
     * @param listener 完了の通知先。ちょうど1回だけ呼ばれる
     */
    public fun show(content: MauiLoadingContent, listener: MauiLoadingCompletionListener) {
        scope.launch {
            reportLoadingCompletion(listener) { beginUse(content) }
        }
    }

    /**
     * 合流 1 件を握ったまま MAUI 側の処理を走らせる (スコープ形)。
     *
     * 処理は表示状態によらず必ず実行され、その完了通知が合流 1 件の終了になる。
     * 合流最後の 1 件なら器の撤去まで待ってから完了の通知が届く。
     *
     * @param content 表示する中身の指定
     * @param action MAUI 側の処理
     * @param listener 完了の通知先。ちょうど1回だけ呼ばれる
     */
    public fun start(
        content: MauiLoadingContent,
        action: MauiLoadingAction,
        listener: MauiLoadingCompletionListener,
    ) {
        scope.launch {
            reportLoadingCompletion(listener) { runScope(content, action) }
        }
    }

    /**
     * 合流数によらず表示を閉じる。出の演出と器の撤去が完了してから通知が届く。
     *
     * @param listener 完了の通知先。ちょうど1回だけ呼ばれる
     */
    public fun hide(listener: MauiLoadingCompletionListener) {
        scope.launch {
            reportLoadingCompletion(listener) { loading.hide() }
        }
    }

    /**
     * 表示中のメッセージを更新する。合流には関与しない。
     *
     * 受理は UI スレッド上で呼ばれた順に直列化されるため、この操作は完了を待たない。
     *
     * @param message 新しいメッセージ
     */
    public fun setMessage(message: String?) {
        scope.launch { loading.setMessage(message) }
    }

    /** 中身の指定に応じた開始の口を選ぶ。既定ローディングとカスタムで合流の数え方は変わらない。 */
    private suspend fun beginUse(content: MauiLoadingContent) {
        val placement = content.placement?.toDialogPlacement()
        val provider = content.provider
        if (provider == null) {
            loading.show(content.message, placement)
        } else {
            loading.show(MauiLoadingViewModel(provider, content.progressReceiver), placement)
        }
    }

    /** 中身の指定に応じたスコープ形の口を選び、MAUI 側の処理の完了まで合流 1 件を握る。 */
    private suspend fun runScope(content: MauiLoadingContent, action: MauiLoadingAction) {
        val placement = content.placement?.toDialogPlacement()
        val provider = content.provider
        if (provider == null) {
            loading.start(content.message, placement) { report -> awaitAction(action, report) }
        } else {
            val viewModel = MauiLoadingViewModel(provider, content.progressReceiver)
            loading.start(viewModel, placement) { report -> awaitAction(action, report) }
        }
    }

    /**
     * MAUI 側の処理を開始し、その完了通知が届くまで待つ。
     *
     * 待ちが打ち切られても MAUI 側の処理は取り消せないため、待つのをやめて戻る。
     */
    private suspend fun awaitAction(
        action: MauiLoadingAction,
        report: (Double) -> Unit,
    ): Unit = suspendCancellableCoroutine { continuation ->
        action.run(MauiLoadingProgressReport { progress -> report(progress) }) {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    public companion object {
        /** 既定の共有インスタンス。 */
        @JvmStatic
        public val shared: MauiLoadingBridge = MauiLoadingBridge(Loading())
    }
}

/**
 * 要求した操作を行い、その結末を通知先へちょうど 1 回だけ伝える。
 *
 * 通知が1つも届かないと呼び出し元 (MAUI facade) が待ち続けるため、想定していない失敗も
 * すべて通知へ変換する。コルーチンのキャンセルだけは呼び出し元の構造に従って伝播させる。
 *
 * @param listener 完了の通知先
 * @param operation 要求された操作
 */
@JvmSynthetic
internal suspend fun reportLoadingCompletion(
    listener: MauiLoadingCompletionListener,
    operation: suspend () -> Unit,
) {
    try {
        operation()
        listener.onCompleted()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        listener.onFailure(failure.message)
    }
}

/**
 * MAUI 側から供給される View をそのまま中身にするカスタム Loading の ViewModel。
 *
 * この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの 1 種類が使われる。
 * 進捗の受け口は常に実装し、MAUI 側の転送先が無いときは何もしない — MAUI 側の ViewModel が
 * 受け口を実装しているかの判定は C# 層が行う。
 *
 * ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
 * 器へ渡る (core/ADR-0015)。
 */
internal class MauiLoadingViewModel(
    private val contentProvider: MauiLoadingContentProvider,
    private val progressReceiver: MauiLoadingProgressReceiver?,
) : LoadingViewModel, LoadingProgressReceiver {

    /**
     * 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
     * 提示先が確保できた後に UI スレッドで呼ばれる。
     *
     * MAUI 側が中身を作れなかったときは失敗を投げる。その失敗は完了の通知の失敗に合流し、
     * MAUI 側が預かっている元の失敗が呼び出し元へ返る (core/ADR-0033・core/ADR-0036)。
     */
    fun createContentView(): View {
        val content = contentProvider.createContent()
            ?: error("The MAUI side could not create the presentation content.")
        MauiDialogContent.applyAttributes(content.view, content.options, content.placement)
        return content.view
    }

    override fun onProgress(progress: Double) {
        progressReceiver?.onProgress(progress)
    }
}
