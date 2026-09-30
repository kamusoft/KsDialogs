package jp.kamusoft.ksdialogs

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * 提示先の Activity と、アプリが前面にいるかを Application のライフサイクル通知から追跡し、
 * 提示先の入れ替わり・前面を離れたこと・画面の破棄を購読者へ伝える。
 *
 * - 提示先は、最後に resume した Activity が start の後に一度でも描画されていれば、その Activity とする
 *   (core/ADR-0044)。resumed でも未描画の画面 (起動画面の下にある画面など) に器を載せると、
 *   利用者に見えないまま Toast の表示時間を使い切るため
 * - 前面は、作成済みで、まだ stop も破棄もされていない Activity が 1 つ以上ある状態とする (core/ADR-0043)
 *
 * 追跡の開始はライブラリが自動で行うため、利用者の初期化コードは要らない。
 * 通知は UI スレッドで届き、提示先と前面かどうかは任意のスレッドから読まれる。
 * Activity は弱参照で保持し、追跡が画面の寿命を延ばさないようにする。
 *
 * @param drawObserver 画面の描画の観測。素の JVM のテストは偽の実装を渡す
 * @param turnPoster UI スレッドの次のメッセージ周回へ処理を回す口。背面の確定に使う
 */
internal class ResumedActivityTracker(
    private val drawObserver: ActivityDrawObserver = DecorViewDrawObserver(),
    private val turnPoster: UiThreadTurnPoster = MainLooperTurnPoster(),
) : Application.ActivityLifecycleCallbacks,
    ResumedActivityProvider,
    AppForegroundProvider,
    ResumedActivityChangeObserver,
    ActivityDestroyObserver,
    AttachedHostRetention {

    private val lock = Any()

    /** 最後に resume して、まだ pause・破棄していない Activity。描画済みかどうかは問わない。UI スレッドだけで触る。 */
    private var lastResumedReference: WeakReference<Activity>? = null

    /** 提示先。[lastResumedReference] が描画済みのときだけ持つ。 */
    @Volatile
    private var hostReference: WeakReference<Activity>? = null

    /** Activity ごとの描画の追跡。UI スレッドだけで触る。 */
    private val drawTrackings = WeakHashMap<Activity, DrawTracking>()

    /** 作成済みで、まだ stop も破棄もされていない Activity。UI スレッドだけで触る。 */
    private val foregroundActivities: MutableSet<Activity> =
        Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    /** 破棄の通知を受けた Activity。UI スレッドだけで触る。 */
    private val destroyedActivities: MutableSet<Activity> =
        Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    @Volatile
    private var foreground = false

    private var isInstalled = false

    private val destroyObservations = mutableListOf<DestroyObservation>()

    private val resumedChangeObservations = mutableListOf<() -> Unit>()

    override val resumedActivity: Activity?
        get() = hostReference?.get()

    override val isInForeground: Boolean
        get() = foreground

    override fun retainsAttachment(activity: Activity): Boolean =
        activity !in destroyedActivities && !activity.isDestroyed

    /** Application のライフサイクル通知の購読を始める。二重に呼んでも一度しか購読しない。 */
    fun install(application: Application) {
        synchronized(lock) {
            if (isInstalled) return
            isInstalled = true
        }
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun observeDestroy(activity: Activity, onDestroyed: () -> Unit): ActivityDestroyRegistration {
        val observation = DestroyObservation(WeakReference(activity), onDestroyed)
        synchronized(lock) { destroyObservations.add(observation) }
        return ActivityDestroyRegistration {
            synchronized(lock) { destroyObservations.remove(observation) }
        }
    }

    override fun observeResumedChange(onChanged: () -> Unit): ResumedActivityChangeRegistration {
        synchronized(lock) { resumedChangeObservations.add(onChanged) }
        return ResumedActivityChangeRegistration {
            synchronized(lock) { resumedChangeObservations.remove(onChanged) }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // onCreate の間は未 start だが、画面を開いている途中なので前面に入れる
        enterForeground(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        enterForeground(activity)
        startDrawTracking(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        lastResumedReference = WeakReference(activity)
        val tracking = drawTrackings[activity]
        if (tracking?.isDrawn != true) {
            // start の通知を受けていない画面 (追跡を始める前に start した画面) と、start の時点で decorView が無く
            // 観測を始められなかった画面も、ここから描画を待ち直す。始まっていない観測には描画が届かない
            if (tracking?.registration?.isObserving != true) startDrawTracking(activity)
            // 描画の合図を取りこぼすと提示先が現れないまま Dialog が返らないので、描画を促しておく。
            // 起動画面の延長は描画の直前で止める仕組みなので、促しても延長は破られない
            drawObserver.requestDraw(activity)
        }
        updateHost()
        notifyResumedChange()
    }

    override fun onActivityPaused(activity: Activity) {
        // 描画済みの印は残す。pause だけからの復帰では再描画が起きないことがあり、印を下ろすと提示先が戻らない
        clearIfLastResumed(activity)
        updateHost()
    }

    override fun onActivityStopped(activity: Activity) {
        stopDrawTracking(activity)
        updateHost()
        leaveForeground(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        destroyedActivities.add(activity)
        clearIfLastResumed(activity)
        stopDrawTracking(activity)
        drawTrackings.remove(activity)
        updateHost()
        leaveForeground(activity)
        // 通知はロックの外で行い、購読者が購読解除しても入れ子にならないようにする
        takeObservations(activity).forEach { it.onDestroyed() }
        notifyResumedChange()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle): Unit = Unit

    /** 提示先の入れ替わりと前面を離れたことを購読者へ伝える。通知はロックの外で行う。 */
    private fun notifyResumedChange() {
        synchronized(lock) { resumedChangeObservations.toList() }.forEach { it() }
    }

    /**
     * start の後の最初の描画を待ち始める。
     *
     * 世代を進めてから観測を張り、stop・破棄で世代が進んだ後に遅れて届いた古い描画は印にしない。
     * 古い描画で印を立てると、次の start の後の描画を待たずに提示先になってしまう。
     */
    private fun startDrawTracking(activity: Activity) {
        val tracking = drawTrackings.getOrPut(activity) { DrawTracking() }
        tracking.registration?.cancel()
        tracking.generation += 1
        tracking.isDrawn = false
        val generation = tracking.generation
        val activityReference = WeakReference(activity)
        tracking.registration = drawObserver.observeNextDraw(activity) {
            activityReference.get()?.let { onDrawn(it, generation) }
        }
    }

    /** 描画の追跡をやめ、描画済みの印を下ろす。 */
    private fun stopDrawTracking(activity: Activity) {
        val tracking = drawTrackings[activity] ?: return
        tracking.registration?.cancel()
        tracking.registration = null
        tracking.generation += 1
        tracking.isDrawn = false
    }

    private fun onDrawn(activity: Activity, generation: Int) {
        val tracking = drawTrackings[activity] ?: return
        if (tracking.generation != generation) return
        tracking.isDrawn = true
        tracking.registration = null
        updateHost()
        notifyResumedChange()
    }

    /** 最後に resume した Activity が描画済みなら、それを提示先にする。 */
    private fun updateHost() {
        val activity = lastResumedReference?.get()
        hostReference = if (activity != null && drawTrackings[activity]?.isDrawn == true) {
            WeakReference(activity)
        } else {
            null
        }
    }

    private fun enterForeground(activity: Activity) {
        foregroundActivities.add(activity)
        foreground = true
    }

    /**
     * 前面の Activity から外し、1 つも無くなったら背面の確定を次のメッセージ周回へ回す。
     *
     * 構成の変更による作り直しでは、旧 Activity の stop・破棄と新 Activity の作成が同じメッセージの中で続く。
     * その間を背面とみなすと、前面の待ちの Toast が見えないまま数え始めてしまうので、次の周回で
     * 前面の Activity が無いままのときだけ背面にする。
     */
    private fun leaveForeground(activity: Activity) {
        if (!foregroundActivities.remove(activity) || foregroundActivities.isNotEmpty()) return
        turnPoster.post {
            if (foregroundActivities.isEmpty() && foreground) {
                foreground = false
                notifyResumedChange()
            }
        }
    }

    /** 最後に resume した Activity 自身の通知のときだけ参照を落とす (次の画面の resumed を追い越して消さない)。 */
    private fun clearIfLastResumed(activity: Activity) {
        if (lastResumedReference?.get() === activity) {
            lastResumedReference = null
        }
    }

    /** 破棄された画面の購読を取り出して一覧から外す。参照が切れた購読も同時に片付ける。 */
    private fun takeObservations(activity: Activity): List<DestroyObservation> = synchronized(lock) {
        val matched = destroyObservations.filter { it.activityReference.get() === activity }
        destroyObservations.removeAll { observation ->
            val observedActivity = observation.activityReference.get()
            observedActivity == null || observedActivity === activity
        }
        matched
    }

    /** 画面1つの描画の追跡。 */
    private class DrawTracking {
        /** stop・破棄と start のたびに進む。遅れて届いた古い描画を見分けるために使う。 */
        var generation: Int = 0

        /** start の後に描画されたか。 */
        var isDrawn: Boolean = false

        /** 描画を待っている間の観測。 */
        var registration: ActivityDrawRegistration? = null
    }

    /** 画面1つに対する破棄の購読。 */
    private class DestroyObservation(
        val activityReference: WeakReference<Activity>,
        val onDestroyed: () -> Unit,
    )

    companion object {
        /** ライブラリ全体で共有する追跡役。 */
        val shared: ResumedActivityTracker = ResumedActivityTracker()
    }
}
