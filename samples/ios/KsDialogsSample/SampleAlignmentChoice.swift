import KsDialogs

/// 属性調整パネルが選べる配置。
///
/// 契約の配置には有効領域いっぱいに広げる選択肢もあるが、パネルは寄せ先の3択だけを扱う。
enum SampleAlignmentChoice: CaseIterable, Identifiable {
    case start
    case center
    case end

    var id: Self { self }

    /// セグメントに表示する文言。
    var label: String {
        switch self {
        case .start: SampleText.alignmentStart
        case .center: SampleText.alignmentCenter
        case .end: SampleText.alignmentEnd
        }
    }

    /// 契約の配置へ言い換える。
    var alignment: DialogAlignment {
        switch self {
        case .start: .start
        case .center: .center
        case .end: .end
        }
    }
}
