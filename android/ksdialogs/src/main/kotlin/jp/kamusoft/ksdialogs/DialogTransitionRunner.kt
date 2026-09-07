package jp.kamusoft.ksdialogs

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 出入りの演出を実行する部品 (core/ADR-0017)。
 *
 * 演出の実効値の固定 (フックの解決)、フックの起動と失敗の吸収、
 * 中身とは別のレイヤに敷いた覆いのフェードを受け持つ。
 * 器はこの部品を持ち、提示・退出のそれぞれで対応する局面を走らせる。
 *
 * 覆いは器が組み立てるため、組み上がった時点で [bindOverlay] により結び付ける。
 * 結び付ける前の局面では覆いのフェードは行われない。
 *
 * @param context 提示先の文脈。フックの完了忘れを知らせる見張りを開発中だけに絞るために読む
 * @param contentView 演出のフックへ渡す中身のホスト View
 */
internal class DialogTransitionRunner(
    private val context: Context,
    private val contentView: View,
) {

    /** 背景の覆い。中身の兄弟として敷かれ、中身とは独立にフェードする。結び付く前は null。 */
    private var overlayView: View? = null

    /** 中身が元々持っていた透明度。出現の演出を始めるときにこの値へ戻す。 */
    private var contentInitialAlpha: Float = contentView.alpha

    /** 実効値と同じ時点で固定された出入りの演出。固定前は null。 */
    var resolvedTransition: DialogTransition? = null
        private set

    /** 固定された覆いのフェード時間。 */
    var resolvedOverlayDuration: Duration = DialogTransition.DEFAULT_DURATION
        private set

    /** 器が組み立てた覆いを結び付ける。覆いも中身と同じく、出現の演出で初めて見えるようにする。 */
    fun bindOverlay(overlayView: View) {
        this.overlayView = overlayView
        overlayView.alpha = 0f
    }

    /**
     * 演出を始めるまで中身を見せない状態にする。
     *
     * 最終位置が演出より先に1フレーム見えるのを防ぐ (core/ADR-0017)。
     * 戻す先の透明度はこの時点の値を控える。
     */
    fun concealContent() {
        contentInitialAlpha = contentView.alpha
        contentView.alpha = 0f
    }

    /**
     * 中身を「入りの演出を終えた見え」へ戻す。
     *
     * 入りの演出の途中で器だけを載せ替える経路では、次の器は演出をやり直さないため、
     * 打ち切られた時点の途中の見え (透明・画面外・縮小) のまま固まってしまう。
     * 器が中身に対して動かす属性 (透明度と、プリセットが動かす位置・倍率) を
     * 演出の終着点へ揃えることで、載せ替えても中身が見えたまま続く。
     */
    fun revealContentAsShown() {
        contentView.alpha = contentInitialAlpha
        contentView.translationX = 0f
        contentView.translationY = 0f
        contentView.scaleX = 1f
        contentView.scaleY = 1f
    }

    /**
     * レイアウトの実効値と同じ時点で出入りの演出を固定する (core/ADR-0017)。
     *
     * 添付されていない側のフックと、省略された覆いの時間には器の既定が入る。
     *
     * @param attached 中身へ添付された演出の組。添付がなければ null
     */
    fun settleSnapshot(attached: DialogTransition?) {
        val resolved = DialogTransition(
            presentation = attached?.presentation ?: DEFAULT_TRANSITION.presentation,
            dismissal = attached?.dismissal ?: DEFAULT_TRANSITION.dismissal,
            overlayDuration = attached?.overlayDuration ?: DEFAULT_TRANSITION.overlayDuration,
        )
        resolvedTransition = resolved
        resolvedOverlayDuration = resolved.overlayDuration ?: DialogTransition.DEFAULT_DURATION
    }

    /**
     * 中身を見せたうえで、出現の演出と覆いの出現フェードを並行して走らせ、両方の完了を待つ。
     *
     * 中身を見せる操作とフックの呼び出しは同じ同期区間に置く。どちらも中断を挟まずに始まるため、
     * フックが最初に置く状態 (滑り込みなら画面外の位置) までがこの区間の中で反映され、
     * フックが効く前の最終位置が1フレーム見える現象を構造的に防ぐ (core/ADR-0017)。
     */
    suspend fun runPresentationPhase(): Unit = coroutineScope {
        contentView.alpha = contentInitialAlpha
        launch(start = CoroutineStart.UNDISPATCHED) { runOverlayFade(toVisible = true) }
        launch(start = CoroutineStart.UNDISPATCHED) {
            runHook(resolvedTransition?.presentation, PRESENTATION_PHASE)
        }
    }

    /** 退出の演出と覆いの消滅フェードを並行して走らせ、両方の完了を待つ。 */
    suspend fun runDismissalPhase(): Unit = coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) { runOverlayFade(toVisible = false) }
        launch(start = CoroutineStart.UNDISPATCHED) {
            runHook(resolvedTransition?.dismissal, DISMISSAL_PHASE)
        }
    }

    /**
     * 覆いを出現・消滅させる。
     *
     * 今の濃さから目標の濃さへ動かすので、途中で打ち切られた出現の続きからでも滑らかに消せる。
     */
    private suspend fun runOverlayFade(toVisible: Boolean) {
        val overlay = overlayView ?: return
        val from = overlay.alpha
        val to = if (toVisible) 1f else 0f
        DialogTransitionAnimator.run(resolvedOverlayDuration, OVERLAY_EASING) { progress ->
            overlay.alpha = from + (to - from) * progress
        }
    }

    /**
     * フックを1本走らせる。
     *
     * フックの失敗は演出の失敗であって結果の失敗ではないため、記録だけ残して先へ進む。
     * 完了しないフックはダイアログを撤去させないので、開発中は一定時間で警告を出す。
     */
    private suspend fun runHook(hook: (suspend (View) -> Unit)?, phase: String) {
        if (hook == null) {
            return
        }
        coroutineScope {
            val watchdog = startHookWarningWatchdog(phase)
            try {
                hook(contentView)
            } catch (cancellation: CancellationException) {
                // 脱出口で取り消した分。演出を捨てて先へ進むのが意図した動きなので、そのまま伝える
                throw cancellation
            } catch (failure: Throwable) {
                Log.w(LOG_TAG, "The Dialog ${phase} hook failed.", failure)
            } finally {
                watchdog?.cancel()
            }
        }
    }

    /** 開発中だけ、長すぎるフックに警告を出す見張りを立てる。打ち切りはしない。 */
    private fun CoroutineScope.startHookWarningWatchdog(phase: String): Job? {
        if (!isDebuggableApp()) {
            return null
        }
        return launch {
            delay(HOOK_WARNING_THRESHOLD)
            Log.w(LOG_TAG, "The Dialog ${phase} hook did not complete. Completing the hook is the caller's responsibility.")
        }
    }

    /** 提示先のアプリが開発用のビルドか。警告ログを開発中だけに絞るために読む。 */
    private fun isDebuggableApp(): Boolean {
        val flags = context.applicationInfo?.flags ?: return false
        return (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private companion object {
        /** 器が既定で使う出入りの演出 (core/ADR-0017)。添付がない側のフックがここから決まる。 */
        val DEFAULT_TRANSITION: DialogTransition = DialogTransition.fade()

        /** 覆いのフェードの進み方。中身の演出によらず器の標準で固定する。 */
        val OVERLAY_EASING: Interpolator = AccelerateDecelerateInterpolator()

        /**
         * 開発中にフックの完了忘れを気づけるようにするための警告までの時間。
         * 契約上の上限ではなく、打ち切りもしない。
         */
        val HOOK_WARNING_THRESHOLD: Duration = 5.seconds

        /** 出現の演出を指す記録用の名前。 */
        const val PRESENTATION_PHASE = "presentation"

        /** 退出の演出を指す記録用の名前。 */
        const val DISMISSAL_PHASE = "dismissal"

        /** 演出まわりの不具合を知らせるときの記録先。 */
        const val LOG_TAG = "KsDialogs"
    }
}
