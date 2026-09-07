package jp.kamusoft.ksdialogs.kmp.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * 自分が受け取った block を実行している最中かどうかを示すディスパッチャ。
 *
 * 実行そのものは dispatch を呼んだスレッドで行い、その区間だけ [isRunningBlock] が true になる。
 * 呼び出し元の文脈からいったん抜けて別のディスパッチャで動く実装では、
 * 抜けた時点で block の実行が終わる (コルーチンが中断して block から戻る) ため、
 * 「この印が立っている間に呼ばれた」ことが、文脈を移していない直接の証拠になる。
 */
internal class ExecutionMarkingDispatcher : CoroutineDispatcher() {
    /** この dispatcher が渡された block を実行中かどうか。 */
    var isRunningBlock: Boolean = false
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val outer = isRunningBlock
        isRunningBlock = true
        try {
            block.run()
        } finally {
            isRunningBlock = outer
        }
    }
}
