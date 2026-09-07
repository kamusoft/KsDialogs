package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * ローディング表示の既定エントリ。
 *
 * [Loading.instance] で手軽に呼び出せるほか、`Loading()` を [KsLoading] として DI 注入しても
 * 同じ状態 (合流カウント・表示世代・表示中のコンテンツ) を共有する (core/ADR-0002・0024)。
 *
 * この型は状態を持たず、すべての呼び出しをプロセス内で唯一の coordinator へ委譲する。
 * そのため既定シングルトンと注入したインスタンスを混ぜて使っても表示は1つに合流する。
 */
public class Loading internal constructor(
    internal val coordinator: LoadingCoordinator,
) : KsLoading {

    /** 既定の状態・レジストリ・取り付け先解決を使うインスタンスを作る。 */
    public constructor() : this(LoadingCoordinator.shared)

    override val registry: LoadingViewRegistry
        get() = coordinator.registry

    override var style: LoadingStyle
        get() = coordinator.settings.style
        set(value) {
            coordinator.settings.style = value
        }

    override var options: DialogOptions
        get() = coordinator.settings.options
        set(value) {
            coordinator.settings.options = value
        }

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        coordinator.beginUse(LoadingContentRequest.Builtin, message, placement)
    }

    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        coordinator.beginUse(LoadingContentRequest.Registered(viewModel), null, placement)
    }

    override suspend fun <VM : LoadingViewModel> show(
        viewModel: VM,
        placement: DialogPlacement?,
        factory: Context.(VM) -> View,
    ) {
        beginInlineUse(viewModel, placement, factory)
    }

    override suspend fun <VM : LoadingViewModel> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ) {
        beginTypedUse(viewModelClass, placement, configure)
    }

    override suspend fun hide() {
        coordinator.hide()
    }

    override suspend fun setMessage(message: String?) {
        coordinator.setMessage(message)
    }

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = coordinator.beginUse(LoadingContentRequest.Builtin, message, placement)
        return runScope(token, action)
    }

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = coordinator.beginUse(LoadingContentRequest.Registered(viewModel), null, placement)
        return runScope(token, action)
    }

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModel: VM,
        placement: DialogPlacement?,
        factory: Context.(VM) -> View,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = beginInlineUse(viewModel, placement, factory)
        return runScope(token, action)
    }

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = beginTypedUse(viewModelClass, placement, configure)
        return runScope(token, action)
    }

    /**
     * 型指定の表示を開始する。
     *
     * 解決は呼び出し時点のエントリのスナップショットで行い、以後の再登録には影響されない。
     * ViewModel の生成と configure を先に済ませてから状態の正へ渡すので、そこから先は
     * インスタンス渡しの表示とまったく同じ合流判定に入る (core/ADR-0035)。
     */
    private suspend fun <VM : LoadingViewModel> beginTypedUse(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): LoadingUseToken {
        viewModelClass.requireReferenceTypeViewModel()
        val entry = coordinator.registry.entry(viewModelClass)
        val viewModelFactory = entry?.viewModelFactory
            ?: throw DialogException.ViewModelFactoryNotRegistered(viewModelClass.viewModelTypeName)
        val viewFactory = entry.viewFactory
            ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)

        // 生成と configure は View factory と同じ UI スレッドで行い、
        // configure の完了までは中身の生成へ進まない (core/ADR-0019・0035)
        val viewModel = withContext(Dispatchers.Main.immediate) {
            // ViewModel factory はクラス参照と対で登録されるため、生成物の型は常に一致する
            @Suppress("UNCHECKED_CAST")
            val created = viewModelFactory.createViewModel() as VM
            configure?.invoke(created)
            created
        }
        return coordinator.beginUse(
            LoadingContentRequest.Resolved(viewModel, viewFactory),
            null,
            placement,
        )
    }

    /** 型消去した factory をそのまま状態の正へ渡す。レジストリは経由しない (core/ADR-0013)。 */
    private suspend fun <VM : LoadingViewModel> beginInlineUse(
        viewModel: VM,
        placement: DialogPlacement?,
        factory: Context.(VM) -> View,
    ): LoadingUseToken = coordinator.beginUse(
        LoadingContentRequest.Inline(viewModel, erasedLoadingViewFactory(factory)),
        null,
        placement,
    )

    /**
     * 合流1件を握ったまま処理を走らせ、成否によらず終了を1回だけ数える。
     *
     * 失敗を握り潰さずに伝播させつつ終了を数えるので、例外・キャンセルで表示が閉じ残らない。
     * 取り消された呼び出しでも終了を数え切れるよう、終了の受理は取り消しの対象から外す。
     */
    private suspend fun <T> runScope(
        token: LoadingUseToken,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        // 報告口は任意スレッドから呼べる。受理は UI スレッド上で呼ばれた順に直列化される
        val report: (Double) -> Unit = { progress -> coordinator.report(progress, token) }
        try {
            val value = action(report)
            withContext(NonCancellable) { coordinator.endUse(token) }
            return value
        } catch (failure: Throwable) {
            withContext(NonCancellable) { coordinator.endUse(token) }
            throw failure
        }
    }

    public companion object {
        /** 既定の singleton エントリ。 */
        public val instance: Loading = Loading()
    }
}
