import SwiftUI

/// Sample アプリのエントリポイント。
@main
struct KsDialogsSampleApp: App {
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
