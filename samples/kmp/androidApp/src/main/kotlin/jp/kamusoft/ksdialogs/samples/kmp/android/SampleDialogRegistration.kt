package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogTransition
import jp.kamusoft.ksdialogs.samples.kmp.BasicDialogViewModel
import jp.kamusoft.ksdialogs.samples.kmp.DeclarativeDialogViewModel
import jp.kamusoft.ksdialogs.samples.kmp.LayoutDialogViewModel
import jp.kamusoft.ksdialogs.samples.kmp.ModelDialogViewModel
import jp.kamusoft.ksdialogs.samples.kmp.SampleText
import jp.kamusoft.ksdialogs.samples.kmp.TextInputDialogViewModel
import jp.kamusoft.ksdialogs.samples.kmp.TransitionDialogViewModel

/**
 * 共有コードの ViewModel 型と Android の View factory の紐付け。
 *
 * View の型は OS ごとに異なるため、登録は Android Native API に対して行う。
 * 共有コードで定義した ViewModel のクラスがそのまま登録キーになる。
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
                ksDialogTransition = viewModel.preset.transition(
                    durationMilliseconds = viewModel.durationMilliseconds,
                    easing = viewModel.easing,
                )
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

        // 中身は ViewModel だけを受け取り、報告口は表示中の ViewModel から引く。
        // 共有コードの ViewModel でも Native の拡張がそのまま効くため、専用の手当ては要らない
        Dialog.instance.registry.register(ModelDialogViewModel::class, ::ModelDialogCardView)

        // 結果型は共有コードの ViewModel の宣言から導かれるため、報告口も文字列の報告口になる
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
