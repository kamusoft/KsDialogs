#if canImport(UIKit)
import KsDialogs
import SwiftUI

/// ナビゲーションバーを持つタブの画面。`NavigationStack` の中身の枠に `ksDialogCurrentPage()` を付ける。
struct CurrentPageSwiftUIPanelPage: View {
    var body: some View {
        NavigationStack {
            Color.clear
                .ksDialogCurrentPage()
                .navigationTitle("Panel")
                .navigationBarTitleDisplayMode(.inline)
        }
    }
}
#endif
