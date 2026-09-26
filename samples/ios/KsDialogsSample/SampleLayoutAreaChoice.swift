import KsDialogs

/// 属性調整パネルが選べる基準領域。
enum SampleLayoutAreaChoice: SampleSegmentChoice {
    case window
    case visibleArea
    case currentPage

    var id: Self { self }

    /// セグメントに表示する文言。
    var label: String {
        switch self {
        case .window: SampleText.layoutAreaWindow
        case .visibleArea: SampleText.layoutAreaVisibleArea
        case .currentPage: SampleText.layoutAreaCurrentPage
        }
    }

    /// 契約の基準領域へ言い換える。
    var layoutArea: DialogLayoutArea {
        switch self {
        case .window: .window
        case .visibleArea: .visibleArea
        case .currentPage: .currentPage
        }
    }
}
