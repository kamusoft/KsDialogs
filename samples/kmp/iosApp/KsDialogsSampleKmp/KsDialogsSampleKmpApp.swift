import SwiftUI

/// Sample アプリのエントリポイント。
@main
struct KsDialogsSampleKmpApp: App {
    init() {
        SampleDialogRegistration.register()
        SampleLoadingRegistration.register()
        SampleToastRegistration.register()
    }

    var body: some Scene {
        WindowGroup {
            SampleMenuScreen()
        }
    }
}
