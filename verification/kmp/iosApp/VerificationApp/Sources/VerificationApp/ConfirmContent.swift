// 登録 API を呼ぶ側がこの package を 1 つ import すれば済むよう、配布物の公開面をそのまま通す。
// アプリ target は共有モジュールの framework をリンクする側にあるため、共有 ViewModel を
// キーにした登録はアプリ target 側に置く。
@_exported import KsDialogs
import SwiftUI

/// 確認ダイアログの中身。
///
/// 結果報告口は表示ごとに渡され、完了か取り消しのどちらか 1 回だけを報告する。
public struct ConfirmContent: View {
    private let message: String
    private let notifier: DialogNotifier<Bool>

    /// 中身を作る。
    /// - Parameters:
    ///   - message: 表示する本文
    ///   - notifier: 結果報告口
    public init(message: String, notifier: DialogNotifier<Bool>) {
        self.message = message
        self.notifier = notifier
    }

    public var body: some View {
        VStack(spacing: 12) {
            Text(message)
            HStack(spacing: 12) {
                Button("Cancel") { notifier.cancel() }
                Button("OK") { notifier.complete(true) }
            }
        }
        .padding()
    }
}
