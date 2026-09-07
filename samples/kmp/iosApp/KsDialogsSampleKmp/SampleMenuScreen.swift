import SampleShared
import SwiftUI
import UIKit

/// Sample のメニュー画面。デモ項目の一覧と直近の結果を表示する。
struct SampleMenuScreen: View {
    @State private var model = SampleMenuModel()
    @State private var showsLayoutPanel = false
    @State private var showsTransitionPanel = false

    var body: some View {
        VStack(spacing: 0) {
            SampleMenuHeader()
            SampleDivider()
            // デモ項目が画面の高さを超える端末でも結果表示エリアまで到達できるよう、
            // 見出しから下をスクロールできる容器に入れる (トランジションのデモ画面と同じ構成)
            ScrollView {
                VStack(spacing: 0) {
                    SampleMenuItemRow(title: SampleText.shared.BASIC_DIALOG_ITEM) {
                        Task { await model.showBasicDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.LAYOUT_DIALOG_ITEM) {
                        showsLayoutPanel = true
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.DECLARATIVE_DIALOG_ITEM) {
                        Task { await model.showDeclarativeDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.TEXT_INPUT_DIALOG_ITEM) {
                        Task { await model.showTextInputDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.INLINE_DIALOG_ITEM) {
                        Task { await model.showInlineDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.TRANSITION_DIALOG_ITEM) {
                        showsTransitionPanel = true
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.MODEL_DIALOG_ITEM) {
                        Task { await model.showModelDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.DEFAULT_LOADING_ITEM) {
                        Task { await model.runDefaultLoading() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.CUSTOM_LOADING_ITEM) {
                        Task { await model.runCustomLoading() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.DEFAULT_TOAST_ITEM) {
                        model.showDefaultToast()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.CUSTOM_TOAST_ITEM) {
                        model.showCustomToast()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.TOAST_STACK_ITEM) {
                        model.showToastStack()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.TOAST_PLACEMENT_ITEM) {
                        model.showToastPlacement()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.shared.TOAST_OVERLAP_ITEM) {
                        Task { await model.runToastOverlap() }
                    }
                    SampleDivider()
                    if let lastResult = model.lastResult {
                        SampleResultArea(result: lastResult)
                    }
                }
            }
        }
        .background { SampleTheme.surface.ignoresSafeArea() }
        .fullScreenCover(isPresented: $showsLayoutPanel) {
            SampleLayoutPanelScreen(
                onResult: { model.updateResult($0) },
                onClose: { showsLayoutPanel = false }
            )
        }
        .fullScreenCover(isPresented: $showsTransitionPanel) {
            SampleTransitionPanelScreen(
                onResult: { model.updateResult($0) },
                onClose: { showsTransitionPanel = false }
            )
        }
        .task { await autoPlay() }
    }

    /// 起動引数で指定されたデモを、メニュー項目のタップと同じ入口で自動再生する。
    ///
    /// 中身をその場で渡す Inline と、画面状態として開くパネルはこの画面が受け持ち、
    /// 残りは共有 Presenter が受け持つ。
    private func autoPlay() async {
        guard let demo = SampleCaptureAutoPlay.shared.consumeDemo(
            options: SampleCaptureArguments.current
        ) else {
            return
        }
        // コールド起動の直後は key window がまだ無く、ライブラリが「提示できる画面がありません」で
        // 失敗することがある (Debug では assertionFailure で止まる)。提示先が出来るまで待ってから再生する
        await waitUntilPresentationHostIsReady()
        // 初回表示と同じターンで画面状態を変えると全画面表示の提示を取りこぼしたため、
        // MainActor のターンを 1 回譲ってから再生する (9 デモの通し撮影で安定を確認済み)
        await Task.yield()
        switch demo {
        case SampleDemoId.inlineDialog: await model.showInlineDialog()
        case SampleDemoId.transitionDialog: showsTransitionPanel = true
        case SampleDemoId.layoutDialog: showsLayoutPanel = true
        // インライン経路を含むため、この画面が受け持つ
        case SampleDemoId.customToast: model.showCustomToast()
        default: await model.autoPlay(demo)
        }
    }

    /// ライブラリの提示先 (前面でアクティブなシーンの key window の rootViewController) が出来るまで待つ。
    ///
    /// 判定はライブラリの提示可否と同じ条件に揃える。上限を超えたら待たずに進み、
    /// 失敗はそのまま結果表示側 (assertionFailure) に任せる。この画面の Task がキャンセルされたら待ちを打ち切る。
    private func waitUntilPresentationHostIsReady() async {
        for _ in 0..<presentationHostWaitAttempts {
            if hasPresentationHost { return }
            do {
                try await Task.sleep(for: .milliseconds(presentationHostWaitIntervalMilliseconds))
            } catch {
                return
            }
        }
    }

    /// 前面でアクティブなシーンに rootViewController 付きの key window があるか。
    private var hasPresentationHost: Bool {
        UIApplication.shared.connectedScenes.contains { scene in
            guard let windowScene = scene as? UIWindowScene,
                  windowScene.activationState == .foregroundActive else { return false }
            return windowScene.windows.contains { $0.isKeyWindow && $0.rootViewController != nil }
        }
    }
}

/// 提示先の準備待ちの刻み (ミリ秒)。
private let presentationHostWaitIntervalMilliseconds = 50
/// 提示先の準備待ちの上限回数 (50ms × 40 = 2 秒)。
private let presentationHostWaitAttempts = 40
