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
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = coordinator.beginUse(LoadingContentRequest.Builtin, message, placement)
        return runScope(token, actionThread, action)
    }

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = coordinator.beginUse(LoadingContentRequest.Registered(viewModel), null, placement)
        return runScope(token, actionThread, action)
    }

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModel: VM,
        placement: DialogPlacement?,
        factory: Context.(VM) -> View,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = beginInlineUse(viewModel, placement, factory)
        return runScope(token, actionThread, action)
    }

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        val token = beginTypedUse(viewModelClass, placement, configure)
        return runScope(token, actionThread, action)
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
     *
     * 処理は呼び出し元の文脈ではなく、指定に応じた dispatcher へ移してから呼ぶ。
     * 呼び出し元のスレッドに関係なく、処理の最初の文が指定のスレッドで実行されるようにするため
     * (core/ADR-0037)。UI スレッドから呼ばれた既定の指定では `immediate` により dispatch を省き、
     * その場で処理を始める。
     */
    private suspend fun <T> runScope(
        token: LoadingUseToken,
        actionThread: LoadingActionThread,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        // 報告口は任意スレッドから呼べる。受理は UI スレッド上で呼ばれた順に直列化される
        val report: (Double) -> Unit = { progress -> coordinator.report(progress, token) }
        val dispatcher = when (actionThread) {
            LoadingActionThread.MAIN -> Dispatchers.Main.immediate
            LoadingActionThread.BACKGROUND -> Dispatchers.Default
        }
        try {
            val value = withContext(dispatcher) { action(report) }
            endUseAfterPendingReports(token)
            return value
        } catch (failure: Throwable) {
            endUseAfterPendingReports(token)
            throw failure
        }
    }

    /**
     * 処理の中から発行済みの報告の受理を済ませてから、合流1件の終了を受理する。
     *
     * 報告は発行したその場で順序付きの列に積まれ、UI スレッド上で順に受理される。
     * 処理がどのスレッドで完了し、呼び出し元がどこで再開しても、終了はその列の区切りまで
     * 受理が進むのを待ってから受理するので、最後の報告を追い越さない。
     * 取り消された呼び出しでも終了を数え切れるよう、取り消しの対象からは外す。
     */
    private suspend fun endUseAfterPendingReports(token: LoadingUseToken) {
        withContext(NonCancellable) {
            coordinator.awaitAcceptedReports()
            coordinator.endUse(token)
        }
    }

    public companion object {
        /** 既定の singleton エントリ。 */
        public val instance: Loading = Loading()
    }
}
