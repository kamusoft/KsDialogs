import KsDialogs

/// Model Dialog の ViewModel。
///
/// 結果型は `Bool` で、完了操作は `true` を報告する。
/// 表示するメッセージは型指定 show の configure から設定されるため、状態は UI スレッドに閉じる。
/// 結果は中身から渡される報告口ではなく、この ViewModel 自身が `notifier` から報告する
/// (core/ADR-0018)。
@MainActor
final class ModelDialogViewModel: DialogViewModel {
    typealias Result = Bool

    /// ダイアログに表示するメッセージ。表示の直前に configure から設定する。
    var message: String = ""

    /// ViewModel factory から作れるように、生成は状態なしで行う。
    nonisolated init() {}

    /// 完了を報告する。
    func complete() {
        notifier?.complete(true)
    }

    /// キャンセルを報告する。
    func cancel() {
        notifier?.cancel()
    }
}
