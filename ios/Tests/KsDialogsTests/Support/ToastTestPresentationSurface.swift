#if canImport(UIKit)
import UIKit

@testable import KsDialogs

/// Toast の取り付け先をテストが用意した View で供給する。
/// 取り付け先を持たない状態にすると「提示環境が無い」状況を再現できる。
@MainActor
final class ToastTestPresentationSurface: ToastPresentationSurface {
    var hostView: UIView?

    init(hostView: UIView?) {
        self.hostView = hostView
    }

    /// 提示先の出現の合図の発火口。`fireHostAppearance()` で合図を送る。
    let hostAppearance = DialogTestHostAppearanceSignal()

    func observeHostAppearance(
        _ handler: @escaping DialogHostAppearanceHandler
    ) -> DialogHostAppearanceRegistration {
        hostAppearance.observe(handler)
    }

    /// 提示先の出現の合図を送る (本番の window の key 化・シーンのアクティブ化に対応する)。
    func fireHostAppearance() {
        hostAppearance.fire()
    }
}
#endif
