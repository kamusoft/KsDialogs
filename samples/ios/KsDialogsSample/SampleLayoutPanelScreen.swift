import SwiftUI

/// レイアウト属性を調整してからダイアログを表示する画面。
///
/// 下部のタブバーで、ナビゲーションバーを持つパネルのタブと、タイトルバーを持たない説明のタブを切り替える。
/// 基準領域「表示中のページ」がバーを除いた領域になることを 2 つのタブで見比べられるよう、
/// 各タブの中身の枠 (バーの内側) を表示中のページとして名乗らせる。
struct SampleLayoutPanelScreen: View {
    /// 確定した結果をメニュー画面へ渡す。
    let onResult: (String) -> Void
    /// メニュー画面へ戻る。
    let onClose: () -> Void

    @State private var model = SampleLayoutPanelModel()
    @State private var selectedTab = SampleLayoutPanelTab.panel

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                SampleLayoutPanelForm(model: model)
                    .ksDialogCurrentPage()
                    .background { SampleTheme.surface.ignoresSafeArea() }
                    .navigationTitle(SampleText.layoutDialogItem)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        SampleLayoutPanelToolbar(
                            onBack: onClose,
                            onShow: { Task { await showLayoutDialog() } }
                        )
                    }
            }
            .tabItem { Label(SampleText.panelTab, systemImage: "list.bullet") }
            .tag(SampleLayoutPanelTab.panel)

            SampleLayoutInfoPage(onShow: { Task { await showLayoutDialog() } })
                .ksDialogCurrentPage()
                .background { SampleTheme.surface.ignoresSafeArea() }
                .tabItem { Label(SampleText.infoTab, systemImage: "info.circle") }
                .tag(SampleLayoutPanelTab.info)
        }
        .tint(SampleTheme.primary)
    }

    private func showLayoutDialog() async {
        if let result = await model.showLayoutDialog() {
            onResult(result)
        }
    }
}

#Preview {
    SampleLayoutPanelScreen(onResult: { _ in }, onClose: {})
}
