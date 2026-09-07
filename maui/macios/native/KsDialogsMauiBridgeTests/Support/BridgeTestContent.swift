import KsDialogsMauiBridge
import UIKit

/// 中身として渡す最小の View。器のレイアウトが決まるだけの大きさを持つ。
final class BridgeTestContentView: UIView {
    override var intrinsicContentSize: CGSize {
        CGSize(width: 120, height: 80)
    }
}

/// 中身の供給元 (MAUI 側) の代わりになるテスト用の供給。
///
/// 供給は提示先が確保できた後に UI スレッドで呼ばれるが、供給の型はスレッドの取り決めを
/// 持たないため、UI スレッド上にいることを前提として中身を組み立てる。
enum BridgeTestContent {
    /// 中身を1回分作る。
    @MainActor
    static func make() -> MauiDialogContent {
        MauiDialogContent(
            view: BridgeTestContentView(),
            options: MauiDialogOptions(),
            placement: MauiDialogPlacement()
        )
    }

    /// 中身を作れる供給。呼ばれた回数を counter に数える。
    static func supplying(_ counter: BridgeTestCallCounter) -> () -> MauiDialogContent? {
        {
            counter.increment()
            // 供給は互換面が UI スレッド上で呼ぶ取り決めであり、作った中身が他のスレッドへ
            // 渡ることはない。供給の型がスレッドの取り決めを持たないため、ここで明示する。
            nonisolated(unsafe) var content: MauiDialogContent?
            MainActor.assumeIsolated { content = make() }
            return content
        }
    }

    /// 中身を作れない供給 (MAUI 側が中身を作れなかった状態)。呼ばれた回数を counter に数える。
    static func unavailable(_ counter: BridgeTestCallCounter) -> () -> MauiDialogContent? {
        {
            counter.increment()
            return nil
        }
    }
}
