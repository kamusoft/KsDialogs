import SampleShared
import SwiftUI

/// 属性調整パネルのタブの中身。配置・移動量・基準領域の行と結果表示エリアを並べる。
struct SampleLayoutPanelForm: View {
    @Bindable var model: SampleLayoutPanelModel

    var body: some View {
        VStack(spacing: 0) {
            SampleSettingRow(title: SampleText.shared.HORIZONTAL_LABEL) {
                SampleSegments(axisLabel: SampleText.shared.HORIZONTAL_LABEL, selection: $model.horizontalAlignment)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.shared.VERTICAL_LABEL) {
                SampleSegments(axisLabel: SampleText.shared.VERTICAL_LABEL, selection: $model.verticalAlignment)
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

            // 3 つの選択肢は横に並べると項目名と同じ行に収まらないため、項目名の下に全幅で置く
            VStack(alignment: .leading, spacing: 8) {
                Text(SampleText.shared.LAYOUT_AREA_LABEL)
                    .font(.system(size: 14))
                    .foregroundStyle(SampleTheme.onSurface)
                SampleSegments(fillsWidth: true, selection: $model.layoutArea)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))
            SampleDivider()

            if let lastResult = model.lastResult {
                SampleResultArea(result: lastResult)
                    .background(SampleTheme.surfaceVariant)
            }

            Spacer(minLength: 0)
        }
    }
}
