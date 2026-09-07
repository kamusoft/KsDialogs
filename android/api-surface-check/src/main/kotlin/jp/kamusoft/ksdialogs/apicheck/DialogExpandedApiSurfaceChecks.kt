package jp.kamusoft.ksdialogs.apicheck

import android.view.View
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.KsDialog
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.compose.showCompose

/** 真偽値の顔で宣言した、利用者が書くのと同じ形の ViewModel。結果型は書かない。 */
public class ConsumerSimpleDialogViewModel(public val message: String) : SimpleDialogViewModel

/** 非真偽値の結果型を宣言した、利用者が書くのと同じ形の ViewModel。 */
public class ConsumerTextDialogViewModel(public val message: String) : DialogViewModel<String>

/**
 * 真偽値の省略形・宣言的 UI・インライン show の公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、型引数を明示しなくても
 * 利用者が意図どおりに書けることを示す。型が意図と違えば代入先の型注釈で失敗する。
 */
public object DialogExpandedApiSurfaceChecks {

    /** 真偽値の顔で宣言した ViewModel の登録では、結果報告口が真偽値に型付く。 */
    public fun acceptsSimpleViewModelRegistration(registry: DialogViewRegistry) {
        registry.register(ConsumerSimpleDialogViewModel::class) { _, notifier ->
            val boundNotifier: DialogNotifier<Boolean> = notifier
            boundNotifier.complete(true)
            View(this)
        }
    }

    /** 真偽値の顔で宣言した ViewModel の show は、真偽値の結果を返す。 */
    public suspend fun acceptsSimpleViewModelShow(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerSimpleDialogViewModel("こんにちは"))

    /** 登録せずに factory をその場で渡して表示できる。 */
    public suspend fun acceptsInlineShow(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerSimpleDialogViewModel("こんにちは")) { _, notifier ->
            val boundNotifier: DialogNotifier<Boolean> = notifier
            boundNotifier.complete(true)
            View(this)
        }

    /** インライン show でも置き場所を引数で供給できる。 */
    public suspend fun acceptsInlineShowWithPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(
            ConsumerSimpleDialogViewModel("こんにちは"),
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
        ) { _, _ -> View(this) }

    /** 結果型を明示した ViewModel でも、インライン show の結果はその型で返る。 */
    public suspend fun acceptsInlineShowWithDeclaredResultType(dialogs: KsDialog): DialogResult<String> =
        dialogs.show(ConsumerTextDialogViewModel("こんにちは")) { _, notifier ->
            val boundNotifier: DialogNotifier<String> = notifier
            boundNotifier.complete("入力された文字列")
            View(this)
        }

    /** 宣言的 UI のコンテンツを登録できる。結果報告口は宣言結果型に固定される。 */
    public fun acceptsComposeRegistration(registry: DialogViewRegistry) {
        registry.registerCompose(ConsumerSimpleDialogViewModel::class) { _, notifier ->
            KsDialogAttributes(
                options = DialogOptions(isCanceledOnTouchOutside = false),
                placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
            )
            val boundNotifier: DialogNotifier<Boolean> = notifier
            boundNotifier.complete(true)
        }
    }

    /** 登録せずに宣言的 UI のコンテンツをその場で渡して表示できる。 */
    public suspend fun acceptsComposeInlineShow(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.showCompose(ConsumerSimpleDialogViewModel("こんにちは")) { _, notifier ->
            KsDialogAttributes(placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
            notifier.complete(true)
        }

    /** 宣言的 UI のインライン show でも置き場所を引数で供給できる。 */
    public suspend fun acceptsComposeInlineShowWithPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.showCompose(
            ConsumerSimpleDialogViewModel("こんにちは"),
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
        ) { _, _ -> }
}
