package jp.kamusoft.ksdialogs

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver

/**
 * Activity の画面の描画を観測する。
 *
 * 提示先は resumed で、かつ描画された Activity なので (core/ADR-0044)、追跡役はこの口で「描画された」を知る。
 * 描画の仕組みを差し替えられるようにするための内部の継ぎ目で、素の JVM のテストは偽の実装で描画を送る。
 * すべて UI スレッドから呼ぶ。
 */
internal interface ActivityDrawObserver {
    /**
     * [activity] の画面の次の描画を 1 回だけ観測する。
     *
     * 描画を受けたら、次のメッセージ周回で [onDrawn] を UI スレッドで呼ぶ。描画を受けた後に購読を解除しても、
     * その知らせは届きうる。古い描画を印にしないかどうかは、呼び出し側が判断する。
     */
    fun observeNextDraw(activity: Activity, onDrawn: () -> Unit): ActivityDrawRegistration

    /** [activity] の画面に描画を促す。描画の直前で描画が止められていれば、促しても描画は起きない。 */
    fun requestDraw(activity: Activity)
}

/** 描画の観測 1 件。 */
internal interface ActivityDrawRegistration {
    /**
     * 描画を受けられる状態か。
     *
     * 画面の decorView がまだ作られておらず、観測を始められていない間は false。
     * false のまま残った観測には描画が届かないので、呼び出し側は観測を張り直す。
     */
    val isObserving: Boolean

    /** 観測をやめる。まだ受けていない描画は、以後は知らせない。 */
    fun cancel()
}

/**
 * UI スレッドの次のメッセージ周回へ処理を回す。
 *
 * 同じメッセージの中で続くライフサイクル通知 (構成の変更による作り直しなど) を、ひとまとまりとして見るために使う。
 */
internal fun interface UiThreadTurnPoster {
    /** [action] を UI スレッドの次のメッセージ周回で実行する。 */
    fun post(action: () -> Unit)
}

/** メインスレッドのメッセージキューへ積む既定の [UiThreadTurnPoster]。 */
internal class MainLooperTurnPoster : UiThreadTurnPoster {
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun post(action: () -> Unit) {
        handler.post(action)
    }
}

/**
 * decorView の実際の描画を観測する既定の [ActivityDrawObserver]。
 *
 * - 描画の合図は [ViewTreeObserver.OnDrawListener]。描画の直前 (PreDraw) で描画が止められている間
 *   (起動画面の延長など) は来ないので、止められている間を描画に数えない
 * - listener は描画の最中には外せないため、外すのと知らせるのは次のメッセージ周回で行う
 * - ウィンドウに付く前の ViewTreeObserver は仮のもので、古い OS では付いたときに OnDrawListener が
 *   引き継がれない。付く前なら、付いた時点で張る
 * - decorView が無い画面は次の周回で取り直す。取り直して張ったときは、その前に最初の描画が済んでいても
 *   後続の描画に頼らずに済むよう、描画を促す
 *
 * @param decorViewOf 画面の decorView を、作らずに読む口。作られていなければ null
 */
internal class DecorViewDrawObserver(
    private val turnPoster: UiThreadTurnPoster = MainLooperTurnPoster(),
    private val decorViewOf: (Activity) -> View? = { it.window?.peekDecorView() },
) : ActivityDrawObserver {

    override fun observeNextDraw(activity: Activity, onDrawn: () -> Unit): ActivityDrawRegistration {
        val observation = DrawObservation(turnPoster, onDrawn)
        val decorView = decorViewOf(activity)
        if (decorView != null) {
            observation.start(decorView)
        } else {
            // 中身を置かずに開いた画面は、resume の直後にフレームワークが decorView を作るので、次の周回で取り直す。
            // その間に最初の描画が済んでいることがあるので、張った後に描画を促す
            turnPoster.post {
                decorViewOf(activity)?.let { view ->
                    observation.start(view)
                    if (observation.isObserving) view.invalidate()
                }
            }
        }
        return observation
    }

    override fun requestDraw(activity: Activity) {
        decorViewOf(activity)?.invalidate()
    }

    /** decorView 1 つに対する、次の描画の観測。UI スレッドだけで触る。 */
    private class DrawObservation(
        private val turnPoster: UiThreadTurnPoster,
        private val onDrawn: () -> Unit,
    ) : ViewTreeObserver.OnDrawListener, View.OnAttachStateChangeListener, ActivityDrawRegistration {

        private var decorView: View? = null
        private var treeObserver: ViewTreeObserver? = null
        private var isCancelled = false
        private var hasReceivedDraw = false

        /** decorView を得て観測を始めたか。ウィンドウに付くのを待っている間も含む。 */
        override val isObserving: Boolean
            get() = !isCancelled && decorView != null

        fun start(view: View) {
            if (isCancelled || decorView != null) return
            decorView = view
            if (view.isAttachedToWindow) {
                addDrawListener(view)
            } else {
                view.addOnAttachStateChangeListener(this)
            }
        }

        override fun cancel() {
            isCancelled = true
            decorView?.removeOnAttachStateChangeListener(this)
            removeDrawListener()
        }

        override fun onViewAttachedToWindow(view: View) {
            view.removeOnAttachStateChangeListener(this)
            if (!isCancelled) addDrawListener(view)
        }

        override fun onViewDetachedFromWindow(view: View): Unit = Unit

        override fun onDraw() {
            if (hasReceivedDraw) return
            hasReceivedDraw = true
            turnPoster.post {
                removeDrawListener()
                onDrawn()
            }
        }

        private fun addDrawListener(view: View) {
            val observer = view.viewTreeObserver
            observer.addOnDrawListener(this)
            treeObserver = observer
        }

        private fun removeDrawListener() {
            // ウィンドウから外れた後の ViewTreeObserver は使えず、張った listener もそれと一緒に消えている
            treeObserver?.takeIf { it.isAlive }?.removeOnDrawListener(this)
            treeObserver = null
        }
    }
}
