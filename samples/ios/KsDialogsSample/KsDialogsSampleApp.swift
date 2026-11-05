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
            // 起動引数 `--showcase <場面>` があれば撮影用の場面を出す (ShowcaseScene.swift)
            if let scene = ShowcaseScene.launchTarget {
                ShowcaseScreen(scene: scene)
            } else {
                SampleMenuScreen()
            }
        }
    }
}
