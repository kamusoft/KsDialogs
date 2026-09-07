import SwiftUI
import UIKit

/// ダイアログの中身として登録する `UIView`。
///
/// 組み立て方は Basic Dialog の中身と同じで、渡されるのが ViewModel だけである点だけが違う。
final class ModelDialogCardHostView: UIView {
    private let hostingController: UIHostingController<ModelDialogCard>

    init(viewModel: ModelDialogViewModel) {
        hostingController = UIHostingController(rootView: ModelDialogCard(viewModel: viewModel))
        super.init(frame: .zero)

        let hostedView = hostingController.view!
        // 角丸の外側を地の色で塗り潰さないよう、載せる側の背景は抜いておく
        hostedView.backgroundColor = .clear
        // 器の中央に置かれるカードなので、画面の安全領域による押し出しは受けない
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
