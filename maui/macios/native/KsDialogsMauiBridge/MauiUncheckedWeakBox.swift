/// 参照を弱く持ったまま並行性検査を跨いで運ぶための箱。
///
/// 中身のスレッド安全性は受け渡し元の責務であり、この箱は保証しない。
/// 中身が解放されたあとは nil を返す。
final class MauiUncheckedWeakBox<Value: AnyObject>: @unchecked Sendable {
    weak var value: Value?

    init(_ value: Value) {
        self.value = value
    }
}
