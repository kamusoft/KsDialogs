import KsDialogs
import Observation

/// トランジションデモ画面が持つ状態。
///
/// 初期値はプリセット `Fade`・時間 250 ms・イージング `Standard`。
@MainActor
@Observable
final class SampleTransitionPanelModel {
    /// 選択中の演出。
    var transitionChoice: SampleTransitionChoice = .fade
    /// 選択中の時間 (ミリ秒)。無演出と自作フックを選んでいる間も値は保たれる。
    var durationMilliseconds: Double = 250
    /// 選択中のイージング。無演出と自作フックを選んでいる間も値は保たれる。
    var easingChoice: SampleEasingChoice = .standard

    /// 直近の結果の表示文言。まだ一度もダイアログを閉じていない間は nil。
    private(set) var lastResult: String?

    /// 時間とイージングを操作できるか。
    var allowsAdjustments: Bool {
        transitionChoice.usesAdjustments
    }

    /// 時間の表示文言。
    var durationText: String {
        SampleText.durationValue(Int(durationMilliseconds.rounded()))
    }

    /// 選んだ演出でダイアログを表示し、結果を直近の結果として取り込む。
    ///
    /// - Returns: 結果表示エリアに出す文言。表示できなかった場合は nil。
    @discardableResult
    func showTransitionDialog() async -> String? {
        // 演出は show の引数では渡せないため、中身へ添付する組として ViewModel に載せて運ぶ
        let viewModel = TransitionDialogViewModel(
            message: SampleText.transitionDialogMessage,
            transition: transitionChoice.transition(
                durationMilliseconds: Int(durationMilliseconds.rounded()),
                easing: easingChoice
            )
        )
        do {
            switch try await Dialog.shared.show(viewModel) {
            case .completed(let value):
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
            return lastResult
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
            return nil
        }
    }
}
