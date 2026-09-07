package jp.kamusoft.ksdialogs.samples.kmp.android

/**
 * 4ルートの Sample が共有する配色。
 *
 * OS 固有の semantic color は使わず、全ルートで同一の RGBA を宣言する (cross/ADR-0007)。
 * 覆い (scrim) はライブラリの器が描くため、ここには持たない。
 */
internal object SampleTheme {
    /** 強調色。完了操作のボタンの塗り。 */
    const val PRIMARY: Int = 0xFF2563EB.toInt()

    /** primary の上に載る文字色。 */
    const val ON_PRIMARY: Int = 0xFFFFFFFF.toInt()

    /** 画面・ダイアログの地の色。 */
    const val SURFACE: Int = 0xFFFFFFFF.toInt()

    /** surface の上に載る文字色。 */
    const val ON_SURFACE: Int = 0xFF1F2937.toInt()

    /** surface の上に載る補助的な文字色。 */
    const val ON_SURFACE_MUTED: Int = 0xFF6B7280.toInt()

    /** 区切り線の色。 */
    const val DIVIDER: Int = 0xFFE5E7EB.toInt()

    /** 無彩色のボタンの塗り。 */
    const val SURFACE_VARIANT: Int = 0xFFF3F4F6.toInt()
}
