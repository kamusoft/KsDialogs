package jp.kamusoft.ksdialogs

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * resumed 状態の Activity を Application のライフサイクル通知から追跡し、画面の破棄を購読者へ伝える。
 *
 * 追跡の開始はライブラリが自動で行うため、利用者の初期化コードは要らない。
 * 通知は UI スレッドで届き、参照は任意のスレッドから読まれる。
 * Activity は弱参照で保持し、追跡が画面の寿命を延ばさないようにする。
 */
internal class ResumedActivityTracker :
    Application.ActivityLifecycleCallbacks,
    ResumedActivityProvider,
    ResumedActivityChangeObserver,
    ActivityDestroyObserver {

    private val lock = Any()

    @Volatile
    private var resumedActivityReference: WeakReference<Activity>? = null

    private var isInstalled = false

    private val destroyObservations = mutableListOf<DestroyObservation>()

    private val resumedChangeObservations = mutableListOf<() -> Unit>()

    override val resumedActivity: Activity?
        get() = resumedActivityReference?.get()

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

    override fun onActivityResumed(activity: Activity) {
        resumedActivityReference = WeakReference(activity)
        notifyResumedChange()
    }

    override fun onActivityPaused(activity: Activity) {
        clearIfTracking(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        clearIfTracking(activity)
        // 通知はロックの外で行い、購読者が購読解除しても入れ子にならないようにする
        takeObservations(activity).forEach { it.onDestroyed() }
        notifyResumedChange()
    }

    /** 提示先の入れ替わりを購読者へ伝える。通知はロックの外で行う。 */
    private fun notifyResumedChange() {
        synchronized(lock) { resumedChangeObservations.toList() }.forEach { it() }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?): Unit = Unit

    override fun onActivityStarted(activity: Activity): Unit = Unit

    override fun onActivityStopped(activity: Activity): Unit = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle): Unit = Unit

    /** 追跡中の Activity 自身の通知のときだけ参照を落とす (次の画面の resumed を追い越して消さない)。 */
    private fun clearIfTracking(activity: Activity) {
        if (resumedActivity === activity) {
            resumedActivityReference = null
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
