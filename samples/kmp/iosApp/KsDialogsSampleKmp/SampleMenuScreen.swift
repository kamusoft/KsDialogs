import SampleShared
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
    ///
    /// 中身をその場で渡す Inline と、画面状態として開くパネルはこの画面が受け持ち、
    /// 残りは共有 Presenter が受け持つ。
    private func autoPlay() async {
        guard let demo = SampleCaptureAutoPlay.shared.consumeDemo(
            options: SampleCaptureArguments.current
        ) else {
            return
        }
        switch demo {
        case SampleDemoId.inlineDialog: await model.showInlineDialog()
        case SampleDemoId.transitionDialog: showsTransitionPanel = true
        case SampleDemoId.layoutDialog: showsLayoutPanel = true
        // インライン経路を含むため、この画面が受け持つ
        case SampleDemoId.customToast: model.showCustomToast()
        default: await model.autoPlay(demo)
        }
    }
}
