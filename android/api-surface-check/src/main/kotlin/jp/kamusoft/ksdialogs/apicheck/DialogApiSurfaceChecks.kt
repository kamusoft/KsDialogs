package jp.kamusoft.ksdialogs.apicheck

import android.content.Context
import android.view.View
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.KsDialog
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement

/** 利用者が書くのと同じ形の ViewModel。真偽値の結果を宣言する。 */
public class ConsumerDialogViewModel(public val message: String) : DialogViewModel<Boolean>

/**
 * 公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 */
public object DialogApiSurfaceChecks {

    /** メタ属性を供給しない既存の呼び出しがそのまま通る。 */
    public suspend fun acceptsShowWithoutPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel("こんにちは"))

    /** placement は show の引数で供給できる。 */
    public suspend fun acceptsShowWithPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(
            ConsumerDialogViewModel("こんにちは"),
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0),
        )

    /** options と placement は中身の View への添付で供給できる。 */
    public fun acceptsAttachmentOnContentView(context: Context) {
        val contentView = View(context)
        contentView.ksDialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            isCanceledOnTouchOutside = false,
        )
        contentView.ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END)
    }

    /** 添付した値は同じ名前で読み出せる。 */
    public fun acceptsReadingAttachedAttributes(contentView: View): Pair<DialogOptions?, DialogPlacement?> =
        contentView.ksDialogOptions to contentView.ksDialogPlacement

    /** ViewModel が宣言した結果型でそのまま受け取れる。 */
    public suspend fun acceptsDeclaredResultType(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerDialogViewModel("こんにちは"))

    /** 結果報告口も宣言結果型に固定される。 */
    public fun notifierIsBoundToDeclaredResultType(registry: DialogViewRegistry) {
        registry.register(ConsumerDialogViewModel::class) { _, notifier ->
            val boundNotifier: DialogNotifier<Boolean> = notifier
            boundNotifier.complete(true)
            View(this)
        }
    }
}
