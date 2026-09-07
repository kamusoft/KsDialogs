package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.LoadingGateway
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

/**
 * Native 実装の代わりに使うローディングの委譲面。
 *
 * 受け取った ViewModel と置き場所を記録し、スコープ形は渡された処理をそのまま走らせる。
 *
 * @param failCustom カスタム Loading の表示で構成エラーを起こすなら、その説明文
 */
internal class TestLoadingGateway(
    private val failCustom: String? = null,
) : LoadingGateway {
    /** 表示に渡された ViewModel を呼ばれた順に記録したもの。既定ローディングなら null が入る。 */
    val shownViewModels: MutableList<LoadingViewModel?> = mutableListOf()

    /** 表示に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<DialogPlacement?> = mutableListOf()

    /** スコープ形で表示が始まった回数。 */
    var scopedStartCount: Int = 0
        private set

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        record(null, placement)
    }

    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        record(viewModel, placement)
    }

    override suspend fun hide() = Unit

    override suspend fun setMessage(message: String?) = Unit

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        record(null, placement)
        scopedStartCount += 1
        return action {}
    }

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        record(viewModel, placement)
        scopedStartCount += 1
        return action {}
    }

    private fun record(viewModel: LoadingViewModel?, placement: DialogPlacement?) {
        if (viewModel != null && failCustom != null) throw DialogException(failCustom)
        shownViewModels += viewModel
        shownPlacements += placement
    }
}
