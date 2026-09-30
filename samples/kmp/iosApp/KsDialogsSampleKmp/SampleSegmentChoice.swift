/// セグメントに並べられる選択肢。
///
/// 並び順は `allCases` の順で、画面にはそれぞれの `label` を出す。
protocol SampleSegmentChoice: CaseIterable, Identifiable, Hashable where AllCases: RandomAccessCollection {
    /// セグメントに表示する文言。
    var label: String { get }
}
