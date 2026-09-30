package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestGate
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestPresentationSurface
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.LoadingTestViewRecorder
import jp.kamusoft.ksdialogs.support.ProgressReceivingLoadingTestViewModel
import jp.kamusoft.ksdialogs.support.UnregisteredLoadingTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 提示先が無いまま始まった Loading の、その後を確かめる。iOS Native の LoadingHostWaitTests のミラー。
 *
 * 処理は通常どおり実行したまま提示先の出現を待ち、現れた時点で表示が続いていれば中身を作って
 * 入りのフックから表示する。表示が先に終われば何も表示しない。
 */
@RunWith(AndroidJUnit4::class)
class LoadingHostWaitTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_HW_01_提示先が現れた時点で処理中なら_入りのフックを経て表示される() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val probe = DialogTransitionProbe()
        val viewRecorder = LoadingTestViewRecorder()
        val gate = LoadingTestGate()

        coroutineScope {
            val scope = async {
                harness.loading.start(LoadingTestViewModel(), factory = { _ ->
                    newContentView().apply {
                        ksDialogTransition = DialogTransition(
                            presentation = probe.immediateHook(DialogTransitionProbe.Phase.PRESENTATION),
                            dismissal = probe.immediateHook(DialogTransitionProbe.Phase.DISMISSAL),
                        )
                    }.also(viewRecorder::record)
                }) { _ ->
                    gate.await()
                    42
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 1 })
            assertEquals("提示先が無い間は中身を作らない", 0, viewRecorder.createdCount)
            assertFalse(harness.isPresenting)

            makeHostAppear(surface)

            assertEquals("提示先が現れた時点で中身が作られる", 1, viewRecorder.createdCount)
            assertTrue("表示される", harness.waitUntilPresenting())
            assertSame(viewRecorder.lastView, harness.contentView)
            assertTrue(
                "入りのフックを経て表示される",
                InstrumentedDialogWaiting.waitUntil {
                    probe.callCount(DialogTransitionProbe.Phase.PRESENTATION) == 1
                },
            )

            gate.open()
            assertEquals("スコープ形は action の戻り値を返す", 42, scope.await())
        }
        assertFalse("action が終わると閉じる", harness.isPresenting)
        assertEquals(0, surface.observerCount)
    }

    @Test
    fun LD_HW_02_提示先が現れる前に処理が終われば_何も表示されない() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val viewRecorder = LoadingTestViewRecorder()

        val value = harness.loading.start(LoadingTestViewModel(), factory = { _ ->
            newContentView().also(viewRecorder::record)
        }) { _ -> 7 }
        assertEquals("スコープ形は action の戻り値を返す", 7, value)
        assertEquals("待ちもやめている", 0, surface.observerCount)

        makeHostAppear(surface)

        assertEquals("中身は作られない", 0, viewRecorder.createdCount)
        assertFalse("表示されない", harness.isPresenting)
        assertNull(harness.container)
    }

    @Test
    fun LD_HW_03_提示先が現れる前に_hide_されれば_何も表示されない() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val viewRecorder = LoadingTestViewRecorder()

        harness.loading.show(LoadingTestViewModel()) { _ -> newContentView().also(viewRecorder::record) }
        assertEquals("提示先の出現を待っている", 1, surface.observerCount)

        harness.loading.hide()
        makeHostAppear(surface)

        assertEquals("中身は作られない", 0, viewRecorder.createdCount)
        assertFalse("表示されない", harness.isPresenting)
        assertNull(harness.container)
        assertEquals("待ちの購読は解除される", 0, surface.observerCount)
    }

    @Test
    fun LD_HW_04_提示先が現れた時点の中身の生成に失敗すると_表示だけを諦めて処理は続く() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val gate = LoadingTestGate()
        val actionCompletions = AtomicInteger(0)
        // 警告ログの中から、この回の失敗を見分けるための印
        val marker = "LD-HW-04-${UUID.randomUUID()}"
        clearKsDialogsLog()

        coroutineScope {
            val scope = async {
                harness.loading.start(
                    LoadingTestViewModel(),
                    factory = { _ -> throw IllegalStateException(marker) },
                ) { _ ->
                    gate.await()
                    actionCompletions.incrementAndGet()
                    5
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 1 })

            makeHostAppear(surface)

            assertTrue(
                "警告ログが残る",
                InstrumentedDialogWaiting.waitUntil { ksDialogsLog().contains(marker) },
            )
            assertTrue(
                "警告の本文",
                ksDialogsLog().contains("Could not create the Loading content. Nothing is presented."),
            )
            assertFalse("Loading は表示されない", harness.isPresenting)
            assertNull(harness.container)
            assertEquals("合流状態は残る", 1, harness.coalescedUseCount)

            gate.open()
            assertEquals("action はそのまま続き、失敗は返らない", 5, scope.await())
        }
        assertEquals(1, actionCompletions.get())
        assertEquals(0, harness.coalescedUseCount)
    }

    @Test
    fun LD_HW_05_表示の前に報告した進捗も_VM_の受け口へ届く() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(ProgressReceivingLoadingTestViewModel::class) { _ ->
            newContentView().also(viewRecorder::record)
        }
        val viewModel = ProgressReceivingLoadingTestViewModel()
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start(viewModel) { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            report.get()(0.25)

            assertTrue(
                "表示の前の進捗が VM の受け口へ届く",
                InstrumentedDialogWaiting.waitUntil { viewModel.receivedProgress == listOf(0.25) },
            )
            assertFalse("まだ表示されていない", harness.isPresenting)
            assertEquals(0, viewRecorder.createdCount)

            gate.open()
            scope.await()
        }
    }

    @Test
    fun LD_HW_06_開始時点で提示先があれば_中身の生成の失敗は今までどおり開始の失敗になる() = runBlocking<Unit> {
        val harness = LoadingTestHarness(currentActivity())
        val actionRuns = AtomicInteger(0)
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            throw IllegalStateException("中身を作れない")
        }

        val failure = runCatching {
            harness.loading.start(LoadingTestViewModel()) { _ -> actionRuns.incrementAndGet() }
        }.exceptionOrNull()

        assertTrue("開始が失敗として返る", failure is IllegalStateException)
        assertEquals("action は実行されない", 0, actionRuns.get())
        assertEquals(0, harness.coalescedUseCount)
        assertFalse(harness.isPresenting)
    }

    @Test
    fun LD_HW_07_提示先が無くても_未登録のカスタム_Loading_は開始の時点で失敗する() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val actionRuns = AtomicInteger(0)

        val failure = runCatching {
            harness.loading.start(UnregisteredLoadingTestViewModel()) { _ -> actionRuns.incrementAndGet() }
        }.exceptionOrNull()

        assertTrue("構成ミスとして失敗する", failure is DialogException.ViewFactoryNotRegistered)
        assertEquals("action は実行されない", 0, actionRuns.get())
        assertEquals("合流も始まらない", 0, harness.coalescedUseCount)
        assertEquals("待ちも始まらない", 0, surface.observerCount)
    }

    /** 提示先を用意して、入れ替わりの通知を UI スレッドで送る。 */
    private suspend fun makeHostAppear(surface: LoadingTestPresentationSurface) {
        val activity = currentActivity()
        withContext(Dispatchers.Main) { surface.changeHost(activity) }
    }

    /** 共通ケース表と同じ内容サイズを持つ、カスタム Loading の中身。 */
    private fun Context.newContentView(): FixedContentSizeView =
        FixedContentSizeView(this, CONTENT_SIZE_PIXELS, CONTENT_SIZE_PIXELS)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    /** このプロセスのログのうち、ライブラリの警告以上を読む。 */
    private fun ksDialogsLog(): String {
        val process = ProcessBuilder("logcat", "-d", "-s", "KsDialogs:W").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        return output
    }

    /** 前の検査の警告と取り違えないよう、読み取り済みのログを捨てる。失敗しても印で見分けられる。 */
    private fun clearKsDialogsLog() {
        runCatching { ProcessBuilder("logcat", "-c").start().waitFor() }
    }

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_SIZE_PIXELS = 200
    }
}
