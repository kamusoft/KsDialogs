import KsDialogs
import SwiftUI

/// Web サイトのスライドや README に載せるスクリーンショットを撮るための場面。
///
/// メニュー画面のデモとは別に、実アプリ風の画面の上へダイアログ・ローディング・トーストを
/// 大きめに出した状態を作る。撮影用の見本なので、4ルートのパリティ対象には数えない。
///
/// 起動引数 `--showcase <rawValue>` で指定すると、メニュー画面の代わりに背景の画面を出し、
/// 表示の直後にその場面を自動で再生する。
///   xcrun simctl launch <UDID> jp.kamusoft.ksdialogs.samples.ios --showcase dialog
enum ShowcaseScene: String, CaseIterable {
    /// 削除確認のダイアログ。
    case dialog
    /// 進捗リング付きのカスタムローディング。
    case loading
    /// 上部の通知風トーストと下部の完了トースト。
    case toast
    /// 何も出さない背景の画面だけ。比較用・合成用。
    case background

    /// 起動引数 `--showcase <rawValue>` で指定された場面。指定なし・定義外なら nil。
    static let launchTarget: ShowcaseScene? = {
        let arguments = ProcessInfo.processInfo.arguments
        guard let keyIndex = arguments.firstIndex(of: "--showcase") else { return nil }
        let valueIndex = arguments.index(after: keyIndex)
        guard valueIndex < arguments.endIndex else { return nil }
        return ShowcaseScene(rawValue: arguments[valueIndex])
    }()

    /// 撮影まで出しっぱなしにするトーストの表示時間 (ミリ秒)。
    private static let toastDurationMilliseconds = 600_000

    /// 場面を再生する。ダイアログは閉じられるまで待つ。
    @MainActor
    func play() async {
        do {
            switch self {
            case .dialog:
                _ = try await Dialog.shared.show(ShowcaseDeleteDialogViewModel()) { _, notifier in
                    ShowcaseDeleteDialogCard(
                        onCancel: { notifier.cancel() },
                        onDelete: { notifier.complete(true) }
                    )
                }
            case .loading:
                try await Loading.shared.show(ShowcaseUploadLoadingViewModel()) { _ in
                    ShowcaseUploadLoadingCard()
                }
            case .toast:
                try Toast.shared.show(
                    ShowcaseReminderToastViewModel(),
                    duration: Self.toastDurationMilliseconds,
                    placement: DialogPlacement(
                        horizontalAlignment: .center,
                        verticalAlignment: .start,
                        offsetX: 0,
                        offsetY: 64
                    )
                ) { _ in
                    ShowcaseReminderToastCard()
                }
                try Toast.shared.show(
                    ShowcaseSavedToastViewModel(),
                    duration: Self.toastDurationMilliseconds,
                    placement: DialogPlacement(
                        horizontalAlignment: .center,
                        verticalAlignment: .end,
                        offsetX: 0,
                        offsetY: -96
                    )
                ) { _ in
                    ShowcaseSavedToastCard()
                }
            case .background:
                break
            }
        } catch {
            assertionFailure("撮影用の場面を再生できませんでした: \(error)")
        }
    }
}

/// 撮影用の場面を再生する画面。背景のノート一覧を出し、表示の直後に場面を 1 回だけ再生する。
struct ShowcaseScreen: View {
    let scene: ShowcaseScene

    /// 画面が作り直されても再生を繰り返さないための印。
    @State private var hasPlayed = false

    var body: some View {
        ShowcaseNotesScreen()
            .onAppear {
                guard !hasPlayed else { return }
                hasPlayed = true
                // 提示先が現れるまで待つのはライブラリの役目なので、表示の直後に始めてよい
                Task { await scene.play() }
            }
    }
}
