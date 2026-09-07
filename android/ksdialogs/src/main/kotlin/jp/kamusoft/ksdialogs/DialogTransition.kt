package jp.kamusoft.ksdialogs

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * ダイアログの出入りの演出 (core/ADR-0017)。
 *
 * 中身の定義へ添付すると、器は出現時に [presentation]、閉鎖時に [dismissal] を呼び、
 * その完了を待ってから次へ進む。フックはどちらも省略でき、省略した側には器の既定の
 * クロスフェードが適用される。指定した側では既定は実行されない (置き換え)。
 *
 * フックが受け取るのは中身のホスト View で、宣言的 UI の中身では中身を包む View になる。
 * 背景の覆いはホスト View の兄弟レイヤとして器が常に扱うため、フックの対象にならない。
 * 覆いのフェード時間は [overlayDuration] に従う (省略時は器の既定値)。
 *
 * フックは Main ディスパッチャで開始される。有限時間で完了することは利用者の責務で、
 * 器はタイムアウトを設けない。フックが投げた失敗は演出の失敗として吸収され、
 * ダイアログの結果には影響しない。
 *
 * @param presentation 出現時の演出。null なら器の既定が使われる
 * @param dismissal 閉鎖時の演出。null なら器の既定が使われる
 * @param overlayDuration 背景の覆いのフェード時間。null なら器の既定値になる
 */
public class DialogTransition(
    public val presentation: (suspend (View) -> Unit)? = null,
    public val dismissal: (suspend (View) -> Unit)? = null,
    public val overlayDuration: Duration? = null,
) {
    public companion object {
        /** プリセットが引数を省略したときの時間。 */
        internal val DEFAULT_DURATION: Duration = 250.milliseconds

        /** ズームの開始・終了倍率。 */
        private const val ZOOM_SCALE = 0.8f

        /**
         * 透明度で出入りする演出。
         *
         * @param duration 片道の時間。成立しない値 (0・負値・無限大) では演出なしで即完了する
         * @param easing 時間に対する進み方
         */
        public fun fade(
            duration: Duration = DEFAULT_DURATION,
            easing: Interpolator = AccelerateDecelerateInterpolator(),
        ): DialogTransition = DialogTransition(
            presentation = { view ->
                view.alpha = 0f
                DialogTransitionAnimator.run(duration, easing) { progress -> view.alpha = progress }
            },
            dismissal = { view ->
                val from = view.alpha
                DialogTransitionAnimator.run(duration, easing) { progress -> view.alpha = from * (1f - progress) }
            },
            overlayDuration = duration,
        )

        /**
         * 指定した辺から滑り込み、同じ辺へ滑り出す演出。
         *
         * @param from 出入り口になる辺
         * @param duration 片道の時間。成立しない値では演出なしで即完了する
         * @param easing 時間に対する進み方
         */
        public fun slide(
            from: DialogTransitionEdge,
            duration: Duration = DEFAULT_DURATION,
            easing: Interpolator = AccelerateDecelerateInterpolator(),
        ): DialogTransition = DialogTransition(
            presentation = { view ->
                val outward = slideOffset(from, view)
                view.translationX = outward.first
                view.translationY = outward.second
                DialogTransitionAnimator.run(duration, easing) { progress ->
                    view.translationX = outward.first * (1f - progress)
                    view.translationY = outward.second * (1f - progress)
                }
            },
            dismissal = { view ->
                val outward = slideOffset(from, view)
                DialogTransitionAnimator.run(duration, easing) { progress ->
                    view.translationX = outward.first * progress
                    view.translationY = outward.second * progress
                }
            },
            overlayDuration = duration,
        )

        /**
         * 少し縮んだ状態から等倍へ広がり、同じ倍率へ縮んで消える演出。
         *
         * @param duration 片道の時間。成立しない値では演出なしで即完了する
         * @param easing 時間に対する進み方
         */
        public fun zoom(
            duration: Duration = DEFAULT_DURATION,
            easing: Interpolator = AccelerateDecelerateInterpolator(),
        ): DialogTransition = DialogTransition(
            presentation = { view ->
                view.scaleX = ZOOM_SCALE
                view.scaleY = ZOOM_SCALE
                view.alpha = 0f
                DialogTransitionAnimator.run(duration, easing) { progress ->
                    val scale = ZOOM_SCALE + (1f - ZOOM_SCALE) * progress
                    view.scaleX = scale
                    view.scaleY = scale
                    view.alpha = progress
                }
            },
            dismissal = { view ->
                val fromAlpha = view.alpha
                DialogTransitionAnimator.run(duration, easing) { progress ->
                    val scale = 1f - (1f - ZOOM_SCALE) * progress
                    view.scaleX = scale
                    view.scaleY = scale
                    view.alpha = fromAlpha * (1f - progress)
                }
            },
            overlayDuration = duration,
        )

        /** 中身の演出を持たない組。覆いだけが器の既定の時間でフェードする。 */
        public fun none(): DialogTransition = DialogTransition(
            presentation = { },
            dismissal = { },
            overlayDuration = DEFAULT_DURATION,
        )

        /**
         * 指定の辺の外側まで View を送り出す移動量 (x, y) を求める。
         *
         * 送り出す距離は、View が載っているウィンドウの縁を越えるところまでを取る。
         */
        private fun slideOffset(edge: DialogTransitionEdge, view: View): Pair<Float, Float> {
            val root = view.rootView
            val viewLocation = IntArray(2).also(view::getLocationInWindow)
            val rootLocation = IntArray(2).also(root::getLocationInWindow)
            val left = (viewLocation[0] - rootLocation[0]).toFloat()
            val top = (viewLocation[1] - rootLocation[1]).toFloat()
            return when (resolvedEdge(edge, view)) {
                DialogTransitionEdge.TOP -> 0f to -(top + view.height)
                DialogTransitionEdge.BOTTOM -> 0f to (root.height - top)
                DialogTransitionEdge.START -> -(left + view.width) to 0f
                DialogTransitionEdge.END -> (root.width - left) to 0f
            }
        }

        /**
         * レイアウト方向に追随する辺を、画面上の辺へ読み替える。
         *
         * 右から左へ読む環境では START が右、END が左になる。
         */
        private fun resolvedEdge(edge: DialogTransitionEdge, view: View): DialogTransitionEdge {
            if (view.layoutDirection != View.LAYOUT_DIRECTION_RTL) {
                return edge
            }
            return when (edge) {
                DialogTransitionEdge.START -> DialogTransitionEdge.END
                DialogTransitionEdge.END -> DialogTransitionEdge.START
                DialogTransitionEdge.TOP, DialogTransitionEdge.BOTTOM -> edge
            }
        }
    }
}
