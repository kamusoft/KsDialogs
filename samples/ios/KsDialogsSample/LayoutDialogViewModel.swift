import KsDialogs

/// Layout Dialog の ViewModel。
///
/// 結果型は `Bool` で、完了操作は `true` を報告する。
/// この型そのものがレジストリの登録キーになる。
final class LayoutDialogViewModel: DialogViewModel {
    typealias Result = Bool

    /// ダイアログに表示するメッセージ。
    let message: String

    /// サイズと位置の計算に使う基準領域。
    /// 静的メタ属性は中身の性質なので、View factory が作った View への添付として供給する。
    let layoutArea: DialogLayoutArea

    /// 全辺そろえで添付する余白 (pt)。基準領域と同じく静的メタ属性なので、View への添付で供給する。
    let dialogMargin: Double

    init(message: String, layoutArea: DialogLayoutArea, dialogMargin: Double) {
        self.message = message
        self.layoutArea = layoutArea
        self.dialogMargin = dialogMargin
    }
}
