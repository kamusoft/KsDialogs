import KsDialogs

/// Layout Dialog の ViewModel。
///
/// 結果型は `Bool` で、完了操作は `true` を報告する。
/// この型そのものがレジストリの登録キーになる。
final class LayoutDialogViewModel: DialogViewModel {
    typealias Result = Bool

    /// ダイアログに表示するメッセージ。
    let message: String

    /// サイズと位置の計算に可視領域を使うか。
    /// 静的メタ属性は中身の性質なので、View factory が作った View への添付として供給する。
    let usesVisibleArea: Bool

    init(message: String, usesVisibleArea: Bool) {
        self.message = message
        self.usesVisibleArea = usesVisibleArea
    }
}
