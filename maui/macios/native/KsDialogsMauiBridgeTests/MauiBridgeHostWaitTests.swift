// 提示先を待っている Dialog の数は Native ライブラリの内部の列が持つため、テスト可能性を有効にした
// 取り込みで読む。互換面の呼び出しが、呼んだ時点で列の順番を取ったかを確かめるのに使う。
@testable import KsDialogs
import KsDialogsMauiBridge
import Testing
import UIKit

extension MauiBridgeSuite {
    /// 提示先の出現を待つダイアログを、互換面の上で確かめる。
    ///
    /// Native ライブラリの単体テストは提示面を差し替えて順序を見るが、実物の提示機構が
    /// 「前の 1 枚の提示の完了を待ってから次を提示する」流れを受け入れて重ねるかは、
    /// シーンを持つホストアプリの上でしか確かめられない。ホストアプリの key window を隠して
    /// 提示先を無くし、もう一度 key にして提示先を出現させる。
    /// 待っている間の打ち切り・閉鎖と、表示中の打ち切りもここで確かめる。
    @Suite("提示先の出現を待つダイアログ")
    @MainActor
    struct HostWaitTests {
        /// 中身を 1 回分作り、作った中身の View を記録する供給。
        private static func supplying(
            _ views: BridgeTestRecorder<UIView>
        ) -> () -> MauiDialogContent? {
            {
                // 供給は互換面が UI スレッド上で呼ぶ取り決めであり、作った中身が他のスレッドへ
                // 渡ることはない。供給の型がスレッドの取り決めを持たないため、ここで明示する。
                nonisolated(unsafe) var content: MauiDialogContent?
                MainActor.assumeIsolated {
                    let made = BridgeTestContent.make()
                    views.record(made.view)
                    content = made
                }
                return content
            }
        }

        /// 提示先を待っている Dialog の数 (呼び出しの時点で順番を取って、まだ列に着いていないものを含む)。
        private static var waitingCount: Int {
            DialogHostWaitQueue.application.waitingCount
        }

        /// key window を隠して、提示先が無い状態を作る。
        private static func hideHost(_ window: UIWindow) async throws {
            // key window を隠すと、前面でアクティブなシーンに key window が無くなる (提示先の不在)。
            window.isHidden = true
            try #require(
                await BridgeTestWaiting.waitUntil { BridgeTestHost.keyWindow == nil },
                "提示先が無い状態を作れる"
            )
        }

        /// 隠した key window を戻し、表示と待ちが残っていないことを確かめる。
        private static func restoreHost(
            _ window: UIWindow,
            dismissing presentations: [MauiDialogPresentation]
        ) async -> Bool {
            if window.isHidden {
                window.makeKeyAndVisible()
            }
            // 閉鎖は何度求めても1回しか効かないため、本体の途中で失敗していてもここで閉じられる。
            for presentation in presentations.reversed() {
                presentation.dismiss()
            }
            return await BridgeTestWaiting.waitUntil {
                BridgeTestHost.keyWindow === window
                    && BridgeTestHost.presentedViewController == nil
                    && waitingCount == 0
            }
        }

        @Test("待っていた 2 枚は、提示先が現れると呼んだ順に提示され、後の 1 枚が手前に重なる")
        func waitingDialogsArePresentedInCallOrderOnRealUIKit() async throws {
            let window = try #require(BridgeTestHost.keyWindow, "ホストアプリの key window がある")
            let root = try #require(window.rootViewController)
            let bridge = MauiDialogBridge()
            let viewsA = BridgeTestRecorder<UIView>()
            let viewsB = BridgeTestRecorder<UIView>()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                try await Self.hideHost(window)

                // UI スレッドから、間に待ち合わせを挟まずに A、B の順で呼ぶ。
                presentations.append(bridge.present(Self.supplying(viewsA)) { closures.record($0) })
                presentations.append(bridge.present(Self.supplying(viewsB)) { closures.record($0) })
                // 呼び出しから戻った時点で、2 枚とも列の順番を取っている (提示処理の開始順に頼らない)。
                #expect(Self.waitingCount == 2, "呼び出しの時点で順番が取られている")

                // 待っている間は、失敗の通知も中身の供給も提示も起きない。
                try await Task.sleep(for: .milliseconds(300))
                #expect(closures.count == 0, "提示先が無くても失敗しない")
                #expect(viewsA.count == 0 && viewsB.count == 0, "提示先が現れるまで中身は作られない")
                #expect(root.presentedViewController == nil, "提示先が現れるまで提示されない")

                // もう一度 key にすると、window が key になった通知で提示先の出現の合図が届く。
                window.makeKeyAndVisible()

                try #require(
                    await BridgeTestWaiting.waitUntil {
                        root.presentedViewController?.presentedViewController != nil
                    },
                    "2 枚とも提示される"
                )
                let first = try #require(root.presentedViewController)
                let second = try #require(first.presentedViewController)
                let viewA = try #require(viewsA.first)
                let viewB = try #require(viewsB.first)
                #expect(viewA.isDescendant(of: first.view), "先に呼んだ A が先に提示される")
                #expect(viewB.isDescendant(of: second.view), "後から呼んだ B が A の手前に重なる")
                #expect(second.presentingViewController === first, "B は A の上から提示されている")

                for presentation in presentations.reversed() {
                    presentation.dismiss()
                }
                try #require(await BridgeTestWaiting.waitUntil { closures.count == 2 })
                #expect(closures.recorded.allSatisfy { $0.kind == .dismissed }, "それぞれが独立に閉じる")
            } cleanup: {
                await Self.restoreHost(window, dismissing: presentations)
            }
        }

        @Test("[PB-MC-06] iOS のブリッジは、待っている間の打ち切りを cancelled の閉鎖通知として届ける")
        func PB_MC_06_cancellingWhileWaitingIsReportedAsCancelled() async throws {
            let window = try #require(BridgeTestHost.keyWindow, "ホストアプリの key window がある")
            let bridge = MauiDialogBridge()
            let views = BridgeTestRecorder<UIView>()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                try await Self.hideHost(window)
                let presentation = bridge.present(Self.supplying(views)) { closures.record($0) }
                presentations.append(presentation)
                try #require(await BridgeTestWaiting.waitUntil { Self.waitingCount == 1 }, "提示先を待っている")

                presentation.cancel()

                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .cancelled, "打ち切りは cancelled として届く")
                #expect(closures.first?.error == nil)
                #expect(Self.waitingCount == 0, "列から外れている")

                // 打ち切った後に提示先が現れても、中身は作られず提示もされない。
                window.makeKeyAndVisible()
                try await Task.sleep(for: .milliseconds(300))
                #expect(views.count == 0, "中身の供給は呼ばれない")
                #expect(BridgeTestHost.presentedViewController == nil, "提示されない")
                #expect(closures.count == 1, "閉鎖の通知はちょうど 1 回")
            } cleanup: {
                await Self.restoreHost(window, dismissing: presentations)
            }
        }

        @Test("[PB-MC-06] iOS のブリッジは、表示中の打ち切りでダイアログを閉じ、cancelled の閉鎖通知を届ける")
        func PB_MC_06_cancellingWhilePresentingClosesAndReportsCancelled() async throws {
            let window = try #require(BridgeTestHost.keyWindow, "ホストアプリの key window がある")
            let bridge = MauiDialogBridge()
            let views = BridgeTestRecorder<UIView>()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                let presentation = bridge.present(Self.supplying(views)) { closures.record($0) }
                presentations.append(presentation)
                try #require(
                    await BridgeTestWaiting.waitUntil { BridgeTestHost.presentedViewController != nil },
                    "提示される"
                )

                presentation.cancel()

                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .cancelled, "打ち切りは cancelled として届く")
                try #require(
                    await BridgeTestWaiting.waitUntil { BridgeTestHost.presentedViewController == nil },
                    "ダイアログが閉じる"
                )
                #expect(views.count == 1)
                #expect(closures.count == 1, "閉鎖の通知はちょうど 1 回")
            } cleanup: {
                await Self.restoreHost(window, dismissing: presentations)
            }
        }

        @Test("互換面を呼んだ直後に打ち切ると、提示先があっても中身を作らずに cancelled が届く")
        func cancellingRightAfterPresentNeverCreatesContent() async throws {
            let window = try #require(BridgeTestHost.keyWindow, "ホストアプリの key window がある")
            let bridge = MauiDialogBridge()
            let views = BridgeTestRecorder<UIView>()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                // Native の show が走り出す前に打ち切る
                let presentation = bridge.present(Self.supplying(views)) { closures.record($0) }
                presentations.append(presentation)
                presentation.cancel()

                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .cancelled, "打ち切りは cancelled として届く")
                try await Task.sleep(for: .milliseconds(300))
                #expect(views.count == 0, "中身の供給は呼ばれない")
                #expect(BridgeTestHost.presentedViewController == nil, "提示されない")
                #expect(closures.count == 1, "閉鎖の通知はちょうど 1 回")
            } cleanup: {
                await Self.restoreHost(window, dismissing: presentations)
            }
        }

        @Test("[PB-MC-07] 中身を作る前にブリッジを閉じると、中身の供給は呼ばれず一度も表示されない")
        func PB_MC_07_dismissingBeforeContentNeverPresents() async throws {
            let window = try #require(BridgeTestHost.keyWindow, "ホストアプリの key window がある")
            let bridge = MauiDialogBridge()
            let views = BridgeTestRecorder<UIView>()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                try await Self.hideHost(window)
                let presentation = bridge.present(Self.supplying(views)) { closures.record($0) }
                presentations.append(presentation)
                try #require(await BridgeTestWaiting.waitUntil { Self.waitingCount == 1 }, "提示先を待っている")

                // 結果が確定した show が閉じる経路と同じ操作。
                presentation.dismiss()

                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .dismissed, "閉鎖要求で閉じたことが届く")
                #expect(Self.waitingCount == 0, "提示先の出現を待たずに止まる")

                window.makeKeyAndVisible()
                try await Task.sleep(for: .milliseconds(300))
                #expect(views.count == 0, "中身の供給は呼ばれない")
                #expect(BridgeTestHost.presentedViewController == nil, "一度も表示されない")
                #expect(closures.count == 1, "閉鎖の通知はちょうど 1 回")
            } cleanup: {
                await Self.restoreHost(window, dismissing: presentations)
            }
        }
    }
}
