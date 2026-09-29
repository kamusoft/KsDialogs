import UIKit

/// 提示の依頼をその場では結ばず、後から結べる利用者の画面。
///
/// 提示機構が提示を受け付けなかったように見せたあと (提示関係も遷移も無い)、`bindHeldPresentation()` で
/// 提示関係を結んで完了通知を流す。「載せられなかった」と判定した後に提示が遅れて結ばれる場面を、
/// 実物の提示機構の上で再現するために使う。
@MainActor
final class BridgeTestLateBindingViewController: UIViewController {
    private var heldViewController: UIViewController?
    private var heldCompletion: (() -> Void)?

    /// 提示の依頼を預かっているか。
    var isHoldingPresentation: Bool {
        heldViewController != nil
    }

    /// 提示の依頼を預かるだけで、提示関係は結ばない。
    override func present(
        _ viewControllerToPresent: UIViewController,
        animated flag: Bool,
        completion: (() -> Void)? = nil
    ) {
        heldViewController = viewControllerToPresent
        heldCompletion = completion
    }

    /// 預かった提示を結び、提示機構の完了通知を流す。
    func bindHeldPresentation() {
        guard let viewController = heldViewController else { return }
        let completion = heldCompletion
        heldViewController = nil
        heldCompletion = nil
        super.present(viewController, animated: false, completion: completion)
    }
}
