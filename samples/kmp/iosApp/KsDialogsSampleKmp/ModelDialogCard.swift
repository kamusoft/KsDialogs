import KsDialogs
import SampleShared
import SwiftUI

/// Model Dialog の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
/// 見た目は Basic Dialog と同一で、違うのは結果の報告経路 —
/// 報告口を factory の引数で受け取らず、表示中の共有 ViewModel から KMP 面のアクセサで引く —
/// だけである。
struct ModelDialogCard: View {
    /// 共有コードで定義された、表示中の ViewModel。
    let viewModel: ModelDialogViewModel

    var body: some View {
        VStack(spacing: 20) {
            Text(viewModel.message)
                .font(.system(size: 15))
                .foregroundStyle(SampleTheme.onSurface)
                .multilineTextAlignment(.center)

            // ボタンの高さは iOS の推奨タップ領域 44pt を下限にする
            HStack(spacing: 10) {
                Button { notifier()?.cancel() } label: {
                    Text(SampleText.shared.CANCEL_ACTION)
                        .font(.system(size: 14))
                        .foregroundStyle(SampleTheme.onSurface)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 10))

                Button { notifier()?.complete(true) } label: {
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

    /// 表示中のこの ViewModel に紐付いた結果報告口。
    ///
    /// 共有コードの ViewModel は iOS Native の ViewModel 契約に準拠しないため、
    /// `viewModel.notifier` ではなく KMP 面のアクセサから引く (core/ADR-0018・kmp/ADR-0004)。
    /// 結果型の取り違えは型付きの失敗になるが、この Sample は登録と同じ真偽値で引いている。
    @MainActor
    private func notifier() -> DialogNotifier<Bool>? {
        try? Dialog.shared.kmp.notifier(for: viewModel)
    }
}
