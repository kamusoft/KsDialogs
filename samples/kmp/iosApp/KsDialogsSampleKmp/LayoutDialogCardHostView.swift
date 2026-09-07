import SwiftUI
import UIKit

/// ダイアログの中身として登録する `UIView`。
///
/// レジストリの View factory は `UIView` を返す契約なので、SwiftUI のカードを
/// `UIHostingController` 越しに載せる。器の自動レイアウトへ寸法を渡すため、
/// 載せた View は四辺で固定する。
final class LayoutDialogCardHostView: UIView {
    private let hostingController: UIHostingController<LayoutDialogCard>

    init(message: String, onCancel: @escaping () -> Void, onComplete: @escaping () -> Void) {
        hostingController = UIHostingController(
            rootView: LayoutDialogCard(message: message, onCancel: onCancel, onComplete: onComplete)
        )
        super.init(frame: .zero)

        let hostedView = hostingController.view!
        // 角丸の外側を地の色で塗り潰さないよう、載せる側の背景は抜いておく
        hostedView.backgroundColor = .clear
        // 器が置き場所を決めるカードなので、画面の安全領域による押し出しは受けない
        hostingController.safeAreaRegions = []
        hostedView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(hostedView)

        NSLayoutConstraint.activate([
            hostedView.topAnchor.constraint(equalTo: topAnchor),
            hostedView.bottomAnchor.constraint(equalTo: bottomAnchor),
            hostedView.leadingAnchor.constraint(equalTo: leadingAnchor),
            hostedView.trailingAnchor.constraint(equalTo: trailingAnchor)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("この View は storyboard からの生成に対応しない")
    }
}
