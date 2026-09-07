@testable import KsDialogsMauiBridgeTestHost
import UIKit

/// key window に重なった器を観測する。
///
/// Toast と Loading の器は提示機構を通らず key window の直下へ重なるため、枚数と同一性は
/// window の subview として観測できる。ホストアプリ自身の画面や、提示機構が作って居座る
/// 中間 View と切り分けるため、観測を始めた時点の顔ぶれを控えておき、その後に増えた分だけを見る。
///
/// 今の顔ぶれだけでは「取り付いた直後に撤去された」器を取りこぼすため、取り付けの履歴も併せて見る。
/// 履歴は提示先の window が控えており、観測の開始時に捨てて数え直す。
@MainActor
struct BridgeTestOverlayObserver {
    private let baseline: Set<ObjectIdentifier>
    private let recordingWindow: BridgeTestHostWindow?

    /// 今の顔ぶれを控え、取り付けの履歴を捨てて観測を始める。
    init() {
        recordingWindow = BridgeTestHost.recordingKeyWindow
        recordingWindow?.resetAddedSubviewHistory()
        baseline = Set(BridgeTestHost.overlayViews.map(ObjectIdentifier.init))
    }

    /// 観測を始めた後に重ねられ、今も残っている View (奥から手前の順)。
    var added: [UIView] {
        BridgeTestHost.overlayViews.filter { !baseline.contains(ObjectIdentifier($0)) }
    }

    /// 観測を始めた後に一度でも重ねられた View (取り付いた順)。撤去済みのものも含む。
    ///
    /// 履歴を控える window が提示先として解決できないときは nil を返す。
    /// 観測できていないことを「取り付けが無かった」と読み替えないため、値の不在として区別する。
    var attachedEver: [UIView]? {
        guard let recordingWindow else { return nil }
        return recordingWindow.addedSubviewHistory.filter { !baseline.contains(ObjectIdentifier($0)) }
    }
}
