import SwiftUI

/// Sample のメニュー画面。デモ項目の一覧と直近の結果を表示する。
struct SampleMenuScreen: View {
    @State private var model = SampleMenuModel()
    @State private var showsLayoutPanel = false
    @State private var showsTransitionPanel = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        VStack(spacing: 0) {
            SampleMenuHeader()
            SampleDivider()
            // デモ項目が画面の高さを超える端末でも結果表示エリアまで到達できるよう、
            // 見出しから下をスクロールできる容器に入れる (トランジションのデモ画面と同じ構成)
            ScrollView {
                VStack(spacing: 0) {
                    SampleMenuItemRow(title: SampleText.basicDialogItem) {
                        Task { await model.showBasicDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.layoutDialogItem) {
                        showsLayoutPanel = true
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.declarativeDialogItem) {
                        Task { await model.showDeclarativeDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.textInputDialogItem) {
                        Task { await model.showTextInputDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.inlineDialogItem) {
                        Task { await model.showInlineDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.transitionDialogItem) {
                        showsTransitionPanel = true
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.modelDialogItem) {
                        Task { await model.showModelDialog() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.defaultLoadingItem) {
                        Task { await model.runDefaultLoading() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.customLoadingItem) {
                        Task { await model.runCustomLoading() }
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.defaultToastItem) {
                        model.showDefaultToast()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.customToastItem) {
                        model.showCustomToast()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.toastStackItem) {
                        model.showToastStack()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.toastPlacementItem) {
                        model.showToastPlacement()
                    }
                    SampleDivider()
                    SampleMenuItemRow(title: SampleText.toastOverlapItem) {
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
        .onChange(of: scenePhase, initial: true) { _, newPhase in
            // 自動再生は、シーンが前面でアクティブになってから始める。
            // ライブラリは前面でアクティブなシーンの key window を提示先にするため、
            // それより前に再生すると Dialog は提示先が無いとして失敗し、Loading と Toast は画面に出ない。
            // 取り出しは 1 回限りなので、背面から戻って再びアクティブになっても繰り返さない。
            // シーンの状態が変わっても再生中のデモを打ち切らないよう、この画面の task ではなく独立した Task で走らせる
            guard newPhase == .active else { return }
            Task { await autoPlay() }
        }
    }

    /// 起動引数で指定されたデモを、メニュー項目のタップと同じ入口で自動再生する。
    private func autoPlay() async {
        guard let demo = SampleCaptureAutoPlay.consumeDemo() else { return }
        switch demo {
        case .basicDialog: await model.showBasicDialog()
        case .declarativeDialog: await model.showDeclarativeDialog()
        case .modelDialog: await model.showModelDialog()
        case .textInputDialog: await model.showTextInputDialog()
        case .inlineDialog: await model.showInlineDialog()
        case .transitionDialog: showsTransitionPanel = true
        case .layoutDialog: showsLayoutPanel = true
        case .defaultLoading: await model.runDefaultLoading()
        case .customLoading: await model.runCustomLoading()
        case .defaultToast: model.showDefaultToast()
        case .customToast: model.showCustomToast()
        case .toastStack: model.showToastStack()
        case .toastPlacement: model.showToastPlacement()
        case .toastOverlap: await model.runToastOverlap()
        }
    }
}

#Preview {
    SampleMenuScreen()
}
