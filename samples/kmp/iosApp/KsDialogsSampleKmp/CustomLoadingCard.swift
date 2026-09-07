import Observation
import SampleShared
import SwiftUI

/// Custom Loading の中身。
///
/// 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
/// 進捗は共有 ViewModel の受け口へ転送された値を購読して映すだけで、報告口を直接受け取ることはない。
struct CustomLoadingCard: View {
    /// 共有コードで定義された、進捗を保持する ViewModel。
    let viewModel: CustomLoadingViewModel

    /// 共有 ViewModel の進捗を SwiftUI が観測できる形に写す入れ物。
    @State private var progress = CustomLoadingProgress()

    var body: some View {
        VStack(spacing: 12) {
            Text(SampleText.shared.CUSTOM_LOADING_TITLE)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SampleTheme.onSurface)

            // 進捗の帯。地を敷いたうえに、報告された割合だけ強調色で塗る
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(SampleTheme.surfaceVariant)
                    Capsule()
                        .fill(SampleTheme.primary)
                        .frame(width: proxy.size.width * progress.value)
                }
            }
            .frame(height: 8)

            Text(SampleText.shared.progressPercentage(progress: progress.value))
                .font(.system(size: 12))
                .foregroundStyle(SampleTheme.onSurfaceMuted)
        }
        .padding(20)
        .frame(width: 210)
        .background(SampleTheme.surface, in: .rect(cornerRadius: 14))
        .onAppear {
            let sink = progress
            sink.value = viewModel.progress
            // 共有 ViewModel の受け口は UI スレッド上で呼ばれる契約なので、その前提で写し取る
            viewModel.onProgressChanged = { reported in
                MainActor.assumeIsolated { sink.value = reported.doubleValue }
            }
        }
        .onDisappear {
            // 撤去された View が更新を受け取り続けないよう、購読を外す
            viewModel.onProgressChanged = nil
        }
    }
}

/// 共有 ViewModel から届いた進捗を SwiftUI へ橋渡しする観測可能な入れ物。
///
/// 共有コードの ViewModel は Swift の観測機構を持たないため、写し先をこちらに用意する。
@MainActor
@Observable
final class CustomLoadingProgress {
    /// 0〜1 に丸めた後の進捗。
    var value: Double = 0
}
