import Foundation

/// 互換面そのものが扱えなかった事象。
///
/// ダイアログの契約が定める結果 (完了 / キャンセル) のどちらでもない値が返った場合や、
/// MAUI 側が中身を供給できなかった場合に使う。
enum MauiDialogBridgeError: LocalizedError {
    /// 契約が定める結果のどれにも当てはまらない値が返った。
    case unsupportedResult

    /// MAUI 側が表示の中身を作れなかった (失敗の詳細は MAUI 側が警告として残している — core/ADR-0033)。
    case contentUnavailable

    var errorDescription: String? {
        switch self {
        case .unsupportedResult:
            "Could not determine the Dialog result."
        case .contentUnavailable:
            "The MAUI side could not create the presentation content."
        }
    }
}
