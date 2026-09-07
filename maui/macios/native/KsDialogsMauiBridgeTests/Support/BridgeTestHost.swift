// 提示先の window は取り付けの履歴を控えるホストアプリ側の型であり、公開面を持たないため
// テスト可能性を有効にした取り込みで参照する。
@testable import KsDialogsMauiBridgeTestHost
import UIKit

/// テスト用ホストアプリが用意した提示先を、テストから読むための入口。
///
/// 互換面は提示先を自分で解決する (前面でアクティブなシーンの key window) ため、テストは
/// 提示先を渡さない。ここでは同じ規則で選んだ window を観測点として取り出す。
@MainActor
enum BridgeTestHost {
    /// 互換面が提示先として解決する window。
    static var keyWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }

    /// 前面でアクティブなシーンが存在するか。
    static var hasForegroundActiveScene: Bool {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .contains { $0.activationState == .foregroundActive }
    }

    /// key window に直接重ねられている View の一覧 (重なり順)。
    static var overlayViews: [UIView] {
        keyWindow?.subviews ?? []
    }

    /// 取り付けの履歴を控える提示先。ホストアプリが用意した window 以外が key になっていれば nil。
    static var recordingKeyWindow: BridgeTestHostWindow? {
        keyWindow as? BridgeTestHostWindow
    }

    /// ホストアプリの画面から提示されている ViewController。提示がなければ nil。
    ///
    /// Dialog の器は提示機構に載るため、提示の有無はここで観測できる。
    static var presentedViewController: UIViewController? {
        keyWindow?.rootViewController?.presentedViewController
    }
}
