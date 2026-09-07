package jp.kamusoft.ksdialogs.samples.android

import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import jp.kamusoft.ksdialogs.DialogTransition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 自作のフックで組む演出。
 *
 * プリセットを使わずに [DialogTransition] を直接組み立てる書き方の実演で、
 * 時間もイージングもこの演出自身が決めるため、画面の調整値は使わない。
 */
internal object SampleCustomTransition {
    /** 入場でホスト View が移動する距離 (dp)。下から持ち上げる分だけ最初に押し下げる。 */
    private const val TRAVEL_DISTANCE_DP = 80

    /** 入場・退場の時間 (ミリ秒)。 */
    private const val DURATION_MILLIS = 300L

    /** 入場は上方向へ移動しながら現れ、退場は透明度だけで消える演出を作る。 */
    fun create(): DialogTransition = DialogTransition(
        presentation = { hostView ->
            hostView.translationY = hostView.context.dp(TRAVEL_DISTANCE_DP).toFloat()
            hostView.alpha = 0f
            // 演出が終わってから戻る (戻るまで器は「表示中」へ進まない)
            await(hostView.animate().translationY(0f).alpha(1f))
        },
        dismissal = { hostView ->
            await(hostView.animate().alpha(0f))
        },
    )

    /** アニメーションを1本走らせ、完了するまで待つ。 */
    private suspend fun await(animator: ViewPropertyAnimator) {
        suspendCancellableCoroutine { continuation ->
            animator
                .setDuration(DURATION_MILLIS)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { continuation.resume(Unit) }
                .start()
        }
    }
}
