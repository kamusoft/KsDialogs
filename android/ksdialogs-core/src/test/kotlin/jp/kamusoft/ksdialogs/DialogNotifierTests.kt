package jp.kamusoft.ksdialogs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("結果はちょうど1回だけ確定する")
class DialogNotifierTests {

    @Test
    fun `確定後の再報告は無効`() {
        val resultChannel = DialogResultChannel()
        val notifier = DialogNotifier<Boolean>(resultChannel)
        val outcomes = mutableListOf<DialogOutcome>()
        resultChannel.onSettle { outcomes.add(it) }

        notifier.complete(true)
        notifier.complete(false)
        notifier.cancel()

        assertEquals(1, outcomes.size)
        assertEquals(true, (outcomes.first() as DialogOutcome.Completed).value)
        assertTrue(resultChannel.isResultSettled)
    }

    @Test
    fun `受け取り側の登録より先に確定した結果も1回だけ届く`() {
        val resultChannel = DialogResultChannel()
        val notifier = DialogNotifier<Boolean>(resultChannel)
        val outcomes = mutableListOf<DialogOutcome>()

        notifier.cancel()
        notifier.complete(true)
        resultChannel.onSettle { outcomes.add(it) }

        assertEquals(1, outcomes.size)
        assertTrue(outcomes.first() is DialogOutcome.Cancelled)
    }
}
