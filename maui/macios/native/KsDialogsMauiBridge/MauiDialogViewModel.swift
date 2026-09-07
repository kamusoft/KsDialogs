import KsDialogs
import UIKit

/// MAUI 側から供給される View をそのまま中身にするダイアログの ViewModel。
///
/// この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの1種類が使われる。
/// 宣言結果型の `Bool` は「この面が閉鎖のために完了を報告した」ことを表すだけで、
/// 利用者の結果値は MAUI 側の報告口が受け持つ。
///
/// ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
/// 器へ渡る (core/ADR-0015)。この面が計算に関与することはない。
final class MauiDialogViewModel: DialogViewModel, @unchecked Sendable {
    typealias Result = Bool

    let presentation: MauiDialogPresentation
    private let contentProvider: MauiDialogContentProvider

    init(
        contentProvider: @escaping MauiDialogContentProvider,
        presentation: MauiDialogPresentation
    ) {
        self.contentProvider = contentProvider
        self.presentation = presentation
    }

    /// 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
    /// 提示先が確保できた後に UI スレッドで呼ばれる。
    ///
    /// MAUI 側が中身を作れなかったときは失敗を投げる。器はそれを提示そのものの失敗として扱い、
    /// 結果を返さずに閉鎖の通知へ載せて返す。
    @MainActor
    func makeContentView() throws -> UIView {
        guard let content = contentProvider() else {
            throw MauiDialogBridgeError.contentUnavailable
        }
        content.applyAttributes(options: content.options, placement: content.placement)
        return content.view
    }
}
