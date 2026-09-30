import KsDialogs
import SampleShared

/// 属性調整パネルが選べる基準領域。
enum SampleLayoutAreaChoice: SampleSegmentChoice {
    case window
    case visibleArea
    case currentPage

    var id: Self { self }

    /// セグメントに表示する文言。
    var label: String {
        switch self {
        case .window: SampleText.shared.LAYOUT_AREA_WINDOW
        case .visibleArea: SampleText.shared.LAYOUT_AREA_VISIBLE_AREA
        case .currentPage: SampleText.shared.LAYOUT_AREA_CURRENT_PAGE
        }
    }

    /// 共有コードが運ぶ選択へ言い換える。
    var preset: SampleLayoutAreaPreset {
        switch self {
        case .window: SampleLayoutAreaPreset.window
        case .visibleArea: SampleLayoutAreaPreset.visibleArea
        case .currentPage: SampleLayoutAreaPreset.currentPage
        }
    }

    /// 共有コードが運んできた選択を、中身へ添付する基準領域へ言い換える。
    static func layoutArea(of preset: SampleLayoutAreaPreset) -> DialogLayoutArea {
        switch preset {
        case .window: .window
        case .currentPage: .currentPage
        default: .visibleArea
        }
    }
}
