package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogTransition

/**
 * ViewModel 型と View factory の紐付け。
 *
 * 利用者アプリと同じ側から、公開 product のレジストリへ登録する。
 */
internal object SampleDialogRegistration {
    /** この Sample が使うダイアログを登録する。 */
    fun register() {
        Dialog.instance.registry.register(BasicDialogViewModel::class) { viewModel, notifier ->
            BasicDialogCardView(
                context = this,
                message = viewModel.message,
                onCancel = { notifier.cancel() },
                onComplete = { notifier.complete(true) },
            )
        }

        Dialog.instance.registry.register(LayoutDialogViewModel::class) { viewModel, notifier ->
            LayoutDialogCardView(
                context = this,
                message = viewModel.message,
                onCancel = { notifier.cancel() },
                onComplete = { notifier.complete(true) },
            ).apply {
                // 基準領域は中身の性質として扱う静的メタ属性なので、View への添付で供給する
                ksDialogOptions = DialogOptions(
                    layoutArea = if (viewModel.usesVisibleArea) {
                        DialogLayoutArea.VISIBLE_AREA
                    } else {
                        DialogLayoutArea.WINDOW
                    },
                )
            }
        }

        Dialog.instance.registry.register(TransitionDialogViewModel::class) { viewModel, notifier ->
            TransitionDialogCardView(
                context = this,
                message = viewModel.message,
                onCancel = { notifier.cancel() },
                onComplete = { notifier.complete(true) },
            ).apply {
                // 演出は show の引数では渡せないため、中身への添付で供給する (core/ADR-0017)
                ksDialogTransition = viewModel.transition
            }
        }

        // Compose のコンテンツを渡す登録。View を返す登録とは別名で、中身の書き方だけが違う
        Dialog.instance.registry.registerCompose(DeclarativeDialogViewModel::class) { viewModel, notifier ->
            DeclarativeDialogCard(
                message = viewModel.message,
                onCancel = { notifier.cancel() },
                onComplete = { notifier.complete(true) },
            )
        }

        // ViewModel 主導の呼び出し面。中身は ViewModel だけを受け取るので、
        // (Context, VM) を受けるコンストラクタの参照をそのまま factory として渡せる
        Dialog.instance.registry.register(ModelDialogViewModel::class, ::ModelDialogCardView)
        // ViewModel factory も登録しておくと、型を渡す show で ViewModel の生成ごと任せられる
        Dialog.instance.registry.registerViewModel(ModelDialogViewModel::class) {
            ModelDialogViewModel()
        }

        // 結果型は ViewModel の宣言から導かれるため、報告口も文字列の報告口になる
        Dialog.instance.registry.register(TextInputDialogViewModel::class) { viewModel, notifier ->
            TextInputDialogCardView(
                context = this,
                message = viewModel.message,
                onCancel = { notifier.cancel() },
                onComplete = { notifier.complete(it) },
            )
        }
    }
}
