import KsDialogsMauiBridge
import Testing
import UIKit

/// 互換面のテスト全体の入れ物。
///
/// テストはテスト用ホストアプリのプロセス内で走り、そのシーンと key window を提示先として
/// 共有する。提示中のダイアログや表示中の Toast は 1 プロセスに 1 つの状態としてまとまって
/// いるため、テストどうしが同時に走ると互いの観測点を汚す。ここで直列化して取り決める。
@Suite("MAUI 互換面", .serialized)
struct MauiBridgeSuite {}

extension MauiBridgeSuite {
    /// テストの実行体で実物の提示先が確保できることを確かめる。
    ///
    /// 互換面は提示先を自動解決する (前面でアクティブなシーンの key window) ため、
    /// シーンを持たない実行体では提示に入れない。ホストアプリの key window が提示先として
    /// 解決されることが、中身の供給を扱う以降のテストの前提になる。
    @Suite("互換面の提示先")
    @MainActor
    struct PresentationHostTests {
        @Test("ホストアプリは前面でアクティブなシーンと key window を持つ")
        func hostProvidesForegroundActiveKeyWindow() throws {
            #expect(BridgeTestHost.hasForegroundActiveScene, "前面でアクティブなシーンがある")
            let keyWindow = try #require(BridgeTestHost.keyWindow, "key window が得られる")
            #expect(keyWindow.rootViewController != nil, "提示の起点になる画面がある")
        }

        @Test("公開 init の互換面は実際に提示できる")
        func publicBridgePresentsOnHostKeyWindow() async throws {
            let bridge = MauiDialogBridge()
            let supply = BridgeTestCallCounter()
            let closures = BridgeTestRecorder<MauiDialogClosure>()

            let presentation = bridge.present(BridgeTestContent.supplying(supply)) {
                closures.record($0)
            }

            try await BridgeTestSharedHost.run {
                try #require(
                    await BridgeTestWaiting.waitUntil {
                        BridgeTestHost.presentedViewController != nil
                    },
                    "提示先不在にならず、実際に提示される"
                )
                #expect(supply.count == 1, "中身の供給は提示先が確保できた後に1回呼ばれる")

                presentation.dismiss()

                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .dismissed, "閉鎖要求で閉じたことが届く")
            } cleanup: {
                // 閉鎖は何度求めても1回しか効かないため、本体の途中で失敗していてもここで閉じられる。
                presentation.dismiss()
                return await BridgeTestWaiting.waitUntil {
                    BridgeTestHost.presentedViewController == nil
                }
            }
        }
    }
}
