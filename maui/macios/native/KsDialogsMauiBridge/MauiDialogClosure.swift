import Foundation

/// ダイアログが閉じた理由の判別。
@objc(KSDMauiDialogClosureKind)
public enum MauiDialogClosureKind: Int, Sendable {
    /// 利用者の操作 (キャンセル・外側タップ) で閉じた。
    case cancelled = 0
    /// 呼び出し側からの閉鎖要求で閉じた。
    case dismissed = 1
    /// 提示できる画面が無く、提示に入れなかった。
    case presentationHostUnavailable = 2
    /// それ以外の理由で提示できなかった。理由は error に入る。
    case failed = 3
}

/// 提示1回ごとに1度だけ届く閉鎖の通知。
@objc(KSDMauiDialogClosure)
public final class MauiDialogClosure: NSObject {
    /// 閉じた理由。
    @objc public let kind: MauiDialogClosureKind
    /// 判別のつかない失敗のときの理由。それ以外では nil。
    @objc public let error: NSError?

    init(kind: MauiDialogClosureKind) {
        self.kind = kind
        error = nil
        super.init()
    }

    init(error: any Error) {
        kind = .failed
        self.error = error as NSError
        super.init()
    }
}
