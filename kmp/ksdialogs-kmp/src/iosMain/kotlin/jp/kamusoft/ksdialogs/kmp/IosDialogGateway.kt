package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignment
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentCenter
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentEnd
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentFill
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogAlignmentStart
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogBridge
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogPlacement
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResult
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindCancelled
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindCompleted
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindError

/**
 * iOS Native ライブラリのレジストリを指すハンドル。
 *
 * View factory の登録は Swift 側のレジストリに対して行い、共有コードからはその同一性の確認に使う。
 * ViewModel factory の表は共有コード側 (このハンドル自身) が持つ (kmp/ADR-0006)。
 * 互換面はレジストリを ObjC に出さないため、ハンドルは共有インスタンス1個で足りる。
 */
internal object IosDialogViewRegistry : SharedDialogViewRegistry()

/**
 * show 1回分の取り消し操作。
 *
 * 「その1枚だけを閉じ、結果を cancelled として確定させる」操作を表す (kmp/ADR-0005)。
 * 互換面が返す show ハンドルは ObjC 側の型なので、共有コードではこの面として扱う。
 */
internal fun interface IosDialogShowCancellation {
    /** この show が表示しているダイアログを閉じる。結果が確定済みなら何も起こらない。 */
    fun cancel()
}

/**
 * ViewModel を渡してダイアログの表示を始め、結果を1回だけ返す面。
 *
 * [IosDialogGateway] が委譲先 (iOS Native ライブラリの互換面) に触れる唯一の口で、
 * 差し替え可能にしてあるため、実提示なしで委譲の契約 (取り消しの追随・結果の1回配送) を検証できる。
 */
internal fun interface IosDialogShowSurface {
    /**
     * 表示を始め、[completion] へ結果を1回だけ返す。
     *
     * @param placement show の引数で渡された置き場所。null なら中身の View への添付が使われる
     * @return この1回の show を取り消すための操作
     */
    fun show(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
        completion: (Result<DialogOutcome>) -> Unit,
    ): IosDialogShowCancellation
}

/**
 * iOS Native ライブラリの ObjC 互換面へ委譲する表示面。
 *
 * 互換面は結果を型消去して運ぶため (kmp/ADR-0002)、判別と結果値をここで委譲面の輸送形に戻す。
 * ViewModel は包み直さずに渡すため、Swift 側の解決キーは共有コードで定義したクラスの ObjC クラスになる。
 */
@OptIn(ExperimentalForeignApi::class)
internal class InteropDialogShowSurface(
    private val bridge: KSDInteropDialogBridge,
) : IosDialogShowSurface {
    override fun show(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
        completion: (Result<DialogOutcome>) -> Unit,
    ): IosDialogShowCancellation {
        val handle = bridge.showViewModel(viewModel, placement = placement?.toInterop()) { result ->
            completion(
                if (result == null) {
                    Result.failure(DialogException(MISSING_RESULT_MESSAGE))
                } else {
                    runCatching { outcomeOf(result) }
                },
            )
        }
        return IosDialogShowCancellation { handle.cancel() }
    }

    private fun outcomeOf(result: KSDInteropDialogResult): DialogOutcome =
        when (result.kind) {
            KSDInteropDialogResultKindCompleted -> DialogOutcome.Completed(result.value)

            KSDInteropDialogResultKindCancelled -> DialogOutcome.Cancelled

            KSDInteropDialogResultKindError -> throw DialogException(
                result.error?.localizedDescription ?: UNKNOWN_FAILURE_MESSAGE,
            )

            else -> throw DialogException(UNKNOWN_FAILURE_MESSAGE)
        }

    private companion object {
        const val MISSING_RESULT_MESSAGE: String = "No Dialog result was delivered."
        const val UNKNOWN_FAILURE_MESSAGE: String = "Failed to show the Dialog."
    }
}

/**
 * iOS 向けの委譲面。表示は [IosDialogShowSurface] に任せ、ここは suspend の作法との橋渡しだけを担う。
 *
 * 呼び出し元のコルーチンがキャンセルされたときは、その show の取り消し操作を通じて
 * 当該ダイアログだけを閉じる。呼び出し元にはコルーチン規約どおり `CancellationException` が伝播し、
 * 内部の結果は cancelled で確定する (kmp/ADR-0005)。
 */
internal class IosDialogGateway(
    private val surface: IosDialogShowSurface,
) : DialogGateway {
    /** 既定の委譲先である ObjC 互換面を使う。 */
    @OptIn(ExperimentalForeignApi::class)
    constructor(bridge: KSDInteropDialogBridge) : this(InteropDialogShowSurface(bridge))

    override val registry: SharedDialogViewRegistry = IosDialogViewRegistry

    override suspend fun present(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
    ): DialogOutcome =
        suspendCancellableCoroutine { continuation ->
            val cancellation = surface.show(viewModel, placement) { continuation.resumeWith(it) }
            // 取り消しは表示が始まる前に来ることもあるため、取り消し操作を得た時点で必ず結び付ける。
            // 結果が確定済みなら取り消しは何も起こさず、確定後に届いた結果は配送されない
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
}

/**
 * 共有コードの置き場所を互換面の輸送形へ写す。
 *
 * 互換面はジェネリクスも Kotlin の値型も運べないため、判別2つと数値2つに開いて渡す。
 * 値の正規化や配置の計算は iOS Native ライブラリの責務なので、ここでは型だけを移し替える (core/ADR-0001)。
 */
@OptIn(ExperimentalForeignApi::class)
internal fun DialogPlacement.toInterop(): KSDInteropDialogPlacement =
    KSDInteropDialogPlacement(
        horizontalAlignment = horizontalAlignment.toInterop(),
        verticalAlignment = verticalAlignment.toInterop(),
        offsetX = offsetX,
        offsetY = offsetY,
    )

/** 共有コードの配置を互換面の判別へ写す。 */
@OptIn(ExperimentalForeignApi::class)
private fun DialogAlignment.toInterop(): KSDInteropDialogAlignment =
    when (this) {
        DialogAlignment.START -> KSDInteropDialogAlignmentStart
        DialogAlignment.CENTER -> KSDInteropDialogAlignmentCenter
        DialogAlignment.END -> KSDInteropDialogAlignmentEnd
        DialogAlignment.FILL -> KSDInteropDialogAlignmentFill
    }
