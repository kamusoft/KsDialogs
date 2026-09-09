import VerificationApp
import VerificationShared

/// 共有コードの ViewModel 型と iOS の View factory の紐付け。
///
/// View の型は OS ごとに異なるため、登録は iOS Native ライブラリに対して行う。
/// 共有コードで定義した ViewModel のクラスがそのまま登録キーになり、
/// 共有コードからの show と同じレジストリを引く。
///
/// 共有コードの ViewModel は Swift の ViewModel 契約に準拠しないため、結果型は `result:` で渡す。
/// 最小例の ViewModel は真偽値を返すので、省略時の既定と同じ (core/ADR-0012)。
enum KmpDialogRegistration {
    /// 最小例の確認ダイアログを登録する。
    @MainActor
    static func register() {
        Dialog.shared.kmp.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmContent(message: viewModel.message, notifier: notifier)
        }
    }

    /// 登録した確認ダイアログを表示して結果を待つ。
    @MainActor
    static func showConfirmation() async throws -> DialogResult<Bool> {
        try await Dialog.shared.kmp.show(ConfirmViewModel(message: "Save?"))
    }
}
