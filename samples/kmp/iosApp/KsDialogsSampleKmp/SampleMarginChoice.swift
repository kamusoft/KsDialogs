import SampleShared

/// 属性調整パネルが選べる余白 (全辺そろえ)。
///
/// 0 は契約の既定値、24 は既定 Toast のデフォルト View が自分に持つ余白と同じ値。
enum SampleMarginChoice: SampleSegmentChoice {
    case zero
    case twentyFour
    case fortyEight

    var id: Self { self }

    /// セグメントに表示する文言。
    var label: String {
        switch self {
        case .zero: SampleText.shared.MARGIN_0
        case .twentyFour: SampleText.shared.MARGIN_24
        case .fortyEight: SampleText.shared.MARGIN_48
        }
    }

    /// 全辺にそろえて添付する余白の値 (pt)。共有コードへはこの値をそのまま運ぶ。
    var value: Double {
        switch self {
        case .zero: 0
        case .twentyFour: 24
        case .fortyEight: 48
        }
    }
}
