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

    /**
     * 表示中のページの矩形のうち、可視領域と重なる部分。
     *
     * 下部のナビゲーションバーやタブを持つ画面では、それらを除いたページの領域が基準になる。
     * 表示中のページは次の順で探し、見つかった最初のものを使う。
     *
     * 1. Compose の `Modifier.ksDialogCurrentPage()` を付けた composable のうち、画面に配置されているもの
     * 2. [DialogCurrentPage.provider] に登録した関数が返す View
     *
     * どちらからもページが得られないときは [VISIBLE_AREA] と同じ結果になる。
     */
    CURRENT_PAGE,
}
