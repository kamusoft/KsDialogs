package jp.kamusoft.ksdialogs

/**
 * サイズと位置の計算を行う基準領域。
 *
 * 水平・垂直の両軸に効く (core/ADR-0008)。
 */
public enum class DialogLayoutArea {
    /** ダイアログを載せるウィンドウの全体。 */
    WINDOW,

    /** ウィンドウからシステムバーなどが占める余白を控除した可視領域。 */
    VISIBLE_AREA,
}
