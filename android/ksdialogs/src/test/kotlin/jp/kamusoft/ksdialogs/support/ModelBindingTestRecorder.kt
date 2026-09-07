package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.DialogNotifier
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 中身の生成時点で ViewModel から読めたものを記録する。
 *
 * 「中身の生成中に結果報告口が引けたか」「configure が設定した状態が中身へ渡ったか」を、
 * 生成された側から観察するために使う。記録は UI スレッドと呼び出し元スレッドの双方から
 * 行われるためロックで保護する。
 */
internal class ModelBindingTestRecorder {
    private val lock = Any()
    private val recordedNotifiers = mutableListOf<DialogNotifier<Boolean>?>()
    private val recordedMessages = mutableListOf<String>()

    /** 中身の生成時に ViewModel から取り出した結果報告口 (生成順)。引けなければ null。 */
    val suppliedNotifiers: List<DialogNotifier<Boolean>?>
        get() = synchronized(lock) { recordedNotifiers.toList() }

    /** 中身の生成時に読んだ ViewModel の状態 (生成順)。 */
    val observedMessages: List<String>
        get() = synchronized(lock) { recordedMessages.toList() }

    /** 中身の生成回数。 */
    val creationCount: Int
        get() = synchronized(lock) { recordedNotifiers.size }

    /** 中身の生成時点で読めたものを記録する。 */
    fun record(notifier: DialogNotifier<Boolean>?, message: String) {
        synchronized(lock) {
            recordedNotifiers.add(notifier)
            recordedMessages.add(message)
        }
    }

    /** index 番目の中身が生成されるまで待ち、そのとき引けた結果報告口を返す。 */
    suspend fun awaitSuppliedNotifier(index: Int): DialogNotifier<Boolean> {
        awaitCreation(index)
        val notifier = suppliedNotifiers[index]
        assertNotNull(notifier, "中身の生成中に結果報告口が引けること")
        return notifier!!
    }

    /** index 番目の中身が生成されるまで待ち、そのとき読んだ状態を返す。 */
    suspend fun awaitObservedMessage(index: Int): String {
        awaitCreation(index)
        return observedMessages[index]
    }

    private suspend fun awaitCreation(index: Int) {
        val created = DialogTestWaiting.waitUntil { creationCount > index }
        assertTrue(created, "index $index の中身が生成されなかった")
    }
}
