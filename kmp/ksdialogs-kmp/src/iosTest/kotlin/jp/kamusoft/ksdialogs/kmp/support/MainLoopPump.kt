package jp.kamusoft.ksdialogs.kmp.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.concurrent.AtomicReference
import kotlin.time.Duration
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
