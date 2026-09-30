import KsDialogsMauiBridge
import Testing
import UIKit

extension MauiBridgeSuite {
    /// 基準領域「表示中のページ」が互換面を通って Native ライブラリへ届くことを、実際の配置で確かめる。
    ///
    /// ページの選び方は MAUI 側 (C# 層) が持ち、互換面はその結果の View を Native ライブラリの登録口へ
    /// 中継するだけなので、ここでは View を返す関数を直接登録して、基準領域の値と登録の両方が届くことを見る。
    @Suite("表示中のページ")
    @MainActor
    struct CurrentPageTests {
        /// 器の既定の余白。互換面は既定値を Native ライブラリから引くため、ここでも同じ値から読む。
        private static let margin = MauiDialogOptions().marginBottom

        /// 配置の比較に許す誤差 (pt)。
        private static let tolerance: CGFloat = 0.5

        @Test("currentPage と登録した View で、ダイアログがその View の safe area の内側に寄る")
        func currentPagePlacesDialogInsideRegisteredView() async throws {
            let window = try #require(BridgeTestHost.keyWindow)
            let root = try #require(window.rootViewController)
            // 画面の上 6 割だけを占めるページ。下にタブバーのような帯が残る画面を模す
            let page = UIView(frame: CGRect(
                x: 0, y: 0, width: root.view.bounds.width, height: root.view.bounds.height * 0.6))
            root.view.addSubview(page)
            MauiDialogCurrentPage.setProvider { page }

            let content = BridgeTestContentView()
            let presentation = MauiDialogBridge().present(Self.supplying(content, area: .currentPage)) { _ in }

            try await BridgeTestSharedHost.run {
                try #require(await Self.waitUntilPlaced(content), "ダイアログが配置される")
                let pageRect = page.convert(page.safeAreaLayoutGuide.layoutFrame, to: window)
                let dialogRect = content.convert(content.bounds, to: window)
                #expect(
                    abs(dialogRect.maxY - (pageRect.maxY - Self.margin)) <= Self.tolerance,
                    "下端がページの下端から余白分内側になる (dialog: \(dialogRect), page: \(pageRect))"
                )
                #expect(
                    abs(dialogRect.maxX - (pageRect.maxX - Self.margin)) <= Self.tolerance,
                    "右端がページの右端から余白分内側になる (dialog: \(dialogRect), page: \(pageRect))"
                )
            } cleanup: {
                presentation.dismiss()
                MauiDialogCurrentPage.setProvider(nil)
                page.removeFromSuperview()
                return await BridgeTestWaiting.waitUntil { BridgeTestHost.presentedViewController == nil }
            }
        }

        /// MAUI 側でページが決まらないとき、C# 層は提示先の window の root の view (safe area の内側が可視領域を覆う View)
        /// を返して可視領域へ落とす。その View が Native ライブラリに受け付けられ、内蔵の view controller 階層の走査を
        /// 経ずに可視領域と同じ配置になることを確かめる。
        ///
        /// 走査なら結果が変わる構成にするため、root の上にタブバー付きの `UITabBarController` を全画面の重ね表示で出す
        /// (root の view は window に残る)。走査は選択中のタブの view まで降り、その safe area の下端はタブバーの上になる。
        @Test("登録した関数が root の view を返すと、view controller 階層の先端と違っても可視領域と同じ配置になる")
        func rootViewAsPageMatchesVisibleArea() async throws {
            let window = try #require(BridgeTestHost.keyWindow)
            let root = try #require(window.rootViewController)
            let tab = UITabBarController()
            let selected = UIViewController()
            selected.tabBarItem = UITabBarItem(title: "Tab", image: nil, tag: 0)
            tab.viewControllers = [selected]
            tab.modalPresentationStyle = .overFullScreen
            await withCheckedContinuation { continuation in
                root.present(tab, animated: false) { continuation.resume() }
            }
            MauiDialogCurrentPage.setProvider { root.view }

            let content = BridgeTestContentView()
            let presentation = MauiDialogBridge().present(Self.supplying(content, area: .currentPage)) { _ in }

            try await BridgeTestSharedHost.run {
                try #require(await Self.waitUntilPlaced(content), "ダイアログが配置される")
                let visibleArea = window.bounds.inset(by: window.safeAreaInsets)
                let traversedPage = selected.view.convert(selected.view.safeAreaLayoutGuide.layoutFrame, to: window)
                // 走査の結果と可視領域が下端で違わなければ、この構成では両者を見分けられない
                try #require(
                    traversedPage.maxY < visibleArea.maxY - Self.tolerance,
                    "走査が選ぶタブの safe area の下端がタブバーの上になる (tab: \(traversedPage), visible: \(visibleArea))"
                )
                let dialogRect = content.convert(content.bounds, to: window)
                #expect(
                    abs(dialogRect.maxY - (visibleArea.maxY - Self.margin)) <= Self.tolerance,
                    "下端が可視領域の下端から余白分内側になる (dialog: \(dialogRect), visible: \(visibleArea), tab: \(traversedPage))"
                )
                #expect(
                    abs(dialogRect.maxX - (visibleArea.maxX - Self.margin)) <= Self.tolerance,
                    "右端が可視領域の右端から余白分内側になる (dialog: \(dialogRect), visible: \(visibleArea))"
                )
            } cleanup: {
                presentation.dismiss()
                MauiDialogCurrentPage.setProvider(nil)
                // ダイアログの器はタブの上に重なっているため、器が閉じてからタブを閉じる
                guard await BridgeTestWaiting.waitUntil({ tab.presentedViewController == nil }) else { return false }
                await withCheckedContinuation { continuation in
                    root.dismiss(animated: false) { continuation.resume() }
                }
                return await BridgeTestWaiting.waitUntil { BridgeTestHost.presentedViewController == nil }
            }
        }

        /// 指定の基準領域で、右下 (End / End) に寄せる中身を供給する。
        private static func supplying(
            _ view: BridgeTestContentView,
            area: MauiDialogLayoutArea
        ) -> () -> MauiDialogContent? {
            {
                // 供給は互換面が UI スレッド上で呼ぶ取り決めであり、作った中身が他のスレッドへ渡ることはない
                nonisolated(unsafe) var content: MauiDialogContent?
                MainActor.assumeIsolated {
                    let options = MauiDialogOptions()
                    options.layoutArea = area
                    let placement = MauiDialogPlacement()
                    placement.horizontalAlignment = .end
                    placement.verticalAlignment = .end
                    content = MauiDialogContent(view: view, options: options, placement: placement)
                }
                return content
            }
        }

        /// 中身が window に載り、出現の演出を終えて位置が動かなくなるまで待つ。
        private static func waitUntilPlaced(_ content: UIView) async -> Bool {
            var previous: CGRect?
            return await BridgeTestWaiting.awaitSettled(stable: .milliseconds(300)) {
                let frame = content.window.map { content.convert(content.bounds, to: $0) }
                defer { previous = frame }
                let placed = frame.map { $0.width > 0 && $0.height > 0 && $0 == previous } ?? false
                return BridgeTestWaiting.Reading(
                    settled: placed && BridgeTestHost.presentedViewController != nil,
                    "frame=\(frame.map { "\($0)" } ?? "nil")"
                )
            }.settled
        }
    }
}
