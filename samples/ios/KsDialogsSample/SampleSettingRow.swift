import SwiftUI

/// 属性調整パネルの1行。左に項目名、右に操作部を置く。
struct SampleSettingRow<Control: View>: View {
    /// 項目名。
    let title: String
    /// 行の右端に置く操作部。
    @ViewBuilder let control: () -> Control

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.system(size: 14))
                .foregroundStyle(SampleTheme.onSurface)
            Spacer(minLength: 8)
            control()
        }
        .padding(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
        // 調整用の行も押しやすさの下限を満たす高さを確保する
        .frame(minHeight: 48)
    }
}
