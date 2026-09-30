import KsDialogsMauiBridge
import Testing
import UIKit

extension MauiBridgeSuite {
    /// 利用者の画面が閉じる途中に呼んだダイアログの提示を、実物の提示機構の上で確かめる。
    ///
    /// 提示先は閉じる途中の画面の手前 (その画面の提示元) になる。提示機構はその提示を閉鎖の遷移の
    /// 終わりまで持ち越し、遷移の終わりに結べなかった提示は拒否して完了通知も届けない。
    /// 持ち越された提示は表示まで進み、拒否された提示は器の消失と同じく cancelled で閉じることを見る。
    /// 提示遷移はシーンを持つホストアプリの上でしか完走しないため、Native ライブラリの単体テストでは見られない。
    @Suite("利用者の画面が閉じる途中のダイアログ")
    @MainActor
    struct PresentationRefusalTests {
        /// 利用者の画面を提示し終えてから、アニメーションつきで閉じ始める。
        private static func beginDismissingUserScreen(from root: UIViewController) async throws {
            let userScreen = UIViewController()
            root.present(userScreen, animated: false)
            try #require(
                await BridgeTestWaiting.waitUntil {
                    userScreen.presentingViewController === root && !userScreen.isBeingPresented
                },
                "利用者の画面が提示される"
            )
            root.dismiss(animated: true)
            try #require(userScreen.isBeingDismissed, "利用者の画面が閉じる途中になる")
        }

        /// 表示中のダイアログを閉じ、提示先に何も残っていないことを確かめる。
        private static func dismissAll(
            _ presentations: [MauiDialogPresentation],
            from root: UIViewController
        ) async -> Bool {
            for presentation in presentations.reversed() {
                presentation.dismiss()
            }
            return await BridgeTestWaiting.waitUntil { root.presentedViewController == nil }
        }

        @Test("閉じる途中に呼んだダイアログは、閉じ終えてから表示される")
        func dialogCalledDuringDismissalIsPresentedAfterward() async throws {
            let root = try #require(BridgeTestHost.keyWindow?.rootViewController)
            let bridge = MauiDialogBridge()
            let supply = BridgeTestCallCounter()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                try await Self.beginDismissingUserScreen(from: root)

                presentations.append(bridge.present(BridgeTestContent.supplying(supply)) { closures.record($0) })

                try #require(
                    await BridgeTestWaiting.waitUntil {
                        root.presentedViewController != nil && root.presentedViewController?.isBeingDismissed == false
                    },
                    "利用者の画面が閉じ終えた後にダイアログが表示される"
                )
                // 持ち越しの判定が済むまで待ってから、取り消されていないことを見る。
                try await Task.sleep(for: .milliseconds(300))
                #expect(closures.count == 0, "持ち越された提示は取り消されない")
                #expect(supply.count == 1)

                presentations.first?.dismiss()
                try #require(await BridgeTestWaiting.waitUntil { closures.count == 1 })
                #expect(closures.first?.kind == .dismissed, "表示されたダイアログとして閉じる")
            } cleanup: {
                await Self.dismissAll(presentations, from: root)
            }
        }

        @Test("閉じる途中に続けて呼んだ 2 枚目は、提示機構が受け付けず cancelled で閉じる")
        func secondDialogCalledDuringDismissalClosesAsCancelled() async throws {
            let root = try #require(BridgeTestHost.keyWindow?.rootViewController)
            let bridge = MauiDialogBridge()
            let supply = BridgeTestCallCounter()
            let closuresA = BridgeTestRecorder<MauiDialogClosure>()
            let closuresB = BridgeTestRecorder<MauiDialogClosure>()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                try await Self.beginDismissingUserScreen(from: root)

                // 2 枚とも同じ提示元 (閉じる途中の画面の手前) から出そうとし、持ち越されるのは 1 枚だけになる。
                presentations.append(bridge.present(BridgeTestContent.supplying(supply)) { closuresA.record($0) })
                presentations.append(bridge.present(BridgeTestContent.supplying(supply)) { closuresB.record($0) })

                try #require(
                    await BridgeTestWaiting.waitUntil { closuresB.count == 1 },
                    "受け付けられなかった 2 枚目も閉鎖の通知が届く (表示されないまま残らない)"
                )
                #expect(closuresB.first?.kind == .cancelled, "器の消失と同じく cancelled として届く")
                #expect(closuresB.first?.error == nil)
                #expect(supply.count == 2, "2 枚とも中身は作られている")
                #expect(closuresA.count == 0, "1 枚目は表示されたまま")
                let presented = try #require(root.presentedViewController, "1 枚目が表示されている")
                #expect(presented.presentedViewController == nil, "2 枚目は重なっていない")

                presentations.first?.dismiss()
                try #require(await BridgeTestWaiting.waitUntil { closuresA.count == 1 })
                #expect(closuresA.first?.kind == .dismissed)
            } cleanup: {
                await Self.dismissAll(presentations, from: root)
            }
        }

        @Test("cancelled で閉じた後に提示が遅れて結ばれても、ダイアログは画面に残らない")
        func lateBoundPresentationAfterCancellationIsRemoved() async throws {
            let root = try #require(BridgeTestHost.keyWindow?.rootViewController)
            let bridge = MauiDialogBridge()
            let supply = BridgeTestCallCounter()
            let closures = BridgeTestRecorder<MauiDialogClosure>()
            let userScreen = BridgeTestLateBindingViewController()
            var presentations: [MauiDialogPresentation] = []

            try await BridgeTestSharedHost.run {
                // 提示を預かって後から結ぶ利用者の画面を最前面に置く。
                root.present(userScreen, animated: false)
                try #require(
                    await BridgeTestWaiting.waitUntil {
                        userScreen.presentingViewController === root && !userScreen.isBeingPresented
                    },
                    "利用者の画面が提示される"
                )

                presentations.append(bridge.present(BridgeTestContent.supplying(supply)) { closures.record($0) })
                try #require(
                    await BridgeTestWaiting.waitUntil { userScreen.isHoldingPresentation },
                    "ダイアログの提示は利用者の画面に預けられている"
                )
                try #require(
                    await BridgeTestWaiting.waitUntil { closures.count == 1 },
                    "載せられなかったダイアログの閉鎖の通知が届く"
                )
                #expect(closures.first?.kind == .cancelled, "器の消失と同じく cancelled として届く")

                // 提示機構が遅れて提示を結び、完了通知を流す。
                userScreen.bindHeldPresentation()

                #expect(
                    await BridgeTestWaiting.waitUntil { userScreen.presentedViewController == nil },
                    "遅れて結ばれた片付け済みのダイアログは画面から外れる"
                )
                #expect(closures.count == 1, "閉鎖の通知は 1 回だけ")
                #expect(closures.first?.kind == .cancelled, "確定済みの結果は変わらない")
            } cleanup: {
                for presentation in presentations {
                    presentation.dismiss()
                }
                root.dismiss(animated: false)
                return await BridgeTestWaiting.waitUntil { root.presentedViewController == nil }
            }
        }
    }
}
