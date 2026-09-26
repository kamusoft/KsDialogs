package jp.kamusoft.ksdialogs.samples.kmp

/**
 * 属性調整パネルが選んだ基準領域。
 *
 * 基準領域は各 OS の View への添付で指定する静的メタ属性なので、共有コードは
 * 「どれを選んだか」だけを運び、View 定義側でその OS の基準領域へ言い換える。
 */
enum class SampleLayoutAreaPreset {
    /** ダイアログを載せるウィンドウの全体。 */
    WINDOW,

    /** システムバーなどを除いた可視領域。 */
    VISIBLE_AREA,

    /** 表示中のページ (タブバーやナビゲーションバーを除いた領域)。 */
    CURRENT_PAGE,
}
