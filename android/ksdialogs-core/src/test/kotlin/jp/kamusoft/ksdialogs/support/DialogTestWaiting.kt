package jp.kamusoft.ksdialogs.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

    /**
     * 条件が満たされるまで待つ。条件は毎回 UI スレッド ([Dispatchers.Main]) の上で読む。
     *
     * UI スレッドが書き換える可変の状態 (coordinator の表示の列など) を条件にするときに使う。
     * テストのスレッドから走査すると、UI スレッドの追加・削除と重なって
     * [ConcurrentModificationException] になり得るため。
     */
    suspend fun waitUntilOnMain(
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        while (System.nanoTime() < deadline) {
            if (withContext(Dispatchers.Main) { condition() }) return true
            delay(POLLING_INTERVAL_MILLIS)
        }
        return withContext(Dispatchers.Main) { condition() }
    }
}
