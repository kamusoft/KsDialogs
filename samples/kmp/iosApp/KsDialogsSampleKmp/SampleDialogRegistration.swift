import KsDialogs
import SampleShared
import UIKit

/// 共有コードの ViewModel 型と iOS の View factory の紐付け。
///
/// View の型は OS ごとに異なるため、登録は iOS Native ライブラリに対して行う。
/// 共有コードで定義した ViewModel のクラスがそのまま登録キーになり、
/// 共有 Presenter からの show と同じレジストリを引く。
///
/// 共有コードの ViewModel は Swift の ViewModel 契約に準拠しないため、結果型は `result:` で渡す。
/// 省略した場合の結果は真偽値になる (core/ADR-0012)。
enum SampleDialogRegistration {
    /// この Sample が使うダイアログを登録する。
    @MainActor
    static func register() {
        Dialog.shared.kmp.register(BasicDialogViewModel.self) { viewModel, notifier in
            BasicDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
        }

        Dialog.shared.kmp.register(LayoutDialogViewModel.self) { viewModel, notifier in
            let view = LayoutDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
            // 基準領域は中身の性質として扱う静的メタ属性なので、View への添付で供給する
            view.ksDialogOptions = DialogOptions(
                layoutArea: viewModel.usesVisibleArea ? .visibleArea : .window
            )
            return view
        }

        Dialog.shared.kmp.register(TransitionDialogViewModel.self) { viewModel, notifier in
            let view = TransitionDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
            // 演出は show の引数では渡せないため、中身への添付で供給する (core/ADR-0017)
            view.ksDialogTransition = SampleTransitionChoice.of(viewModel.preset).transition(
                durationMilliseconds: Int(viewModel.durationMilliseconds),
                easing: SampleEasingChoice.of(viewModel.easing)
            )
            return view
        }

        // SwiftUI の View をそのまま返す登録。UIView を返す登録と同じ名前で、戻り値の型だけが違う
        Dialog.shared.kmp.register(DeclarativeDialogViewModel.self) { viewModel, notifier in
            DeclarativeDialogCard(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
        }

        // 中身は ViewModel だけを受け取り、報告口は KMP 面のアクセサから引く (core/ADR-0018)
        Dialog.shared.kmp.register(ModelDialogViewModel.self) { viewModel in
            ModelDialogCardHostView(viewModel: viewModel)
        }

        // 共有コードの ViewModel が宣言する結果型を渡すと、報告口も文字列の報告口になる
        Dialog.shared.kmp.register(TextInputDialogViewModel.self, result: String.self) { viewModel, notifier in
            TextInputDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete($0) }
            )
        }
    }
}
