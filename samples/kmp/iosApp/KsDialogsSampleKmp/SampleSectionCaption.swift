import SwiftUI

/// 調整画面の区画の見出し。
struct SampleSectionCaption: View {
    /// 見出しの文言。
    let title: String

    var body: some View {
        Text(title)
            .font(.system(size: 12))
            .foregroundStyle(SampleTheme.onSurfaceMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 12, leading: 16, bottom: 8, trailing: 16))
    }
}
