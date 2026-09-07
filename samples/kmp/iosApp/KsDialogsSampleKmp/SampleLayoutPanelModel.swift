import Observation
import SampleShared

/// 属性調整パネルが持つ状態。
///
/// 初期値は契約の既定値 (中央配置・移動なし・可視領域基準) に揃える。
/// 表示と結果の言い換えは共有 Presenter が受け持つ。
@MainActor
@Observable
final class SampleLayoutPanelModel {
    private let presenter = SamplePresenter()

    /// 水平方向の配置。
    var horizontalAlignment: SampleAlignmentChoice = .center
    /// 垂直方向の配置。
    var verticalAlignment: SampleAlignmentChoice = .center
    /// 水平方向の移動量の入力。
    var offsetX: String = "0"
    /// 垂直方向の移動量の入力。
    var offsetY: String = "0"
    /// サイズと位置の計算に可視領域を使うか。
    var usesVisibleArea: Bool = true

    /// 直近の結果の表示文言。まだ一度もダイアログを閉じていない間は nil。
    private(set) var lastResult: String?

    /// 調整した属性でダイアログを表示し、結果を直近の結果として取り込む。
    ///
    /// - Returns: 結果表示エリアに出す文言。表示できなかった場合は nil。
    @discardableResult
    func showLayoutDialog() async -> String? {
        // 置き場所は呼び出しごとに変わるので show の引数で渡す
        let placement = Ksdialogs_kmpDialogPlacement(
            horizontalAlignment: horizontalAlignment.alignment,
            verticalAlignment: verticalAlignment.alignment,
            offsetX: Double(offsetX) ?? 0,
            offsetY: Double(offsetY) ?? 0
        )
        do {
            lastResult = try await presenter.showLayoutDialog(
                placement: placement,
                usesVisibleArea: usesVisibleArea
            )
            return lastResult
        } catch {
            // 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める
            assertionFailure("ダイアログを表示できませんでした: \(error)")
            return nil
        }
    }
}
