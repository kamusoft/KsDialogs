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
        /// 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。
        ///
        /// 「先に表示した Toast の器はそのまま残る」は、観察を終えるまで 1 枚目が期限切れに
        /// ならないことが前提になる。実行機の速さで観察の所要は変わるので、期限に余裕を積むのではなく
        /// **観察の所要と関係の無い長さ**にして、片付けは期限切れを待たずに撤去で行う。
        private static let toastDuration = NSNumber(value: 60_000)

        /// 追加の通知が来ないことを見届けるための待ち。
        private static let settleTimeout = Duration.milliseconds(300)

        /// 重なりが落ち着くのを待つ上限。
        ///
        /// 落ち着き待ちは成立した時点で抜けるので、上限は実行機が遅い回の余裕として置く
        /// (使い切るのは落ち着かなかった回だけで、そのときは観測履歴が説明文に載る)。
        private static let settleWaitTimeout = Duration.seconds(2)

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
                // 途中の並びとも一致する。独立した 4 つの観測 — 今の枚数・一度でも取り付いた数・
                // 中身の供給が呼ばれ切ったこと・先頭が最初の器のままであること — が揃って
                // 落ち着くまで待ってから、枚数と同一性を見る。
                let settled = await BridgeTestWaiting.awaitSettled(timeout: Self.settleWaitTimeout) {
                    let attachedEver = overlays.attachedEver
                    return BridgeTestWaiting.Reading(
                        settled: overlays.added.count == 2
                            && attachedEver?.count == 2
                            && overlays.added.first === firstContainer
                            && failingSupply.count == 1,
                        "added=\(overlays.added.count)"
                            + " attachedEver=\(attachedEver.map { "\($0.count)" } ?? "読めない")"
                            + " failingSupply=\(failingSupply.count)"
                            + " firstKept=\(overlays.added.first === firstContainer)"
                    )
                }
                try #require(settled.settled, settled.message("重なった器が 2 枚で落ち着く"))

                #expect(failingSupply.count == 1, "中身の供給は1回だけ呼ばれる")
                let attached = try #require(overlays.attachedEver, "器の取り付けを観測できる")
                #expect(attached.count == 2, "中身なしの1枚は器を一度も取り付けない")
                #expect(overlays.added.count == 2, "中身なしの1枚は器を増やさない")
                #expect(
                    overlays.added.first === firstContainer,
                    "先に表示した Toast の器はそのまま残る"
                )
            } cleanup: {
                // 表示時間は観察の所要から独立させてあるので、期限切れを待つと試験時間が伸びる。
                // 次のテストの観測点を汚さないよう、重なった器をここで画面から外す。
                // 器を直接外すため橋渡しの側は「表示中」のまま残るが、表示時間の期限タイマーは
                // 橋渡しを保持しないので、このテストが終われば発火してもどこにも作用しない。
                for container in overlays.added {
                    container.removeFromSuperview()
                }
                return await BridgeTestWaiting.waitUntil(timeout: Self.settleTimeout) {
                    overlays.added.isEmpty
                }
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
