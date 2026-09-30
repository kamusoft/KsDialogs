package jp.kamusoft.ksdialogs

import android.content.Context
import android.os.SystemClock
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.FirstDrawBlockingTestActivity
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.ToastTestAnnouncer
import jp.kamusoft.ksdialogs.support.ToastTestHarness.Companion.readOnMain as readToastOnMain
import jp.kamusoft.ksdialogs.support.ToastTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * 提示先は resumed で、かつ描画された Activity であることを、実物の描画で確かめる。
 *
 * 最初の描画を描画の直前で止めた画面 (起動画面の延長と同じ仕組み) を開き、Dialog・Loading・Toast を
 * 既定の提示面 (画面の追跡役を読む面) で出す。前面の判定も同じ追跡役から読むので、描画を止めた画面は
 * 前面の待ちになる。
 */
@RunWith(AndroidJUnit4::class)
class DrawnHostPresentationTests {

    @Test
    fun PB_HA_04_resumed_だが未描画の_Activity_には_Dialog_Loading_Toast_のどれも載らず_描画の後に載る() =
        runBlocking<Unit> {
            val activity = FirstDrawBlockingTestActivity.launch()
            try {
                // Dialog: 提示先を待つ列はこのテスト専用にし、他のテストの show と干渉させない
                val dialogRegistry = DialogViewRegistry()
                val dialogContexts = CopyOnWriteArrayList<Context>()
                val dialogNotifier = AtomicReference<DialogNotifier<Boolean>>()
                dialogRegistry.register(PlainTestDialogViewModel::class) { _, notifier ->
                    dialogNotifier.set(notifier)
                    dialogContexts.add(this)
                    View(this)
                }
                val dialogs = Dialog(
                    dialogRegistry,
                    ActivityDialogPresentationSurface(hostWaitQueue = DialogHostWaitQueue()),
                )

                val loadingHarness = LoadingTestHarness(ActivityLoadingPresentationSurface())

                val toastCoordinator = ToastCoordinator(
                    registry = ToastViewRegistry(),
                    settings = ToastSettings(),
                    presentationSurface = ActivityToastPresentationSurface(),
                    announcer = ToastTestAnnouncer(),
                    loadingFrontKeeper = ToastLoadingFrontKeeper { },
                )
                val toast = Toast(toastCoordinator)

                coroutineScope {
                    val showDialog = async { dialogs.show(PlainTestDialogViewModel("PB-HA-04")) }
                    loadingHarness.loading.show("PB-HA-04")
                    toast.show("PB-HA-04", TOAST_DURATION_MILLIS)

                    // 描画が止まっている間は、どれも載らないことを一定時間見届ける
                    delay(BLOCKED_OBSERVATION_MILLIS)
                    assertEquals("描画は止まっている", 0, activity.drawCount)
                    assertTrue("Dialog の中身は作られない", dialogContexts.isEmpty())
                    assertFalse("Loading は載らない", loadingHarness.isPresenting)
                    assertFalse("Toast は載らない", readToastOnMain { toastCoordinator.isPresenting })
                    assertEquals("Toast は提示先を待っている", 1, readToastOnMain { toastCoordinator.displayCount })

                    withContext(Dispatchers.Main) { activity.releaseDraw() }

                    assertTrue(
                        "最初の描画の後に Dialog が載る",
                        InstrumentedDialogWaiting.waitUntil { dialogContexts.size == 1 },
                    )
                    assertSame("描画された画面に載る", activity, dialogContexts.single())
                    assertTrue("最初の描画の後に Loading が載る", loadingHarness.waitUntilPresenting())
                    assertTrue(
                        "最初の描画の後に Toast が載る",
                        InstrumentedDialogWaiting.waitUntil { readToastOnMain { toastCoordinator.isPresenting } },
                    )
                    assertTrue("描画は起きている", activity.drawCount > 0)

                    // 表示を残さずに片付ける
                    dialogNotifier.get().complete(true)
                    withTimeout(CLEANUP_TIMEOUT_MILLIS) { showDialog.await() }
                    loadingHarness.tearDown()
                    assertTrue(
                        "Toast が表示時間で消える",
                        InstrumentedDialogWaiting.waitUntil(CLEANUP_TIMEOUT_MILLIS) {
                            readToastOnMain { toastCoordinator.displayCount } == 0
                        },
                    )
                }
            } finally {
                activity.finishAndWait()
            }
        }

    @Test
    fun PB_HA_11_作り直しの間は背面と判定されず_前面の待ちの_Toast_は新しい画面に載った時点から数え始める() =
        runBlocking<Unit> {
            var activity = FirstDrawBlockingTestActivity.launch()
            try {
                val toastCoordinator = ToastCoordinator(
                    registry = ToastViewRegistry(),
                    settings = ToastSettings(),
                    presentationSurface = ActivityToastPresentationSurface(),
                    announcer = ToastTestAnnouncer(),
                    loadingFrontKeeper = ToastLoadingFrontKeeper { },
                )
                val toast = Toast(toastCoordinator)

                // 描画の前に受理するので、前面の待ちになる
                toast.show(ToastTestViewModel("PB-HA-11"), RECREATION_TOAST_DURATION_MILLIS) { _ ->
                    View(this).apply {
                        // 撤去が期限の到達の直後に終わるよう、出入りの演出を待たない
                        ksDialogTransition = DialogTransition(presentation = {}, dismissal = {})
                    }
                }
                assertTrue(
                    "提示先を待っている",
                    InstrumentedDialogWaiting.waitUntil { readToastOnMain { toastCoordinator.displayCount } == 1 },
                )

                // 構成の変更による作り直し。新しい画面も描画を止めたまま、duration より長く置く
                activity = activity.recreateAndWait()
                delay(RECREATION_TOAST_DURATION_MILLIS + OVER_DURATION_WAIT_MILLIS)
                assertTrue("前面と判定されている", ResumedActivityTracker.shared.isInForeground)
                assertEquals(
                    "作り直しの間に期限が決まっていれば、ここまでに破棄されている",
                    1,
                    readToastOnMain { toastCoordinator.displayCount },
                )
                assertFalse("描画の前は載らない", readToastOnMain { toastCoordinator.isPresenting })

                val releasedAt = SystemClock.uptimeMillis()
                val recreated = activity
                withContext(Dispatchers.Main) { recreated.releaseDraw() }

                val presented = InstrumentedDialogWaiting.waitUntil { readToastOnMain { toastCoordinator.isPresenting } }
                assertTrue(
                    "新しい画面の描画の後に Toast が載る (描画 ${recreated.drawCount} 回・" +
                        "提示先は新しい画面か ${ResumedActivityTracker.shared.resumedActivity === recreated}・" +
                        "表示 ${readToastOnMain { toastCoordinator.displayCount }} 枚)",
                    presented,
                )
                assertTrue(
                    InstrumentedDialogWaiting.waitUntil(CLEANUP_TIMEOUT_MILLIS) {
                        readToastOnMain { toastCoordinator.displayCount } == 0
                    },
                )
                val elapsed = SystemClock.uptimeMillis() - releasedAt
                assertTrue(
                    "載った時点から数えた duration の到達で消える (描画の停止を解いてから $elapsed ms)",
                    elapsed >= RECREATION_TOAST_DURATION_MILLIS &&
                        elapsed < RECREATION_TOAST_DURATION_MILLIS + DEADLINE_ALLOWANCE_MILLIS,
                )
            } finally {
                activity.finishAndWait()
            }
        }

    private companion object {
        /** 描画を止めたまま、どれも載らないことを見届ける時間。 */
        const val BLOCKED_OBSERVATION_MILLIS = 800L

        /**
         * Toast の表示時間。描画を止めている間に使い切らず、片付けで待ちすぎない長さにする。
         */
        const val TOAST_DURATION_MILLIS = 4_000

        const val CLEANUP_TIMEOUT_MILLIS = 10_000L

        /** 作り直しをまたいで前面の待ちにする Toast の表示時間。 */
        const val RECREATION_TOAST_DURATION_MILLIS = 1_500

        /** duration を明らかに超えたと言えるまで余分に置く時間。 */
        const val OVER_DURATION_WAIT_MILLIS = 1_000L

        /** 期限の到達から表示リストが空になるまでの遅れとして許す幅。 */
        const val DEADLINE_ALLOWANCE_MILLIS = 1_000L
    }
}
