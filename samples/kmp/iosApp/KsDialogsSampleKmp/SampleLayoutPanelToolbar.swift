import SampleShared
import SwiftUI

/// 属性調整パネルのナビゲーションバーに置く操作。メニューへ戻る操作と表示操作を持つ。
struct SampleLayoutPanelToolbar: ToolbarContent {
    /// メニュー画面へ戻る操作。
    let onBack: () -> Void
    /// 調整した属性でダイアログを出す操作。
    let onShow: () -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            Button(action: onBack) {
                Text("‹")
                    .font(.system(size: 20))
                    .foregroundStyle(SampleTheme.onSurface)
                    // 記号1文字でも押しやすさの下限を満たす領域を確保する
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(.rect)
            }
            // 記号のままでは読み上げが操作の意味を伝えないため、名前を与える
            .accessibilityLabel("戻る")
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button(SampleText.shared.SHOW_ACTION, action: onShow)
                .buttonStyle(.borderedProminent)
                .tint(SampleTheme.primary)
                .fontWeight(.semibold)
        }
    }
}
