import SampleShared
import SwiftUI

/// 属性調整パネルのタイトル帯。メニューへ戻る操作と表示操作を持つ。
struct SampleLayoutPanelHeader: View {
    /// メニュー画面へ戻る操作。
    let onBack: () -> Void
    /// 調整した属性でダイアログを出す操作。
    let onShow: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Text("‹")
                    .font(.system(size: 20))
                    .foregroundStyle(SampleTheme.onSurface)
                    // 記号1文字でも押しやすさの下限を満たす領域を確保する
                    .frame(width: 44, height: 44, alignment: .leading)
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)
            // 記号のままでは読み上げが操作の意味を伝えないため、名前を与える
            .accessibilityLabel("戻る")

            Text(SampleText.shared.LAYOUT_DIALOG_ITEM)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(SampleTheme.onSurface)

            Spacer(minLength: 8)

            Button(action: onShow) {
                Text(SampleText.shared.SHOW_ACTION)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(SampleTheme.onPrimary)
                    .padding(.horizontal, 18)
                    .frame(minHeight: 44)
                    .background(SampleTheme.primary, in: .rect(cornerRadius: 10))
            }
            .buttonStyle(.plain)
        }
        .padding(EdgeInsets(top: 20, leading: 16, bottom: 12, trailing: 16))
    }
}
