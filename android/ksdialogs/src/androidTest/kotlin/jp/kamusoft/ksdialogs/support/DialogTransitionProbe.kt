package jp.kamusoft.ksdialogs.support

import android.os.Looper
import android.view.View
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

/** 出入りの演出フックの呼ばれ方を記録する観測子。 */
internal class DialogTransitionProbe {

    /** 演出の局面。 */
    enum class Phase {
        /** 出現。 */
        PRESENTATION,

        /** 退出。 */
        DISMISSAL,
    }

    /** フックの進み方として観測できる出来事。 */
    sealed interface Event {
        /** フックが始まった。 */
        data class Started(val phase: Phase) : Event

        /** フックが最後まで走った。 */
        data class Finished(val phase: Phase) : Event

        /** フックが取り消された。 */
        data class Cancelled(val phase: Phase) : Event

        /** フックが失敗した。 */
        data class Failed(val phase: Phase) : Event
    }

    /** フックが呼ばれた時点の状況。 */
    class Call(
        val hostView: View,
        val isOnMainThread: Boolean,
        val isAttachedToWindow: Boolean,
        val width: Int,
        val height: Int,
    )

    private val lock = Any()
    private val recordedEvents = mutableListOf<Event>()
    private val recordedCalls = mutableListOf<Pair<Phase, Call>>()

    /** 観測できた出来事を、起きた順に並べたもの。 */
    val events: List<Event>
        get() = synchronized(lock) { recordedEvents.toList() }

    /** その局面のフックが呼ばれた回数。 */
    fun callCount(phase: Phase): Int = synchronized(lock) { recordedCalls.count { it.first == phase } }

    /** その局面のフックが最初に呼ばれた時点の状況。 */
    fun firstCall(phase: Phase): Call? =
        synchronized(lock) { recordedCalls.firstOrNull { it.first == phase }?.second }

    /** その出来事が観測できているか。 */
    fun hasEvent(event: Event): Boolean = synchronized(lock) { recordedEvents.contains(event) }

    /** その場で完了するフック。 */
    fun immediateHook(phase: Phase): suspend (View) -> Unit = hook(phase) {}

    /** 門が開くまで完了しないフック。 */
    fun gatedHook(phase: Phase, gate: DialogTransitionGate): suspend (View) -> Unit = hook(phase) { gate.await() }

    /** 決して完了しないフック。 */
    fun neverEndingHook(phase: Phase): suspend (View) -> Unit = hook(phase) { awaitCancellation() }

    /** 例外を投げるフック。 */
    fun failingHook(phase: Phase): suspend (View) -> Unit = hook(phase) {
        error("演出の失敗")
    }

    /** 指定の時間だけかかるフック。 */
    fun delayedHook(phase: Phase, millis: Long): suspend (View) -> Unit = hook(phase) { delay(millis) }

    private fun hook(phase: Phase, body: suspend () -> Unit): suspend (View) -> Unit = { hostView ->
        synchronized(lock) {
            recordedCalls.add(
                phase to Call(
                    hostView = hostView,
                    isOnMainThread = Looper.myLooper() == Looper.getMainLooper(),
                    isAttachedToWindow = hostView.isAttachedToWindow,
                    width = hostView.width,
                    height = hostView.height,
                ),
            )
            recordedEvents.add(Event.Started(phase))
        }
        try {
            body()
            synchronized(lock) { recordedEvents.add(Event.Finished(phase)) }
        } catch (cancellation: CancellationException) {
            synchronized(lock) { recordedEvents.add(Event.Cancelled(phase)) }
            throw cancellation
        } catch (failure: Throwable) {
            synchronized(lock) { recordedEvents.add(Event.Failed(phase)) }
            throw failure
        }
    }
}

/** フックの完了する時点をテストから決めるための門。 */
internal class DialogTransitionGate {
    private val opened = CompletableDeferred<Unit>()

    /** 門を開き、待っているフックを完了させる。 */
    fun open() {
        opened.complete(Unit)
    }

    /** 門が開くまで待つ。 */
    suspend fun await() {
        opened.await()
    }
}
