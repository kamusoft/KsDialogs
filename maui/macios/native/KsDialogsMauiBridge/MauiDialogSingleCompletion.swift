import Foundation

/// 完了通知をちょうど1回だけ通す門。
///
/// 演出の実行口へ渡す完了通知は、呼び出し側の作りによって複数回届き得る。
/// 2回目以降をここで落とすことで、器が受け取る完了はつねに1回になる。
/// 任意のスレッドから呼ばれるためロックで保護する。
final class MauiDialogSingleCompletion: @unchecked Sendable {
    private let lock = NSLock()
    private var handler: (() -> Void)?

    /// - Parameter handler: 最初の1回だけ実行する処理。
    init(_ handler: @escaping () -> Void) {
        self.handler = handler
    }

    /// 完了を通す。2回目以降は何も起こさない。
    func complete() {
        lock.lock()
        let pending = handler
        handler = nil
        lock.unlock()
        pending?()
    }
}
