package jp.kamusoft.ksdialogs.kmp

import kotlin.reflect.KClass

/**
 * Toast の Native ライブラリへの委譲面。
 *
 * 共有コードと OS ごとの実装の継ぎ目で、受け取るのは ViewModel の実例だけである。
 * 差し替え可能にしてあるため、Native 実装なしで委譲の契約 (引数の対応・失敗の伝播) を検証できる。
 */
internal interface ToastGateway {
    /** デフォルト View でメッセージを表示する。 */
    fun show(message: String, durationMs: Int?, placement: DialogPlacement?)

    /** 登録済みのカスタム Toast View を表示する。 */
    fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?)
}

/**
 * 委譲面の上に立つ [KsToast] の実装。
 *
 * ViewModel factory の表を持ち、型を渡す表示の前段 (ViewModel の生成と configure) を担う。
 * 生成と configure が済んだ ViewModel は、実例を渡す表示とまったく同じ経路で委譲面へ入る。
 */
internal class GatewayKsToast(private val gateway: ToastGateway) : KsToast {
    // Dialog と違い、レジストリを Native 側ハンドル (`SharedDialogViewRegistry` を継承した Android の
    // レジストリ) と同一性を合わせる必要がないため、ViewModel factory 表の持ち主はこの前段になる
    private val sharedRegistry = SharedToastViewRegistry()

    override val registry: ToastViewRegistry
        get() = sharedRegistry

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        gateway.show(message, durationMs, placement)
    }

    @Throws(DialogException::class)
    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        gateway.show(viewModel, durationMs, placement)
    }

    override fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int?,
        placement: DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ) {
        // 生成と configure は呼び出し元のスレッドでそのまま行うため、失敗は同期に伝播する (kmp/ADR-0006)
        val viewModel = sharedRegistry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        gateway.show(viewModel, durationMs, placement)
    }
}
