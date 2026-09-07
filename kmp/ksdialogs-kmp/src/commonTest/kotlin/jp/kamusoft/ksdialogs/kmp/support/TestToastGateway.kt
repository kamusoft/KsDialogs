package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.ToastGateway
import jp.kamusoft.ksdialogs.kmp.ToastViewModel

/**
 * Native 実装の代わりに使う Toast の委譲面。
 *
 * 受理された表示の ViewModel・duration・置き場所を呼ばれた順に記録する。
 */
internal class TestToastGateway : ToastGateway {
    /** 受理された表示の ViewModel を呼ばれた順に記録したもの。デフォルト View なら null が入る。 */
    val shownViewModels: MutableList<ToastViewModel?> = mutableListOf()

    /** 受理された表示の duration を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownDurations: MutableList<Int?> = mutableListOf()

    /** 受理された表示の置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<DialogPlacement?> = mutableListOf()

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        record(null, durationMs, placement)
    }

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        record(viewModel, durationMs, placement)
    }

    private fun record(viewModel: ToastViewModel?, durationMs: Int?, placement: DialogPlacement?) {
        shownViewModels += viewModel
        shownDurations += durationMs
        shownPlacements += placement
    }
}
