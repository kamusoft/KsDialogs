import KsDialogs
import UIKit

/// スコープ形の処理が進捗を報告する口。任意のスレッドから呼べる。
public typealias MauiLoadingProgressReport = (Double) -> Void

/// MAUI 側が持つスコープ形の処理。
///
/// 第1引数は進捗の報告口、第2引数は処理が終わったときに呼ぶ完了通知。
/// 処理の成否は MAUI 側が自分で扱うため、この面へは伝えない。成否によらず完了通知を1回だけ呼ぶと、
/// 合流1件の終了として数えられる。
public typealias MauiLoadingAction = (@escaping MauiLoadingProgressReport, @escaping () -> Void) -> Void

/// 表示1回ごとに1度だけ届く完了の通知。失敗したときだけ理由が入る。
public typealias MauiLoadingCompletion = (NSError?) -> Void

/// MAUI 形態の Loading のための ObjC 互換面 (maui/ADR-0001)。
///
/// 合流カウント・表示世代・最新のメッセージと進捗はすべて Native ライブラリの coordinator が持ち、
/// この面は MAUI 側の呼び出しをそこへ渡すだけである (core/ADR-0024)。
/// ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
/// カスタム Loading の中身は表示のたびに MAUI 側から供給される。
@objc(KSDMauiLoadingBridge)
public final class MauiLoadingBridge: NSObject, Sendable {
    /// 既定の共有インスタンス。
    @objc(sharedBridge)
    public static let shared = MauiLoadingBridge()

    private let loading: any KsLoading

    public override init() {
        let loading = Loading()
        // 中身は表示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        loading.registry.register(MauiLoadingViewModel.self) { viewModel in
            try viewModel.makeContentView()
        }
        self.loading = loading
        super.init()
    }

    /// 既定ローディングの見た目を設定する。器は各表示の開始時にこの値を読む (core/ADR-0023)。
    /// - Parameter style: MAUI 側で設定されたスタイル。
    @objc(applyStyle:)
    public func applyStyle(_ style: MauiLoadingStyle) {
        loading.style = LoadingStyle(style)
    }

    /// 既定ローディングの器メタ属性を設定する。器は各表示の開始時にこの値を読む。
    /// - Parameter options: MAUI 側で設定された静的メタ属性。
    @objc(applyOptions:)
    public func applyOptions(_ options: MauiDialogOptions) {
        loading.options = DialogOptions(options)
    }

    /// 合流1件を開始して表示する。
    ///
    /// 通知が届くのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
    /// 中身の供給が失敗した場合はその理由が失敗の通知として届く。
    /// - Parameters:
    ///   - content: 表示する中身の指定。
    ///   - completion: 完了の通知。ちょうど1回だけ呼ばれる。
    @objc(showContent:completion:)
    public func show(_ content: MauiLoadingContent, completion: @escaping MauiLoadingCompletion) {
        let boxedCompletion = MauiUncheckedSendableBox(completion)
        let boxedContent = MauiUncheckedSendableBox(content)
        let loading = loading
        Task { @MainActor in
            await MauiLoadingBridge.report(boxedCompletion.value) {
                try await MauiLoadingBridge.beginUse(loading, boxedContent.value)
            }
        }
    }

    /// 合流1件を握ったまま MAUI 側の処理を走らせる (スコープ形)。
    ///
    /// 処理は表示状態によらず必ず実行され、その完了通知が合流1件の終了になる。
    /// 合流最後の1件なら器の撤去まで待ってから完了の通知が届く。
    /// - Parameters:
    ///   - content: 表示する中身の指定。
    ///   - action: MAUI 側の処理。
    ///   - completion: 完了の通知。ちょうど1回だけ呼ばれる。
    @objc(startContent:action:completion:)
    public func start(
        _ content: MauiLoadingContent,
        action: @escaping MauiLoadingAction,
        completion: @escaping MauiLoadingCompletion
    ) {
        let boxedCompletion = MauiUncheckedSendableBox(completion)
        let boxedContent = MauiUncheckedSendableBox(content)
        let boxedAction = MauiUncheckedSendableBox(action)
        let loading = loading
        Task { @MainActor in
            await MauiLoadingBridge.report(boxedCompletion.value) {
                try await MauiLoadingBridge.runScope(loading, boxedContent.value, boxedAction.value)
            }
        }
    }

    /// 合流数によらず表示を閉じる。出の演出と器の撤去が完了してから通知が届く。
    /// - Parameter completion: 完了の通知。ちょうど1回だけ呼ばれる。
    @objc(hideWithCompletion:)
    public func hide(completion: @escaping MauiLoadingCompletion) {
        let boxedCompletion = MauiUncheckedSendableBox(completion)
        let loading = loading
        Task { @MainActor in
            await MauiLoadingBridge.report(boxedCompletion.value) {
                await loading.hide()
            }
        }
    }

    /// 表示中のメッセージを更新する。合流には関与しない。
    ///
    /// 受理は UI スレッド上で呼ばれた順に直列化されるため、この操作は完了を待たない。
    /// - Parameter message: 新しいメッセージ。
    @objc(setLoadingMessage:)
    public func setMessage(_ message: String?) {
        let loading = loading
        Task { @MainActor in
            await loading.setMessage(message)
        }
    }

    /// 中身の指定に応じた開始の口を選ぶ。既定ローディングとカスタムで合流の数え方は変わらない。
    @MainActor
    private static func beginUse(_ loading: any KsLoading, _ content: MauiLoadingContent) async throws {
        let placement = content.placement.map { DialogPlacement($0) }
        guard let contentProvider = content.contentProvider else {
            await loading.show(message: content.message, placement: placement)
            return
        }
        let viewModel = MauiLoadingViewModel(
            contentProvider: contentProvider,
            progressReceiver: content.progressReceiver
        )
        try await loading.show(viewModel, placement: placement)
    }

    /// 中身の指定に応じたスコープ形の口を選び、MAUI 側の処理の完了まで合流1件を握る。
    @MainActor
    private static func runScope(
        _ loading: any KsLoading,
        _ content: MauiLoadingContent,
        _ action: @escaping MauiLoadingAction
    ) async throws {
        let placement = content.placement.map { DialogPlacement($0) }
        let boxedAction = MauiUncheckedSendableBox(action)
        guard let contentProvider = content.contentProvider else {
            try await loading.start(message: content.message, placement: placement) { report in
                await MauiLoadingBridge.awaitAction(boxedAction.value, report)
            }
            return
        }
        let viewModel = MauiLoadingViewModel(
            contentProvider: contentProvider,
            progressReceiver: content.progressReceiver
        )
        try await loading.start(viewModel, placement: placement) { report in
            await MauiLoadingBridge.awaitAction(boxedAction.value, report)
        }
    }

    /// MAUI 側の処理を開始し、その完了通知が届くまで待つ。
    ///
    /// 完了通知が何度届いても待ちは1回しか解けない。
    private static func awaitAction(
        _ action: @escaping MauiLoadingAction,
        _ report: @escaping @Sendable (Double) -> Void
    ) async {
        let boxedAction = MauiUncheckedSendableBox(action)
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            let boxedContinuation = MauiUncheckedSendableBox(continuation)
            let once = MauiDialogSingleCompletion { boxedContinuation.value.resume() }
            boxedAction.value(report) { once.complete() }
        }
    }

    /// 要求した操作を行い、その結末を通知先へちょうど1回だけ伝える。
    ///
    /// 通知が1つも届かないと呼び出し元 (MAUI facade) が待ち続けるため、想定していない失敗も
    /// すべて通知へ変換する。
    @MainActor
    private static func report(
        _ completion: @escaping MauiLoadingCompletion,
        _ operation: @MainActor () async throws -> Void
    ) async {
        do {
            try await operation()
            completion(nil)
        } catch {
            completion(error as NSError)
        }
    }
}

/// MAUI 側から供給される View をそのまま中身にするカスタム Loading の ViewModel。
///
/// この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの1種類が使われる。
/// 進捗の受け口は常に実装し、MAUI 側の転送先が無いときは何もしない — MAUI 側の ViewModel が
/// 受け口を実装しているかの判定は C# 層が行う。
///
/// ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
/// 器へ渡る (core/ADR-0015)。
final class MauiLoadingViewModel: LoadingViewModel, LoadingProgressReceiver, @unchecked Sendable {
    private let contentProvider: MauiLoadingContentProvider
    private let progressReceiver: MauiLoadingProgressReceiver?

    init(
        contentProvider: @escaping MauiLoadingContentProvider,
        progressReceiver: MauiLoadingProgressReceiver?
    ) {
        self.contentProvider = contentProvider
        self.progressReceiver = progressReceiver
    }

    /// 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
    /// 提示先が確保できた後に UI スレッドで呼ばれる。
    ///
    /// MAUI 側が中身を作れなかったときは失敗を投げる。器はそれを開始そのものの失敗として扱い、
    /// 表示を行わずに完了の通知へ載せて返す。
    @MainActor
    func makeContentView() throws -> UIView {
        guard let content = contentProvider() else {
            throw MauiDialogBridgeError.contentUnavailable
        }
        content.applyAttributes(options: content.options, placement: content.placement)
        return content.view
    }

    func onProgress(_ progress: Double) {
        progressReceiver?(progress)
    }
}
