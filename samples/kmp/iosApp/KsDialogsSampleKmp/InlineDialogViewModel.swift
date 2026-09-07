import KsDialogs

/// Inline Dialog の ViewModel。
///
/// 中身をその場で渡す表示は各 OS の Native API にしかないため、この ViewModel も OS 側に置く。
/// 結果型を宣言していないため、結果は既定の真偽値になる (core/ADR-0012)。
final class InlineDialogViewModel: DialogViewModel {
    /// ダイアログに表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
