import Foundation
import KsDialogs

/// 提示したダイアログ1枚を閉じるための handle。
///
/// 閉鎖は結果の報告によって起きるため、報告口が渡ってくるまでは閉鎖要求を保持し、
/// 渡った時点で実行する (提示の直後に閉鎖を求められても取りこぼさない)。
/// 閉鎖は何度求めても1回しか効かない (結果の確定がちょうど1回だから)。
@objc(KSDMauiDialogPresentation)
public final class MauiDialogPresentation: NSObject, @unchecked Sendable {
    private let lock = NSLock()
    private var notifier: DialogNotifier<Bool>?
    private var isDismissRequested = false

    /// 提示した1枚を閉じる。
    @objc
    public func dismiss() {
        lock.lock()
        if let notifier {
            self.notifier = nil
            lock.unlock()
            notifier.complete(true)
        } else {
            isDismissRequested = true
            lock.unlock()
        }
    }

    /// その提示の報告口を結び付ける。
    func attach(_ notifier: DialogNotifier<Bool>) {
        lock.lock()
        if isDismissRequested {
            isDismissRequested = false
            lock.unlock()
            notifier.complete(true)
        } else {
            self.notifier = notifier
            lock.unlock()
        }
    }
}
