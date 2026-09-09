import SwiftUI

/// 消費者検証アプリのエントリポイント。
@main
struct VerificationKmpApp: App {
    init() {
        KmpDialogRegistration.register()
    }

    var body: some Scene {
        WindowGroup {
            Button("Show confirmation") {
                Task { _ = try? await KmpDialogRegistration.showConfirmation() }
            }
        }
    }
}
