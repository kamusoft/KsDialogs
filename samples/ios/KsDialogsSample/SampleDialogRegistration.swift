import KsDialogs

/// ViewModel 型と View factory の紐付け。
///
/// 利用者アプリと同じ側から、公開 product のレジストリへ登録する。
enum SampleDialogRegistration {
    /// この Sample が使うダイアログを登録する。
    @MainActor
    static func register() {
        Dialog.shared.registry.register(BasicDialogViewModel.self) { viewModel, notifier in
            BasicDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
        }

        Dialog.shared.registry.register(LayoutDialogViewModel.self) { viewModel, notifier in
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

        // SwiftUI の View をそのまま返す登録。UIView を返す登録と同じ名前で、戻り値の型だけが違う
        Dialog.shared.registry.register(DeclarativeDialogViewModel.self) { viewModel, notifier in
            DeclarativeDialogCard(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
        }

        Dialog.shared.registry.register(TransitionDialogViewModel.self) { viewModel, notifier in
            let view = TransitionDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete(true) }
            )
            // 演出は show の引数では渡せないため、中身への添付で供給する (core/ADR-0017)
            view.ksDialogTransition = viewModel.transition
            return view
        }

        // ViewModel 主導の呼び出し面。中身は ViewModel だけを受け取り、報告は ViewModel 自身が行う
        Dialog.shared.registry.register(ModelDialogViewModel.self) { viewModel in
            ModelDialogCardHostView(viewModel: viewModel)
        }
        // ViewModel factory も登録しておくと、型を渡す show で ViewModel の生成ごと任せられる
        Dialog.shared.registry.register(ModelDialogViewModel.self) {
            ModelDialogViewModel()
        }

        // 結果型は ViewModel の宣言から導かれるため、報告口も文字列の報告口になる
        Dialog.shared.registry.register(TextInputDialogViewModel.self) { viewModel, notifier in
            TextInputDialogCardHostView(
                message: viewModel.message,
                onCancel: { notifier.cancel() },
                onComplete: { notifier.complete($0) }
            )
        }
    }
}
