package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.DialogViewRegistry
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.reflect.KClass

/**
 * ダイアログを表示せずに決まった結果を返す差し替え実装。
 *
 * @param result show が返す結果
 * @param fakeRegistry 差し替え時のレジストリ。ViewModel factory はここに登録する
 */
internal class FakeKsDialog(
    private val result: DialogResult<*>,
    private val fakeRegistry: FakeDialogViewRegistry = FakeDialogViewRegistry(),
) : KsDialog {
    override val registry: DialogViewRegistry
        get() = fakeRegistry

    /** show に渡された ViewModel を呼ばれた順に記録したもの。 */
    val shownViewModels: MutableList<DialogViewModel<*>> = mutableListOf()

    /** show に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<DialogPlacement?> = mutableListOf()

    override suspend fun <R> show(
        viewModel: DialogViewModel<R>,
        placement: DialogPlacement?,
    ): DialogResult<R> {
        shownViewModels += viewModel
        shownPlacements += placement
        @Suppress("UNCHECKED_CAST")
        return result as DialogResult<R>
    }

    override suspend fun <R, VM : DialogViewModel<R>> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): DialogResult<R> {
        val viewModel = fakeRegistry.viewModelFactories.create(viewModelClass)
        configure?.invoke(viewModel)
        return show(viewModel, placement)
    }
}

/**
 * プラットフォームの View 型を一切参照しない共有コード側の呼び出し役。
 *
 * 共有コードは中身の View を供給せず、ViewModel を渡して結果を待つだけで済むことを示すために使う。
 * このファイルが属する共有ソースには View 型そのものが存在しないため、
 * 「UI 層を参照しない」ことはコンパイル単位の構成として保証される。
 */
internal class TextPromptPresenter(private val dialogs: KsDialog) {
    /** 文字列の結果を宣言する ViewModel を表示し、宣言結果型のまま受け取る。 */
    suspend fun prompt(): String =
        when (val result = dialogs.show(StringTestDialogViewModel())) {
            is DialogResult.Completed -> result.value
            DialogResult.Cancelled -> ""
        }
}

/**
 * 契約 interface だけに依存する共有コード側の呼び出し役。
 *
 * ダイアログの結果で分岐するロジックが、表示なしで検証できることを示すために使う。
 */
internal class ConfirmationPresenter(private val dialogs: KsDialog) {
    /** 確認ダイアログの結果を、共有コードが扱う語彙へ変換する。 */
    suspend fun confirm(): String =
        when (val result = dialogs.show(BooleanTestDialogViewModel())) {
            is DialogResult.Completed -> if (result.value) "承諾" else "拒否"
            DialogResult.Cancelled -> "中断"
        }
}
