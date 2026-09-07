package jp.kamusoft.ksdialogs.kmp

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

/**
 * ローディングの Native ライブラリへの委譲面。
 *
 * 共有コードと OS ごとの実装の継ぎ目で、受け取るのは ViewModel の実例だけである。
 * 差し替え可能にしてあるため、Native 実装なしで委譲の契約 (引数・進捗・戻り値の対応) を検証できる。
 */
internal interface LoadingGateway {
    /** 既定ローディングを表示し、合流1件を開始する。 */
    suspend fun show(message: String?, placement: DialogPlacement?)

    /** 登録済みのカスタム Loading View を表示し、合流1件を開始する。 */
    suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?)

    /** 表示を閉じる。 */
    suspend fun hide()

    /** 表示中のメッセージを更新する。 */
    suspend fun setMessage(message: String?)

    /** 既定ローディングを表示したまま処理を実行し、その戻り値を返す。 */
    suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T

    /** 登録済みのカスタム Loading View を表示したまま処理を実行し、その戻り値を返す。 */
    suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T
}

/**
 * 委譲面の上に立つ [KsLoading] の実装。
 *
 * ViewModel factory の表を持ち、型を渡す表示の前段 (ViewModel の生成と configure) を担う。
 * 生成と configure が済んだ ViewModel は、実例を渡す表示とまったく同じ経路で委譲面へ入る。
 */
internal class GatewayKsLoading(private val gateway: LoadingGateway) : KsLoading {
    // Dialog と違い、レジストリを Native 側ハンドル (`SharedDialogViewRegistry` を継承した Android の
    // レジストリ) と同一性を合わせる必要がないため、ViewModel factory 表の持ち主はこの前段になる
    private val sharedRegistry = SharedLoadingViewRegistry()

    override val registry: LoadingViewRegistry
        get() = sharedRegistry

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        gateway.show(message, placement)
    }

    @Throws(DialogException::class, CancellationException::class)
    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        gateway.show(viewModel, placement)
    }

    override suspend fun <VM : LoadingViewModel> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ) {
        gateway.show(configured(viewModelClass, configure), placement)
    }

    override suspend fun hide() {
        gateway.hide()
    }

    override suspend fun setMessage(message: String?) {
        gateway.setMessage(message)
    }

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = gateway.start(message, placement, action)

    @Throws(DialogException::class, CancellationException::class)
    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = gateway.start(viewModel, placement, action)

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = gateway.start(configured(viewModelClass, configure), placement, action)

    /**
     * ViewModel factory で ViewModel を作り、configure を適用して返す。
     *
     * 呼び出し元の文脈でそのまま実行し、UI スレッドへは移さない (kmp/ADR-0006)。
     */
    private suspend fun <VM : LoadingViewModel> configured(
        viewModelClass: KClass<VM>,
        configure: (suspend (VM) -> Unit)?,
    ): VM {
        val viewModel = sharedRegistry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        return viewModel
    }
}
