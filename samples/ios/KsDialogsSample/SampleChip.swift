import SwiftUI

/// 選択肢を1つ表すチップ。
struct SampleChip: View {
    /// チップの文言。読み上げ名にもそのまま使う。
    let label: String
    /// 選択中か。
    let isSelected: Bool
    /// チップの高さの下限。
    let minHeight: CGFloat
    /// 文言の大きさ。
    let fontSize: CGFloat
    /// 選択操作。
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: fontSize, weight: isSelected ? .semibold : .regular))
                .foregroundStyle(isSelected ? SampleTheme.onPrimary : SampleTheme.onSurface)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .padding(.horizontal, 6)
                .frame(maxWidth: .infinity, minHeight: minHeight)
                .background(isSelected ? SampleTheme.primary : SampleTheme.surface, in: .rect(cornerRadius: 10))
                .overlay {
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(isSelected ? SampleTheme.primary : SampleTheme.divider, lineWidth: 1)
                }
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
