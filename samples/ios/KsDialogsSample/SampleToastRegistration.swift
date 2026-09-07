import KsDialogs

/// カスタム Toast の ViewModel 型と、View factory・ViewModel factory の紐付け。
///
/// Toast のレジストリは Dialog / Loading のものとは独立しているため、登録もこちらへ行う。
enum SampleToastRegistration {
    /// この Sample が使うカスタム Toast を登録する。
    @MainActor
    static func register() {
        // SwiftUI の View をそのまま返す登録。Dialog の登録と同じく、戻り値の型で選ばれる
        Toast.shared.registry.register(CustomToastViewModel.self) { viewModel in
            CustomToastCard(viewModel: viewModel)
        }
        // 型を渡す表示 (show(CustomToastViewModel.self)) が実体を作るための factory
        Toast.shared.registry.register(
            CustomToastViewModel.self,
            viewModel: { CustomToastViewModel() }
        )
    }
}
