package jp.kamusoft.ksdialogs

/**
 * 結果が確定した原因。公開 API には出さず、提示層が退出の進め方を決めるためだけに使う。
 *
 * 「どの操作でキャンセルされたか」を利用者へ返さない結果通知のルールは変わらない。
 */
internal enum class DialogDismissalOrigin {
    /** 中身からの結果報告。 */
    REPORT,

    /** 覆いへのタップ。 */
    OUTSIDE_TAP,

    /** 戻るボタン操作。 */
    BACK_PRESS,

    /** show を待っている呼び出し元の取り消し。 */
    CALLER_CANCELLATION,

    /**
     * 器が画面を失ったこと (画面破棄・ウィンドウの取り外し)。
     *
     * この原因では器が演出できないため、退出の演出は行わない。
     */
    HOST_LOST,
}
