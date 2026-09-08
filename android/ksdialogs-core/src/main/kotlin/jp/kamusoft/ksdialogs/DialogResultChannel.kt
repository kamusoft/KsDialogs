package jp.kamusoft.ksdialogs

/**
 * 1回の show に対応する結果チャネル。
 *
 * 任意のスレッドからの報告を受け付け、最初の報告だけを有効として下流へ流す (2回目以降は捨てる)。
 * この最初の1回で結果が不可逆に確定する (ラッチ) 一方、呼び出し元への配送は
 * 器の撤去まで進んだ提示層が別に行う (core/ADR-0017)。
 *
 * 観察者 (提示層・器) のハンドラ登録より先に報告が来る場合があるため、
 * 確定済みのチャネルへ登録されたハンドラはその場で確定値を受け取る。
 */
internal class DialogResultChannel {
    private val lock = Any()
    private var isSettled = false
    private var latchedOutcome: DialogOutcome? = null
    private var latchedOrigin: DialogDismissalOrigin? = null
    private val settleHandlers = mutableListOf<(DialogOutcome) -> Unit>()
    private var callerCancellationHandler: (() -> Unit)? = null

    /** 結果が確定済みかどうか。 */
    val isResultSettled: Boolean
        get() = synchronized(lock) { isSettled }

    /** 確定した結果。未確定なら null。ハンドラの登録状況とは無関係にいつでも読める。 */
    val settledOutcome: DialogOutcome?
        get() = synchronized(lock) { latchedOutcome }

    /** 結果を確定させた原因。未確定なら null。 */
    val resultOrigin: DialogDismissalOrigin?
        get() = synchronized(lock) { latchedOrigin }

    /**
     * 結果を確定させる。確定済みなら何もしない。
     *
     * @param outcome 確定させる結果
     * @param origin その結果を確定させた原因。提示層が退出の進め方を決めるために読む
     */
    fun settle(outcome: DialogOutcome, origin: DialogDismissalOrigin = DialogDismissalOrigin.REPORT) {
        val handlersToInvoke = synchronized(lock) {
            if (isSettled) return
            isSettled = true
            latchedOutcome = outcome
            latchedOrigin = origin
            settleHandlers.toList().also { settleHandlers.clear() }
        }
        // ハンドラの実行はロックの外で行い、報告側のスレッドを長く止めない
        handlersToInvoke.forEach { it(outcome) }
    }

    /** 結果確定時に一度だけ呼ばれるハンドラを登録する。登録時点で確定済みなら即座に呼ぶ。 */
    fun onSettle(handler: (DialogOutcome) -> Unit) {
        val settledOutcome = synchronized(lock) {
            if (!isSettled) {
                settleHandlers.add(handler)
                null
            } else {
                latchedOutcome
            }
        }
        settledOutcome?.let(handler)
    }

    /**
     * 呼び出し元の取り消しを知らせる。
     *
     * 未確定ならキャンセルとして確定させたうえで、確定済みかどうかによらず観察者へ伝える。
     * 確定済みでも伝えるのは、退出の途中でも呼び出し元が待つのをやめられるようにするため。
     */
    fun cancelFromCaller() {
        settle(DialogOutcome.Cancelled, DialogDismissalOrigin.CALLER_CANCELLATION)
        val observer = synchronized(lock) { callerCancellationHandler }
        observer?.invoke()
    }

    /** 呼び出し元の取り消しを観察するハンドラを登録する。 */
    fun onCallerCancellation(handler: () -> Unit) {
        synchronized(lock) { callerCancellationHandler = handler }
    }
}
