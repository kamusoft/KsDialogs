#if canImport(UIKit)
import UIKit

/// ダイアログの提示起点となる window を供給する。
/// アプリの window 取得手段を差し替えられるようにするための内部の継ぎ目。
///
/// 提示先を選ぶ規則 (`keyWindow`) と、提示先が現れたかもしれないことの合図
/// (`observeHostAppearance`) を同じ供給元に置く。規則を変えるときに、合図の見る通知も
/// 一緒に見直せるようにするため。
protocol DialogKeyWindowProvider: Sendable {
    @MainActor var keyWindow: UIWindow? { get }

    /// 提示先が現れたかもしれないことの合図を購読する。
    ///
    /// 合図は「現れたかもしれない」ことだけを知らせる。受け取った側は `keyWindow` を読み直して、
    /// 表示するか待ち続けるかを決める。合図の順序や回数に意味を持たせない。
    /// - Parameter handler: 合図のたびに UI スレッドで呼ばれる
    /// - Returns: 購読1件。解除するまで合図が届き続ける
    @MainActor func observeHostAppearance(
        _ handler: @escaping DialogHostAppearanceHandler
    ) -> DialogHostAppearanceRegistration
}
#endif
