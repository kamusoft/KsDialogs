import Foundation

/// 互換面から届いた通知を到着順に控える記録口。
///
/// 通知は互換面が UI スレッドへ移して呼ぶが、通知の型 (`(値) -> Void`) は
/// スレッドの取り決めを持たないため、記録側で排他を取る。
final class BridgeTestRecorder<Value>: @unchecked Sendable {
    private let lock = NSLock()
    private var values: [Value] = []

    /// 届いた通知を1件控える。
    func record(_ value: Value) {
        lock.lock()
        values.append(value)
        lock.unlock()
    }

    /// 届いた件数。
    var count: Int {
        lock.lock()
        defer { lock.unlock() }
        return values.count
    }

    /// 届いた通知の一覧 (到着順)。
    var recorded: [Value] {
        lock.lock()
        defer { lock.unlock() }
        return values
    }

    /// 最初に届いた通知。1件も届いていなければ nil。
    var first: Value? {
        recorded.first
    }
}

/// 呼ばれた回数だけを数える記録口。
final class BridgeTestCallCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var value = 0

    /// 1回分を数える。
    func increment() {
        lock.lock()
        value += 1
        lock.unlock()
    }

    /// 数えた回数。
    var count: Int {
        lock.lock()
        defer { lock.unlock() }
        return value
    }
}
