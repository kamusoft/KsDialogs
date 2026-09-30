package jp.kamusoft.ksdialogs

import android.app.Activity
import android.view.View
import jp.kamusoft.ksdialogs.support.DialogTestWaiting
import jp.kamusoft.ksdialogs.support.DialogUiThreadTest
import jp.kamusoft.ksdialogs.support.TrackerTestDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 提示先でなくなった画面に載っている Loading・Toast の器を、画面が破棄されるまで付けたままにすることを確かめる。
 *
 * 画面の追跡役・既定の提示面・coordinator を実物でつなぎ、ライフサイクルの通知と描画はテストが送る。
 * 器の出し入れは、通知のたびに載っている器が入れ替わったかどうか (同じ器のままか) で数える。
 */
@DisplayName("提示先でなくなった画面の器の扱い")
class AttachedHostRetentionTests : DialogUiThreadTest() {

    private class RetentionLoadingViewModel : LoadingViewModel

    private class RetentionToastViewModel : ToastViewModel

    /** 描画済みの画面の stop・背面の確定・start・resume・描画を順に送る (ホームへ下がって履歴から戻る)。 */
    private fun TrackerTestDriver.goBackgroundAndReturn(activity: Activity) {
        tracker.onActivityPaused(activity)
        tracker.onActivityStopped(activity)
        turnPoster.runNextTurn()
        check(!tracker.isInForeground) { "背面と確定していない" }
        tracker.onActivityStarted(activity)
        tracker.onActivityResumed(activity)
        drawObserver.draw(activity)
    }

    @Test
    fun `描画済みの画面に載った Loading は、背面へ下がって戻っても外れない`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = LoadingCoordinator(
            LoadingViewRegistry(),
            LoadingSettings(),
            ActivityLoadingPresentationSurface(tracker, tracker, tracker),
        )
        coordinator.registry.register(RetentionLoadingViewModel::class) { context, _ -> View(context) }
        val activity = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Loading(coordinator).show(RetentionLoadingViewModel(), null)
        val attached = requireNotNull(coordinator.presentedContainer) { "Loading が載らない" }

        var detachCount = 0
        withContext(Dispatchers.Main) {
            // coordinator より後に購読するので、各通知で coordinator が器を扱い終えた状態を読める
            tracker.observeResumedChange { if (coordinator.presentedContainer !== attached) detachCount += 1 }
            driver.goBackgroundAndReturn(activity)
        }

        assertEquals(0, detachCount, "背面の間と戻った直後に器が外れている")
        assertSame(attached, coordinator.presentedContainer, "戻った後も同じ器が載っている")
        Loading(coordinator).hide()
    }

    @Test
    fun `描画済みの画面に載った Toast は、背面へ下がって戻っても外れない`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = newToastCoordinator(tracker)
        val activity = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Toast(coordinator).show(RetentionToastViewModel(), LONG_DURATION_MILLIS, null)
        assertTrue(DialogTestWaiting.waitUntilOnMain { coordinator.presentedContainers.size == 1 })
        val attached = withContext(Dispatchers.Main) { coordinator.presentedContainers.single() }

        var detachCount = 0
        withContext(Dispatchers.Main) {
            tracker.observeResumedChange {
                if (coordinator.presentedContainers.singleOrNull() !== attached) detachCount += 1
            }
            driver.goBackgroundAndReturn(activity)
        }

        assertEquals(0, detachCount, "背面の間と戻った直後に器が外れている")
        assertSame(
            attached,
            withContext(Dispatchers.Main) { coordinator.presentedContainers.single() },
            "戻った後も同じ器が載っている",
        )
        withContext(Dispatchers.Main) { coordinator.discardAll() }
    }

    @Test
    fun `画面が破棄されたら Toast の器は外れ、描画された次の画面へ載り直す`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = newToastCoordinator(tracker)
        val previous = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Toast(coordinator).show(RetentionToastViewModel(), LONG_DURATION_MILLIS, null)
        assertTrue(DialogTestWaiting.waitUntilOnMain { coordinator.presentedContainers.size == 1 })
        val attached = withContext(Dispatchers.Main) { coordinator.presentedContainers.single() }

        // 構成の変更による作り直し: 旧画面は破棄され、新しい画面は描画されるまで提示先にならない
        val next = withContext(Dispatchers.Main) {
            tracker.onActivityPaused(previous)
            tracker.onActivityStopped(previous)
            tracker.onActivityDestroyed(previous)
            driver.launch()
        }
        assertFalse(withContext(Dispatchers.Main) { coordinator.isPresenting }, "破棄された画面の器が残っている")

        withContext(Dispatchers.Main) { driver.drawObserver.draw(next) }

        val reattached = withContext(Dispatchers.Main) { coordinator.presentedContainers.singleOrNull() }
        assertNotNull(reattached, "描画された新しい画面へ載り直さない")
        assertNotSame(attached, reattached)
        withContext(Dispatchers.Main) { coordinator.discardAll() }
    }

    @Test
    fun `背面の間に画面が破棄されたら Loading の器は外れる`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = LoadingCoordinator(
            LoadingViewRegistry(),
            LoadingSettings(),
            ActivityLoadingPresentationSurface(tracker, tracker, tracker),
        )
        coordinator.registry.register(RetentionLoadingViewModel::class) { context, _ -> View(context) }
        val activity = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Loading(coordinator).show(RetentionLoadingViewModel(), null)
        assertNotNull(coordinator.presentedContainer)

        withContext(Dispatchers.Main) {
            tracker.onActivityPaused(activity)
            tracker.onActivityStopped(activity)
            driver.turnPoster.runNextTurn()
        }
        assertNotNull(coordinator.presentedContainer, "背面の間は器を付けたまま")

        withContext(Dispatchers.Main) { tracker.onActivityDestroyed(activity) }

        assertEquals(null, coordinator.presentedContainer, "破棄された画面の器が残っている")
        Loading(coordinator).hide()
    }

    @Test
    fun `破棄されていない画面に載った Loading は、描画済みの別の画面が現れた時点でその画面へ移る`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = newLoadingCoordinator(tracker)
        val previous = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Loading(coordinator).show(RetentionLoadingViewModel(), null)
        val attached = requireNotNull(coordinator.presentedContainer) { "Loading が載らない" }

        // 上に別の画面が開く: 前の画面は破棄されず、次の画面は描画されるまで提示先にならない
        val next = withContext(Dispatchers.Main) {
            tracker.onActivityPaused(previous)
            driver.launch()
        }
        assertSame(attached, coordinator.presentedContainer, "次の画面の描画の前は前の画面に付けたまま")

        withContext(Dispatchers.Main) {
            driver.drawObserver.draw(next)
            tracker.onActivityStopped(previous)
        }

        val moved = coordinator.presentedContainer
        assertNotNull(moved, "描画された次の画面へ載らない")
        assertNotSame(attached, moved, "前の画面の器のまま残っている")
        Loading(coordinator).hide()
    }

    @Test
    fun `破棄されていない画面に載った Toast は、描画済みの別の画面が現れた時点でその画面へ移る`() = runBlocking {
        val driver = TrackerTestDriver()
        val tracker = driver.tracker
        val coordinator = newToastCoordinator(tracker)
        val previous = withContext(Dispatchers.Main) { driver.launchAndDraw() }
        Toast(coordinator).show(RetentionToastViewModel(), LONG_DURATION_MILLIS, null)
        assertTrue(DialogTestWaiting.waitUntilOnMain { coordinator.presentedContainers.size == 1 })
        val attached = withContext(Dispatchers.Main) { coordinator.presentedContainers.single() }

        val next = withContext(Dispatchers.Main) {
            tracker.onActivityPaused(previous)
            driver.launch()
        }
        assertSame(
            attached,
            withContext(Dispatchers.Main) { coordinator.presentedContainers.singleOrNull() },
            "次の画面の描画の前は前の画面に付けたまま",
        )

        withContext(Dispatchers.Main) {
            driver.drawObserver.draw(next)
            tracker.onActivityStopped(previous)
        }

        val moved = withContext(Dispatchers.Main) { coordinator.presentedContainers.singleOrNull() }
        assertNotNull(moved, "描画された次の画面へ載らない")
        assertNotSame(attached, moved, "前の画面の器のまま残っている")
        withContext(Dispatchers.Main) { coordinator.discardAll() }
    }

    private fun newLoadingCoordinator(tracker: ResumedActivityTracker): LoadingCoordinator {
        val coordinator = LoadingCoordinator(
            LoadingViewRegistry(),
            LoadingSettings(),
            ActivityLoadingPresentationSurface(tracker, tracker, tracker),
        )
        coordinator.registry.register(RetentionLoadingViewModel::class) { context, _ -> View(context) }
        return coordinator
    }

    private fun newToastCoordinator(tracker: ResumedActivityTracker): ToastCoordinator {
        val coordinator = ToastCoordinator(
            registry = ToastViewRegistry(),
            settings = ToastSettings(),
            presentationSurface = ActivityToastPresentationSurface(tracker, tracker, tracker, tracker),
            announcer = ToastAccessibilityAnnouncer { _, _, _ -> },
            loadingFrontKeeper = ToastLoadingFrontKeeper { },
        )
        coordinator.registry.register(RetentionToastViewModel::class) { context, _ -> View(context) }
        return coordinator
    }

    private companion object {
        /** テストの間に期限が来ない表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 60_000
    }
}
