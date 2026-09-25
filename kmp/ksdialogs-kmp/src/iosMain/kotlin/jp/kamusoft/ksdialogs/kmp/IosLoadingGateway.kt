package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropLoadingBridge
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropLoadingUseHandle
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS Native ライブラリの ObjC 互換面へ委譲する Loading の委譲面。
 *
 * 合流カウント・表示世代・最新のメッセージと進捗はすべて Native ライブラリ側が持ち、
 * この面は共有コードの呼び出しをそこへ渡すだけである (core/ADR-0024)。
 * 互換面は合流1件の開始と終了を別々に受けるため、開始で受け取ったハンドルが
 * そのままその1件の終了と進捗報告の宛先になる。
 *
 * ViewModel は包み直さずに渡すため、Swift 側の解決キーは共有コードで定義したクラスの ObjC クラスになる。
 * 進捗の受け口 ([LoadingProgressReceiver]) は共有コード側の面で互換面からは見えないので、
 * 転送はこの面が組み立てた報告先を通して行う。
 *
 * この面は共有コードの内部にあり Swift / ObjC へは公開されないため、`@Throws` は書かない。
 * カスタム Loading の解決に失敗したときの [DialogException] は、公開契約 [KsLoading] を実装する
 * [GatewayKsLoading] を通って呼び出し元へ伝わる。
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosLoadingGateway(
    private val bridge: KSDInteropLoadingBridge,
) : LoadingGateway {

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        beginBuiltin(message, placement)
    }

    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        beginCustom(viewModel, placement)
    }

    override suspend fun hide() {
        suspendCoroutine { continuation ->
            bridge.hideWithCompletion { continuation.resume(Unit) }
        }
    }

    override suspend fun setMessage(message: String?) {
        suspendCoroutine { continuation ->
            bridge.setLoadingMessage(message) { continuation.resume(Unit) }
        }
    }

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T = runScope(beginBuiltin(message, placement), actionThread, action)

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T = runScope(beginCustom(viewModel, placement), actionThread, action)

    /** 既定ローディングで合流1件を開始する。 */
    private suspend fun beginBuiltin(
        message: String?,
        placement: DialogPlacement?,
    ): KSDInteropLoadingUseHandle =
        suspendCoroutine { continuation ->
            bridge.beginBuiltinWithMessage(message, placement = placement?.toInterop()) { handle, error ->
                continuation.resumeWith(begun(handle, error))
            }
        }

    /**
     * カスタム Loading で合流1件を開始する。
     *
     * 進捗の報告先は、その ViewModel が受け口を実装しているときだけ渡す。
     * 実装していなければ転送は行われず、誤りにもならない。
     */
    private suspend fun beginCustom(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
    ): KSDInteropLoadingUseHandle {
        val receiver = viewModel as? LoadingProgressReceiver
        val progress: ((Double) -> Unit)? = receiver?.let { { value: Double -> it.onProgress(value) } }
        return suspendCoroutine { continuation ->
            bridge.beginViewModel(
                viewModel,
                placement = placement?.toInterop(),
                progress = progress,
            ) { handle, error ->
                continuation.resumeWith(begun(handle, error))
            }
        }
    }

    /** 開始の通知を共有コードの契約 (ハンドル、または throw 系チャネル) に載せ替える。 */
    private fun begun(
        handle: KSDInteropLoadingUseHandle?,
        error: NSError?,
    ): Result<KSDInteropLoadingUseHandle> =
        if (handle != null) {
            Result.success(handle)
        } else {
            Result.failure(DialogException(error?.localizedDescription ?: UNKNOWN_FAILURE_MESSAGE))
        }

    /**
     * 合流1件を握ったまま処理を走らせ、成否によらず終了を1回だけ数える。
     *
     * 失敗を握り潰さずに伝播させつつ終了を数えるので、例外・キャンセルで表示が閉じ残らない。
     * 取り消された呼び出しでも終了を数え切れるよう、終了の受理は取り消しの対象から外す。
     *
     * 処理は呼び出し元の文脈ではなく、指定に応じた dispatcher へ移してから呼ぶ。
     * 呼び出し元のスレッドに関係なく、処理の最初の文が指定のスレッドで実行されるようにするため
     * (core/ADR-0037)。共有コードは UI スレッドを知らないので、切り替えはこの委譲面が持つ (kmp/ADR-0006)。
     *
     * 報告と終了の順序は互換面が保証する。互換面は報告と終了を呼ばれた順に 1 本の列へ積んでから
     * UI スレッドで受理するため、処理の中で (どのスレッドからでも) 報告を呼び終えてから処理が戻れば、
     * 終了はその報告の後に受理される。処理がどのスレッドで完了し、呼び出し元がどこで再開しても変わらない。
     */
    private suspend fun <T> runScope(
        handle: KSDInteropLoadingUseHandle,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        // 報告口は任意スレッドから呼べる。受理は UI スレッド上で呼ばれた順に直列化される
        val report: (Double) -> Unit = { progress -> bridge.reportProgress(progress, handle = handle) }
        try {
            val value = withContext(actionThread.dispatcher()) { action(report) }
            withContext(NonCancellable) { endUse(handle) }
            return value
        } catch (failure: Throwable) {
            withContext(NonCancellable) { endUse(handle) }
            throw failure
        }
    }

    /** 合流1件の終了を待つ。合流最後の1件なら器の撤去の完了まで待つ。 */
    private suspend fun endUse(handle: KSDInteropLoadingUseHandle) {
        suspendCoroutine { continuation ->
            bridge.endUse(handle) { continuation.resume(Unit) }
        }
    }

    /**
     * 処理を始めるスレッドの指定に対応する dispatcher。
     *
     * UI スレッドから既定の指定で呼ばれたときは、`immediate` によりメインキューへの積み直しを省いて
     * その場で処理を始める。
     */
    private fun LoadingActionThread.dispatcher(): CoroutineDispatcher =
        when (this) {
            LoadingActionThread.MAIN -> Dispatchers.Main.immediate
            LoadingActionThread.BACKGROUND -> Dispatchers.Default
        }

    private companion object {
        const val UNKNOWN_FAILURE_MESSAGE: String = "Failed to show the Loading."
    }
}
