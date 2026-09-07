package jp.kamusoft.ksdialogs.kmp

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

/**
 * Native ライブラリへの委譲面。
 *
 * 共有コードと OS ごとの実装の継ぎ目で、結果値は型を消したまま運び、
 * 宣言結果型への復元は [GatewayKsDialog] が行う。
 * 差し替え可能にしてあるため、Native 実装なしで委譲の契約 (引数・結果・show との1対1) を検証できる。
 */
internal interface DialogGateway {
    /**
     * 委譲先のレジストリを指すハンドル。
     *
     * View factory の紐付けは委譲先 (Native ライブラリ) が持ち、ViewModel factory の表は
     * このハンドル自身が持つ (kmp/ADR-0006)。
     */
    val registry: SharedDialogViewRegistry

    /**
     * ViewModel の型で View を解決してダイアログを提示し、型を消した結果を返す。
     *
     * [placement] は show の引数で渡された置き場所で、null なら中身の View への添付が使われる。
     */
    suspend fun present(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
    ): DialogOutcome
}

/**
 * 結果の型消去表現。
 *
 * 公開 API の [DialogResult] と違い結果値の型を持たない、委譲面の輸送形。
 */
internal sealed interface DialogOutcome {
    /** 結果値つきの完了。 */
    class Completed(val value: Any?) : DialogOutcome

    /** 結果値を持たないキャンセル。 */
    data object Cancelled : DialogOutcome
}

/**
 * 委譲面の上に立つ [KsDialog] の実装。
 *
 * 型を消して運ばれた結果値を ViewModel の宣言結果型へ戻す役割と、
 * 型を渡す show の前段 (ViewModel の生成と configure) を持つ。
 */
internal class GatewayKsDialog(private val gateway: DialogGateway) : KsDialog {
    override val registry: DialogViewRegistry
        get() = gateway.registry

    @Throws(DialogException::class, CancellationException::class)
    override suspend fun <R> show(
        viewModel: DialogViewModel<R>,
        placement: DialogPlacement?,
    ): DialogResult<R> =
        when (val outcome = gateway.present(viewModel, placement)) {
            is DialogOutcome.Cancelled -> DialogResult.Cancelled
            is DialogOutcome.Completed -> {
                // 結果値は ViewModel が宣言した結果型に固定された報告口からしか入らないため、ここでの型は常に一致する
                @Suppress("UNCHECKED_CAST")
                DialogResult.Completed(outcome.value as R)
            }
        }

    override suspend fun <R, VM : DialogViewModel<R>> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): DialogResult<R> {
        // 生成と configure は呼び出し元の文脈でそのまま行い、完了してから提示の経路へ入る (kmp/ADR-0006)
        val viewModel = gateway.registry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        return show(viewModel, placement)
    }
}
