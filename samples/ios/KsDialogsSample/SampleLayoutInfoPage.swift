import SwiftUI

/// タイトルバーを持たないタブの中身。説明文と、パネルの設定をそのまま使う表示操作を置く。
struct SampleLayoutInfoPage: View {
    /// パネルの設定でダイアログを出す操作。
    let onShow: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(SampleText.infoTabBody)
                .font(.system(size: 14))
                .foregroundStyle(SampleTheme.onSurface)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            Button(action: onShow) {
                Text(SampleText.showAction)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(SampleTheme.onPrimary)
                    .padding(.horizontal, 18)
                    .frame(minHeight: 44)
                    .background(SampleTheme.primary, in: .rect(cornerRadius: 10))
            }
            .buttonStyle(.plain)

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 24, leading: 16, bottom: 16, trailing: 16))
    }
}
