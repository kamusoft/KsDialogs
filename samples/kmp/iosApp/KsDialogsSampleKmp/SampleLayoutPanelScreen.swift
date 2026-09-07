import SampleShared
import SwiftUI

/// レイアウト属性を調整してからダイアログを表示する画面。
struct SampleLayoutPanelScreen: View {
    /// 確定した結果をメニュー画面へ渡す。
    let onResult: (String) -> Void
    /// メニュー画面へ戻る。
    let onClose: () -> Void

    @State private var model = SampleLayoutPanelModel()

    var body: some View {
        VStack(spacing: 0) {
            SampleLayoutPanelHeader(
                onBack: onClose,
                onShow: { Task { await showLayoutDialog() } }
            )
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.HORIZONTAL_LABEL) {
                SampleAlignmentSegments(
                    axisLabel: SampleText.shared.HORIZONTAL_LABEL,
                    selection: $model.horizontalAlignment
                )
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.VERTICAL_LABEL) {
                SampleAlignmentSegments(
                    axisLabel: SampleText.shared.VERTICAL_LABEL,
                    selection: $model.verticalAlignment
                )
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.OFFSET_X_LABEL) {
                SampleOffsetField(label: SampleText.shared.OFFSET_X_LABEL, text: $model.offsetX)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.OFFSET_Y_LABEL) {
                SampleOffsetField(label: SampleText.shared.OFFSET_Y_LABEL, text: $model.offsetY)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.USE_VISIBLE_AREA_LABEL) {
                Toggle("", isOn: $model.usesVisibleArea)
                    .labelsHidden()
                    .tint(SampleTheme.primary)
                    // 行の項目名は別の要素なので、トグル自身にも読み上げ名を与える
                    .accessibilityLabel(SampleText.shared.USE_VISIBLE_AREA_LABEL)
            }
            SampleDivider()

            if let lastResult = model.lastResult {
                SampleResultArea(result: lastResult)
                    .background(SampleTheme.surfaceVariant)
            }

            Spacer(minLength: 0)
        }
        .background { SampleTheme.surface.ignoresSafeArea() }
    }

    private func showLayoutDialog() async {
        if let result = await model.showLayoutDialog() {
            onResult(result)
        }
    }
}
