import SampleShared
import SwiftUI

/// Text Input Dialog の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
/// 入力した文字列がそのまま結果値になる。
struct TextInputDialogCard: View {
    /// 表示するメッセージ。
    let message: String
    /// キャンセル操作。
    let onCancel: () -> Void
    /// 完了操作。入力中の文字列を結果として渡す。
    let onComplete: (String) -> Void

    /// 入力中の文字列。初期値は空。
    @State private var input = ""

    var body: some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.system(size: 15))
                .foregroundStyle(SampleTheme.onSurface)
                .multilineTextAlignment(.center)

            // 入力欄もボタンと同じく iOS の推奨タップ領域 44pt を下限にする
            TextField(
                "",
                text: $input,
                prompt: Text(SampleText.shared.TEXT_INPUT_PLACEHOLDER)
                    .foregroundStyle(SampleTheme.onSurfaceMuted)
            )
            .font(.system(size: 15))
            .foregroundStyle(SampleTheme.onSurface)
            .padding(.horizontal, 12)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background {
                RoundedRectangle(cornerRadius: 10)
                    .stroke(SampleTheme.divider, lineWidth: 1)
            }

            HStack(spacing: 10) {
                Button(action: onCancel) {
                    Text(SampleText.shared.CANCEL_ACTION)
                        .font(.system(size: 14))
                        .foregroundStyle(SampleTheme.onSurface)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))

                Button {
                    onComplete(input)
                } label: {
                    Text(SampleText.shared.COMPLETE_ACTION)
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

