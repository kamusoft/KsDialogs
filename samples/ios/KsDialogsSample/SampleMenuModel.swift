import KsDialogs
import Observation

/// 進捗を報告する刻みの数。0 から 1 までをこの数で割った値を順に報告する。
/// ローディングの処理は UI スレッドの外でも走るため、刻みの定数は画面から独立して置く。
private let loadingStepCount = 4
/// メッセージを差し替える刻み。
private let loadingMessageUpdateStep = 2
/// 刻みごとの待ち時間 (ミリ秒) の既定値。
private let defaultLoadingStepIntervalMilliseconds = 400

/// Toast を重ねて置くときの上方向オフセット (論理単位) の下段。契約既定と同じ高さ。
private let toastLowerOffsetY: Double = -80
/// 重ねて置くときの中段。
private let toastMiddleOffsetY: Double = -160
/// 重ねて置くときの上段。
private let toastUpperOffsetY: Double = -240
/// 上部中央へ置くときの下方向オフセット (論理単位)。
private let toastTopOffsetY: Double = 80

/// Custom Toast の登録経路の表示時間 (ミリ秒)。
private let customToastRegisteredDurationMilliseconds = 3000
/// Custom Toast のインライン経路の表示時間 (ミリ秒)。登録経路より先に消える値にする。
private let customToastInlineDurationMilliseconds = 2000

/// Toast Stack の 1 枚目の表示時間 (ミリ秒)。
private let toastStackFirstDurationMilliseconds = 2000
/// Toast Stack の 2 枚目の表示時間 (ミリ秒)。
private let toastStackSecondDurationMilliseconds = 3000
/// Toast Stack の 3 枚目の表示時間 (ミリ秒)。
private let toastStackThirdDurationMilliseconds = 4000

/// Toast Placement の表示時間 (ミリ秒)。配置を見比べられるよう既定より長くする。
private let toastPlacementDurationMilliseconds = 3000

/// Toast Overlap の Toast の表示時間 (ミリ秒)。Dialog と Loading の時系列を跨ぐ長さにする。
private let toastOverlapToastDurationMilliseconds = 10000
/// Toast Overlap の Dialog を出しておく時間 (ミリ秒)。
private let toastOverlapDialogDurationMilliseconds = 2000
/// Toast Overlap の Loading を出しておく時間 (ミリ秒)。
private let toastOverlapLoadingDurationMilliseconds = 2000
/// Toast の満了を確実に過ぎてから結果を出すための余白 (ミリ秒)。
private let toastOverlapResultMarginMilliseconds = 500

/// 可視領域の下部中央から上方向へ動かした配置。
private func bottomToastPlacement(offsetY: Double) -> DialogPlacement {
    DialogPlacement(
        horizontalAlignment: .center,
        verticalAlignment: .end,
        offsetX: 0,
        offsetY: offsetY
    )
}

/// 可視領域の上部中央へ置く配置。契約既定 (下部中央) と対になる位置。
private let topToastPlacement = DialogPlacement(
    horizontalAlignment: .center,
    verticalAlignment: .start,
    offsetX: 0,
    offsetY: toastTopOffsetY
)

/// メニュー画面が持つ状態。
///
/// デモ項目を起動してダイアログの結果を待ち、直近の結果を表示用の文言として保持する。
@MainActor
@Observable
final class SampleMenuModel {
    /// 直近の結果の表示文言。まだ一度もダイアログを閉じていない間は nil。
    private(set) var lastResult: String?

    /// 刻みごとの待ち時間 (ミリ秒)。起動引数で指定があればその値になる。
    private let loadingStepIntervalMilliseconds =
        SampleCaptureOptions.current.loadingStepIntervalMilliseconds
            ?? defaultLoadingStepIntervalMilliseconds

    /// 別画面のデモが確定させた結果を直近の結果として取り込む。
    func updateResult(_ result: String) {
        lastResult = result
    }

    /// Basic Dialog を表示し、結果を直近の結果として取り込む。
    func showBasicDialog() async {
        let viewModel = BasicDialogViewModel(message: SampleText.basicDialogMessage)
        do {
            switch try await Dialog.shared.show(viewModel) {
            case .completed(let value):
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Declarative Dialog を表示し、結果を直近の結果として取り込む。
    ///
    /// 中身が SwiftUI で書かれていても、呼び出し方も結果の返り方も Basic Dialog と変わらない。
    func showDeclarativeDialog() async {
        let viewModel = DeclarativeDialogViewModel(message: SampleText.declarativeDialogMessage)
        do {
            switch try await Dialog.shared.show(viewModel) {
            case .completed(let value):
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Model Dialog を表示し、結果を直近の結果として取り込む。
    ///
    /// ViewModel のインスタンスは渡さず、型と configure だけを渡す。
    /// 生成はレジストリの ViewModel factory が行い、結果は ViewModel 自身が報告する。
    func showModelDialog() async {
        do {
            let result = try await Dialog.shared.show(ModelDialogViewModel.self) { viewModel in
                viewModel.message = SampleText.modelDialogMessage
            }
            switch result {
            case .completed(let value):
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。
    func showTextInputDialog() async {
        let viewModel = TextInputDialogViewModel(message: SampleText.textInputDialogMessage)
        do {
            switch try await Dialog.shared.show(viewModel) {
            case .completed(let value):
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Inline Dialog を表示し、結果を直近の結果として取り込む。
    ///
    /// 中身はこの場で渡すため、この ViewModel 型はレジストリに登録していない (core/ADR-0013)。
    func showInlineDialog() async {
        let viewModel = InlineDialogViewModel(message: SampleText.inlineDialogMessage)
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
                lastResult = SampleText.completedResult(value)
            case .cancelled:
                lastResult = SampleText.cancelledResult
            }
        } catch {
            // 提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
        }
    }

    /// Default Loading を実行し、完了を直近の結果として取り込む。
    ///
    /// スコープ形の start は処理の間だけ既定ローディングを出し、処理の完了で自動的に閉じる。
    /// 処理は 0 から 1 まで進捗を段階的に報告し、途中で表示中のメッセージを差し替える。
    func runDefaultLoading() async {
        // 進捗を報告するクロージャは MainActor の外で動き、隔離されたプロパティを読めないため、
        // ここで値を取り出しておく
        let stepIntervalMilliseconds = loadingStepIntervalMilliseconds
        do {
            try await Loading.shared.start(message: SampleText.loadingStartMessage) { report in
                for step in 0...loadingStepCount {
                    report(Double(step) / Double(loadingStepCount))
                    if step == loadingMessageUpdateStep {
                        await Loading.shared.setMessage(SampleText.loadingUpdateMessage)
                    }
                    try await Task.sleep(for: .milliseconds(stepIntervalMilliseconds))
                }
            }
            lastResult = SampleText.loadingCompletedResult
        } catch {
            // 処理の失敗は呼び出し元へ伝わる。Sample の処理は失敗しないので開発中に気づけるよう止める
            assertionFailure("ローディングの処理が失敗しました: \(error)")
        }
    }

    /// Custom Loading を実行し、完了を直近の結果として取り込む。
    ///
    /// 呼び出しの形は Default Loading と同じスコープ形で、渡すのが登録済みの ViewModel の**型**である点だけが違う。
    /// 実体はレジストリの ViewModel factory が作る。
    /// 報告した進捗は ViewModel の受け口へ転送され、中身のカスタム View がそれを読んで表示を更新する。
    func runCustomLoading() async {
        // 進捗を報告するクロージャは MainActor の外で動き、隔離されたプロパティを読めないため、
        // ここで値を取り出しておく
        let stepIntervalMilliseconds = loadingStepIntervalMilliseconds
        do {
            try await Loading.shared.start(CustomLoadingViewModel.self) { report in
                for step in 0...loadingStepCount {
                    report(Double(step) / Double(loadingStepCount))
                    try await Task.sleep(for: .milliseconds(stepIntervalMilliseconds))
                }
            }
            lastResult = SampleText.loadingCompletedResult
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ローディングを表示できませんでした: \(error)")
        }
    }

    /// Default Toast を表示する。
    ///
    /// duration も配置も渡さないので、デフォルト View が契約既定の配置に出て既定 duration で消える。
    /// Toast は fire-and-forget なので戻り値も待機もなく、結果表示も変えない (core/ADR-0031)。
    func showDefaultToast() {
        Toast.shared.show(message: SampleText.defaultToastMessage)
    }

    /// Custom Toast を表示する。
    ///
    /// 登録経路とインライン経路の 2 枚を続けて出す。同じ配置では重なって見分けられないため、
    /// 上方向オフセットを変えて 2 段に置く。
    func showCustomToast() {
        do {
            // 登録経路は型を渡し、実体はレジストリの ViewModel factory が作る。文言は configure で入れる
            try Toast.shared.show(
                CustomToastViewModel.self,
                duration: customToastRegisteredDurationMilliseconds,
                placement: bottomToastPlacement(offsetY: toastMiddleOffsetY),
                configure: { viewModel in
                    viewModel.message = SampleText.customToastMessage
                }
            )
            // 中身はこの場で渡すため、この ViewModel 型は登録していない (core/ADR-0013)
            try Toast.shared.show(
                InlineToastViewModel(message: SampleText.inlineToastMessage),
                duration: customToastInlineDurationMilliseconds,
                placement: bottomToastPlacement(offsetY: toastLowerOffsetY)
            ) { viewModel in
                InlineToastCard(viewModel: viewModel)
            }
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("Toast を表示できませんでした: \(error)")
        }
    }

    /// Toast Stack を表示する。
    ///
    /// 3 枚を続けて出し、duration の短いものから独立して消える様子を見せる。
    /// 3 枚目は長文で、デフォルト View が複数行に折り返して高さを伸ばすことを確かめる。
    func showToastStack() {
        Toast.shared.show(
            message: SampleText.toastStackFirstMessage,
            duration: toastStackFirstDurationMilliseconds,
            placement: bottomToastPlacement(offsetY: toastLowerOffsetY)
        )
        Toast.shared.show(
            message: SampleText.toastStackSecondMessage,
            duration: toastStackSecondDurationMilliseconds,
            placement: bottomToastPlacement(offsetY: toastMiddleOffsetY)
        )
        Toast.shared.show(
            message: SampleText.toastStackThirdMessage,
            duration: toastStackThirdDurationMilliseconds,
            placement: bottomToastPlacement(offsetY: toastUpperOffsetY)
        )
    }

    /// Toast Placement を表示する。show の引数で契約既定と違う配置へ上書きする。
    func showToastPlacement() {
        Toast.shared.show(
            message: SampleText.toastPlacementMessage,
            duration: toastPlacementDurationMilliseconds,
            placement: topToastPlacement
        )
    }

    /// Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。
    ///
    /// 操作者に依存しない固定の時系列で自動進行する — Toast を出したまま Dialog を重ね、
    /// Dialog を閉じたあと Loading を重ねる。Loading は Toast より前面に出る (core/ADR-0030)。
    /// Loading が終わっても Toast は残っており、duration の満了で消えてから結果を出す。
    func runToastOverlap() async {
        Toast.shared.show(
            message: SampleText.toastOverlapMessage,
            duration: toastOverlapToastDurationMilliseconds
        )
        await showOverlapDialog()
        await runOverlapLoading()

        // Toast の残り時間 (と満了を跨ぐ余白) を待ってから結果を出す
        let remainingMilliseconds = toastOverlapToastDurationMilliseconds
            - toastOverlapDialogDurationMilliseconds
            - toastOverlapLoadingDurationMilliseconds
            + toastOverlapResultMarginMilliseconds
        try? await Task.sleep(for: .milliseconds(remainingMilliseconds))
        lastResult = SampleText.toastOverlapCompletedResult
    }

    /// Toast Overlap の Dialog を出し、一定時間後に自動で閉じる。
    ///
    /// 待機を打ち切ると結果は cancelled で確定してダイアログが閉じる
    /// (呼び出し元の Task をキャンセルしたときの iOS での見え方)。結果は使わない。
    private func showOverlapDialog() async {
        let viewModel = BasicDialogViewModel(message: SampleText.basicDialogMessage)
        let showTask = Task { try await Dialog.shared.show(viewModel) }
        try? await Task.sleep(for: .milliseconds(toastOverlapDialogDurationMilliseconds))
        showTask.cancel()
        _ = try? await showTask.value
    }

    /// Toast Overlap の Loading を出し、処理の完了で自動的に閉じる。
    private func runOverlapLoading() async {
        do {
            try await Loading.shared.start(message: SampleText.loadingStartMessage) { _ in
                try await Task.sleep(for: .milliseconds(toastOverlapLoadingDurationMilliseconds))
            }
        } catch {
            // 処理の失敗は呼び出し元へ伝わる。Sample の処理は失敗しないので開発中に気づけるよう止める
            assertionFailure("ローディングの処理が失敗しました: \(error)")
        }
    }
}
