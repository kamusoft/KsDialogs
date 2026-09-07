import KsDialogs

/// インライン経路のカスタム Toast の ViewModel。
///
/// 中身をその場で渡す表示は各 OS の Native API にしかないため、この ViewModel も OS 側に置く。
/// この型はレジストリに登録しない (core/ADR-0013)。
final class InlineToastViewModel: ToastViewModel {
    /// Toast に表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
