package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestGate
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestPresentationSurface
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.LoadingTestViewRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 開始が成立しなかったときに、合流状態・購読・器が残らないことを確かめる。
 *
 * 中身を作る factory は利用者のコードであり、失敗することがある。失敗した開始は
 * 「開始しなかった」状態へ戻さないと、以後の Loading が成立しない世代へ合流してしまう。
 * 呼び出し元が取り消されて身分証を受け取れなかった場合も同じく、終了を数え切る必要がある。
 */
@RunWith(AndroidJUnit4::class)
class LoadingStartFailureTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 中身の生成に失敗した開始は合流も購読も残さない() = runBlocking<Unit> {
        val surface = LoadingTestPresentationSurface(currentActivity())
        val harness = LoadingTestHarness(surface)
        val actionRuns = AtomicInteger(0)
        val factoryCalls = AtomicInteger(0)
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            factoryCalls.incrementAndGet()
            throw IllegalStateException("中身を作れない")
        }

        val failure = runCatching {
            harness.loading.start(LoadingTestViewModel()) { _ -> actionRuns.incrementAndGet() }
        }.exceptionOrNull()

        assertTrue("factory の失敗がそのまま伝わる", failure is IllegalStateException)
        assertEquals("factory は呼ばれている", 1, factoryCalls.get())
        assertEquals("action は実行されない", 0, actionRuns.get())
        assertEquals("合流は残らない", 0, harness.coalescedUseCount)
        assertFalse("表示は成立しない", harness.isPresenting)
        assertNull("中身も残らない", harness.contentView)
        assertEquals("提示先の購読も残らない", 0, surface.observerCount)

        // 後続の開始は失敗した世代へ合流せず、新しい表示として成立する
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            newContentView().also(viewRecorder::record)
        }
        harness.loading.show(LoadingTestViewModel())

        assertTrue("後続の表示は成立する", harness.isPresenting)
        assertEquals(1, harness.coalescedUseCount)
        assertSame(viewRecorder.lastView, harness.contentView)

        harness.loading.hide()
    }

    @Test
    fun 提示先の復帰時の生成失敗は呼び出し元へ投げ返さない() = runBlocking<Unit> {
        // 提示先が不在のまま開始し、中身の生成が提示先の入れ替わりまで遅れる状況を作る
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        val factoryCalls = AtomicInteger(0)
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            factoryCalls.incrementAndGet()
            throw IllegalStateException("中身を作れない")
        }
        val gate = LoadingTestGate()

        coroutineScope {
            val scope = async {
                harness.loading.start(LoadingTestViewModel()) { _ -> gate.await() }
            }
            assertTrue(
                "提示先が無くても合流は成立する",
                InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 1 },
            )
            assertEquals("この時点では中身を作らない", 0, factoryCalls.get())

            // 画面の入れ替わりの通知は UI スレッドで届く。ここで生成の失敗が起きる
            withContext(Dispatchers.Main) { surface.changeHost(currentActivity()) }

            assertEquals("入れ替わりで中身の生成が試みられる", 1, factoryCalls.get())
            assertFalse("生成に失敗した表示は成立しない", harness.isPresenting)
            assertNull(harness.contentView)
            assertEquals("走行中の処理の合流は保たれる", 1, harness.coalescedUseCount)

            // 失敗する生成は繰り返さない
            withContext(Dispatchers.Main) { surface.changeHost(currentActivity()) }
            assertEquals("次の入れ替わりでは作り直さない", 1, factoryCalls.get())

            gate.open()
            scope.await()
        }

        assertEquals("処理の完了で合流は数え切られる", 0, harness.coalescedUseCount)
        assertFalse(harness.isPresenting)
        assertEquals("提示先の購読も残らない", 0, surface.observerCount)
    }

    @Test
    fun 提示先の復帰時の致命的な失敗は握り潰さずに通知元へ伝わる() = runBlocking<Unit> {
        // 表示を諦めて処理を続けるのは中身の作り手が投げる通常の失敗までで、
        // 実行の継続そのものが成り立たない失敗まで隠すと、原因が見えなくなる
        val surface = LoadingTestPresentationSurface(null)
        val harness = LoadingTestHarness(surface)
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            throw FatalContentFailure()
        }
        val gate = LoadingTestGate()

        coroutineScope {
            val scope = async {
                harness.loading.start(LoadingTestViewModel()) { _ -> gate.await() }
            }
            assertTrue(
                "提示先が無くても合流は成立する",
                InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 1 },
            )

            val notified = runCatching {
                withContext(Dispatchers.Main) { surface.changeHost(currentActivity()) }
            }.exceptionOrNull()

            assertTrue("致命的な失敗は通知元へ伝わる", notified is FatalContentFailure)
            assertFalse("表示は成立しない", harness.isPresenting)
            assertEquals("走行中の処理の合流は保たれる", 1, harness.coalescedUseCount)

            gate.open()
            scope.await()
        }

        assertEquals("処理の完了で合流は数え切られる", 0, harness.coalescedUseCount)
        assertEquals("提示先の購読も残らない", 0, surface.observerCount)
    }

    @Test
    fun 受理直後に取り消された開始は合流も器も残さない() = runBlocking<Unit> {
        val callerJob = AtomicReference<Job?>(null)
        // 合流の受理が済んだ直後 (提示先を読む瞬間) に呼び出し元を取り消し、
        // 身分証を受け取れないまま開始だけが確定する競合を決定的に起こす
        val surface = HostReadHookSurface(currentActivity()) { callerJob.get()?.cancel() }
        val harness = LoadingTestHarness(surface)
        val actionRuns = AtomicInteger(0)
        val callerDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        try {
            val job = CoroutineScope(callerDispatcher).launch(start = CoroutineStart.LAZY) {
                harness.loading.start("待機中") { _ -> actionRuns.incrementAndGet() }
            }
            callerJob.set(job)
            job.start()
            job.join()

            assertTrue("呼び出し元は取り消される", job.isCancelled)
            assertEquals("action は実行されない", 0, actionRuns.get())
            assertEquals("合流は残らない", 0, harness.coalescedUseCount)
            assertFalse("器は撤去済み", harness.isPresenting)
            assertNull(harness.container)
        } finally {
            callerDispatcher.close()
            harness.tearDown()
        }
    }

    /** 共通ケース表と同じ内容サイズを持つ、カスタム Loading の中身。 */
    private fun Context.newContentView(): FixedContentSizeView =
        FixedContentSizeView(this, CONTENT_SIZE_PIXELS, CONTENT_SIZE_PIXELS)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    /**
     * 提示先を読んだ瞬間に指定の処理を差し込む面。
     *
     * 提示先を読むのは合流の受理が済んだ後の器の組み立てなので、
     * 「状態は確定したが呼び出し元へ結果が渡っていない」時点をここで捕まえられる。
     */
    private class HostReadHookSurface(
        private val host: Context,
        private val onHostRead: () -> Unit,
    ) : LoadingPresentationSurface {

        override val hostContext: Context?
            get() {
                onHostRead()
                return host
            }

        override fun observeHostChange(onHostChanged: () -> Unit): LoadingHostRegistration =
            LoadingHostRegistration { }
    }

    /**
     * 実行の継続そのものが成り立たない失敗の代役。
     *
     * メモリ枯渇のような本物の致命的な失敗をテストで起こすことはできないため、
     * 同じ扱いを受ける種類 (Error) の失敗をここで作る。
     */
    private class FatalContentFailure : Error("中身を作れない (致命的)")

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_SIZE_PIXELS = 200
    }
}
