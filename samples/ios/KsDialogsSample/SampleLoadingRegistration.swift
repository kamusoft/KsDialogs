import KsDialogs

/// カスタム Loading の ViewModel 型と、View factory・ViewModel factory の紐付け。
///
/// Loading のレジストリは Dialog のものとは独立しているため、登録もこちらへ行う。
enum SampleLoadingRegistration {
    /// この Sample が使うカスタム Loading を登録する。
    @MainActor
    static func register() {
        // SwiftUI の View をそのまま返す登録。Dialog の登録と同じく、戻り値の型で選ばれる
        Loading.shared.registry.register(CustomLoadingViewModel.self) { viewModel in
            CustomLoadingCard(viewModel: viewModel)
        }
        // 型を渡す表示 (start(CustomLoadingViewModel.self)) が実体を作るための factory
        Loading.shared.registry.register(
            CustomLoadingViewModel.self,
            viewModel: { CustomLoadingViewModel() }
        )
    }
}
