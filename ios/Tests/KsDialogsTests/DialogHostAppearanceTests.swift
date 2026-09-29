#if canImport(UIKit)
import Testing
import UIKit

@testable import KsDialogs

/// 提示先の出現の合図を確かめる。
///
/// 合図は提示先を選ぶ規則を持つ供給元が、window の key 化とシーンのアクティブ化の 2 つの通知から作る。
/// 合図は「現れたかもしれない」ことだけを知らせ、受け取った側が提示先を読み直して判断する。
/// 各機能は、待っている表示がある間だけ購読を張る。
@Suite("提示先の出現の合図", .serialized, .awaitsMainActorResponsive)
@MainActor
struct DialogHostAppearanceTests {
    /// 観察の途中で期限が来ないだけの Toast の長さ。
    private static let longToastDuration = 5000

    @Test("[PB-HI-01] key 化とシーンのアクティブ化の両方で、合図が届く")
    func PB_HI_01_bothNotificationsDeliverTheSignal() {
        let provider = ApplicationKeyWindowProvider()
        let counter = DialogTestCallCounter()
        let center = NotificationCenter.default

        // 通知は UI スレッドから送るので、合図はその場で届く。
        // この検査は中断点を持たないため、並行して走る別の検査の通知が紛れ込まない。
        let registration = provider.observeHostAppearance {
            counter.increment()
        }

        center.post(name: UIWindow.didBecomeKeyNotification, object: nil)
        #expect(counter.count == 1, "window が key になった通知で合図が 1 回届く")

        center.post(name: UIScene.didActivateNotification, object: nil)
        #expect(counter.count == 2, "シーンがアクティブになった通知で合図が 1 回届く")

        registration.cancel()
        center.post(name: UIWindow.didBecomeKeyNotification, object: nil)
        center.post(name: UIScene.didActivateNotification, object: nil)
        #expect(counter.count == 2, "購読を解除したあとは届かない")
    }

    @Test("[PB-HI-02] 合図が来ても提示先の条件を満たさなければ待ち続け、満たした合図で表示する")
    func PB_HI_02_waitsUntilSignalArrivesWithHostAvailable() async throws {
        // 3 機能の既定の面を、同じ供給元の上に組み立てる (本番と同じ中継の経路)。
        let provider = DialogTestKeyWindowProvider(keyWindow: nil)
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        // 提示遷移はシーンを持たないテストランナーでは完走しないので、閉鎖の依頼をその場で完了させる
        // 提示元を置く (後始末で Dialog を閉じ終えるため)。
        let presenter = RecordingPresentingViewController()
        window.rootViewController = presenter
        window.isHidden = false
        defer { window.isHidden = true }
        let dialogRegistry = DialogViewRegistry()
        let dialogSupply = DialogTestCallCounter()
        dialogRegistry.register(BasicTestDialogViewModel.self) { _, _ in
            dialogSupply.increment()
            return DialogTestContentView()
        }
        let dialog = Dialog(
            registry: dialogRegistry,
            presentationSurface: UIKitDialogPresentationSurface(
                keyWindowProvider: provider,
                hostWaitQueue: DialogHostWaitQueue()
            )
        )
        let loadingCoordinator = LoadingCoordinator(
            registry: LoadingViewRegistry(),
            settings: LoadingSettings(),
            presentationSurface: KeyWindowLoadingPresentationSurface(keyWindowProvider: provider)
        )
        let loading = Loading(coordinator: loadingCoordinator)
        let toastCoordinator = ToastCoordinator(
            registry: ToastViewRegistry(),
            settings: ToastSettings(),
            presentationSurface: KeyWindowToastPresentationSurface(keyWindowProvider: provider),
            announcer: ToastTestAnnouncer()
        )
        let toast = Toast(coordinator: toastCoordinator)
        let gate = DialogTransitionGate()

        let scope = Task {
            try await loading.start { _ in try await gate.wait() }
        }
        toast.show(message: "提示先を待つ", duration: Self.longToastDuration)
        let dialogShow = Task { try await dialog.show(BasicTestDialogViewModel(message: "提示先を待つ")) }
        try #require(await DialogTestWaiting.waitUntil { loadingCoordinator.coalescedUseCount == 1 })
        await toastCoordinator.acceptanceQueue.drain()
        try #require(toastCoordinator.displayCount == 1)
        try #require(await DialogTestWaiting.waitUntil { provider.hostAppearance.activeRegistrationCount == 3 })

        provider.fireHostAppearance()

        #expect(loadingCoordinator.isPresenting == false, "提示先が無いままの合図では Loading は待ち続ける")
        #expect(toastCoordinator.isPresenting == false, "提示先が無いままの合図では Toast は待ち続ける")
        #expect(presenter.presentedViewController == nil, "提示先が無いままの合図では Dialog は待ち続ける")
        #expect(dialogSupply.count == 0, "Dialog の中身はまだ作られない")
        #expect(provider.hostAppearance.activeRegistrationCount == 3, "3 つとも購読を続ける")

        provider.keyWindow = window
        provider.fireHostAppearance()

        #expect(loadingCoordinator.isPresenting, "提示先を満たした合図で Loading が表示される")
        #expect(toastCoordinator.isPresenting, "提示先を満たした合図で Toast が表示される")
        #expect(loadingCoordinator.presentedContainer?.view.window === window)
        #expect(toastCoordinator.presentedContainers.first?.view.window === window)
        // Dialog は列から明けたあと、呼び出し元の再開を経て中身を作り、提示先の先端から提示する。
        #expect(
            await DialogTestWaiting.waitUntil { presenter.presentedViewController is DialogContainerViewController },
            "提示先を満たした合図で Dialog が表示される"
        )
        #expect(dialogSupply.count == 1)
        #expect(provider.hostAppearance.activeRegistrationCount == 0, "表示したら購読を解除する")

        // シーンを持たないテストランナーでは提示遷移が完走せず、器が window に載らない。
        // 器の出入りは window に載った時点から始まるので、後始末のために手で載せてから打ち切る。
        let container = try #require(presenter.presentedViewController as? DialogContainerViewController)
        container.view.frame = window.bounds
        window.addSubview(container.view)
        dialogShow.cancel()
        #expect(try await dialogShow.value == .cancelled)
        gate.open()
        try await scope.value
    }

    @Test("[PB-HI-04] DialogError の case に提示先の不在が無い")
    func PB_HI_04_dialogErrorHasNoPresentationHostUnavailableCase() {
        // 網羅的な switch が既定の分岐なしでコンパイルできることが、case が 6 つだけである証拠になる。
        let cases: [DialogError] = [
            .viewFactoryNotRegistered(viewModelType: "T"),
            .viewFactoryTypeMismatch(viewModelType: "T"),
            .resultTypeMismatch(expected: "E", actual: "A"),
            .viewModelFactoryNotRegistered(viewModelType: "T"),
            .viewModelFactoryTypeMismatch(viewModelType: "T"),
            .viewModelAlreadyShowing(viewModelType: "T"),
        ]
        let labels = cases.map(Self.caseLabel(of:))

        #expect(Set(labels).count == 6, "列挙した case はすべて別の分岐に入る")
    }

    /// `DialogError` のすべての case を既定の分岐なしで網羅する。
    /// case が増えるとここがコンパイルできなくなる。
    private static func caseLabel(of error: DialogError) -> String {
        switch error {
        case .viewFactoryNotRegistered: "viewFactoryNotRegistered"
        case .viewFactoryTypeMismatch: "viewFactoryTypeMismatch"
        case .resultTypeMismatch: "resultTypeMismatch"
        case .viewModelFactoryNotRegistered: "viewModelFactoryNotRegistered"
        case .viewModelFactoryTypeMismatch: "viewModelFactoryTypeMismatch"
        case .viewModelAlreadyShowing: "viewModelAlreadyShowing"
        }
    }

    @Test("[PB-HI-03] 待っている表示が無くなると、購読が解除される")
    func PB_HI_03_registrationsAreCancelledWhenNothingWaits() async throws {
        let toastHarness = ToastTestHarness(hasHost: false)
        defer { toastHarness.tearDown() }
        let loadingHarness = LoadingTestHarness(hasHost: false)
        defer { loadingHarness.tearDown() }
        let gate = DialogTransitionGate()

        toastHarness.toast.show(message: "提示先が現れない", duration: 200)
        let scope = Task {
            try await loadingHarness.loading.start { _ in try await gate.wait() }
        }
        await toastHarness.drainAcceptance()
        try #require(await DialogTestWaiting.waitUntil { loadingHarness.coordinator.coalescedUseCount == 1 })
        #expect(toastHarness.surface.hostAppearance.activeRegistrationCount == 1, "Toast は待ちの購読を張る")
        #expect(loadingHarness.surface.hostAppearance.activeRegistrationCount == 1, "Loading は待ちの購読を張る")

        #expect(await toastHarness.waitUntilEmpty(), "Toast の期限が満了する")
        gate.open()
        try await scope.value

        #expect(toastHarness.surface.hostAppearance.activeRegistrationCount == 0, "Toast の購読は解除される")
        #expect(loadingHarness.surface.hostAppearance.activeRegistrationCount == 0, "Loading の購読は解除される")
    }
}
#endif
