import SwiftUI

/// デモ項目を起動する行。
struct SampleMenuItemRow: View {
    /// 起動項目の文言。4ルートの対応を取る単位。
    let title: String
    /// 行の起動操作。
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 0) {
                Text(title)
                    .font(.system(size: 15))
                    .foregroundStyle(SampleTheme.onSurface)
                Spacer(minLength: 8)
                Text("›")
                    .font(.system(size: 15))
                    .foregroundStyle(SampleTheme.onSurfaceMuted)
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
            // 一覧の行も押しやすさの下限を満たす高さを確保する
            .frame(minHeight: 48)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}
