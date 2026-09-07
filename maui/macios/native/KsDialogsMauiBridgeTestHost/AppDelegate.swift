import UIKit

/// 互換面のテストを走らせるためだけの最小アプリ。
///
/// 互換面は提示先を「前面でアクティブなシーンの key window」として自動解決するため、
/// テストの実行体にはシーンを前面に持つ実アプリのプロセスが要る。
/// このアプリは画面を持たず、シーンと key window を用意することだけを仕事にする。
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(
            name: nil,
            sessionRole: connectingSceneSession.role
        )
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }
}
