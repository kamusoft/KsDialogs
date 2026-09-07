import SwiftUI

/// Custom Loading の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
/// 進捗は ViewModel の受け口へ転送された値を読むだけで、報告口を直接受け取ることはない。
struct CustomLoadingCard: View {
    /// 進捗を保持する ViewModel。
    let viewModel: CustomLoadingViewModel

    var body: some View {
        VStack(spacing: 12) {
            Text(SampleText.customLoadingTitle)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SampleTheme.onSurface)

            // 進捗の帯。地を敷いたうえに、報告された割合だけ強調色で塗る
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(SampleTheme.surfaceVariant)
                    Capsule()
                        .fill(SampleTheme.primary)
                        .frame(width: proxy.size.width * viewModel.progress)
                }
            }
            .frame(height: 8)

            Text(SampleText.progressPercentage(viewModel.progress))
                .font(.system(size: 12))
                .foregroundStyle(SampleTheme.onSurfaceMuted)
        }
        .padding(20)
        .frame(width: 210)
        .background(SampleTheme.surface, in: .rect(cornerRadius: 14))
    }
}

#Preview {
    CustomLoadingCard(viewModel: CustomLoadingViewModel())
}
