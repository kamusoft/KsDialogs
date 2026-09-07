import SwiftUI

/// 配置を3択から選ぶセグメント。
struct SampleAlignmentSegments: View {
    /// このセグメントが属する行の項目名。読み上げ名を行ごとに一意にするために使う。
    let axisLabel: String
    /// 選択中の配置。
    @Binding var selection: SampleAlignmentChoice

    var body: some View {
        HStack(spacing: 0) {
            ForEach(SampleAlignmentChoice.allCases) { choice in
                Button {
                    selection = choice
                } label: {
                    Text(choice.label)
                        .font(.system(size: 13, weight: choice == selection ? .semibold : .regular))
                        .foregroundStyle(choice == selection ? SampleTheme.onPrimary : SampleTheme.onSurfaceMuted)
                        .padding(.horizontal, 12)
                        .frame(minHeight: 34)
                        .background(
                            choice == selection ? SampleTheme.primary : Color.clear,
                            in: .rect(cornerRadius: 8)
                        )
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                // 同じ文言の選択肢が2行に並ぶため、読み上げ名は行の文言と組にして一意にする
                .accessibilityLabel("\(axisLabel) \(choice.label)")
                .accessibilityAddTraits(choice == selection ? [.isSelected] : [])
            }
        }
        .padding(3)
        .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))
    }
}
