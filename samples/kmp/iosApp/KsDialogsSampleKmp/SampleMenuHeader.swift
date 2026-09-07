import SampleShared
import SwiftUI

/// メニュー画面のタイトル帯。
struct SampleMenuHeader: View {
    var body: some View {
        Text(SampleText.shared.MENU_TITLE)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(SampleTheme.onSurface)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 20, leading: 16, bottom: 12, trailing: 16))
    }
}
