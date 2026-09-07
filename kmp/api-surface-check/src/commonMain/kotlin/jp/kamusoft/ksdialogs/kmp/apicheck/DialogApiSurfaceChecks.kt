package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.KsDialog

/** 利用者が書くのと同じ形の ViewModel。真偽値の結果を宣言する。 */
public class ConsumerDialogViewModel : DialogViewModel<Boolean> {
    /** 表示前に整えられる状態。 */
    public var message: String = ""
}

/**
 * 共有コードの公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 */
public object DialogApiSurfaceChecks {

    /** placement を供給しない既存の呼び出しがそのまま通る。 */
    public suspend fun acceptsShowWithoutPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel())

    /** placement は show の引数で供給できる。 */
    public suspend fun acceptsShowWithPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(
            ConsumerDialogViewModel(),
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0),
        )

    /** ViewModel が宣言した結果型でそのまま受け取れる。 */
    public suspend fun acceptsDeclaredResultType(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel())

    /** ViewModel factory はラムダでもコンストラクタ参照でも登録できる。 */
    public fun PB_KT_10_registersViewModelFactory(dialogs: KsDialog) {
        dialogs.registry.registerViewModel(ConsumerDialogViewModel::class) { ConsumerDialogViewModel() }
        dialogs.registry.registerViewModel(ConsumerDialogViewModel::class, ::ConsumerDialogViewModel)
    }

    /** 型を渡す show は configure も placement も省略できる。 */
    public suspend fun PB_KT_10_acceptsTypedShow(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel::class)

    /** 型を渡す show には placement と suspend の configure を渡せる。 */
    public suspend fun PB_KT_10_acceptsTypedShowWithPlacementAndConfigure(
        dialogs: KsDialog,
    ): DialogResult<Boolean> =
        dialogs.show(
            ConsumerDialogViewModel::class,
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0),
        ) { viewModel ->
            viewModel.message = loadMessage()
        }

    /** configure から suspend 関数を呼べることを示すための取得処理。 */
    private suspend fun loadMessage(): String = kotlin.run { "読み込んだ文言" }
}
