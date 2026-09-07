import SwiftUI

/// インライン経路のカスタム Toast の中身。
///
/// 登録経路と同じく地を自分で描き、登録経路と見分けが付くよう無彩色の配色にする。
struct InlineToastCard: View {
    /// メッセージを運ぶ ViewModel。
    let viewModel: InlineToastViewModel

    var body: some View {
        Text(viewModel.message)
            .font(.system(size: 14))
            .foregroundStyle(SampleTheme.onSurface)
            .padding(EdgeInsets(top: 11, leading: 18, bottom: 11, trailing: 18))
            .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 12))
            .overlay {
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(SampleTheme.divider, lineWidth: 1)
            }
    }
}
