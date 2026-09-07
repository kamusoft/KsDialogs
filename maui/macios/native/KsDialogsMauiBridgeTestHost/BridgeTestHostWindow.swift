import UIKit

/// 直下に重ねられた View を控えるテスト用ホストアプリの window。
///
/// 器が取り付けられた直後に撤去される退行は、後から `subviews` を見るだけでは
/// 「一度も取り付けられなかった」場合と区別が付かない。取り付けの瞬間を取りこぼさないため、
/// 追加の通知を受けた View を履歴として残し、テスト側が「一度でも取り付いたか」を見られるようにする。
/// 履歴は取り付いた View を保持し続けるので、観測の区切りごとに捨てる。
final class BridgeTestHostWindow: UIWindow {
    /// 履歴を控え始めてから直下に重ねられた View (取り付いた順)。撤去済みのものも残る。
    private(set) var addedSubviewHistory: [UIView] = []

    override func didAddSubview(_ subview: UIView) {
        super.didAddSubview(subview)
        addedSubviewHistory.append(subview)
    }

    /// 履歴を捨てて、ここから先の取り付けだけを控える。
    func resetAddedSubviewHistory() {
        addedSubviewHistory.removeAll()
    }
}
