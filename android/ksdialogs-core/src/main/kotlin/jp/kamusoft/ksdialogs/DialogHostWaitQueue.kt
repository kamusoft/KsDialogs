package jp.kamusoft.ksdialogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * 提示先の出現を待っている Dialog の列 (core/ADR-0039)。
 *
 * 待っている Dialog は呼んだ順に並び、提示先が現れたら先頭の 1 枚ずつ明ける。明けた 1 枚の提示が
 * 終わる (器のウィンドウを追加する) までは次を明けないので、後から呼んだものが手前に重なる。
 * 列に待っている Dialog があるうちの新しい show は、提示先があっても後ろに並び、先に待っている
 * Dialog を追い越さない。
 *
 * 列を持つのは OS に渡す前の待ちだけで、渡したあとの重なりはウィンドウの追加順に任せる
 * (core/ADR-0006)。提示先の入れ替わりの通知は、列が空でない間だけ 1 本購読する。
 *
 * 同じ提示先を共有する show は同じ列に並ぶ必要があるため、既定の提示面はすべて [application] を使う。
 * テスト用の提示面は、互いに干渉しないよう自分の列を持つ。
 *
 * [waitingCount] を除き、すべて UI スレッドから呼ぶ。
 */
internal class DialogHostWaitQueue {
    /** 待っている show (呼んだ順)。 */
    private val waiters = ArrayDeque<Waiter>()

    /** 列から明けて、提示が終わっていない 1 枚の番。 */
    private var presentingSlot: DialogHostWaitSlot? = null

    /** 提示先の入れ替わりの購読。列が空でない間だけ張る。 */
    private var registration: DialogHostRegistration? = null

    @Volatile
    private var waitingCountSnapshot: Int = 0

    /** 待っている show の数。任意のスレッドから読める。 */
    val waitingCount: Int
        get() = waitingCountSnapshot

    /**
     * 提示してよくなるまで待つ。
     *
     * 列が空で、提示中の番も無く、提示先があれば、待たずに提示へ進む。それ以外は列の後ろに並び、
     * 提示先が現れて先頭に来た時点か、結果が確定した時点のどちらかで明ける。
     * 呼び出し元の打ち切りは結果チャネルの取り消しとして確定させ、確定と同じく列から外したうえで
     * キャンセルを投げ直す。
     *
     * @param surface 提示に使う面。提示先の有無はこの面で読む
     * @param resultChannel この show の結果チャネル。待っている間の確定を観察する
     */
    suspend fun waitForTurn(
        surface: DialogPresentationSurface,
        resultChannel: DialogResultChannel,
    ): DialogHostWaitTurn {
        if (waiters.isEmpty() && presentingSlot == null && surface.canPresent) {
            return DialogHostWaitTurn.Ready(null)
        }
        var returnsToFront = false
        while (true) {
            val waiter = Waiter(surface)
            // 確定の通知は任意のスレッドから届く。番の受け渡しは先着 1 回なので、列の操作は UI スレッドに残す
            resultChannel.onSettle { waiter.turn.complete(DialogHostWaitTurn.Settled) }
            enqueue(waiter, atFront = returnsToFront)
            val turn = try {
                waiter.turn.await()
            } catch (cancellation: CancellationException) {
                // 呼び出し元の打ち切りは、キャンセルとして結果を確定させる
                resultChannel.cancelFromCaller()
                abandon(waiter)
                throw cancellation
            }
            when (turn) {
                is DialogHostWaitTurn.Settled -> {
                    abandon(waiter)
                    return turn
                }
                is DialogHostWaitTurn.Ready -> {
                    // 明けてから再開するまでの間に確定していれば (呼び出し元の打ち切りを含む)、提示しない
                    if (resultChannel.isResultSettled) {
                        turn.slot?.finish()
                        return DialogHostWaitTurn.Settled
                    }
                    if (surface.canPresent) {
                        return turn
                    }
                    // 明けてから再開するまでの間に提示先が消えた。順番を保ったまま先頭で待ち直す
                    turn.slot?.relinquish()
                    returnsToFront = true
                }
            }
        }
    }

    /** 番を持っていた 1 枚の提示が終わった。 */
    fun slotDidFinish(slot: DialogHostWaitSlot, advances: Boolean) {
        if (presentingSlot !== slot) return
        presentingSlot = null
        if (advances) {
            advance()
        }
    }

    /** 列に並べ、入れ替わりの購読を用意する。 */
    private fun enqueue(waiter: Waiter, atFront: Boolean) {
        if (atFront) {
            waiters.addFirst(waiter)
        } else {
            waiters.addLast(waiter)
        }
        updateWaitingCount()
        if (registration == null) {
            registration = waiter.surface.observeHostChange { advance() }
        }
        // 通知を待たずに提示先が既にある場合 (提示中の番が無いのに列が残っていた場合) も先へ進める
        advance()
    }

    /**
     * 待ちを終えた show を列から外す。
     *
     * 打ち切りと確定が、番の受け渡しと入れ違いになった場合は、受け取っていた番を返して次を明けさせる。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun abandon(waiter: Waiter) {
        if (waiters.remove(waiter)) {
            updateWaitingCount()
            releaseRegistrationIfIdle()
        }
        val turn = if (waiter.turn.isCompleted) waiter.turn.getCompleted() else null
        if (turn is DialogHostWaitTurn.Ready) {
            turn.slot?.finish()
        }
        // 先頭が外れたことで、次の 1 枚が明けられるようになっていれば明ける
        advance()
    }

    /** 提示先があり、提示中の番が無ければ、列の先頭の 1 枚だけを明ける。 */
    private fun advance() {
        while (presentingSlot == null) {
            val head = waiters.firstOrNull() ?: break
            if (head.turn.isCompleted) {
                // 確定済みで、本人が列から外れに来るのを待っている。順番を塞がないよう先に外す
                waiters.removeFirst()
                updateWaitingCount()
                continue
            }
            // 通知は「現れたかもしれない」ことだけを知らせるので、提示先を読み直す
            if (!head.surface.canPresent) break
            waiters.removeFirst()
            updateWaitingCount()
            val slot = DialogHostWaitSlot(this)
            presentingSlot = slot
            // 状態を整えてから再開させる (再開がこの呼び出しの中で走っても、列は一貫している)
            if (!head.turn.complete(DialogHostWaitTurn.Ready(slot))) {
                // 別スレッドの確定と入れ違った。番は渡っていないので次の 1 枚へ進む
                presentingSlot = null
            }
        }
        releaseRegistrationIfIdle()
    }

    /** 待っている show が無くなったら、入れ替わりの購読を解除する。 */
    private fun releaseRegistrationIfIdle() {
        if (waiters.isNotEmpty()) return
        registration?.cancel()
        registration = null
    }

    private fun updateWaitingCount() {
        waitingCountSnapshot = waiters.size
    }

    /** 列に並んでいる show 1 件。 */
    private class Waiter(
        /** この show が提示に使う面。明けてよいかは、この面の提示先を読み直して決める。 */
        val surface: DialogPresentationSurface,
    ) {
        /** 番の受け渡し。先着 1 回だけ効く。 */
        val turn = CompletableDeferred<DialogHostWaitTurn>()
    }

    companion object {
        /** アプリケーションの提示先 (resumed な Activity) を使う show が並ぶ列。 */
        val application: DialogHostWaitQueue = DialogHostWaitQueue()
    }
}
