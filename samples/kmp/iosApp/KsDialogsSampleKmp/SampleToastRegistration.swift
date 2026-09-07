import KsDialogs
import SampleShared

/// 共有コードのカスタム Toast の ViewModel 型と iOS の View factory の紐付け。
///
/// 共有コードの ViewModel は Swift の ViewModel 契約に準拠しないため、登録は KMP 面 (`kmp`) へ行う。
/// 共有コードで定義した ViewModel のクラスがそのまま登録キーになり、
/// 共有 Presenter からの表示と同じレジストリを引く。
enum SampleToastRegistration {
    /// この Sample が使うカスタム Toast を登録する。
    @MainActor
    static func register() {
        // SwiftUI の View をそのまま返す登録。UIView を返す登録と同じ名前で、戻り値の型だけが違う
        Toast.shared.kmp.register(CustomToastViewModel.self) { viewModel in
            CustomToastCard(viewModel: viewModel)
        }
    }
}
