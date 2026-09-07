import SwiftUI

/// 移動量を入れる数値欄。
struct SampleOffsetField: View {
    /// この欄が属する行の項目名。画面には出さず、読み上げ名として与える。
    let label: String
    /// 入力中の文字列。数値として読めない間は 0 として扱う。
    @Binding var text: String

    var body: some View {
        TextField("", text: $text)
            .accessibilityLabel(label)
            .keyboardType(.numbersAndPunctuation)
            .multilineTextAlignment(.trailing)
            .font(.system(size: 14, design: .monospaced))
            .foregroundStyle(SampleTheme.onSurface)
            .padding(.horizontal, 10)
            .frame(width: 64, height: 36)
            .background {
                RoundedRectangle(cornerRadius: 8)
                    .stroke(SampleTheme.divider, lineWidth: 1)
            }
    }
}
