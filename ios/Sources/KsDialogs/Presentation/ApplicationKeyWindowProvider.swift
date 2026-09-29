#if canImport(UIKit)
import UIKit

/// 前面でアクティブなシーンの key window をアプリケーションから取得する既定の供給元。
final class ApplicationKeyWindowProvider: DialogKeyWindowProvider {
    @MainActor
    var keyWindow: UIWindow? {
        let snapshots = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .map {
                DialogWindowSceneSnapshot(
                    isForegroundActive: $0.activationState == .foregroundActive,
                    windows: $0.windows
                )
            }
        return Self.selectKeyWindow(from: snapshots)
    }

    /// 提示先の出現の合図を購読する。
    ///
    /// 提示先の条件は「前面でアクティブなシーン」と「key window」の 2 つからなる。
    /// どちらか片方だけを見ると、もう片方が後から満たされたときに合図が来ない
    /// (起動直後は window が key になった後にシーンがアクティブになる)。
    /// そこで、window が key になった通知とシーンがアクティブになった通知の両方で合図を送る。
    @MainActor
    func observeHostAppearance(
        _ handler: @escaping DialogHostAppearanceHandler
    ) -> DialogHostAppearanceRegistration {
        let center = NotificationCenter.default
        let tokens = Self.hostAppearanceNotifications.map { name in
            center.addObserver(forName: name, object: nil, queue: nil) { _ in
                Self.deliverOnMainThread(handler)
            }
        }
        let box = UncheckedSendableBox(tokens)
        return DialogHostAppearanceRegistration {
            for token in box.value {
                center.removeObserver(token)
            }
        }
    }

    /// 合図のきっかけにする通知。提示先の条件の 2 つの要素に 1 つずつ対応する。
    static let hostAppearanceNotifications: [Notification.Name] = [
        UIWindow.didBecomeKeyNotification,
        UIScene.didActivateNotification,
    ]

    /// 合図を UI スレッドで届ける。
    ///
    /// UIKit はこれらの通知を UI スレッドで送るので、ふだんはその場で届ける
    /// (届く順序を通知の順序と揃え、送った直後に受け取り側の状態が変わっているようにする)。
    /// UI スレッド以外から送られた場合だけ、UI スレッドへ移してから届ける。
    private static func deliverOnMainThread(_ handler: @escaping DialogHostAppearanceHandler) {
        if Thread.isMainThread {
            MainActor.assumeIsolated {
                handler()
            }
        } else {
            Task { @MainActor in
                handler()
            }
        }
    }

    /// 提示起点にできる window を選ぶ。
    /// 前面でアクティブなシーンの key window だけを採用し、無ければ nil を返す
    /// (背面・非アクティブのシーンや key でない window へ重ねると、利用者から見えない場所に出てしまう)。
    @MainActor
    static func selectKeyWindow(from snapshots: [DialogWindowSceneSnapshot]) -> UIWindow? {
        snapshots
            .filter(\.isForegroundActive)
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }
}
#endif
