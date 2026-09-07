import KsDialogs

/// Text Input Dialog の ViewModel。
///
/// 結果型は `String` で、完了操作は入力された文字列を報告する。
/// この型そのものがレジストリの登録キーになる。
final class TextInputDialogViewModel: DialogViewModel {
    typealias Result = String

    /// ダイアログに表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
