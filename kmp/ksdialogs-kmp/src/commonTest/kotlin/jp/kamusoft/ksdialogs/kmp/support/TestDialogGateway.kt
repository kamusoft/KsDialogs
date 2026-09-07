package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogGateway
import jp.kamusoft.ksdialogs.kmp.DialogOutcome
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.SharedDialogViewRegistry

/** 検証用のレジストリハンドル。同一性の確認と ViewModel factory の登録に使う。 */
internal class TestDialogViewRegistry : SharedDialogViewRegistry()

/**
 * Native 実装の代わりに使う委譲面。
 *
 * 受け取った ViewModel を記録し、あらかじめ決めた結果を返す (または構成エラーを投げる)。
 *
 * @property registry この委譲面が指すレジストリハンドル
 * @param respond 委譲1回ごとの応答。例外を投げれば構成エラーの経路になる
 */
internal class TestDialogGateway(
    override val registry: SharedDialogViewRegistry = TestDialogViewRegistry(),
    private val respond: () -> DialogOutcome,
) : DialogGateway {
    /** 委譲された ViewModel を呼ばれた順に記録したもの。 */
    val presentedViewModels: MutableList<DialogViewModel<*>> = mutableListOf()

    /** 委譲された置き場所を呼ばれた順に記録したもの。show で指定がなければ null が入る。 */
    val presentedPlacements: MutableList<DialogPlacement?> = mutableListOf()

    override suspend fun present(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
    ): DialogOutcome {
        presentedViewModels += viewModel
        presentedPlacements += placement
        return respond()
    }

    internal companion object {
        /** 結果値つきで完了する委譲面。 */
        fun completing(value: Any?): TestDialogGateway =
            TestDialogGateway { DialogOutcome.Completed(value) }

        /** キャンセルで完了する委譲面。 */
        fun cancelling(): TestDialogGateway =
            TestDialogGateway { DialogOutcome.Cancelled }

        /** 構成エラーで失敗する委譲面。 */
        fun failing(message: String): TestDialogGateway =
            TestDialogGateway { throw DialogException(message) }
    }
}
