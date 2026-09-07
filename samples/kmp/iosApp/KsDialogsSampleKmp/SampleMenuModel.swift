import KsDialogs
import Observation
import SampleShared

/// Toast を重ねて置くときの上方向オフセット (論理単位) の下段。契約既定と同じ高さ。
private let toastLowerOffsetY: Double = -80

/// Custom Toast のインライン経路の表示時間 (ミリ秒)。登録経路より先に消える値にする。
private let customToastInlineDurationMilliseconds = 2000

/// メニュー画面が持つ状態。
///
/// デモ項目の起動と結果の言い換えは共有 Presenter が受け持ち、この型は表示用の保持だけを行う。
@MainActor
@Observable
final class SampleMenuModel {
    private let presenter = SamplePresenter(options: SampleCaptureArguments.current)

    /// 直近の結果の表示文言。まだ一度もダイアログを閉じていない間は nil。
    private(set) var lastResult: String?

    /// 別画面のデモが確定させた結果を直近の結果として取り込む。
    func updateResult(_ result: String) {
        lastResult = result
    }

    /// 指定されたデモを共有 Presenter の入口で再生し、結果を直近の結果として取り込む。
    ///
    /// 共有 Presenter の外にあるデモ (Inline・パネル系) はこの経路では何も起こらない。
    func autoPlay(_ demo: SampleDemoId) async {
        do {
            if let result = try await presenter.autoPlay(demo: demo) {
                lastResult = result
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("デモを再生できませんでした: \(error)")
        }
    }

    /// Basic Dialog を表示し、結果を直近の結果として取り込む。
    func showBasicDialog() async {
        do {
            lastResult = try await presenter.showBasicDialog()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Declarative Dialog を表示し、結果を直近の結果として取り込む。
    func showDeclarativeDialog() async {
        do {
            lastResult = try await presenter.showDeclarativeDialog()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Model Dialog を表示し、結果を直近の結果として取り込む。
    ///
    /// 呼び出しは共有 Presenter に閉じており、違うのは中身側の結果の報告経路だけである。
    func showModelDialog() async {
        do {
            lastResult = try await presenter.showModelDialog()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。
    func showTextInputDialog() async {
        do {
            lastResult = try await presenter.showTextInputDialog()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Default Loading を実行し、完了を直近の結果として取り込む。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この型は結果の文言を受け取るだけである。
    func runDefaultLoading() async {
        do {
            lastResult = try await presenter.runDefaultLoading()
        } catch {
            // 処理の失敗は呼び出し元へ伝わる。Sample の処理は失敗しないので開発中に気づけるよう止める
            assertionFailure("ローディングの処理が失敗しました: \(error)")
        }
    }

    /// Custom Loading を実行し、完了を直近の結果として取り込む。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この画面が受け持つのは中身の View の登録だけである。
    func runCustomLoading() async {
        do {
            lastResult = try await presenter.runCustomLoading()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ローディングを表示できませんでした: \(error)")
        }
    }

    /// Default Toast を表示する。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この型が受け持つのは起動操作だけである。
    /// Toast は fire-and-forget なので戻り値も待機もなく、結果表示も変えない (core/ADR-0031)。
    func showDefaultToast() {
        presenter.showDefaultToast()
    }

    /// Custom Toast を表示する。
    ///
    /// 登録経路は共有 Presenter が表示し、中身をその場で渡すインライン経路は
    /// iOS Native API にしかないためこの型が受け持つ。
    /// 同じ配置では重なって見分けられないため、2 枚は上方向オフセットを変えて置く。
    func showCustomToast() {
        do {
            try presenter.showCustomToast()
            // この ViewModel 型はレジストリに登録していない (core/ADR-0013)
            try Toast.shared.show(
                InlineToastViewModel(message: SampleText.shared.INLINE_TOAST_MESSAGE),
                duration: customToastInlineDurationMilliseconds,
                placement: DialogPlacement(
                    horizontalAlignment: .center,
                    verticalAlignment: .end,
                    offsetX: 0,
                    offsetY: toastLowerOffsetY
                )
            ) { viewModel in
                InlineToastCard(viewModel: viewModel)
            }
        } catch {
            // 未登録の ViewModel 型は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("Toast を表示できませんでした: \(error)")
        }
    }

    /// Toast Stack を表示する。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この型が受け持つのは起動操作だけである。
    func showToastStack() {
        presenter.showToastStack()
    }

    /// Toast Placement を表示する。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この型が受け持つのは起動操作だけである。
    func showToastPlacement() {
        presenter.showToastPlacement()
    }

    /// Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。
    ///
    /// 呼び出しは共有 Presenter に閉じており、この型は結果の文言を受け取るだけである。
    func runToastOverlap() async {
        do {
            lastResult = try await presenter.runToastOverlap()
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("Toast を表示できませんでした: \(error)")
        }
    }

    /// Inline Dialog を表示し、結果を直近の結果として取り込む。
    ///
    /// 中身をその場で渡す表示は iOS Native API にしかないため、共有 Presenter は経由しない。
    /// この ViewModel 型はレジストリに登録していない (core/ADR-0013)。
    func showInlineDialog() async {
        let viewModel = InlineDialogViewModel(message: SampleText.shared.INLINE_DIALOG_MESSAGE)
        do {
            let result = try await Dialog.shared.show(viewModel) { viewModel, notifier in
                InlineDialogCardHostView(
                    message: viewModel.message,
                    onCancel: { notifier.cancel() },
                    onComplete: { notifier.complete(true) }
                )
            }
            switch result {
            case .completed(let value):
                lastResult = SampleText.shared.completedResult(value: value)
            case .cancelled:
                lastResult = SampleText.shared.CANCELLED_RESULT
            }
        } catch {
            // 提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }
}
