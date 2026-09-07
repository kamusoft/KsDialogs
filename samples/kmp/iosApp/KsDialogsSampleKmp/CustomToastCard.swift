import SampleShared
import SwiftUI

/// 登録経路のカスタム Toast の中身。
///
/// Toast には覆いが無く、デフォルト View も使わないため、この中身が自分で地を描く。
/// 受け取るのは共有コードで定義された ViewModel で、この View だけが iOS 側に置かれる。
struct CustomToastCard: View {
    /// 共有コードで定義された、メッセージを運ぶ ViewModel。
    let viewModel: CustomToastViewModel

    var body: some View {
        HStack(spacing: 10) {
            Text(SampleCustomToastBadge.mark)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(SampleTheme.primary)
                .frame(width: 20, height: 20)
                .background(SampleTheme.onPrimary, in: .circle)

            Text(viewModel.message)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SampleTheme.onPrimary)
        }
        .padding(EdgeInsets(top: 12, leading: 18, bottom: 12, trailing: 18))
        .background(SampleTheme.primary, in: .rect(cornerRadius: 12))
    }
}

/// 登録経路のカスタム Toast が出すバッジの記号。文言ではなく見た目の記号なので View 側に持つ。
enum SampleCustomToastBadge {
    /// バッジの中に出す記号。
    static let mark = "✓"
}
