package jp.kamusoft.ksdialogs.kmp.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * 委譲先の処理が UI スレッドで動くのを待ちながら suspend 処理を完了させる。
 *
 * Native ライブラリの互換面は提示処理を UI スレッドへ回すため、テスト本体が UI スレッドを掴んだままだと
 * その処理が動けない。処理自体は別スレッドで進め、UI スレッドは待っている間 run loop を回す。
 *
 * @param timeout これを超えても完了しなければ失敗させる
 */
internal fun <T> runPumpingMainLoop(
    timeout: Duration = 10.seconds,
    block: suspend () -> T,
): T {
    val outcome = AtomicReference<Result<T>?>(null)
    CoroutineScope(Dispatchers.Default).launch {
        outcome.value = runCatching { block() }
    }

    val started = TimeSource.Monotonic.markNow()
    while (outcome.value == null && started.elapsedNow() < timeout) {
        NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
    }
    return checkNotNull(outcome.value) { "処理が $timeout 以内に完了しませんでした。" }.getOrThrow()
}

/**
 * UI スレッドの run loop を、[condition] が満たされるか [timeout] が過ぎるまで回す。
 *
 * @return 最後に評価した [condition] の値
 */
internal fun pumpMainLoopUntil(
    timeout: Duration = 10.seconds,
    condition: () -> Boolean,
): Boolean {
    val started = TimeSource.Monotonic.markNow()
    while (!condition() && started.elapsedNow() < timeout) {
        NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
    }
    return condition()
}

/**
 * 共有コードの show を別スレッドで始め、失敗も完了もせずに提示先を待っていることを確かめてから打ち切る。
 *
 * テストの実行体には提示先の画面が無いため、登録済みの ViewModel の show は失敗せずに待ち続ける
 * (core/ADR-0041)。未登録の ViewModel は待たずにその場で失敗する。この違いを View factory の解決に
 * 成功した印として使う。待っていることは、未登録の失敗が十分に届く [waitingPeriod] の間、
 * UI スレッドを回しても結果も失敗も返らないことで判定する。
 *
 * @param waitingPeriod 待っていると判定するまで UI スレッドを回す時間
 * @param whileWaiting 待っていることを確かめたあと、打ち切る前に行う観察
 * @param show 待たせたい show の呼び出し
 * @return show の呼び出し元へ伝播した [CancellationException]
 */
internal fun cancelWhileWaitingForHost(
    waitingPeriod: Duration = 500.milliseconds,
    whileWaiting: () -> Unit = {},
    show: suspend () -> Unit,
): CancellationException {
    val returned = AtomicInt(0)
    val thrown = AtomicReference<Throwable?>(null)
    val showing = CoroutineScope(Dispatchers.Default).launch {
        try {
            show()
            returned.value = 1
        } catch (failure: Throwable) {
            // 構成エラーをテストの失敗の外へ逃がさず、何が伝播したかを判定に使う
            thrown.value = failure
        }
    }

    pumpMainLoopUntil(timeout = waitingPeriod) { !showing.isActive }
    assertNull(thrown.value, "登録済みの ViewModel の show が待たずに失敗しました: ${thrown.value}")
    assertEquals(0, returned.value, "提示先が無いのに show が結果を返しました。")
    assertTrue(showing.isActive, "show が提示先を待っていません。")
    whileWaiting()

    showing.cancel()
    assertTrue(pumpMainLoopUntil { showing.isCompleted }, "打ち切ったあとも show が終わりません。")
    assertEquals(0, returned.value, "打ち切った show が結果を返しました。")
    return assertIs<CancellationException>(
        thrown.value,
        "打ち切った show の呼び出し元へキャンセルの通知が伝播しませんでした: ${thrown.value}",
    )
}
