import Foundation
import KsDialogs

/// 提示したダイアログ1枚を閉じる・打ち切るための handle。
///
/// 閉鎖は結果の報告によって起きる。中身を作る前 (提示先の出現を待っている間を含む) に閉鎖を
/// 求められたときは、中身を作らずに Native の show そのものを止める。中身を作ったあとは報告口で閉じる。
/// 閉鎖は何度求めても1回しか効かない (結果の確定がちょうど1回だから)。
///
/// 打ち切りは Native の show を走らせている Task を止める。待っている間なら一度も表示されず、
/// 表示中なら閉じて、どちらも閉鎖の通知は cancelled で届く。
@objc(KSDMauiDialogPresentation)
public final class MauiDialogPresentation: NSObject, @unchecked Sendable {
    private let lock = NSLock()
    private var notifier: DialogNotifier<Bool>?
    private var isDismissRequested = false
    private var isCancelRequested = false
    private var show: Task<Void, Never>?

    /// 提示した1枚を閉じる。
    @objc
    public func dismiss() {
        lock.lock()
        if let notifier {
            self.notifier = nil
            lock.unlock()
            notifier.complete(true)
            return
        }
        isDismissRequested = true
        let show = show
        lock.unlock()
        // 中身を作る前なら、提示先の出現を待たずに show を止める。
        show?.cancel()
    }

    /// この show を打ち切る。待っている間なら一度も表示せず、表示中なら閉じる。
    /// 閉鎖の通知は cancelled で届く。結果が確定済みなら、確定した結果のまま閉じる。
    @objc
    public func cancel() {
        lock.lock()
        isCancelRequested = true
        let show = show
        lock.unlock()
        show?.cancel()
    }

    /// Native の show を走らせている Task を結び付ける。
    /// 結び付ける前に止めることを求められていたら、すぐに止める。
    func bind(show task: Task<Void, Never>) {
        lock.lock()
        show = task
        let isStopRequested = isDismissRequested || isCancelRequested
        lock.unlock()
        if isStopRequested {
            task.cancel()
        }
    }

    /// 中身を作ってよいか。閉鎖か打ち切りをすでに求められていれば作らない。
    func canSupplyContent() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return !isDismissRequested && !isCancelRequested
    }

    /// 中身を作る前に閉鎖を求められて、show を止めたか。
    /// 打ち切りでもあるときは、打ち切りとして扱う。
    var wasDismissedBeforeContent: Bool {
        lock.lock()
        defer { lock.unlock() }
        return isDismissRequested && !isCancelRequested
    }

    /// その提示の報告口を結び付ける。
    func attach(_ notifier: DialogNotifier<Bool>) {
        lock.lock()
        if isDismissRequested {
            // 中身を作り始めたあとに届いた閉鎖要求。作った中身はすぐに閉じる。
            isDismissRequested = false
            lock.unlock()
            notifier.complete(true)
        } else {
            self.notifier = notifier
            lock.unlock()
        }
    }
}
