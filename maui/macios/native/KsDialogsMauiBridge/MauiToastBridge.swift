import KsDialogs
import UIKit

/// MAUI 形態の Toast のための ObjC 互換面 (maui/ADR-0001)。
///
/// 表示中のリスト・重なり順・各表示の計時はすべて Native ライブラリの coordinator が持ち、
/// この面は MAUI 側の呼び出しをそこへ渡すだけである (core/ADR-0030)。
/// ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
/// カスタム Toast の中身は表示のたびに MAUI 側から供給される。
@objc(KSDMauiToastBridge)
public final class MauiToastBridge: NSObject, Sendable {
    /// 既定の共有インスタンス。
    @objc(sharedBridge)
    public static let shared = MauiToastBridge()

    private let toast: any KsToast

    public override init() {
        let toast = Toast()
        // 中身は表示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        toast.registry.register(MauiToastViewModel.self) { viewModel in
            try viewModel.makeContentView()
        }
        self.toast = toast
        super.init()
    }

    /// Toast の一括設定を反映する。器は各表示の受理時にこの値を読む (core/ADR-0032)。
    /// - Parameter style: MAUI 側で設定された一括設定。
    @objc(applyStyle:)
    public func applyStyle(_ style: MauiToastStyle) {
        toast.style = ToastStyle(style)
    }

    /// 表示1枚を受理する。
    ///
    /// fire-and-forget なので結末の通知は無く、受理そのものも失敗しない (core/ADR-0031)。
    /// この面が使う ViewModel 型は常に登録済みなので、登録経路の解決も失敗しない。
    /// - Parameter content: 表示する中身の指定。
    @objc(showContent:)
    public func show(_ content: MauiToastContent) {
        let placement = content.placement.map { DialogPlacement($0) }
        let duration = content.duration?.intValue
        guard let contentProvider = content.contentProvider else {
            toast.show(message: content.message ?? "", duration: duration, placement: placement)
            return
        }
        try? toast.show(
            MauiToastViewModel(contentProvider: contentProvider),
            duration: duration,
            placement: placement
        )
    }
}

/// MAUI 側から供給される View をそのまま中身にするカスタム Toast の ViewModel。
///
/// この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの1種類が使われる。
///
/// ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
/// 器へ渡る (core/ADR-0015)。
final class MauiToastViewModel: ToastViewModel, @unchecked Sendable {
    private let contentProvider: MauiToastContentProvider

    init(contentProvider: @escaping MauiToastContentProvider) {
        self.contentProvider = contentProvider
    }

    /// 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
    /// 提示先が確保できた後に UI スレッドで呼ばれる。
    ///
    /// MAUI 側が中身を作れなかったときは失敗を投げる。器はそれを受理後の失敗として扱い、
    /// 警告を残してこの1枚だけを破棄する (他の表示には影響しない)。
    @MainActor
    func makeContentView() throws -> UIView {
        guard let content = contentProvider() else {
            throw MauiDialogBridgeError.contentUnavailable
        }
        content.applyAttributes(options: content.options, placement: content.placement)
        return content.view
    }
}
