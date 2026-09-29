#if canImport(UIKit)
import Foundation
import UIKit

/// 表示中の Toast 1枚分の状態 (core/ADR-0030 の「1 Toast 1器」)。
///
/// 中身は取り付けの時点で作るので、取り付け先を待つ間は中身の指定だけを持つ。
/// 取り付けた後は、器・ViewModel・演出フックへの参照を、撤去の完了までここが握る。
/// 消滅の期限は受理時点で決まった単調時計の時刻で持つので、器を作り直しても巻き戻らない。
@MainActor
final class ToastDisplay {
    /// 中身の指定。取り付けの時点でここから中身を作る。
    let request: ToastContentRequest

    /// 受理時点で読んだスタイル。表示中の設定変更には追随しない。
    let style: ToastStyle

    /// カスタム Toast View の ViewModel。取り付け前とデフォルト View では nil。
    var viewModel: AnyObject?

    /// 表示 API の引数で渡された配置。
    let showPlacement: DialogPlacement?

    /// show 引数も添付も無いときに採る配置 (style のアプリ既定配置 か 契約既定値)。
    let fallbackPlacement: DialogPlacement

    /// 消滅の期限。受理時点から duration を進めた単調時計の時刻。
    let deadline: ContinuousClock.Instant

    /// 取り付け済みの器。取り付け先がまだ無い間は nil。
    var container: ToastContainerViewController?

    /// 期限を待つ仕事。
    var timerTask: Task<Void, Never>?

    /// 撤去へ進んだか。期限の到達と後始末が重なっても1回しか進まないための印。
    var isFinishing = false

    init(
        request: ToastContentRequest,
        style: ToastStyle,
        showPlacement: DialogPlacement?,
        fallbackPlacement: DialogPlacement,
        deadline: ContinuousClock.Instant
    ) {
        self.request = request
        self.style = style
        self.showPlacement = showPlacement
        self.fallbackPlacement = fallbackPlacement
        self.deadline = deadline
    }

    /// 撤去の完了後に、保持していた参照を手放す。
    func releaseResources() {
        container = nil
        timerTask = nil
        viewModel = nil
    }
}
#endif
