import Foundation

/// 互換面から届いた失敗の判別。
///
/// 失敗の判別子は互換面の内部型であり、テスト標的からは見えない。
/// 利用者に届く形 (失敗の出どころと説明) で照合する。
enum BridgeTestFailure {
    /// 「MAUI 側が表示の中身を作れなかった」失敗の説明。
    static let contentUnavailableDescription = "The MAUI side could not create the presentation content."

    /// その失敗が「MAUI 側が表示の中身を作れなかった」ものか。
    static func isContentUnavailable(_ error: NSError?) -> Bool {
        guard let error else { return false }
        return error.domain.hasSuffix("MauiDialogBridgeError")
            && error.localizedDescription == contentUnavailableDescription
    }
}
