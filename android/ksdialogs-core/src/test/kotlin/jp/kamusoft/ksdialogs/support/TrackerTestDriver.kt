package jp.kamusoft.ksdialogs.support

import android.app.Activity
import jp.kamusoft.ksdialogs.ActivityDrawObserver
import jp.kamusoft.ksdialogs.ActivityDrawRegistration
import jp.kamusoft.ksdialogs.ResumedActivityTracker
import jp.kamusoft.ksdialogs.UiThreadTurnPoster

/**
 * 画面の描画を手で送る [ActivityDrawObserver]。
 *
 * 素の JVM の `Activity()` はウィンドウを持たず描画が起きないので、描画の合図はテストが送る。
 * 既定の実装と同じく、描画を受けた後の知らせは観測を解除しても届く。
 */
internal class ManualActivityDrawObserver : ActivityDrawObserver {
    private class Observation(val activity: Activity, val onDrawn: () -> Unit)

    private val lock = Any()
    private val observations = mutableListOf<Observation>()
    private val receivedDraws = mutableListOf<Observation>()
    private val drawRequests = mutableListOf<Activity>()
    private val activitiesWithoutDecorView = mutableSetOf<Activity>()
    private var observationStartCount = 0

    /** 描画を促された画面。促された順に並ぶ。 */
    val requestedDraws: List<Activity>
        get() = synchronized(lock) { drawRequests.toList() }

    /** 描画を待っている観測の数。 */
    val activeObservationCount: Int
        get() = synchronized(lock) { observations.size }

    /** 始められた観測の数 (decorView が無く、始められなかった観測は数えない)。 */
    val startedObservationCount: Int
        get() = synchronized(lock) { observationStartCount }

    /**
     * [activity] の decorView がまだ無い状態にする。
     *
     * この間に張られた観測は始まらず、描画を受けない ([ActivityDrawRegistration.isObserving] が false)。
     * 既定の実装で、次の周回で取り直しても decorView が無かった観測に当たる。
     */
    fun withholdDecorView(activity: Activity) {
        synchronized(lock) { activitiesWithoutDecorView.add(activity) }
    }

    /** [activity] の decorView が作られた状態にする。以後に張られた観測は描画を受ける。 */
    fun provideDecorView(activity: Activity) {
        synchronized(lock) { activitiesWithoutDecorView.remove(activity) }
    }

    override fun observeNextDraw(activity: Activity, onDrawn: () -> Unit): ActivityDrawRegistration {
        val observation = Observation(activity, onDrawn)
        val isStarted = synchronized(lock) {
            if (activity in activitiesWithoutDecorView) {
                false
            } else {
                observations.add(observation)
                observationStartCount += 1
                true
            }
        }
        return object : ActivityDrawRegistration {
            @Volatile
            private var isCancelled = false

            override val isObserving: Boolean
                get() = isStarted && !isCancelled

            override fun cancel() {
                isCancelled = true
                synchronized(lock) { observations.remove(observation) }
            }
        }
    }

    override fun requestDraw(activity: Activity) {
        synchronized(lock) { drawRequests.add(activity) }
    }

    /** [activity] が描画され、次のメッセージ周回でその知らせが届いたところまで進める。呼んだスレッドで知らせる。 */
    fun draw(activity: Activity) {
        receiveDraw(activity)
        deliverReceivedDraws()
    }

    /** [activity] が描画されたが、知らせはまだ次のメッセージ周回を待っている状態にする。 */
    fun receiveDraw(activity: Activity) {
        synchronized(lock) {
            val matched = observations.filter { it.activity === activity }
            observations.removeAll(matched)
            receivedDraws.addAll(matched)
        }
    }

    /** 受けたまま待っている描画の知らせを届ける (次のメッセージ周回が来たところ)。呼んだスレッドで知らせる。 */
    fun deliverReceivedDraws() {
        val delivering = synchronized(lock) { receivedDraws.toList().also { receivedDraws.clear() } }
        delivering.forEach { it.onDrawn() }
    }
}

/**
 * 次のメッセージ周回へ回された処理を溜め、テストが周回を進めたときに実行する [UiThreadTurnPoster]。
 */
internal class ManualTurnPoster : UiThreadTurnPoster {
    private val lock = Any()
    private val pending = mutableListOf<() -> Unit>()

    /** 次の周回を待っている処理の数。 */
    val pendingCount: Int
        get() = synchronized(lock) { pending.size }

    override fun post(action: () -> Unit) {
        synchronized(lock) { pending.add(action) }
    }

    /** 次のメッセージ周回へ進め、溜まっていた処理を呼んだスレッドで実行する。 */
    fun runNextTurn() {
        val running = synchronized(lock) { pending.toList().also { pending.clear() } }
        running.forEach { it() }
    }
}

/** 描画と次のメッセージ周回をテストが送る追跡役と、その操作口の組。 */
internal class TrackerTestDriver {
    val drawObserver = ManualActivityDrawObserver()
    val turnPoster = ManualTurnPoster()
    val tracker = ResumedActivityTracker(drawObserver, turnPoster)

    /** 画面を作成から resume まで進め、描画させる。提示先になったところで返る。 */
    fun launchAndDraw(activity: Activity = Activity()): Activity {
        launch(activity)
        drawObserver.draw(activity)
        return activity
    }

    /** 画面を作成から resume まで進める。描画はまだ起きない。 */
    fun launch(activity: Activity = Activity()): Activity {
        tracker.onActivityCreated(activity, null)
        tracker.onActivityStarted(activity)
        tracker.onActivityResumed(activity)
        return activity
    }
}
