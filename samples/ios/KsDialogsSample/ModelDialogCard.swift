import SwiftUI

/// Model Dialog の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
/// 見た目は Basic Dialog と同一で、違うのは結果の報告経路 —
/// 報告口を factory の引数で受け取らず、ViewModel が自分で報告する — だけである。
struct ModelDialogCard: View {
    /// 表示と報告を受け持つ ViewModel。
    let viewModel: ModelDialogViewModel

    var body: some View {
        VStack(spacing: 20) {
            Text(viewModel.message)
                .font(.system(size: 15))
                .foregroundStyle(SampleTheme.onSurface)
                .multilineTextAlignment(.center)

            // ボタンの高さは iOS の推奨タップ領域 44pt を下限にする
            HStack(spacing: 10) {
                Button { viewModel.cancel() } label: {
                    Text(SampleText.cancelAction)
                        .font(.system(size: 14))
                        .foregroundStyle(SampleTheme.onSurface)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))

                Button { viewModel.complete() } label: {
                    Text(SampleText.completeAction)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(SampleTheme.onPrimary)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(SampleTheme.primary, in: .rect(cornerRadius: 10))
            }
        }
        .padding(EdgeInsets(top: 24, leading: 20, bottom: 20, trailing: 20))
        .frame(width: 272)
        .background(SampleTheme.surface, in: .rect(cornerRadius: 20))
    }
}

#Preview {
    let viewModel = ModelDialogViewModel()
    viewModel.message = SampleText.modelDialogMessage
    return ModelDialogCard(viewModel: viewModel)
}
