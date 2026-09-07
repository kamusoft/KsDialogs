import KsDialogsMauiBridge
import Testing
import UIKit

extension MauiBridgeSuite {
    /// MAUI 側が中身を作れなかった (供給が nil を返した) ときの届き方を確かめる。
    ///
    /// 中身の供給は提示先が確保できた後に呼ばれるため、この経路は「提示先はあるが中身が無い」
    /// 状態を作って初めて通る。届き方は面ごとに違う — Dialog は閉鎖の通知に失敗として、
    /// Loading は完了の通知に理由を載せて、Toast は受理を失敗させずその1枚だけを破棄して
    /// 返す (core/ADR-0033 の iOS 側)。
    @Suite("中身なしの供給")
    @MainActor
    struct ContentSupplyTests {
        /// 観察の途中で期限が来ないだけの長さ (ミリ秒)。
        private static let toastDuration = NSNumber(value: 3000)

        /// 追加の通知が来ないことを見届けるための待ち。
        private static let settleTimeout = Duration.milliseconds(300)

        @Test("[BV-MA-01] Dialog は提示されず閉鎖の通知が失敗としてちょうど1回届く")
        func BV_MA_01_dialogContentUnavailable() async throws {
            let bridge = MauiDialogBridge()
            let supply = BridgeTestCallCounter()
            let closures = BridgeTestRecorder<MauiDialogClosure>()

            _ = bridge.present(BridgeTestContent.unavailable(supply)) { closures.record($0) }

            try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 }, "閉鎖の通知が届く")
            #expect(supply.count == 1, "中身の供給は1回だけ呼ばれる")
            let closure = try #require(closures.first)
            #expect(closure.kind == .failed, "互換面の失敗として届く")
            #expect(
                BridgeTestFailure.isContentUnavailable(closure.error),
                "理由は MAUI 側が中身を作れなかったこと"
            )
            #expect(BridgeTestHost.presentedViewController == nil, "ダイアログは提示されない")

            _ = await BridgeTestWaiting.waitUntil(timeout: Self.settleTimeout) { closures.count > 1 }
            #expect(closures.count == 1, "通知は1回だけで、後から追加で届かない")
        }

        @Test("[BV-MA-02] Loading (表示形) は表示されず完了の通知に失敗が載って1回届く")
        func BV_MA_02_loadingShowContentUnavailable() async throws {
            let bridge = MauiLoadingBridge()
            let overlays = BridgeTestOverlayObserver()
            let supply = BridgeTestCallCounter()
            let completions = BridgeTestRecorder<NSError?>()

            bridge.show(Self.unavailableLoadingContent(supply)) { completions.record($0) }

            try #require(await BridgeTestWaiting.waitUntil { completions.count == 1 }, "完了の通知が届く")
            #expect(supply.count == 1, "中身の供給は1回だけ呼ばれる")
            #expect(
                BridgeTestFailure.isContentUnavailable(completions.first ?? nil),
                "完了の通知に中身を作れなかったことが理由として載る"
            )
            let attached = try #require(overlays.attachedEver, "器の取り付けを観測できる")
            #expect(attached.isEmpty, "Loading の器は一度も取り付けられない")

            _ = await BridgeTestWaiting.waitUntil(timeout: Self.settleTimeout) { completions.count > 1 }
            #expect(completions.count == 1, "通知は1回だけで、後から追加で届かない")
        }

        @Test("[BV-MA-07] Loading (スコープ形) は MAUI 側の処理も実行せずに失敗が1回届く")
        func BV_MA_07_loadingStartContentUnavailable() async throws {
            let bridge = MauiLoadingBridge()
            let overlays = BridgeTestOverlayObserver()
            let supply = BridgeTestCallCounter()
            let actionCalls = BridgeTestCallCounter()
            let completions = BridgeTestRecorder<NSError?>()

            bridge.start(
                Self.unavailableLoadingContent(supply),
                action: { _, finish in
                    actionCalls.increment()
                    finish()
                },
                completion: { completions.record($0) }
            )

            try #require(await BridgeTestWaiting.waitUntil { completions.count == 1 }, "完了の通知が届く")
            #expect(supply.count == 1, "中身の供給は1回だけ呼ばれる")
            #expect(actionCalls.count == 0, "MAUI 側の処理は実行されない")
            #expect(
                BridgeTestFailure.isContentUnavailable(completions.first ?? nil),
                "完了の通知に中身を作れなかったことが理由として載る"
            )
            let attached = try #require(overlays.attachedEver, "器の取り付けを観測できる")
            #expect(attached.isEmpty, "Loading の器は一度も取り付けられない")

            _ = await BridgeTestWaiting.waitUntil(timeout: Self.settleTimeout) { completions.count > 1 }
            #expect(completions.count == 1, "通知は1回だけで、後から追加で届かない")
        }

        @Test("[BV-MA-03] Toast はその1枚だけが破棄され、表示中の別の Toast は残る")
        func BV_MA_03_toastContentUnavailable() async throws {
            let bridge = MauiToastBridge()
            let overlays = BridgeTestOverlayObserver()

            try await BridgeTestSharedHost.run {
                let firstSupply = BridgeTestCallCounter()
                bridge.show(Self.toastContent(BridgeTestContent.supplying(firstSupply)))
                try #require(
                    await BridgeTestWaiting.waitUntil { overlays.added.count == 1 },
                    "先に要求した Toast が表示される"
                )
                let firstContainer = try #require(overlays.added.first)

                // 受理は順に直列化されるため、後続の1枚が取り付くまで待てば、
                // 中身なしの1枚の処理は済んでいる。
                let failingSupply = BridgeTestCallCounter()
                bridge.show(Self.toastContent(BridgeTestContent.unavailable(failingSupply)))
                let followingSupply = BridgeTestCallCounter()
                bridge.show(Self.toastContent(BridgeTestContent.supplying(followingSupply)))

                try #require(
                    await BridgeTestWaiting.waitUntil { overlays.added.count == 2 },
                    "後続の Toast は受理も表示もされる"
                )
                // 枚数が 2 に達した瞬間は、「中身なしの1枚が器を作り、後続がまだ取り付いていない」
                // 途中の並びとも一致する。落ち着くまで待ってから枚数と同一性を見る。
                _ = await BridgeTestWaiting.waitUntil(timeout: Self.settleTimeout) {
                    overlays.added.count > 2
                }

                #expect(failingSupply.count == 1, "中身の供給は1回だけ呼ばれる")
                let attached = try #require(overlays.attachedEver, "器の取り付けを観測できる")
                #expect(attached.count == 2, "中身なしの1枚は器を一度も取り付けない")
                #expect(overlays.added.count == 2, "中身なしの1枚は器を増やさない")
                #expect(
                    overlays.added.first === firstContainer,
                    "先に表示した Toast の器はそのまま残る"
                )
            } cleanup: {
                // Toast は期限が来れば自分で消える。次のテストの観測点を汚さないよう全部消えるまで待つ。
                await BridgeTestWaiting.waitUntil(timeout: .seconds(10)) { overlays.added.isEmpty }
            }
        }

        /// 中身を作れないカスタム Loading の表示指定。
        private static func unavailableLoadingContent(
            _ counter: BridgeTestCallCounter
        ) -> MauiLoadingContent {
            MauiLoadingContent(
                contentProvider: BridgeTestContent.unavailable(counter),
                placement: nil,
                progressReceiver: nil
            )
        }

        /// 観察の途中で消えない長さのカスタム Toast の表示指定。
        private static func toastContent(
            _ contentProvider: @escaping () -> MauiDialogContent?
        ) -> MauiToastContent {
            MauiToastContent(
                contentProvider: contentProvider,
                duration: toastDuration,
                placement: nil
            )
        }
    }
}
