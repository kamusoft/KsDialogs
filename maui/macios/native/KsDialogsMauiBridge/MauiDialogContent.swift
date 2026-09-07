import KsDialogs
import UIKit

/// MAUI 側の演出を実行する口の形。
///
/// 第1引数はコンテンツのホスト View、第2引数は演出が終わったときに呼ぶ完了通知。
/// ObjC 互換面では async を直接表せないため、完了をコールバックで返す形にしてある。
public typealias MauiDialogTransitionRunner = (UIView, @escaping () -> Void) -> Void

/// ダイアログの中身と、それに効くメタ属性を新規に供給する関数。
///
/// 供給元 (MAUI 側) は自分の失敗をこの境界の外へ漏らさず、中身を作れなかったときは nil を返す。
/// managed / native の呼び出し境界を例外が越えると未処理の障害になるため、失敗は値で返す取り決めにしてある。
/// nil を受けた提示は結果を返さずに失敗し、その失敗が閉鎖の通知として MAUI 側へ届く。
public typealias MauiDialogContentProvider = () -> MauiDialogContent?

/// 提示1回分の中身と、その中身に効くメタ属性。
///
/// MAUI 側が読んだメタ属性を受け取り、Native ライブラリの添付面へ写して渡す。
@objc(KSDMauiDialogContent)
public final class MauiDialogContent: NSObject {
    /// ダイアログの中身になる View。
    @objc public let view: UIView

    /// その中身に添付された静的メタ属性。
    @objc public let options: MauiDialogOptions

    /// その提示の置き場所 (MAUI 側で合成済みの実効値)。
    @objc public let placement: MauiDialogPlacement

    /// MAUI 側が供給した出現の演出。未供給なら Native ライブラリの既定が使われる。
    private var presentationRunner: MauiDialogTransitionRunner?

    /// MAUI 側が供給した退出の演出。未供給なら Native ライブラリの既定が使われる。
    private var dismissalRunner: MauiDialogTransitionRunner?

    @objc
    public init(view: UIView, options: MauiDialogOptions, placement: MauiDialogPlacement) {
        self.view = view
        self.options = options
        self.placement = placement
        super.init()
    }

    /// MAUI 側の演出をこの中身へ結び付ける。
    ///
    /// 供給された側だけが Native ライブラリの添付面に載り、未供給の側には器の既定が適用される。
    /// 実行の実体はこの中身が保持するため、供給元は中身と同じ寿命の間だけ生きていればよい。
    /// 背景の覆いは Native ライブラリの器が駆動するため、その時間だけを値として受け取る。
    /// - Parameters:
    ///   - presentation: 出現の演出の実行口。nil なら器の既定が使われる
    ///   - dismissal: 退出の演出の実行口。nil なら器の既定が使われる
    ///   - overlayDuration: 背景の覆いのフェード時間 (秒)。nil なら器の既定値が使われる
    @MainActor
    @objc(installTransitionWithPresentation:dismissal:overlayDuration:)
    public func installTransition(
        presentation: MauiDialogTransitionRunner?,
        dismissal: MauiDialogTransitionRunner?,
        overlayDuration: NSNumber?
    ) {
        presentationRunner = presentation
        dismissalRunner = dismissal
        guard presentation != nil || dismissal != nil || overlayDuration != nil else {
            view.ksDialogTransition = nil
            return
        }
        // 添付は中身が持つ View に載るため、ここから中身を強く持つと参照が輪になる。
        // 実行の実体は中身が保持しており、中身はダイアログの寿命の間ずっと生きている。
        let boxedContent = MauiUncheckedWeakBox(self)
        var presentationHook: DialogTransition.Hook?
        if presentation != nil {
            presentationHook = { hostView in
                await MauiDialogContent.awaitRun(hostView) { view, completion in
                    guard let content = boxedContent.value else {
                        completion()
                        return
                    }
                    content.runPresentation(on: view, completion: completion)
                }
            }
        }
        var dismissalHook: DialogTransition.Hook?
        if dismissal != nil {
            dismissalHook = { hostView in
                await MauiDialogContent.awaitRun(hostView) { view, completion in
                    guard let content = boxedContent.value else {
                        completion()
                        return
                    }
                    content.runDismissal(on: view, completion: completion)
                }
            }
        }
        view.ksDialogTransition = DialogTransition(
            presentation: presentationHook,
            dismissal: dismissalHook,
            overlayDuration: overlayDuration?.doubleValue
        )
    }

    /// 出現の演出を実行し、終わったら完了を1回だけ返す。
    /// 演出が供給されていなければ、その場で完了を返す。
    @MainActor
    @objc(runPresentationOnView:completion:)
    public func runPresentation(on hostView: UIView, completion: @escaping () -> Void) {
        run(presentationRunner, on: hostView, completion: completion)
    }

    /// 退出の演出を実行し、終わったら完了を1回だけ返す。
    /// 演出が供給されていなければ、その場で完了を返す。
    @MainActor
    @objc(runDismissalOnView:completion:)
    public func runDismissal(on hostView: UIView, completion: @escaping () -> Void) {
        run(dismissalRunner, on: hostView, completion: completion)
    }

    /// 実行口を呼び、完了通知が何度届いても1回だけ通す。
    @MainActor
    private func run(
        _ runner: MauiDialogTransitionRunner?,
        on hostView: UIView,
        completion: @escaping () -> Void
    ) {
        let once = MauiDialogSingleCompletion(completion)
        guard let runner else {
            once.complete()
            return
        }
        runner(hostView) { once.complete() }
    }

    /// 完了コールバック型の実行口を待つ。
    /// 待ちが打ち切られたら、演出の完了を待つのをやめて戻る (MAUI 側の処理は取り消せない)。
    @MainActor
    private static func awaitRun(
        _ hostView: UIView,
        _ start: @MainActor (UIView, @escaping () -> Void) -> Void
    ) async {
        let awaiter = MauiDialogTransitionAwaiter()
        await withTaskCancellationHandler {
            await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
                awaiter.attach(continuation)
                start(hostView) { awaiter.finish() }
            }
        } onCancel: {
            awaiter.finish()
        }
    }

    /// MAUI 側が読み直したメタ属性を、この中身の View の添付面へ写す。
    ///
    /// 契約が定める採用時点は初回のネイティブレイアウトパス完了時点であり、
    /// そこまでの添付変更は採用される (core/ADR-0015)。器がその時点の値を読めるよう、
    /// MAUI 側はレイアウトパスの中でこの操作を呼んで添付を更新する。
    ///
    /// インスタンスの操作にしてあるのは、静的な操作 (ObjC のクラスメソッド) が
    /// アプリへの静的リンク時に取り除かれ、実行時に見つからなくなるためである。
    /// - Parameters:
    ///   - options: MAUI 側で読んだ静的メタ属性。
    ///   - placement: MAUI 側で読んだ置き場所。
    @MainActor
    @objc(applyAttributesWithOptions:placement:)
    public func applyAttributes(
        options: MauiDialogOptions,
        placement: MauiDialogPlacement
    ) {
        view.ksDialogOptions = DialogOptions(options)
        view.ksDialogPlacement = DialogPlacement(placement)
    }
}
