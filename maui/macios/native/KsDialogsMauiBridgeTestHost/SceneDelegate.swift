import UIKit

/// テスト用ホストアプリのシーン。
///
/// 空の画面を持つ window を1つ作って key にする。互換面はこの window を提示先として解決し、
/// テストはその view 階層を観測点に使う。器が取り付いた瞬間もテストの観測点になるため、
/// window は取り付けの履歴を控える `BridgeTestHostWindow` にする。
final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = BridgeTestHostWindow(windowScene: windowScene)
        let root = UIViewController()
        root.view.backgroundColor = .systemBackground
        window.rootViewController = root
        window.makeKeyAndVisible()
        self.window = window
    }
}
