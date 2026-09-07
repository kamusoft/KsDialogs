import SwiftUI

/// 4ルートの Sample が共有する配色。
///
/// OS 固有の semantic color は使わず、全ルートで同一の RGBA を宣言する (cross/ADR-0007)。
/// 覆い (scrim) はライブラリの器が描くため、ここには持たない。
enum SampleTheme {
    /// 強調色。完了操作のボタンの塗り。
    static let primary = Color(red: 0x25 / 255, green: 0x63 / 255, blue: 0xEB / 255)
    /// primary の上に載る文字色。
    static let onPrimary = Color(red: 1, green: 1, blue: 1)
    /// 画面・ダイアログの地の色。
    static let surface = Color(red: 1, green: 1, blue: 1)
    /// surface の上に載る文字色。
    static let onSurface = Color(red: 0x1F / 255, green: 0x29 / 255, blue: 0x37 / 255)
    /// surface の上に載る補助的な文字色。
    static let onSurfaceMuted = Color(red: 0x6B / 255, green: 0x72 / 255, blue: 0x80 / 255)
    /// 区切り線の色。
    static let divider = Color(red: 0xE5 / 255, green: 0xE7 / 255, blue: 0xEB / 255)
    /// 無彩色のボタンの塗り。
    static let surfaceVariant = Color(red: 0xF3 / 255, green: 0xF4 / 255, blue: 0xF6 / 255)
}
