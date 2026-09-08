package jp.kamusoft.ksdialogs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.Interpolator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration

/**
 * 時間つきのアニメーションを1本走らせ、その完了を待てるようにする実行部。
 *
 * プリセットのフックと器の覆いのフェードは、どちらもここを通る。
 */
internal object DialogTransitionAnimator {

    /**
     * その時間でアニメーションを組めるか。
     *
     * 0・負値・無限大・ミリ秒に落とすと 0 になる長さは演出として成立しないため、
     * アニメーションを組まずに最終状態へ飛ばす合図として使う (例外にもオーバーフローにもしない)。
     */
    fun isAnimatable(duration: Duration): Boolean =
        duration.isFinite() && duration.isPositive() && duration.inWholeMilliseconds > 0

    /**
     * 指定の時間・イージングで進み具合 0→1 を動かし、完了するまで待つ。
     *
     * 時間が成立しない値なら最終状態 (1) を即座に反映して戻る。
     * 待っている間にキャンセルされたら、アニメーションを最終状態で打ち切って戻る。
     *
     * @param duration 片道の時間
     * @param easing 時間に対する進み方
     * @param onProgress 進み具合 (0〜1) を受け取り、その時点の見た目を反映する
     */
    suspend fun run(
        duration: Duration,
        easing: Interpolator,
        onProgress: (Float) -> Unit,
    ) {
        if (!isAnimatable(duration)) {
            onProgress(1f)
            return
        }
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration.inWholeMilliseconds
            this.interpolator = easing
            addUpdateListener { onProgress(it.animatedValue as Float) }
        }
        try {
            suspendCancellableCoroutine { continuation ->
                animator.addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                    },
                )
                animator.start()
            }
        } finally {
            if (animator.isRunning) {
                // 待ちが打ち切られた分。中途半端な見た目を残さないよう最終状態へ飛ばす
                animator.cancel()
                onProgress(1f)
            }
        }
    }
}
