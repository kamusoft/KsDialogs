import KsDialogs

/// Inline Dialog の ViewModel。
///
/// 結果型を宣言していないため、結果は既定の真偽値になる (core/ADR-0012)。
/// レジストリには登録せず、表示のたびに中身を直接渡すため、この型が登録キーになることはない。
final class InlineDialogViewModel: DialogViewModel {
    /// ダイアログに表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
