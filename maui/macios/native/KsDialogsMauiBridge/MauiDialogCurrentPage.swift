import KsDialogs
import UIKit

/// 基準領域「表示中のページ」のページを MAUI 側から教える口 (ObjC 互換面の表現)。
///
/// 表示中のページは MAUI 側がページ構成を辿って決め、その platform view を返す関数をここに登録する。
/// この面は受け取った関数をそのまま Native ライブラリの登録口 (`DialogCurrentPage.provider`) へ渡すだけで、
/// ページの選び方にも矩形の求め方にも関与しない。
@objc(KSDMauiDialogCurrentPage)
public final class MauiDialogCurrentPage: NSObject {
    /// 表示中のページの View を返す関数を登録する。`nil` を渡すと登録を解除する。
    ///
    /// Native ライブラリは表示の開始時に登録内容を捕まえるため、差し替えは次の表示から効く。
    /// 関数は UI スレッドで呼ばれる。
    /// - Parameter provider: 表示中のページの View を返す関数。見つからなければ nil を返す。
    @objc(setProvider:)
    @MainActor
    public static func setProvider(_ provider: (() -> UIView?)?) {
        guard let provider else {
            DialogCurrentPage.provider = nil
            return
        }
        // ObjC 境界から来た関数は Sendable と宣言されていないが、呼ぶのは常に UI スレッドに限られる
        let boxed = MauiUncheckedSendableBox(provider)
        DialogCurrentPage.provider = { boxed.value() }
    }
}
