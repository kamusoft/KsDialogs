package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
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
        action: suspend ((Double) -> Unit) -> T,
    ): T = runScope(beginBuiltin(message, placement), action)

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = runScope(beginCustom(viewModel, placement), action)

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
     */
    private suspend fun <T> runScope(
        handle: KSDInteropLoadingUseHandle,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        // 報告口は任意スレッドから呼べる。受理は UI スレッド上で呼ばれた順に直列化される
        val report: (Double) -> Unit = { progress -> bridge.reportProgress(progress, handle = handle) }
        try {
            val value = action(report)
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

    private companion object {
        const val UNKNOWN_FAILURE_MESSAGE: String = "Failed to show the Loading."
    }
}
