package jp.kamusoft.ksdialogs.support

import kotlinx.coroutines.delay

/** 提示・結果確定のような非同期の状態変化を待ち合わせるための補助。 */
internal object DialogTestWaiting {
    private const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    private const val POLLING_INTERVAL_MILLIS = 5L

    /** 条件が満たされるまで待つ。時間切れになったら false を返す。 */
    suspend fun waitUntil(
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        while (System.nanoTime() < deadline) {
            if (condition()) return true
            delay(POLLING_INTERVAL_MILLIS)
        }
        return condition()
    }
}
