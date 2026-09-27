import SwiftUI

/// 属性調整パネルのタブの中身。配置・移動量・基準領域・余白の行と結果表示エリアを並べる。
struct SampleLayoutPanelForm: View {
    @Bindable var model: SampleLayoutPanelModel

    var body: some View {
        VStack(spacing: 0) {
            SampleSettingRow(title: SampleText.horizontalLabel) {
                SampleSegments(axisLabel: SampleText.horizontalLabel, selection: $model.horizontalAlignment)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.verticalLabel) {
                SampleSegments(axisLabel: SampleText.verticalLabel, selection: $model.verticalAlignment)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.offsetXLabel) {
                SampleOffsetField(label: SampleText.offsetXLabel, text: $model.offsetX)
            }
            SampleDivider()

            SampleSettingRow(title: SampleText.offsetYLabel) {
                SampleOffsetField(label: SampleText.offsetYLabel, text: $model.offsetY)
            }
            SampleDivider()

            // 3 つの選択肢は横に並べると項目名と同じ行に収まらないため、項目名の下に全幅で置く
            VStack(alignment: .leading, spacing: 8) {
                Text(SampleText.layoutAreaLabel)
                    .font(.system(size: 14))
                    .foregroundStyle(SampleTheme.onSurface)
                SampleSegments(fillsWidth: true, selection: $model.layoutArea)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))
            SampleDivider()

            // 余白の選択肢は数字だけでは何の値か読めないため、行の項目名と組にした読み上げ名にする
            SampleSettingRow(title: SampleText.marginLabel) {
                SampleSegments(axisLabel: SampleText.marginLabel, selection: $model.margin)
            }
            SampleDivider()

            if let lastResult = model.lastResult {
                SampleResultArea(result: lastResult)
                    .background(SampleTheme.surfaceVariant)
            }

            Spacer(minLength: 0)
        }
    }
}
