package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.ToastViewModel
import jp.kamusoft.ksdialogs.kmp.ToastViewRegistry
import kotlin.reflect.KClass

/**
 * 表示を伴わずに受理された Toast を書き留める差し替え実装。
 *
 * 表示そのものは Native 実装の担当なので、共有コード側の検証はこの面で行う。
 */
internal class FakeKsToast(
    private val fakeRegistry: FakeToastViewRegistry = FakeToastViewRegistry(),
) : KsToast {
    override val registry: ToastViewRegistry
        get() = fakeRegistry

    /** 受理された表示のメッセージを呼ばれた順に記録したもの。カスタム Toast なら null が入る。 */
    val shownMessages: MutableList<String?> = mutableListOf()

    /** 受理された表示のカスタム ViewModel を呼ばれた順に記録したもの。既定 View なら null が入る。 */
    val shownViewModels: MutableList<ToastViewModel?> = mutableListOf()

    /** 受理された表示の duration を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownDurations: MutableList<Int?> = mutableListOf()

    /** 受理された表示の置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<DialogPlacement?> = mutableListOf()

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        record(null, message, durationMs, placement)
    }

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        record(viewModel, null, durationMs, placement)
    }

    override fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int?,
        placement: DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ) {
        val viewModel = fakeRegistry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        show(viewModel, durationMs, placement)
    }

    private fun record(
        viewModel: ToastViewModel?,
        message: String?,
        durationMs: Int?,
        placement: DialogPlacement?,
    ) {
        shownViewModels += viewModel
        shownMessages += message
        shownDurations += durationMs
        shownPlacements += placement
    }
}

/** 検証用のカスタム Toast の ViewModel。レジストリの型キーとデータの運搬を兼ねる。 */
internal class PlainTestToastViewModel(
    /** 中身の View が読む文言。 */
    val message: String = "保存しました",
) : ToastViewModel

/**
 * プラットフォームの View 型を一切参照しない共有コード側の処理。
 *
 * 共有コードは中身の View もスタイルも供給せず、メッセージか ViewModel を渡すだけで
 * 通知を出せることを示すために使う。
 */
internal class SaveNotifier(private val toast: KsToast) {
    /** 保存の完了をデフォルト View で知らせる。 */
    fun notifySaved() {
        toast.show("保存しました", durationMs = 2000)
    }

    /** 保存の完了をカスタム Toast で知らせる。中身の登録は各 OS 側にある。 */
    fun notifySaved(viewModel: ToastViewModel) {
        toast.show(viewModel, placement = DialogPlacement(offsetY = 8.0))
    }
}
