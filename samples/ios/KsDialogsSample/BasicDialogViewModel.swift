import KsDialogs

/// Basic Dialog の ViewModel。
///
/// 結果型は `Bool` で、完了操作は `true` を報告する。
/// この型そのものがレジストリの登録キーになる。
final class BasicDialogViewModel: DialogViewModel {
    typealias Result = Bool

    /// ダイアログに表示するメッセージ。
    let message: String

    init(message: String) {
        self.message = message
    }
}
