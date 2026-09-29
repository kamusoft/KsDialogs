#if canImport(UIKit)
import UIKit

@testable import KsDialogs

/// テストが用意した window を提示起点として供給する。
/// window を持たない状態にすると「提示先が存在しない」状況を再現できる。
@MainActor
final class DialogTestKeyWindowProvider: DialogKeyWindowProvider {
    var keyWindow: UIWindow?

    init(keyWindow: UIWindow?) {
        self.keyWindow = keyWindow
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
