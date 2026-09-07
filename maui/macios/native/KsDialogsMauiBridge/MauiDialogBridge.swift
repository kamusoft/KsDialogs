import KsDialogs
import UIKit

/// MAUI 形態のための ObjC 互換面 (maui/ADR-0001)。
///
/// MAUI 側で実体化された platform view を中身にして、Native ライブラリのダイアログとして提示する。
/// ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
/// 表面は「提示する」「閉じる」の2操作と、提示1回ごとの通知1本だけで構成する。
@objc(KSDMauiDialogBridge)
public final class MauiDialogBridge: NSObject, Sendable {
    /// 既定の共有インスタンス。
    @objc(sharedBridge)
    public static let shared = MauiDialogBridge()

    private let dialogs: any KsDialog

    public override init() {
        let dialog = Dialog()
        // 中身は提示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        dialog.registry.register(MauiDialogViewModel.self) { viewModel, notifier in
            // 報告口を結び付けるのは中身が作れた後にする。中身を作れずに提示が失敗したときは
            // 結果の確定が起きないため、先に結び付けると handle が解放されるまで報告口が残る
            let contentView = try viewModel.makeContentView()
            viewModel.presentation.attach(notifier)
            return contentView
        }
        dialogs = dialog
        super.init()
    }

    /// 中身の View をダイアログとして提示し、閉じたときに通知をちょうど1回返す。
    ///
    /// 提示先はライブラリが自動解決するため、呼び出し側は提示先を渡さない。
    /// 提示先が存在しない場合は通知が失敗として届き、中身の供給も呼ばれない。
    /// 中身の供給は提示先が確保できた後に UI スレッドで呼ばれるため、
    /// 呼び出し側は任意のスレッドからこの操作を呼べる。
    /// 供給元が中身を作れなかった (nil を返した) 場合も、通知が失敗として届く。
    /// メタ属性は供給された中身への添付として、そのまま Native ライブラリへ渡る。
    /// - Parameters:
    ///   - contentProvider: ダイアログの中身と、それに効くメタ属性を新規に供給する関数。
    ///   - completion: 閉鎖の通知。ちょうど1回だけ呼ばれる。
    /// - Returns: 提示した1枚を閉じるための handle。
    @objc(presentContentProvider:completion:)
    public func present(
        _ contentProvider: @escaping MauiDialogContentProvider,
        completion: @escaping (MauiDialogClosure) -> Void
    ) -> MauiDialogPresentation {
        let presentation = MauiDialogPresentation()
        let viewModel = MauiDialogViewModel(
            contentProvider: contentProvider,
            presentation: presentation
        )
        let boxedCompletion = MauiUncheckedSendableBox(completion)
        Task { @MainActor in
            do {
                switch try await dialogs.show(viewModel) {
                case .completed:
                    // 完了はこの面が閉鎖のために使う経路なので、利用者操作によるキャンセルと区別する
                    boxedCompletion.value(MauiDialogClosure(kind: .dismissed))
                case .cancelled:
                    boxedCompletion.value(MauiDialogClosure(kind: .cancelled))
                @unknown default:
                    // 判別できない結果を利用者操作や閉鎖要求に読み替えると意味論が壊れるため、失敗として返す
                    boxedCompletion.value(MauiDialogClosure(error: MauiDialogBridgeError.unsupportedResult))
                }
            } catch DialogError.presentationHostUnavailable {
                boxedCompletion.value(MauiDialogClosure(kind: .presentationHostUnavailable))
            } catch {
                boxedCompletion.value(MauiDialogClosure(error: error))
            }
        }
        return presentation
    }
}
