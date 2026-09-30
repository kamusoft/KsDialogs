import SwiftUI

/// 選択肢を横一列に並べて 1 つを選ぶセグメント。
struct SampleSegments<Choice: SampleSegmentChoice>: View {
    /// このセグメントが属する行の項目名。同じ文言の選択肢が複数の行に並ぶときだけ渡し、
    /// 読み上げ名を行ごとに一意にする。nil なら選択肢の文言だけを読み上げ名にする。
    var axisLabel: String?
    /// 行の幅いっぱいに広げ、選択肢の幅を等分するか。
    var fillsWidth = false
    /// 選択中の選択肢。
    @Binding var selection: Choice

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Choice.allCases) { choice in
                Button {
                    selection = choice
                } label: {
                    Text(choice.label)
                        .font(.system(size: 13, weight: choice == selection ? .semibold : .regular))
                        .foregroundStyle(choice == selection ? SampleTheme.onPrimary : SampleTheme.onSurfaceMuted)
                        .lineLimit(1)
                        .padding(.horizontal, 12)
                        .frame(maxWidth: fillsWidth ? .infinity : nil, minHeight: 34)
                        .background(
                            choice == selection ? SampleTheme.primary : Color.clear,
                            in: .rect(cornerRadius: 8)
                        )
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(accessibilityName(of: choice))
                .accessibilityAddTraits(choice == selection ? [.isSelected] : [])
            }
        }
        .padding(3)
        .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))
    }

    /// 選択肢の読み上げ名。同じ文言が2行に並ぶ場合は行の文言と組にして一意にする。
    private func accessibilityName(of choice: Choice) -> String {
        guard let axisLabel else { return choice.label }
        return "\(axisLabel) \(choice.label)"
    }
}
