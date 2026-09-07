import KsDialogs

/// インライン経路のカスタム Toast の ViewModel。
///
/// 中身はこの場で渡すため、この型はレジストリに登録しない (core/ADR-0013)。
final class InlineToastViewModel: ToastViewModel {
    /// Toast に表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
