import KsDialogs

/// Declarative Dialog の ViewModel。
///
/// 結果型を宣言していないため、結果は既定の真偽値になる (core/ADR-0012)。
/// この型そのものがレジストリの登録キーになる。
final class DeclarativeDialogViewModel: DialogViewModel {
    /// ダイアログに表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
