package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel
import jp.kamusoft.ksdialogs.kmp.LoadingViewRegistry
import kotlin.reflect.KClass

/**
 * 表示を伴わずに合流の開始・終了と進捗の報告を書き留める差し替え実装。
 *
 * 表示そのものは Native 実装の担当なので、共有コード側の検証はこの面で行う。
 * 進捗の報告は表示中のカスタム ViewModel が受け口を実装していれば転送し、
 * Native 側の転送と同じ観察ができるようにする。
 */
internal class FakeKsLoading(
    private val fakeRegistry: FakeLoadingViewRegistry = FakeLoadingViewRegistry(),
) : KsLoading {
    override val registry: LoadingViewRegistry
        get() = fakeRegistry

    /** 開始された表示に渡されたメッセージを呼ばれた順に記録したもの。 */
    val startedMessages: MutableList<String?> = mutableListOf()

    /** 開始された表示に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val startedPlacements: MutableList<DialogPlacement?> = mutableListOf()

    /** 開始された表示のカスタム ViewModel を呼ばれた順に記録したもの。既定ローディングなら null が入る。 */
    val startedViewModels: MutableList<LoadingViewModel?> = mutableListOf()

    /** 報告された進捗を呼ばれた順に記録したもの。 */
    val reportedProgress: MutableList<Double> = mutableListOf()

    /** 更新されたメッセージを呼ばれた順に記録したもの。 */
    val updatedMessages: MutableList<String?> = mutableListOf()

    /** 表示中かどうか。開始で立ち、終了と [hide] で下りる。 */
    var isPresenting: Boolean = false
        private set

    /** [hide] が呼ばれた回数。 */
    var hideCount: Int = 0
        private set

    private var presentedViewModel: LoadingViewModel? = null

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        begin(null, message, placement)
    }

    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        begin(viewModel, null, placement)
    }

    override suspend fun <VM : LoadingViewModel> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ) {
        show(configured(viewModelClass, configure), placement)
    }

    override suspend fun hide() {
        hideCount += 1
        isPresenting = false
        presentedViewModel = null
    }

    override suspend fun setMessage(message: String?) {
        updatedMessages += message
    }

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        begin(null, message, placement)
        return runScope(action)
    }

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        begin(viewModel, null, placement)
        return runScope(action)
    }

    override suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = start(configured(viewModelClass, configure), placement, action)

    /** 登録済みの factory で ViewModel を作り、configure を適用して返す。 */
    private suspend fun <VM : LoadingViewModel> configured(
        viewModelClass: KClass<VM>,
        configure: (suspend (VM) -> Unit)?,
    ): VM {
        val viewModel = fakeRegistry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        return viewModel
    }

    private fun begin(viewModel: LoadingViewModel?, message: String?, placement: DialogPlacement?) {
        startedViewModels += viewModel
        startedMessages += message
        startedPlacements += placement
        presentedViewModel = viewModel
        isPresenting = true
    }

    /** 表示を握ったまま処理を走らせ、成否によらず終了を1回だけ数える。 */
    private suspend fun <T> runScope(action: suspend ((Double) -> Unit) -> T): T {
        val report: (Double) -> Unit = { progress ->
            reportedProgress += progress
            (presentedViewModel as? LoadingProgressReceiver)?.onProgress(progress)
        }
        try {
            return action(report)
        } finally {
            isPresenting = false
            presentedViewModel = null
        }
    }
}

/**
 * プラットフォームの View 型を一切参照しない共有コード側の処理。
 *
 * 共有コードは中身の View を供給せず、処理をスコープ形へ渡して戻り値を受け取るだけで済むことを
 * 示すために使う。このファイルが属する共有ソースには View 型そのものが存在しないため、
 * 「UI 層を参照しない」ことはコンパイル単位の構成として保証される。
 */
internal class UploadPresenter(private val loading: KsLoading) {
    /** 既定ローディングを出したまま処理を走らせ、その戻り値を共有コードが受け取る。 */
    suspend fun upload(steps: Int): String =
        loading.start(message = "アップロード中") { report ->
            repeat(steps) { index -> report((index + 1).toDouble() / steps) }
            "完了"
        }

    /** カスタム Loading を出したまま処理を走らせる。進捗は ViewModel の受け口へ届く。 */
    suspend fun upload(viewModel: LoadingViewModel, steps: Int): String =
        loading.start(viewModel) { report ->
            repeat(steps) { index -> report((index + 1).toDouble() / steps) }
            "完了"
        }
}
