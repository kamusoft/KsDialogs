/// 属性調整パネルの画面が下部のタブバーで切り替えるタブ。
enum SampleLayoutPanelTab: Hashable {
    /// 属性を調整するタブ。上部にナビゲーションバーを持つ。
    case panel
    /// 説明文と表示操作だけを置くタブ。タイトルバーを持たない。
    case info
}
