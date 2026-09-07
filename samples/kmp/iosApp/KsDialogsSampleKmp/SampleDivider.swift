import SwiftUI

/// メニューの行を仕切る 1pt の区切り線。
struct SampleDivider: View {
    var body: some View {
        Rectangle()
            .fill(SampleTheme.divider)
            .frame(height: 1)
    }
}
