package jp.kamusoft.ksdialogs.compose.support

import android.graphics.Rect
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/** 提示と結果確定を待つ上限 (ミリ秒)。 */
internal const val PRESENTATION_TIMEOUT_MILLIS = 10_000L

/** 実測した外形 (px) を論理単位 (dp) に直す。 */
internal fun Rect.toLogicalRect(density: Float): DialogLayoutCase.Rect = DialogLayoutCase.Rect(
    x = left / density.toDouble(),
    y = top / density.toDouble(),
    w = width() / density.toDouble(),
    h = height() / density.toDouble(),
)

/** 上限つきで待つ。 */
internal suspend fun <T> CompletableDeferred<T>.awaitPresented(): T =
    withTimeout(PRESENTATION_TIMEOUT_MILLIS) { await() }
