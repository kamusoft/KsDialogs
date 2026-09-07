import SampleShared
import SwiftUI

/// 直近の結果を表示するエリア。
struct SampleResultArea: View {
    /// 直近の結果の表示文言。
    let result: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(SampleText.shared.RESULT_CAPTION)
                .font(.system(size: 12))
                .foregroundStyle(SampleTheme.onSurfaceMuted)
            Text(result)
                .font(.system(size: 14, design: .monospaced))
                .foregroundStyle(SampleTheme.onSurface)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
    }
}
