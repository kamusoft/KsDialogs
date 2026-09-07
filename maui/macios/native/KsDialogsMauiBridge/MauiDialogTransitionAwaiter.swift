import Foundation

/// 完了コールバック型の実行口を、待てる形 (async) に変える橋渡し。
///
/// 完了通知と待ちの打ち切りのどちらが先に来ても、待っている側はちょうど1回だけ解放される。
/// 打ち切りで解放したあとに完了通知が届いても何も起こらない。
/// 任意のスレッドから触られるためロックで保護する。
final class MauiDialogTransitionAwaiter: @unchecked Sendable {
    private let lock = NSLock()
    private var continuation: CheckedContinuation<Void, Never>?
    private var isFinished = false

    /// 待ち手を結び付ける。すでに終わっていればその場で解放する。
    func attach(_ continuation: CheckedContinuation<Void, Never>) {
        lock.lock()
        guard !isFinished else {
            lock.unlock()
            continuation.resume()
            return
        }
        self.continuation = continuation
        lock.unlock()
    }

    /// 待ちを終える。2回目以降は何も起こさない。
    func finish() {
        lock.lock()
        guard !isFinished else {
            lock.unlock()
            return
        }
        isFinished = true
        let pending = continuation
        continuation = nil
        lock.unlock()
        pending?.resume()
    }
}
