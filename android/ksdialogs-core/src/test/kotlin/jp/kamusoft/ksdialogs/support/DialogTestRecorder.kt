package jp.kamusoft.ksdialogs.support

import android.view.View
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogResult
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * View factory の呼び出しと show の完了を記録する。
 * 記録は UI スレッドと呼び出し元スレッドの双方から行われるためロックで保護する。
 */
internal class DialogTestRecorder<R> {
    private val lock = Any()
    private val recordedViews = mutableListOf<View>()
    private val recordedNotifiers = mutableListOf<DialogNotifier<R>>()
    private val recordedResults = mutableListOf<DialogResult<R>>()

    /** factory が生成した View の一覧 (生成順)。 */
    val createdViews: List<View>
        get() = synchronized(lock) { recordedViews.toList() }

    /** show 1回ごとの結果報告口の一覧 (生成順)。 */
    val notifiers: List<DialogNotifier<R>>
        get() = synchronized(lock) { recordedNotifiers.toList() }

    /** show が返した結果の一覧 (確定順)。 */
    val results: List<DialogResult<R>>
        get() = synchronized(lock) { recordedResults.toList() }

    /** factory が生成した View と、その回の結果報告口を記録する。 */
    fun record(view: View, notifier: DialogNotifier<R>) {
        synchronized(lock) {
            recordedViews.add(view)
            recordedNotifiers.add(notifier)
        }
    }

    /** show が返した結果を記録する。 */
    fun record(result: DialogResult<R>) {
        synchronized(lock) { recordedResults.add(result) }
    }

    /** index 番目の View が生成されるまで待ち、その回の結果報告口を返す。 */
    suspend fun awaitNotifier(index: Int): DialogNotifier<R> {
        val appeared = DialogTestWaiting.waitUntil { notifiers.size > index }
        assertTrue(appeared, "index $index の View factory が呼ばれなかった")
        return notifiers[index]
    }
}
