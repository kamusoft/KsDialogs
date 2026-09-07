import SwiftUI

/// Transition Dialog の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
struct TransitionDialogCard: View {
    /// 表示するメッセージ。
    let message: String
    /// キャンセル操作。
    let onCancel: () -> Void
    /// 完了操作。
    let onComplete: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Text(message)
                .font(.system(size: 15))
                .foregroundStyle(SampleTheme.onSurface)
                .multilineTextAlignment(.center)

            // ボタンの高さは iOS の推奨タップ領域 44pt を下限にする
            HStack(spacing: 10) {
                Button(action: onCancel) {
                    Text(SampleText.cancelAction)
                        .font(.system(size: 14))
                        .foregroundStyle(SampleTheme.onSurface)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))

                Button(action: onComplete) {
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
    TransitionDialogCard(
        message: SampleText.transitionDialogMessage,
        onCancel: {},
        onComplete: {}
    )
}
